#include <assert.h>
#include <errno.h>
#include <netinet/in.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>
#include <linux/if_tun.h>
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
    int socket_fd = -1, tunnel_fd = -1;

    static pthread_cond_t config_cond = PTHREAD_COND_INITIALIZER;
    static pthread_mutex_t config_mutex = PTHREAD_MUTEX_INITIALIZER;

    static volatile int received_configuration = 0;
    static pthread_t receive_pid = -1, timer_pid = -1, forward_pid = -1;
    static time_t last_heartbeat_recv = -1, last_heartbeat_send = -1;

    char ip[20], route[20], dns1[20], dns2[20], dns3[20];
    int out_byte, out_pkt, in_byte, in_pkt;

    static void print_packet(uint8_t *packet, size_t len) {
//    static char str[4096 * 3];
//    char *curr = str;
//    for (size_t i = 0; i < len; i++) {
//        curr += sprintf(curr, "%02x ", packet[i]);
//    }
//    LOGD("%s", str);
    }

    static uint8_t *read_exact(int fd, size_t len) {
        static size_t pos = 0;
        static size_t end = 0;
        const static size_t buffer_size = 40960;
        static uint8_t buffer[buffer_size];
        if (pos + len >= buffer_size) {
            // not enough size, put back to beginning
            memmove(buffer, buffer + pos, end - pos);
            end = end - pos;
            pos = 0;
        }

        uint8_t *result = buffer + pos;
        uint8_t *ptr = buffer + end;
        while (ptr < buffer + pos + len) {
            ssize_t read_bytes = read(fd, ptr, buffer_size - end);
            if (read_bytes < 0) {
                if (read_bytes == -1 && errno == EAGAIN) {
                    continue;
                }
                LOGE("Error reading from socket: %s", strerror(errno));
                assert(false);
            } else {
                ptr += read_bytes;
                end += read_bytes;
            }
        }

        pos += len;
        return result;
    }

    static void signal_handler(int signal) {
        if (signal == SIGUSR2) {
            pthread_exit(EXIT_SUCCESS);
        }
    }

// read from socket
    static void *receive_thread(void *args) {

        uint8_t buffer[4096];

        LOGI("Reading thread started");
        signal(SIGUSR2, signal_handler);

        while (true) {
            message_header_t *msg =
                    (message_header_t *) read_exact(socket_fd, sizeof(message_header_t));
            uint32_t msg_length = msg->length;
            uint8_t msg_type = msg->type;

            size_t len = msg_length - sizeof(message_header_t);
            if (len >= 0) {
                uint8_t *body = read_exact(socket_fd, len);
                if (msg_type == MSG_TYPE_RESPONSE) {
                    // copy reply to TUN
//                LOGD("Received 103 len: %ld", len);
//                print_packet(body, len);
                    write(tunnel_fd, body, len);
                } else if (msg_type == MSG_TYPE_IP_RESPONSE) {
                    // configuration from server
                    char buffer[1024];
                    memcpy(buffer, body, len);
                    buffer[len] = '\0';
                    LOGD("received 101: %s", buffer);
                    sscanf(buffer, "%s %s %s %s %s", ip, route, dns1, dns2, dns3);
                    // wake up sleeping thread
                    pthread_mutex_lock(&config_mutex);
                    received_configuration = 1;
                    pthread_cond_signal(&config_cond);
                    pthread_mutex_unlock(&config_mutex);
                } else if (msg_type == MSG_TYPE_HEARTBEAT) {
                    // heartbeat packet
                    LOGI("Received heartbeat from server");
                    last_heartbeat_recv = time(NULL);
                } else {
                    // unknown type
                    LOGE("Unrecognised msg type %d with length %d", msg_type,
                         msg_length);
                }
            }

            in_pkt++;
            in_byte += msg_length;
        }
        return NULL;
    }

// manage heartbeat
    static void *timer_thread(void *args) {

        LOGI("Timer thread started");
        signal(SIGUSR2, signal_handler);
        last_heartbeat_recv = time(NULL);

        while (true) {
            time_t current_time = time(NULL);
            if (last_heartbeat_recv != -1 &&
                current_time - last_heartbeat_recv > 60) {
                // close connection
                LOGE("Server heartbeat timeout");
                timer_pid = -1; // prevent itself to be killed
                disconnect_socket();
                pthread_exit((void *) EXIT_FAILURE);
            }
            if (last_heartbeat_send == -1 ||
                current_time - last_heartbeat_send >= 20) {
                // send heartbeat
                message_header_t heartbeat = {.length = sizeof(message_header_t), .type = MSG_TYPE_HEARTBEAT};
                if (write(socket_fd, &heartbeat, heartbeat.length) < 0) {
                    LOGE("Failed to send heartbeat: %s", strerror(errno));
                } else {
                    LOGI("Sent heartbeat to server");
                    last_heartbeat_send = current_time;
                    out_pkt++;
                    out_byte += sizeof(message_header_t);
                }
            }
            sleep(1);
        }

        return NULL;
    }

// forward packet from TUN to socket
    static void *forward_thread(void *args) {
        LOGI("Forward thread started");
        signal(SIGUSR2, signal_handler);
        uint8_t buffer[4096];

        while (true) {
            uint8_t *current = buffer;
            ssize_t read_bytes = read(tunnel_fd, buffer, sizeof(buffer));
            if (read_bytes < 0) {
                continue;
            }
            uint8_t *end = current + read_bytes;

            struct iphdr *hdr = (struct iphdr *) current;
            if (hdr->version != 4) {
                LOGD("Not a ipv4 packet");
                continue;
            }
            uint8_t *ip = current;
            uint16_t len = ntohs(hdr->tot_len);
            uint8_t header_len = hdr->ihl * 4;
            current += header_len;
            assert(current <= end);

            uint8_t *body = current;
            uint16_t body_len = len - header_len;
            current += body_len;
            assert(current == end);

            message_t data;
            data.header.type = MSG_TYPE_REQUEST;
            memcpy(data.data, ip, len);
            data.header.length = len + sizeof(message_header_t);
//        LOGD("Sent %d", data.header.length);
//        print_packet(data.data, len);
            if (write(socket_fd, &data, data.header.length) < 0) {
                LOGE("Error writing payload to socket: %s", strerror(errno));
            } else {
                out_pkt++;
                out_byte += data.header.length;
            }
        }
        return NULL;
    }


    int connect_socket(const char *addr_s, int port) {
        LOGI("Starting setup process");
//
//    struct addrinfo hints = { 0 };
//    struct addrinfo *res;
//    int gai_err;
//    int s;
//
//    hints.ai_family = AF_INET6;
//    hints.ai_socktype = SOCK_STREAM;
//
//    gai_err = getaddrinfo("fe80::dd65:a055:eb91:de98%wlan0", "5555", &hints, &res);
//
//    if (gai_err)
//    {
//        fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(gai_err));
//        return 1;
//    }
//
        struct sockaddr_in6 addr;
//    socket_fd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
        socket_fd = socket(AF_INET6, SOCK_STREAM, 0);
        if (socket_fd < 0) {
            LOGE("Error creating socket: %s", strerror(errno));
            return -1;
        }
        LOGI("IPv6 TCP socket created");
        bzero(&addr, sizeof(struct sockaddr_in6));
        addr.sin6_family = AF_INET6;
        addr.sin6_port = htons(port);

        inet_pton(AF_INET6, addr_s, (struct sockaddr *) &addr.sin6_addr);
        if (connect(socket_fd, (struct sockaddr *) &addr,
                    sizeof(struct sockaddr_in6)) < 0) {
            LOGE("Failed to connect to server [%s]:%d: %s", addr_s, port,
                 strerror(errno));
            return -1;
        }

        LOGI("Connected to server %s", addr_s);
        out_byte = out_pkt = in_byte = in_pkt = 0;

        // thread for handling incoming messages
        pthread_create(&receive_pid, NULL, receive_thread, NULL);
        // thread for managing keepalive messages
        pthread_create(&timer_pid, NULL, timer_thread, NULL);

        return 0;
    }

    int request_configuration() {

        // send request
        message_header_t ip_request = {.length = sizeof(message_header_t), .type = MSG_TYPE_IP_REQUEST};
        if (write(socket_fd, &ip_request, ip_request.length) < 0) {
            LOGE("Failed to send connection request: %s", strerror(errno));
            return -1;
        }

        LOGI("IP request sent");

        // wait for configuration
        pthread_mutex_lock(&config_mutex);
        while (received_configuration == 0) {
            pthread_cond_wait(&config_cond, &config_mutex);
        }
        pthread_mutex_unlock(&config_mutex);

        LOGI("IP configuration received: %s %s %s %s %s", ip, route, dns1, dns2,
             dns3);

        return 0;
    }

    void disconnect_socket() {

        LOGI("Starting tearup process");

        if (receive_pid != -1) {
            pthread_kill(receive_pid, SIGUSR2);
            receive_pid = -1;
            LOGI("Read thread terminated");
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
    }

    void setup_tunnel(int tunnel_fd_) {
        LOGD("Got VPN TUN fd: %d", tunnel_fd_);
        tunnel_fd = tunnel_fd_;
        pthread_create(&forward_pid, NULL, forward_thread, NULL);
    }
}