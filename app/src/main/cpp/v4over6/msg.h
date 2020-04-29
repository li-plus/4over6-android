#pragma once

#include <cstdint>

namespace v4over6 {

#define MSG_TYPE_IP_REQUEST 100
#define MSG_TYPE_IP_RESPONSE 101
#define MSG_TYPE_REQUEST 102
#define MSG_TYPE_RESPONSE 103
#define MSG_TYPE_HEARTBEAT 104

#define MSG_DATA_SIZE 4096

    typedef struct __attribute__ ((packed)) {
        uint32_t length;
        uint8_t type;
    } message_header_t;

    typedef struct __attribute__ ((packed)) {
        message_header_t header;
        uint8_t data[MSG_DATA_SIZE];
    } message_t;
}