#ifndef FILMTRACKER_JNI_COMMON_H
#define FILMTRACKER_JNI_COMMON_H

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "FilmTracker"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace filmtracker {
namespace jni {

/**
 * 从 jobject 中获取 nativeHandle 字段
 */
inline jlong getNativeHandle(JNIEnv* env, jobject obj, const char* fieldName = "nativeHandle") {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldID = env->GetFieldID(clazz, fieldName, "J");
    return env->GetLongField(obj, fieldID);
}

/**
 * 设置 jobject 的 nativeHandle 字段
 */
inline void setNativeHandle(JNIEnv* env, jobject obj, jlong handle, const char* fieldName = "nativeHandle") {
    jclass clazz = env->GetObjectClass(obj);
    jfieldID fieldID = env->GetFieldID(clazz, fieldName, "J");
    env->SetLongField(obj, fieldID, handle);
}

} // namespace jni
} // namespace filmtracker

#endif // FILMTRACKER_JNI_COMMON_H
