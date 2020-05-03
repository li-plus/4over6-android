#pragma once

#include <cstdint>

namespace v4over6 {

    constexpr uint8_t MSG_TYPE_IP_REQUEST = 100;
    constexpr uint8_t MSG_TYPE_IP_RESPONSE = 101;
    constexpr uint8_t MSG_TYPE_REQUEST = 102;
    constexpr uint8_t MSG_TYPE_RESPONSE = 103;
    constexpr uint8_t MSG_TYPE_HEARTBEAT = 104;
    constexpr uint8_t MSG_TYPE_UNSUPPORTED = 199;
    constexpr uint8_t MSG_TYPE_ENCRYPTED = 200;
    constexpr uint8_t MSG_TYPE_NO_TYPE = 255;

    constexpr size_t MSG_DATA_SIZE = 2048;

    constexpr uint8_t ADDITIONAL_DATA[] = "19260817";
    constexpr uint8_t ADDITIONAL_DATA_LEN = sizeof(ADDITIONAL_DATA) / sizeof(uint8_t);

    struct __attribute__ ((packed)) MsgHeader {
        uint32_t length;
        uint8_t type;
    };

    struct __attribute__ ((packed)) Msg {
        MsgHeader header;
        uint8_t data[MSG_DATA_SIZE];
    };

    constexpr size_t HEADER_LEN = sizeof(MsgHeader);
}