#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_teaminfernale_gazetrackeropencv_MainActivity_getMessage(JNIEnv *env, jobject instance) {

    // TODO


    return (*env)->NewStringUTF(env, "Messaggio da NDK");
}