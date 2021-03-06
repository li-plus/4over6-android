#pragma once

#include <stdint.h>

namespace v4over6 {
    struct Ipv4Config {
        char ip[32] = "\0";
        char route[32] = "\0";
        char dns1[32] = "\0";
        char dns2[32] = "\0";
        char dns3[32] = "\0";
    };

    struct Statistics {
        uint64_t out_bytes = 0;
        uint64_t out_packets = 0;
        uint64_t in_bytes = 0;
        uint64_t in_packets = 0;
    };

    int connect_socket(const char *addr_s, int port, bool enable_encrypt, const uint8_t *uuid);

    int request_ipv4_config();

    void disconnect_socket();

    void setup_tunnel(int tun_fd);

    Ipv4Config get_ipv4_config();

    Statistics get_statistics();

    bool is_running();
}