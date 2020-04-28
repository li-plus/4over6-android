#include <jni.h>
#include <cstdlib>
#include <string.h>

#define TAG "jni_interface"
#include "log.h"

#include "4over6.h"

extern "C"
JNIEXPORT jboolean JNICALL
Java_top_liplus_v4over6_activity_MainActivity_connect(JNIEnv *env, jobject instance, jstring addr_,
                                                      jstring port_) {
    const char *addr_s = env->GetStringUTFChars(addr_, 0);
    const char *port_s = env->GetStringUTFChars(port_, 0);

    strcpy(port, port_s);
    strcpy(remote, addr_s);
    int ret = establish_connection(addr_s, atoi(port_s));

    env->ReleaseStringUTFChars(addr_, addr_s);
    env->ReleaseStringUTFChars(port_, port_s);

    return (ret < 0) ? JNI_FALSE : JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_activity_MainActivity_disconnect(JNIEnv *env, jobject instance) {
    tearup_connection();
}

extern "C"
JNIEXPORT jobject JNICALL
Java_top_liplus_v4over6_activity_MainActivity_requestIpv4Config(JNIEnv *env, jobject instance,
                                                                jobject config) {
    int ret = request_configuration();
    if (ret < 0) {
        return NULL;
    }
    jclass clazz = env->GetObjectClass(config);

    jfieldID field_id;
    jstring jstr;

    LOGI("setting ipv4");
    field_id = env->GetFieldID(clazz, "ipv4", "Ljava/lang/String;");
    jstr = env->NewStringUTF(ip);
    env->SetObjectField(config, field_id, jstr);

    LOGI("setting ipv6");
    field_id = env->GetFieldID(clazz, "ipv6", "Ljava/lang/String;");
    jstr = env->NewStringUTF(remote);
    env->SetObjectField(config, field_id, jstr);

    LOGI("setting port");
    field_id = env->GetFieldID(clazz, "port", "Ljava/lang/String;");
    jstr = env->NewStringUTF(port);
    env->SetObjectField(config, field_id, jstr);

    LOGI("setting searchDomain");
    field_id = env->GetFieldID(clazz, "searchDomain", "Ljava/lang/String;");
    jstr = env->NewStringUTF("tsinghua.edu.cn");
    env->SetObjectField(config, field_id, jstr);

    field_id = env->GetFieldID(clazz, "route", "Ljava/lang/String;");
    jstr = env->NewStringUTF(route);
    env->SetObjectField(config, field_id, jstr);

    LOGI("setting dns1");
    field_id = env->GetFieldID(clazz, "dns1", "Ljava/lang/String;");
    jstr = env->NewStringUTF(dns1);
    env->SetObjectField(config, field_id, jstr);

    LOGI("setting dns2");
    field_id = env->GetFieldID(clazz, "dns2", "Ljava/lang/String;");
    jstr = env->NewStringUTF(dns2);
    env->SetObjectField(config, field_id, jstr);

    LOGI("setting dns3");
    field_id = env->GetFieldID(clazz, "dns3", "Ljava/lang/String;");
    jstr = env->NewStringUTF(dns3);
    env->SetObjectField(config, field_id, jstr);

    LOGI("setting socketfd");
    field_id = env->GetFieldID(clazz, "socketFd", "I");
    env->SetIntField(config, field_id, socket_fd);

    return config;
}

extern "C"
JNIEXPORT void JNICALL
Java_top_liplus_v4over6_activity_MainActivity_initTunnel(JNIEnv *env, jobject instance,
                                                         jint tunnel_fd) {
    setup_tun(tunnel_fd);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_top_liplus_v4over6_activity_MainActivity_getStatistics(JNIEnv *env, jobject instance,
                                                            jobject data) {
    jclass clazz = env->GetObjectClass(data);
    jfieldID field_id;

    field_id = env->GetFieldID(clazz, "state", "Z");
    env->SetBooleanField(clazz, field_id, socket_fd != -1 ? JNI_TRUE : JNI_FALSE);

    field_id = env->GetFieldID(clazz, "uploadPackets", "I");
    env->SetIntField(clazz, field_id, out_pkt);

    field_id = env->GetFieldID(clazz, "uploadBytes", "I");
    env->SetIntField(clazz, field_id, out_byte);

    field_id = env->GetFieldID(clazz, "downloadPackets", "I");
    env->SetIntField(clazz, field_id, in_pkt);

    field_id = env->GetFieldID(clazz, "downloadBytes", "I");
    env->SetIntField(clazz, field_id, in_byte);

    return data;
}