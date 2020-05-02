#include <jni.h>

#define TAG "jni_interface"

#include "msg.h"
#include "v4over6.h"

extern "C"
JNIEXPORT jint JNICALL
Java_top_liplus_v4over6_vpn_V4over6_connectSocket(JNIEnv *env, jclass type, jstring addr_,
                                                  jint port) {
    const char *addr = env->GetStringUTFChars(addr_, 0);

    int ret = v4over6::connect_socket(addr, port);

    env->ReleaseStringUTFChars(addr_, addr);

    return ret;
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_vpn_V4over6_disconnectSocket(JNIEnv *env, jclass type) {
    v4over6::disconnect_socket();
}

extern "C"
JNIEXPORT jint JNICALL
Java_top_liplus_v4over6_vpn_V4over6_requestIpv4Config(JNIEnv *env, jclass type) {
    return v4over6::request_ipv4_config();
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_vpn_V4over6_setupTunnel(JNIEnv *env, jclass type, jint tunnel_fd) {
    v4over6::setup_tunnel(tunnel_fd);
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_vpn_V4over6_getStatistics(JNIEnv *env, jclass type, jobject stats) {
    jclass clazz = env->GetObjectClass(stats);
    jfieldID field_id;

    v4over6::Statistics native_stats = v4over6::get_statistics();

    field_id = env->GetFieldID(clazz, "uploadPackets", "I");
    env->SetIntField(stats, field_id, native_stats.out_packets);

    field_id = env->GetFieldID(clazz, "uploadBytes", "I");
    env->SetIntField(stats, field_id, native_stats.out_bytes);

    field_id = env->GetFieldID(clazz, "downloadPackets", "I");
    env->SetIntField(stats, field_id, native_stats.in_packets);

    field_id = env->GetFieldID(clazz, "downloadBytes", "I");
    env->SetIntField(stats, field_id, native_stats.in_bytes);
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_vpn_V4over6_getIpv4Config(JNIEnv *env, jclass type, jobject config) {
    v4over6::Ipv4Config native_config = v4over6::get_ipv4_config();

    jclass clazz = env->GetObjectClass(config);

    jfieldID field_id;
    jstring jstr;

    field_id = env->GetFieldID(clazz, "ipv4", "Ljava/lang/String;");
    jstr = env->NewStringUTF(native_config.ip);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "route", "Ljava/lang/String;");
    jstr = env->NewStringUTF(native_config.route);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "dns1", "Ljava/lang/String;");
    jstr = env->NewStringUTF(native_config.dns1);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "dns2", "Ljava/lang/String;");
    jstr = env->NewStringUTF(native_config.dns2);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "dns3", "Ljava/lang/String;");
    jstr = env->NewStringUTF(native_config.dns3);
    env->SetObjectField(config, field_id, jstr);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_top_liplus_v4over6_vpn_V4over6_isRunning(JNIEnv *env, jclass type) {
    return (jboolean) v4over6::is_running();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_top_liplus_v4over6_vpn_V4over6_isConnecting(JNIEnv *env, jclass type) {
    return (jboolean) v4over6::is_connecting();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_top_liplus_v4over6_vpn_V4over6_isDisconnecting(JNIEnv *env, jclass type) {
    return (jboolean) v4over6::is_disconnecting();
}