cmake_minimum_required(VERSION 3.4.1)

aux_source_directory(src/main/cpp/v4over6 DIR_SRCS)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -O3")

add_library(v4over6 SHARED ${DIR_SRCS})

add_library(libsodium STATIC IMPORTED)
message("Building on ABI ${ANDROID_ABI}")
set_target_properties(libsodium PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/libsodium/${ANDROID_ABI}/lib/libsodium.a)

find_library(LOG_LIB log)

target_include_directories(v4over6 PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/libsodium/${ANDROID_ABI}/include)
target_link_libraries(v4over6 ${LOG_LIB} libsodium)
