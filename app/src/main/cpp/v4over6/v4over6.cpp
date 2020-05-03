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
#include <sodium.h>

#define TAG "lib4over6"

#include "msg.h"
#include "v4over6.h"
#include "log.h"

namespace v4over6 {
    static Ipv4Config config;
    static Statistics stats;

    static int socket_fd = -1, tunnel_fd = -1;

    static pthread_cond_t config_cond = PTHREAD_COND_INITIALIZER;
    static pthread_mutex_t config_mutex = PTHREAD_MUTEX_INITIALIZER;

    static volatile int received_configuration = 0;
    static pthread_t receive_pid = -1, timer_pid = -1, forward_pid = -1;
    static time_t last_heartbeat_recv = -1, last_heartbeat_send = -1;
    static bool enable_encrypt = false;
    static uint8_t key[crypto_aead_xchacha20poly1305_ietf_KEYBYTES] = {0};

    static bool encrypt_msg(Msg &dst, const Msg &src) {
        uint8_t nonce[crypto_aead_xchacha20poly1305_ietf_NPUBBYTES];
        uint8_t local_key[crypto_aead_xchacha20poly1305_ietf_KEYBYTES];
        randombytes_buf(nonce, sizeof nonce);
        unsigned long long ciphertext_len;
        if(src.header.length + sizeof nonce + crypto_aead_xchacha20poly1305_ietf_ABYTES > MSG_DATA_SIZE)
            return false;
        memcpy(dst.data, nonce, sizeof nonce);
        memcpy(local_key, key, 16 * sizeof(uint8_t));
        memcpy(local_key + 16, nonce, 16 * sizeof(uint8_t));
        auto cipher = ((uint8_t *) dst.data) + sizeof nonce;
        crypto_aead_xchacha20poly1305_ietf_encrypt(cipher, &ciphertext_len, (const uint8_t *) &src, src.header.length,
                                                           ADDITIONAL_DATA, ADDITIONAL_DATA_LEN, nullptr, nonce, local_key);
        dst.header.type = MSG_TYPE_ENCRYPTED;
        dst.header.length = HEADER_LEN + sizeof nonce + ciphertext_len;
        return true;
    }

    static bool decrypt_msg(Msg &dst, const Msg &src) {
        uint8_t local_key[crypto_aead_xchacha20poly1305_ietf_KEYBYTES];
        uint8_t nonce[crypto_aead_xchacha20poly1305_ietf_NPUBBYTES];
        memcpy(nonce, src.data, sizeof nonce);
        memcpy(local_key, key, 16 * sizeof(uint8_t));
        memcpy(local_key + 16, nonce, 16 * sizeof(uint8_t));
        unsigned long long decrypted_len;
        auto cipher = ((uint8_t *) src.data) + sizeof nonce;
        if (src.header.length > sizeof nonce + HEADER_LEN) {
            size_t cipher_len = src.header.length - HEADER_LEN - sizeof nonce;
            return crypto_aead_xchacha20poly1305_ietf_decrypt((uint8_t *) &dst, &decrypted_len, nullptr, cipher,
                                                              cipher_len,
                                                              ADDITIONAL_DATA, ADDITIONAL_DATA_LEN, nonce, local_key) == 0;
        }
        return false;
    }

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

    // forward packets from socket to tunnel
    static void *receive_thread(void *args) {
        LOGI("Receive thread started");

        while (true) {
            Msg msg;
            if(socket_fd < 0) break;
            ssize_t read_bytes = read_exact(socket_fd, (uint8_t *) &msg.header, sizeof(MsgHeader));
            if (read_bytes < 0) {
                LOGE("Error reading from socket: %s", strerror(errno));
                continue;
            }

            size_t data_len = msg.header.length - HEADER_LEN;
            if (data_len < 0 || data_len > MSG_DATA_SIZE) {
                continue;
            }

            read_bytes = read_exact(socket_fd, (uint8_t *) msg.data, data_len);
            if (read_bytes < 0) {
                LOGE("Error reading from socket: %s", strerror(errno));
                continue;
            }

            bool dec_ok = false;
            if (msg.header.type == MSG_TYPE_ENCRYPTED) {
                if (enable_encrypt) {
                    Msg src;
                    memcpy(&src, &msg, sizeof(uint8_t) * msg.header.length);
                    if (decrypt_msg(msg, src)) {
                        dec_ok = true;
                        data_len = msg.header.length - HEADER_LEN;
                    } else {
                        LOGE("Failed to decrypt msg from server");
                    }
                } else {
                    // client does not support encryption
                    LOGE("Received encrypted msg, but client does not support it");
                }
                if (!dec_ok) {
                    // send nak
                    msg.header.type = MSG_TYPE_UNSUPPORTED;
                    msg.header.length = HEADER_LEN;
                    if (write(socket_fd, &msg, HEADER_LEN) < 0) {
                        LOGE("Failed to send NAK: %s", strerror(errno));
                    }
                    continue;
                }
            }
            switch (msg.header.type) {
                case MSG_TYPE_RESPONSE: write(tunnel_fd, msg.data, data_len); break;
                case MSG_TYPE_IP_RESPONSE: {
                    char buffer[MSG_DATA_SIZE];
                    memcpy(buffer, msg.data, data_len);
                    buffer[data_len] = '\0';
                    sscanf(buffer, "%s %s %s %s %s",
                           config.ip, config.route, config.dns1, config.dns2, config.dns3);
                    // wake up sleeping thread
                    pthread_mutex_lock(&config_mutex);
                    received_configuration = 1;
                    pthread_mutex_unlock(&config_mutex);
                    pthread_cond_signal(&config_cond);
                }
                break;
                case MSG_TYPE_HEARTBEAT:
                LOGI("Heartbeat received");
                last_heartbeat_recv = time(NULL);
                break;
                case MSG_TYPE_UNSUPPORTED:
                LOGI("Server does not support encryption");
                enable_encrypt = false;
                break;
                default:
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
        last_heartbeat_recv = time(NULL);

        while (true) {
            if(socket_fd < 0) break;
            time_t current_time = time(NULL);
            if (last_heartbeat_recv != -1 && current_time - last_heartbeat_recv > 60) {
                // close connection
                LOGE("Server heartbeat timeout");
                timer_pid = -1; // prevent itself to be killed
                disconnect_socket();
                break;
            }
            if (last_heartbeat_send == -1 ||
                current_time - last_heartbeat_send >= 20) {
                // send heartbeat
                Msg heartbeat = {
                    .header = {
                        .length = HEADER_LEN,
                        .type = MSG_TYPE_HEARTBEAT
                    }
                };
                Msg enc;
                Msg *msg_send = &heartbeat;
                if (enable_encrypt) {
                    if(encrypt_msg(enc, heartbeat)) {
                        msg_send = &enc;
                    } else {
                        LOGE("Failed to encrypt heartbeat");
                    }
                }
                if (write(socket_fd, msg_send, msg_send->header.length) < 0) {
                    LOGE("Failed to send heartbeat: %s", strerror(errno));
                } else {
                    LOGI("Heartbeat sent");
                    last_heartbeat_send = current_time;
                    stats.out_packets++;
                    stats.out_bytes += msg_send->header.length;
                }
            }
            sleep(1);
        }
        return NULL;
    }

    // forward packets from tunnel to socket
    static void *forward_thread(void *args) {
        LOGI("Forward thread started");
        uint8_t buffer[MSG_DATA_SIZE];

        while (true) {
            if(socket_fd < 0) break;
            ssize_t read_bytes = read(tunnel_fd, buffer, sizeof(buffer));
            if (read_bytes < 0) {
                continue;
            }
            struct iphdr *hdr = (struct iphdr *) buffer;
            if (hdr->version != 4) {
                LOGD("Received an IPv%d packet from tunnel", hdr->version);
                continue;
            }
            uint16_t tot_len = ntohs(hdr->tot_len);
            uint16_t header_len = hdr->ihl * 4;
            if (!(header_len <= tot_len && read_bytes == tot_len)) {
                LOGE("Received a broken IPv4 packet from tunnel");
                continue;
            }

            Msg msg, dst;
            msg.header.type = MSG_TYPE_REQUEST;
            msg.header.length = tot_len + sizeof(MsgHeader);
            memcpy(msg.data, buffer, tot_len);
            Msg *msg_send = &msg;
            if (enable_encrypt) {
                if (encrypt_msg(dst, msg)) {
                    msg_send = &dst;
                } else {
                    LOGE("Failed to encrypt msg");
                }
            }
            if (write(socket_fd, msg_send, msg_send->header.length) < 0) {
                LOGE("Error writing to socket: %s", strerror(errno));
            } else {
                stats.out_packets++;
                stats.out_bytes += msg_send->header.length;
            }
        }
        return NULL;
    }

    int connect_socket(const char *addr_s, int port, bool encrypt, const uint8_t *uuid) {
        LOGI("Connecting to server [%s]:%d", addr_s, port);
        enable_encrypt = encrypt;
        if (encrypt) {
            LOGI("Attempting to enable encrypt");
            memcpy(key, uuid, 16 * sizeof(uint8_t));
        }
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
        Msg ip_request = {.header={.length = sizeof(MsgHeader), .type = MSG_TYPE_IP_REQUEST}};
        Msg enc;
        Msg *msg_send = &ip_request;

        if (enable_encrypt) {
            if (encrypt_msg(enc, ip_request)) {
                msg_send = &enc;
            } else {
                LOGE("Failed to encrypt IP request");
            }
        }

        if (write(socket_fd, msg_send, msg_send->header.length) < 0) {
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

        if (receive_pid != -1 && pthread_kill(receive_pid, 0) == 0) {
            receive_pid = -1;
            LOGI("Receive thread terminated");
        }

        if (timer_pid != -1 && pthread_kill(timer_pid, 0) == 0) {
            timer_pid = -1;
            LOGI("Timer thread terminated");
        }

        if (forward_pid != -1 && pthread_kill(forward_pid, 0) == 0) {
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

        if (tunnel_fd != -1) {
            close(tunnel_fd);
            tunnel_fd = -1;
            LOGI("Tunnel socket closed");
        }
    }

    void setup_tunnel(int tunnel_fd_) {
        LOGI("Setting up tunnel file descriptor: %d", tunnel_fd_);
        tunnel_fd = tunnel_fd_;
        pthread_create(&forward_pid, NULL, forward_thread, NULL);
    }

    Ipv4Config get_ipv4_config() {
        return config;
    }

    Statistics get_statistics() {
        return stats;
    }

    bool is_running() {
        return socket_fd != -1 && tunnel_fd != -1;
    }
}