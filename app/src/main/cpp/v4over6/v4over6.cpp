#include <errno.h>
#include <netinet/in.h>
#include <pthread.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>
#include <linux/ip.h>
#include <net/if.h>
#include <stdint.h>
#include <string.h>
#include <arpa/inet.h>
#include <strings.h>
#include <netdb.h>

#define TAG "lib4over6"

#include "msg.h"
#include "v4over6.h"
#include "log.h"

namespace v4over6 {
    static Ipv4Config config;
    static Statistics stats;
    static ServerConfig server_config;

    static int socket_fd = -1, tunnel_fd = -1;

    static pthread_cond_t config_cond = PTHREAD_COND_INITIALIZER;
    static pthread_mutex_t config_mutex = PTHREAD_MUTEX_INITIALIZER;

    static volatile int received_configuration = 0;
    static pthread_t receive_pid = -1, timer_pid = -1, forward_pid = -1;
    static time_t last_heartbeat_recv = -1, last_heartbeat_send = -1;

    static ssize_t read_exact(int fd, uint8_t *buf, size_t count) {
        uint8_t *cur = buf;
        uint8_t *end = buf + count;
        while (cur < end) {
            ssize_t read_bytes = read(fd, cur, end - cur);
            if (read_bytes < 0) {
                if (read_bytes == -1 && errno == EAGAIN) {
                    continue;
                }
                return -1;
            }
            cur += read_bytes;
        }
        return count;
    }

    static void signal_handler(int signal) {
        if (signal == SIGUSR2) {
            pthread_exit(EXIT_SUCCESS);
        }
    }

    // forward packets from socket to tunnel
    static void *receive_thread(void *args) {
        LOGI("Receive thread started");
        signal(SIGUSR2, signal_handler);

        while (true) {
            Msg msg;
            ssize_t read_bytes = read_exact(socket_fd, (uint8_t *) &msg.header, sizeof(MsgHeader));
            if (read_bytes < 0) {
                LOGE("Error reading from socket: %s", strerror(errno));
                continue;
            }

            size_t data_len = msg.header.length - sizeof(MsgHeader);
            if (data_len < 0 || data_len > MSG_DATA_SIZE) {
                continue;
            }

            read_bytes = read_exact(socket_fd, (uint8_t *) msg.data, data_len);
            if (read_bytes < 0) {
                LOGE("Error reading from socket: %s", strerror(errno));
                continue;
            }

            if (msg.header.type == MSG_TYPE_RESPONSE) {
                write(tunnel_fd, msg.data, data_len);
            } else if (msg.header.type == MSG_TYPE_IP_RESPONSE) {
                char buffer[MSG_DATA_SIZE];
                memcpy(buffer, msg.data, data_len);
                buffer[data_len] = '\0';
                sscanf(buffer, "%s %s %s %s %s",
                       config.ip, config.route, config.dns1, config.dns2, config.dns3);
                // wake up sleeping thread
                pthread_mutex_lock(&config_mutex);
                received_configuration = 1;
                pthread_cond_signal(&config_cond);
                pthread_mutex_unlock(&config_mutex);
            } else if (msg.header.type == MSG_TYPE_HEARTBEAT) {
                LOGI("Heartbeat received");
                last_heartbeat_recv = time(NULL);
            } else {
                LOGE("Invalid message: type %d, length %d", msg.header.type, msg.header.length);
            }

            stats.in_packets++;
            stats.in_bytes += msg.header.length;
        }
        return NULL;
    }

    // handle heartbeat
    static void *timer_thread(void *args) {
        LOGI("Timer thread started");
        signal(SIGUSR2, signal_handler);
        last_heartbeat_recv = time(NULL);

        while (true) {
            time_t current_time = time(NULL);
            if (last_heartbeat_recv != -1 && current_time - last_heartbeat_recv > 60) {
                // close connection
                LOGE("Server heartbeat timeout");
                timer_pid = -1; // prevent itself to be killed
                disconnect_socket();
                pthread_exit(EXIT_SUCCESS);
            }
            if (last_heartbeat_send == -1 ||
                current_time - last_heartbeat_send >= 20) {
                // send heartbeat
                MsgHeader heartbeat = {
                        .length = sizeof(MsgHeader),
                        .type = MSG_TYPE_HEARTBEAT
                };
                if (write(socket_fd, &heartbeat, heartbeat.length) < 0) {
                    LOGE("Failed to send heartbeat: %s", strerror(errno));
                } else {
                    LOGI("Heartbeat sent");
                    last_heartbeat_send = current_time;
                    stats.out_packets++;
                    stats.out_bytes += sizeof(MsgHeader);
                }
            }
            sleep(1);
        }
        return NULL;
    }

    // forward packets from tunnel to socket
    static void *forward_thread(void *args) {
        LOGI("Forward thread started");
        signal(SIGUSR2, signal_handler);
        uint8_t buffer[MSG_DATA_SIZE];

        while (true) {
            ssize_t read_bytes = read(tunnel_fd, buffer, sizeof(buffer));
            if (read_bytes < 0) {
                continue;
            }
            struct iphdr *hdr = (struct iphdr *) buffer;
            if (hdr->version != 4) {
                LOGE("Received an IPv6 packet from tunnel");
                continue;
            }
            uint16_t tot_len = ntohs(hdr->tot_len);
            uint16_t header_len = hdr->ihl * 4;
            if (!(header_len <= tot_len && read_bytes == tot_len)) {
                LOGE("Received a broken IPv4 packet from tunnel");
                continue;
            }

            Msg msg;
            msg.header.type = MSG_TYPE_REQUEST;
            msg.header.length = tot_len + sizeof(MsgHeader);
            memcpy(msg.data, buffer, tot_len);
            if (write(socket_fd, &msg, msg.header.length) < 0) {
                LOGE("Error writing to socket: %s", strerror(errno));
            } else {
                stats.out_packets++;
                stats.out_bytes += msg.header.length;
            }
        }
        return NULL;
    }

    int connect_socket(const char *addr_s, int port) {
        size_t addr_len = strlen(addr_s);
        if (addr_len > sizeof(server_config.ipv6)) {
            LOGE("Error creating socket: Invalid address");
            return -1;
        }
        memcpy(server_config.ipv6, addr_s, addr_len);
        server_config.port = port;

        LOGI("Connecting to server [%s]:%d", addr_s, port);
        struct sockaddr_in6 addr;
        socket_fd = socket(AF_INET6, SOCK_STREAM, 0);
        if (socket_fd < 0) {
            LOGE("Error creating socket: %s", strerror(errno));
            return -1;
        }
        LOGI("IPv6 socket created");
        bzero(&addr, sizeof(struct sockaddr_in6));
        addr.sin6_family = AF_INET6;
        addr.sin6_port = htons(port);

        inet_pton(AF_INET6, addr_s, (struct sockaddr *) &addr.sin6_addr);
        if (connect(socket_fd, (struct sockaddr *) &addr, sizeof(struct sockaddr_in6)) < 0) {
            LOGE("Failed to connect to server: %s", strerror(errno));
            return -1;
        }

        LOGI("Successfully connected");
        stats.out_bytes = stats.out_packets = stats.in_bytes = stats.in_packets = 0;

        // thread for handling incoming messages
        pthread_create(&receive_pid, NULL, receive_thread, NULL);
        // thread for managing keepalive messages
        pthread_create(&timer_pid, NULL, timer_thread, NULL);

        return socket_fd;
    }

    int request_ipv4_config() {

        // send request
        MsgHeader ip_request = {.length = sizeof(MsgHeader), .type = MSG_TYPE_IP_REQUEST};
        if (write(socket_fd, &ip_request, ip_request.length) < 0) {
            LOGE("Failed to send IPv4 config request: %s", strerror(errno));
            return -1;
        }

        LOGI("IPv4 config request sent");

        // wait for configuration
        timespec timeout = {.tv_sec = time(NULL) + 5, .tv_nsec = 0};
        pthread_mutex_lock(&config_mutex);
        while (received_configuration == 0) {
            if (pthread_cond_timedwait(&config_cond, &config_mutex, &timeout) == ETIMEDOUT) {
                LOGE("IPv4 config timeout");
                return -1;
            }
        }
        pthread_mutex_unlock(&config_mutex);

        LOGI("IPv4 config received: IP %s, Route %s, DNS %s, %s, %s",
             config.ip, config.route, config.dns1, config.dns2, config.dns3);

        return 0;
    }

    void disconnect_socket() {
        LOGI("Disconnecting from server");

        if (receive_pid != -1) {
            pthread_kill(receive_pid, SIGUSR2);
            receive_pid = -1;
            LOGI("Receive thread terminated");
        }

        if (timer_pid != -1) {
            pthread_kill(timer_pid, SIGUSR2);
            timer_pid = -1;
            LOGI("Timer thread terminated");
        }

        if (forward_pid != -1) {
            pthread_kill(forward_pid, SIGUSR2);
            forward_pid = -1;
            LOGI("Forward thread terminated");
        }

        pthread_cond_init(&config_cond, NULL);
        pthread_mutex_init(&config_mutex, NULL);

        received_configuration = 0;

        if (socket_fd != -1) {
            close(socket_fd);
            socket_fd = -1;
            LOGI("IPv6 socket closed");
        }

        tunnel_fd = -1;
    }

    void setup_tunnel(int tunnel_fd_) {
        LOGI("Setting up tunnel file descriptor: %d", tunnel_fd_);
        tunnel_fd = tunnel_fd_;
        pthread_create(&forward_pid, NULL, forward_thread, NULL);
    }

    Ipv4Config get_ipv4_config() {
        return config;
    }

    ServerConfig get_server_config() {
        return server_config;
    }

    Statistics get_statistics() {
        return stats;
    }

    bool is_running() {
        return socket_fd != -1 && tunnel_fd != -1;
    }

    bool is_connecting() {
        return socket_fd != -1 && tunnel_fd == -1;
    }
}