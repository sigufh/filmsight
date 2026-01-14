#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include "raw_processor.h"
#include "image_converter.h"
#include "raw_decoder.h"
#include "image_processor_engine.h"
#include "basic_adjustment_params.h"
#include <string>
#include <vector>
#include <exception>
#include <cstring>

#define LOG_TAG "FilmTracker"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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
        // 将图像数据存储在堆上，返回指针
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
        
        // 创建图像和元数据指针
        LinearImage* imagePtr = new LinearImage(std::move(image));
        RawMetadata* metadataPtr = metadata;
        
        // 返回两个指针的数组 [imagePtr, metadataPtr]
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

// 胶片引擎已移除

/**
 * 初始化图像处理引擎（模块化）
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeInit(JNIEnv *env, jobject thiz) {
    ImageProcessorEngine* engine = new ImageProcessorEngine();
    return reinterpret_cast<jlong>(engine);
}

/**
 * 应用基础调整（曝光、对比度、饱和度）
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyBasicAdjustments(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr,
    jfloat exposure, jfloat contrast, jfloat saturation) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    
    if (!engine || !image) {
        LOGE("Invalid pointers in nativeApplyBasicAdjustments");
        return;
    }
    
    engine->applyBasicAdjustments(*image, exposure, contrast, saturation);
}

/**
 * 应用色调调整（高光、阴影、白场、黑场）
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyToneAdjustments(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr,
    jfloat highlights, jfloat shadows, jfloat whites, jfloat blacks) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    
    if (!engine || !image) {
        LOGE("Invalid pointers in nativeApplyToneAdjustments");
        return;
    }
    
    engine->applyToneAdjustments(*image, highlights, shadows, whites, blacks);
}

/**
 * 应用清晰度和自然饱和度
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyPresence(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr,
    jfloat clarity, jfloat vibrance) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    
    if (!engine || !image) {
        LOGE("Invalid pointers in nativeApplyPresence");
        return;
    }
    
    engine->applyPresence(*image, clarity, vibrance);
}

/**
 * 应用色调曲线（已移除，保留空实现以兼容）
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyToneCurves(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr, jlong paramsPtr) {
    // 空实现，不做任何事
}

/**
 * 应用 HSL 调整（已移除，保留空实现以兼容）
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyHSL(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr, jlong paramsPtr) {
    // 空实现，不做任何事
}

// FilmParamsNative 相关方法已移除 - 使用 BasicAdjustmentParamsNative 替代

/**
 * 释放图像处理引擎
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeRelease(
    JNIEnv *env, jobject thiz, jlong enginePtr) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    if (engine) {
        delete engine;
    }
}

// FilmParamsNative 和 FilmEngineNative 相关方法已移除 - 使用 BasicAdjustmentParamsNative 替代

/**
 * 获取图像尺寸
 */
JNIEXPORT jintArray JNICALL
Java_com_filmtracker_app_native_ImageConverterNative_nativeGetImageSize(
    JNIEnv *env, jobject thiz, jlong imagePtr) {
    
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    if (!image) {
        return nullptr;
    }
    
    jintArray result = env->NewIntArray(2);
    jint size[2] = {static_cast<jint>(image->width), static_cast<jint>(image->height)};
    env->SetIntArrayRegion(result, 0, 2, size);
    return result;
}

/**
 * 转换为输出图像（sRGB）
 */
JNIEXPORT jbyteArray JNICALL
Java_com_filmtracker_app_native_ImageConverterNative_nativeLinearToSRGB(
    JNIEnv *env, jobject thiz, jlong imagePtr) {
    
    LOGI("nativeLinearToSRGB: Starting");
    
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    if (!image) {
        LOGE("nativeLinearToSRGB: Image pointer is null");
        return nullptr;
    }
    
    LOGI("nativeLinearToSRGB: Image size=%dx%d", image->width, image->height);
    
    try {
        LOGI("nativeLinearToSRGB: Calling ImageConverter::linearToSRGB");
        OutputImage output = ImageConverter::linearToSRGB(*image);
        LOGI("nativeLinearToSRGB: Conversion completed, data size=%zu bytes", output.data.size());
        
        LOGI("nativeLinearToSRGB: Creating byte array");
        jbyteArray result = env->NewByteArray(output.data.size());
        if (result == nullptr) {
            LOGE("nativeLinearToSRGB: Failed to create byte array");
            return nullptr;
        }
        
        LOGI("nativeLinearToSRGB: Copying data to byte array");
        env->SetByteArrayRegion(result, 0, output.data.size(), 
                               reinterpret_cast<const jbyte*>(output.data.data()));
        
        LOGI("nativeLinearToSRGB: Completed successfully");
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in nativeLinearToSRGB: %s", e.what());
        return nullptr;
    } catch (...) {
        LOGE("Unknown exception in nativeLinearToSRGB");
        return nullptr;
    }
}

/**
 * 释放图像资源
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageConverterNative_nativeReleaseImage(
    JNIEnv *env, jobject thiz, jlong imagePtr) {
    
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    if (image) {
        delete image;
    }
}

/**
 * 将 Android Bitmap 转换为线性域图像
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_ImageConverterNative_nativeBitmapToLinear(
    JNIEnv *env, jobject thiz, jobject bitmap) {
    
    if (bitmap == nullptr) {
        LOGE("Bitmap is null");
        return 0;
    }
    
    AndroidBitmapInfo info;
    int result = AndroidBitmap_getInfo(env, bitmap, &info);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to get bitmap info: %d", result);
        return 0;
    }
    
    // 支持多种格式
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 && 
        info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
        LOGE("Unsupported bitmap format: %d", info.format);
        return 0;
    }
    
    void* pixels = nullptr;
    result = AndroidBitmap_lockPixels(env, bitmap, &pixels);
    if (result != ANDROID_BITMAP_RESULT_SUCCESS || pixels == nullptr) {
        LOGE("Failed to lock bitmap pixels: %d", result);
        return 0;
    }
    
    try {
        const uint8_t* pixelData = reinterpret_cast<const uint8_t*>(pixels);
        
        // 如果是 RGB_565，需要转换
        if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
            // 创建临时 RGBA 缓冲区
            uint32_t pixelCount = info.width * info.height;
            std::vector<uint8_t> rgbaData(pixelCount * 4);
            
            const uint16_t* rgb565 = reinterpret_cast<const uint16_t*>(pixels);
            for (uint32_t i = 0; i < pixelCount; ++i) {
                uint16_t pixel = rgb565[i];
                uint8_t r = ((pixel >> 11) & 0x1F) << 3;
                uint8_t g = ((pixel >> 5) & 0x3F) << 2;
                uint8_t b = (pixel & 0x1F) << 3;
                
                rgbaData[i * 4 + 0] = r;
                rgbaData[i * 4 + 1] = g;
                rgbaData[i * 4 + 2] = b;
                rgbaData[i * 4 + 3] = 255;
            }
            
            LinearImage linear = ImageConverter::sRGBToLinear(
                rgbaData.data(),
                info.width,
                info.height
            );
            
            AndroidBitmap_unlockPixels(env, bitmap);
            
            LinearImage* linearPtr = new LinearImage(std::move(linear));
            return reinterpret_cast<jlong>(linearPtr);
        } else {
            // RGBA_8888 格式
            LinearImage linear = ImageConverter::sRGBToLinear(
                pixelData,
                info.width,
                info.height
            );
            
            AndroidBitmap_unlockPixels(env, bitmap);
            
            LinearImage* linearPtr = new LinearImage(std::move(linear));
            return reinterpret_cast<jlong>(linearPtr);
        }
    } catch (const std::exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("Exception in bitmap conversion: %s", e.what());
        return 0;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("Unknown exception in bitmap conversion");
        return 0;
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
