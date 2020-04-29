#pragma once

#include <stdint.h>

namespace v4over6 {
    extern char ip[20], route[20], dns1[20], dns2[20], dns3[20];
    extern int out_byte, out_pkt, in_byte, in_pkt;
    extern int socket_fd;

    int connect_socket(const char *addr, int port);

    int request_configuration();

    void disconnect_socket();

    void setup_tunnel(int tun_fd);
}