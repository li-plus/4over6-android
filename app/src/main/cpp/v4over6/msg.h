#pragma once

#include <cstdint>

namespace v4over6 {

#define MSG_TYPE_IP_REQUEST 100
#define MSG_TYPE_IP_RESPONSE 101
#define MSG_TYPE_REQUEST 102
#define MSG_TYPE_RESPONSE 103
#define MSG_TYPE_HEARTBEAT 104

#define MSG_DATA_SIZE 4096

    struct __attribute__ ((packed)) MsgHeader {
        uint32_t length;
        uint8_t type;
    };

    struct __attribute__ ((packed)) Msg {
        MsgHeader header;
        uint8_t data[MSG_DATA_SIZE];
    };
}