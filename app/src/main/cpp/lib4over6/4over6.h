#pragma once

#include <stdint.h>

namespace v4over6 {
    extern char ip[20], route[20], dns1[20], dns2[20], dns3[20];
    extern int out_byte, out_pkt, in_byte, in_pkt;
    extern int socket_fd;

    typedef struct __attribute__ ((packed)) {
        uint32_t length;
        uint8_t type;
    } message_header_t;

    typedef struct __attribute__ ((packed)) {
        message_header_t header;
        uint8_t data[4096];
    } message_t;

    int establish_connection(const char *addr, int port);

    int request_configuration();

    void tearup_connection();

    void setup_tun(int tun_fd);
}