#include <jni.h>

#define TAG "jni_interface"

#include "msg.h"
#include "v4over6.h"

extern "C"
JNIEXPORT jint JNICALL
Java_top_liplus_v4over6_activity_MainActivity_connectSocket(JNIEnv *env, jobject instance,
                                                            jstring addr_, jint port) {
    const char *addr = env->GetStringUTFChars(addr_, 0);

    int ret = v4over6::connect_socket(addr, port);

    env->ReleaseStringUTFChars(addr_, addr);

    return ret;
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_activity_MainActivity_disconnectSocket(JNIEnv *env, jobject instance) {
    v4over6::disconnect_socket();
}

extern "C"
JNIEXPORT jint JNICALL
Java_top_liplus_v4over6_activity_MainActivity_requestIpv4Config(JNIEnv *env, jobject instance) {
    return v4over6::request_ipv4_config();
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_activity_MainActivity_setupTunnel(JNIEnv *env, jobject instance,
                                                         jint tunnel_fd) {
    v4over6::setup_tunnel(tunnel_fd);
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_activity_MainActivity_getStatistics(JNIEnv *env, jobject instance,
                                                            jobject stats) {
    jclass clazz = env->GetObjectClass(stats);
    jfieldID field_id;

    field_id = env->GetFieldID(clazz, "uploadPackets", "I");
    env->SetIntField(stats, field_id, v4over6::out_pkt);

    field_id = env->GetFieldID(clazz, "uploadBytes", "I");
    env->SetIntField(stats, field_id, v4over6::out_byte);

    field_id = env->GetFieldID(clazz, "downloadPackets", "I");
    env->SetIntField(stats, field_id, v4over6::in_pkt);

    field_id = env->GetFieldID(clazz, "downloadBytes", "I");
    env->SetIntField(stats, field_id, v4over6::in_byte);
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_activity_MainActivity_getIpv4Config(JNIEnv *env, jobject instance,
                                                            jobject config) {
    jclass clazz = env->GetObjectClass(config);

    jfieldID field_id;
    jstring jstr;

    field_id = env->GetFieldID(clazz, "ipv4", "Ljava/lang/String;");
    jstr = env->NewStringUTF(v4over6::ip);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "route", "Ljava/lang/String;");
    jstr = env->NewStringUTF(v4over6::route);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "dns1", "Ljava/lang/String;");
    jstr = env->NewStringUTF(v4over6::dns1);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "dns2", "Ljava/lang/String;");
    jstr = env->NewStringUTF(v4over6::dns2);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "dns3", "Ljava/lang/String;");
    jstr = env->NewStringUTF(v4over6::dns3);
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "socketFd", "I");
    env->SetIntField(config, field_id, v4over6::socket_fd);
}