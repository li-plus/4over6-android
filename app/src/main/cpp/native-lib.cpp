#include <jni.h>
#include <string>

extern "C"

JNIEXPORT jstring JNICALL
Java_top_liplus_v4over6_activity_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
