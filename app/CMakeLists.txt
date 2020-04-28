cmake_minimum_required(VERSION 3.4.1)

aux_source_directory(src/main/cpp/lib4over6 DIR_SRCS)

add_library(lib4over6 SHARED ${DIR_SRCS})

find_library(LOG_LIB log)

target_link_libraries(lib4over6 ${LOG_LIB})
