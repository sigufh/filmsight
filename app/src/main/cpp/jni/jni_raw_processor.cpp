#include "jni_common.h"
#include "../raw/raw_processor.h"
#include "../raw/raw_decoder.h"
#include <string>
#include <vector>

using namespace filmtracker;

extern "C" {

/**
 * 初始化 RAW 处理器
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_RawProcessorNative_nativeInit(JNIEnv *env, jobject thiz) {
    RawProcessor* processor = new RawProcessor();
    return reinterpret_cast<jlong>(processor);
}

/**
 * 加载 RAW 图像
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_RawProcessorNative_nativeLoadRaw(
    JNIEnv *env, jobject thiz, jlong nativePtr, jstring filePath) {
    
    RawProcessor* processor = reinterpret_cast<RawProcessor*>(nativePtr);
    if (!processor) {
        LOGE("RawProcessor is null");
        return 0;
    }
    
    if (filePath == nullptr) {
        LOGE("File path is null");
        return 0;
    }
    
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    if (path == nullptr) {
        LOGE("Failed to get string chars");
        return 0;
    }
    
    LOGI("nativeLoadRaw: Starting, path=%s", path);
    
    RawMetadata metadata;
    
    try {
        LOGI("nativeLoadRaw: Calling processor->loadRaw");
        LinearImage image = processor->loadRaw(path, metadata);
        LOGI("nativeLoadRaw: loadRaw completed, image size=%dx%d", image.width, image.height);
        
        env->ReleaseStringUTFChars(filePath, path);
        
        LOGI("nativeLoadRaw: Creating LinearImage pointer");
        LinearImage* imagePtr = new LinearImage(std::move(image));
        LOGI("nativeLoadRaw: Successfully created LinearImage pointer");
        return reinterpret_cast<jlong>(imagePtr);
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(filePath, path);
        LOGE("Exception loading RAW: %s", e.what());
        return 0;
    } catch (...) {
        env->ReleaseStringUTFChars(filePath, path);
        LOGE("Unknown exception loading RAW");
        return 0;
    }
}

/**
 * 加载 RAW 图像并返回元数据
 */
JNIEXPORT jlongArray JNICALL
Java_com_filmtracker_app_native_RawProcessorNative_nativeLoadRawWithMetadata(
    JNIEnv *env, jobject thiz, jlong nativePtr, jstring filePath) {
    
    RawProcessor* processor = reinterpret_cast<RawProcessor*>(nativePtr);
    if (!processor) {
        LOGE("RawProcessor is null");
        return nullptr;
    }
    
    if (filePath == nullptr) {
        LOGE("File path is null");
        return nullptr;
    }
    
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    if (path == nullptr) {
        LOGE("Failed to get string chars");
        return nullptr;
    }
    
    LOGI("nativeLoadRawWithMetadata: Starting, path=%s", path);
    
    RawMetadata* metadata = new RawMetadata();
    
    try {
        LOGI("nativeLoadRawWithMetadata: Calling processor->loadRaw");
        LinearImage image = processor->loadRaw(path, *metadata);
        LOGI("nativeLoadRawWithMetadata: loadRaw completed, image size=%dx%d", image.width, image.height);
        
        env->ReleaseStringUTFChars(filePath, path);
        
        LinearImage* imagePtr = new LinearImage(std::move(image));
        RawMetadata* metadataPtr = metadata;
        
        jlongArray result = env->NewLongArray(2);
        jlong ptrs[2] = {reinterpret_cast<jlong>(imagePtr), reinterpret_cast<jlong>(metadataPtr)};
        env->SetLongArrayRegion(result, 0, 2, ptrs);
        
        LOGI("nativeLoadRawWithMetadata: Successfully created pointers");
        return result;
    } catch (const std::exception& e) {
        env->ReleaseStringUTFChars(filePath, path);
        delete metadata;
        LOGE("Exception loading RAW: %s", e.what());
        return nullptr;
    } catch (...) {
        env->ReleaseStringUTFChars(filePath, path);
        delete metadata;
        LOGE("Unknown exception loading RAW");
        return nullptr;
    }
}

/**
 * 提取 RAW 文件的嵌入式 JPEG 预览
 */
JNIEXPORT jbyteArray JNICALL
Java_com_filmtracker_app_native_RawProcessorNative_nativeExtractPreview(
    JNIEnv *env, jobject thiz, jlong nativePtr, jstring filePath) {
    
    if (filePath == nullptr) {
        LOGE("File path is null");
        return nullptr;
    }
    
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    if (path == nullptr) {
        LOGE("Failed to get string chars");
        return nullptr;
    }
    
    LOGI("nativeExtractPreview: Extracting preview from %s", path);
    
    std::vector<uint8_t> jpegData;
    bool success = extractRawPreview(path, jpegData);
    
    env->ReleaseStringUTFChars(filePath, path);
    
    if (!success || jpegData.empty()) {
        LOGE("nativeExtractPreview: Failed to extract preview");
        return nullptr;
    }
    
    LOGI("nativeExtractPreview: Successfully extracted %zu bytes", jpegData.size());
    
    jbyteArray result = env->NewByteArray(jpegData.size());
    env->SetByteArrayRegion(result, 0, jpegData.size(), 
                           reinterpret_cast<const jbyte*>(jpegData.data()));
    
    return result;
}

/**
 * 获取 RAW 文件的原图尺寸
 */
JNIEXPORT jintArray JNICALL
Java_com_filmtracker_app_native_RawProcessorNative_nativeGetRawImageSize(
    JNIEnv *env, jobject thiz, jlong nativePtr, jstring filePath) {
    
    if (filePath == nullptr) {
        LOGE("File path is null");
        return nullptr;
    }
    
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    if (path == nullptr) {
        LOGE("Failed to get string chars");
        return nullptr;
    }
    
    LOGI("nativeGetRawImageSize: Getting size from %s", path);
    
    uint32_t width = 0;
    uint32_t height = 0;
    bool success = getRawFileInfo(path, width, height);
    
    env->ReleaseStringUTFChars(filePath, path);
    
    if (!success || width == 0 || height == 0) {
        LOGE("nativeGetRawImageSize: Failed to get image size");
        return nullptr;
    }
    
    LOGI("nativeGetRawImageSize: Image size = %ux%u", width, height);
    
    jintArray result = env->NewIntArray(2);
    jint size[2] = {static_cast<jint>(width), static_cast<jint>(height)};
    env->SetIntArrayRegion(result, 0, 2, size);
    
    return result;
}

/**
 * RawMetadataNative getter 函数
 */
JNIEXPORT jint JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getWidth(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? static_cast<jint>(metadata->width) : 0;
}

JNIEXPORT jint JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getHeight(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? static_cast<jint>(metadata->height) : 0;
}

JNIEXPORT jint JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getBitsPerSample(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? static_cast<jint>(metadata->bitsPerSample) : 0;
}

JNIEXPORT jfloat JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getIso(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? metadata->iso : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getExposureTime(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? metadata->exposureTime : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getAperture(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? metadata->aperture : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getFocalLength(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? metadata->focalLength : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getWhiteBalanceTemperature(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? metadata->whiteBalance[0] : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getWhiteBalanceTint(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? metadata->whiteBalance[1] : 0.0f;
}

JNIEXPORT jstring JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getCameraModel(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    if (!metadata) {
        return nullptr;
    }
    return env->NewStringUTF(metadata->cameraModel);
}

JNIEXPORT jstring JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getColorSpace(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    if (!metadata) {
        return nullptr;
    }
    return env->NewStringUTF(metadata->colorSpace);
}

JNIEXPORT jfloat JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getBlackLevel(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? metadata->blackLevel : 0.0f;
}

JNIEXPORT jfloat JNICALL
Java_com_filmtracker_app_native_RawMetadataNative_getWhiteLevel(JNIEnv *env, jobject thiz, jlong nativePtr) {
    RawMetadata* metadata = reinterpret_cast<RawMetadata*>(nativePtr);
    return metadata ? metadata->whiteLevel : 0.0f;
}

} // extern "C"
