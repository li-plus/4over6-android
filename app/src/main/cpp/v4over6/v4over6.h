#pragma once

#include <stdint.h>

namespace v4over6 {
    struct Ipv4Config {
        char ip[20] = "\0";
        char route[20] = "\0";
        char dns1[20] = "\0";
        char dns2[20] = "\0";
        char dns3[20] = "\0";
    };

    struct Statistics {
        int out_bytes = 0;
        int out_packets = 0;
        int in_bytes = 0;
        int in_packets = 0;
    };

    int connect_socket(const char *addr, int port);

    int request_ipv4_config();

    void disconnect_socket();

    void setup_tunnel(int tun_fd);

    Ipv4Config get_ipv4_config();

    Statistics get_statistics();
}