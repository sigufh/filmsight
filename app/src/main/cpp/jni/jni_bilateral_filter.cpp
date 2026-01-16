#include "jni_common.h"
#include "../include/bilateral_filter.h"

using namespace filmtracker;

extern "C" {

/**
 * 设置双边滤波器配置
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BilateralFilterNative_nativeSetConfig(
    JNIEnv *env, jclass clazz,
    jboolean enableCache,
    jboolean enableFastApproximation,
    jboolean enableGPU,
    jint maxCacheSize,
    jint maxCacheMemoryMB,
    jfloat fastApproxThreshold,
    jint gpuThresholdPixels) {
    
    LOGI("========== JNI: BilateralFilter Configuration Request ==========");
    LOGI("nativeSetConfig: Received configuration from Kotlin layer:");
    LOGI("  - enableCache: %d", enableCache);
    LOGI("  - enableFastApproximation: %d", enableFastApproximation);
    LOGI("  - enableGPU: %d", enableGPU);
    LOGI("  - maxCacheSize: %d", maxCacheSize);
    LOGI("  - maxCacheMemoryMB: %d", maxCacheMemoryMB);
    LOGI("  - fastApproxThreshold: %.2f", fastApproxThreshold);
    LOGI("  - gpuThresholdPixels: %d", gpuThresholdPixels);
    
    BilateralFilter::Config config;
    config.enableCache = enableCache;
    config.enableFastApproximation = enableFastApproximation;
    config.enableGPU = enableGPU;
    config.maxCacheSize = static_cast<size_t>(maxCacheSize);
    config.maxCacheMemoryMB = static_cast<size_t>(maxCacheMemoryMB);
    config.fastApproxThreshold = fastApproxThreshold;
    config.gpuThresholdPixels = static_cast<uint32_t>(gpuThresholdPixels);
    
    LOGI("nativeSetConfig: Passing configuration to C++ layer...");
    BilateralFilter::setConfig(config);
    
    LOGI("nativeSetConfig: Configuration successfully applied");
    LOGI("================================================================");
}

/**
 * 获取双边滤波器配置
 */
JNIEXPORT jobject JNICALL
Java_com_filmtracker_app_native_BilateralFilterNative_nativeGetConfig(
    JNIEnv *env, jclass clazz) {
    
    BilateralFilter::Config config = BilateralFilter::getConfig();
    
    // 查找 Config 类
    jclass configClass = env->FindClass("com/filmtracker/app/native/BilateralFilterNative$Config");
    if (!configClass) {
        LOGE("Failed to find BilateralFilterNative$Config class");
        return nullptr;
    }
    
    // 尝试查找全参数构造函数
    jmethodID constructor = env->GetMethodID(configClass, "<init>", "(ZZZIIIFI)V");
    
    if (constructor) {
        // 如果找到了构造函数，直接使用
        jobject configObj = env->NewObject(configClass, constructor,
            static_cast<jboolean>(config.enableCache),
            static_cast<jboolean>(config.enableFastApproximation),
            static_cast<jboolean>(config.enableGPU),
            static_cast<jint>(config.maxCacheSize),
            static_cast<jint>(config.maxCacheMemoryMB),
            static_cast<jfloat>(config.fastApproxThreshold),
            static_cast<jint>(config.gpuThresholdPixels));
        return configObj;
    }
    
    // 如果没找到，使用默认构造函数并设置字段
    env->ExceptionClear(); // 清除异常
    constructor = env->GetMethodID(configClass, "<init>", "()V");
    if (!constructor) {
        LOGE("Failed to find default Config constructor");
        return nullptr;
    }
    
    jobject configObj = env->NewObject(configClass, constructor);
    if (!configObj) {
        LOGE("Failed to create Config object");
        return nullptr;
    }
    
    // 设置各个字段
    jfieldID enableCacheField = env->GetFieldID(configClass, "enableCache", "Z");
    jfieldID enableFastApproxField = env->GetFieldID(configClass, "enableFastApproximation", "Z");
    jfieldID enableGPUField = env->GetFieldID(configClass, "enableGPU", "Z");
    jfieldID maxCacheSizeField = env->GetFieldID(configClass, "maxCacheSize", "I");
    jfieldID maxCacheMemoryMBField = env->GetFieldID(configClass, "maxCacheMemoryMB", "I");
    jfieldID fastApproxThresholdField = env->GetFieldID(configClass, "fastApproxThreshold", "F");
    jfieldID gpuThresholdPixelsField = env->GetFieldID(configClass, "gpuThresholdPixels", "I");
    
    if (!enableCacheField || !enableFastApproxField || !enableGPUField || 
        !maxCacheSizeField || !maxCacheMemoryMBField || !fastApproxThresholdField || 
        !gpuThresholdPixelsField) {
        LOGE("Failed to find one or more Config fields");
        return nullptr;
    }
    
    env->SetBooleanField(configObj, enableCacheField, config.enableCache);
    env->SetBooleanField(configObj, enableFastApproxField, config.enableFastApproximation);
    env->SetBooleanField(configObj, enableGPUField, config.enableGPU);
    env->SetIntField(configObj, maxCacheSizeField, config.maxCacheSize);
    env->SetIntField(configObj, maxCacheMemoryMBField, config.maxCacheMemoryMB);
    env->SetFloatField(configObj, fastApproxThresholdField, config.fastApproxThreshold);
    env->SetIntField(configObj, gpuThresholdPixelsField, config.gpuThresholdPixels);
    
    return configObj;
}

/**
 * 获取双边滤波器统计信息
 */
JNIEXPORT jobject JNICALL
Java_com_filmtracker_app_native_BilateralFilterNative_nativeGetStats(
    JNIEnv *env, jclass clazz) {
    
    BilateralFilter::Stats stats = BilateralFilter::getStats();
    
    // 查找 Stats 类
    jclass statsClass = env->FindClass("com/filmtracker/app/native/BilateralFilterNative$Stats");
    if (!statsClass) {
        LOGE("Failed to find BilateralFilterNative$Stats class");
        return nullptr;
    }
    
    // 查找构造函数
    jmethodID constructor = env->GetMethodID(statsClass, "<init>", "(JJJJJJD)V");
    if (!constructor) {
        LOGE("Failed to find Stats constructor");
        return nullptr;
    }
    
    // 创建 Stats 对象
    jobject statsObj = env->NewObject(statsClass, constructor,
        static_cast<jlong>(stats.totalCalls),
        static_cast<jlong>(stats.standardCalls),
        static_cast<jlong>(stats.fastApproxCalls),
        static_cast<jlong>(stats.gpuCalls),
        static_cast<jlong>(stats.cacheHits),
        static_cast<jlong>(stats.cacheMisses),
        static_cast<jdouble>(stats.avgProcessingTimeMs));
    
    return statsObj;
}

/**
 * 重置双边滤波器统计信息
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BilateralFilterNative_nativeResetStats(
    JNIEnv *env, jclass clazz) {
    
    BilateralFilter::resetStats();
    LOGI("BilateralFilter stats reset");
}

/**
 * 清除双边滤波器缓存
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BilateralFilterNative_nativeClearCache(
    JNIEnv *env, jclass clazz) {
    
    BilateralFilter::clearCache();
    LOGI("BilateralFilter cache cleared");
}

/**
 * 初始化默认配置
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BilateralFilterNative_nativeInitializeDefaultConfig(
    JNIEnv *env, jclass clazz) {
    
    LOGI("========== JNI: Initialize Default Configuration ==========");
    LOGI("nativeInitializeDefaultConfig: Initializing default configuration...");
    
    BilateralFilter::initializeDefaultConfig();
    
    // 获取并记录配置
    std::string configStr = BilateralFilter::getConfigString();
    LOGI("nativeInitializeDefaultConfig: Configuration initialized successfully");
    LOGI("%s", configStr.c_str());
    LOGI("===========================================================");
}

} // extern "C"
