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
 * 应用色调曲线
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyToneCurves(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr, jlong paramsPtr) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(paramsPtr);
    
    if (!engine || !image) {
        LOGE("Invalid pointers in nativeApplyToneCurves: engine=%p, image=%p", engine, image);
        return;
    }
    
    if (!params) {
        LOGE("nativeApplyToneCurves: params is null");
        return;
    }
    
    if (!params->curveParams) {
        LOGI("nativeApplyToneCurves: No curve params set, skipping");
        return;
    }
    
    LOGI("nativeApplyToneCurves: Applying curves");
    engine->applyToneCurves(*image, *params->curveParams);
}

/**
 * 应用 HSL 调整
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyHSL(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr, jlong paramsPtr) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(paramsPtr);
    
    if (!engine || !image || !params) {
        LOGE("Invalid pointers in nativeApplyHSL");
        return;
    }
    
    // 检查是否有 HSL 参数
    if (!params->hslParams) {
        LOGI("nativeApplyHSL: No HSL params set, skipping");
        return;
    }
    
    if (!params->hslParams->enableHSL) {
        LOGI("nativeApplyHSL: HSL disabled, skipping");
        return;
    }
    
    LOGI("nativeApplyHSL: Applying HSL adjustments");
    engine->applyHSL(*image, *params->hslParams);
}

/**
 * 应用颜色调整
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyColorAdjustments(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr, jlong paramsPtr) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(paramsPtr);
    
    if (!engine || !image || !params) {
        LOGE("Invalid pointers in nativeApplyColorAdjustments");
        return;
    }
    
    engine->applyColorAdjustments(*image, *params);
}

/**
 * 应用效果
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyEffects(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr, jlong paramsPtr) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(paramsPtr);
    
    if (!engine || !image || !params) {
        LOGE("Invalid pointers in nativeApplyEffects");
        return;
    }
    
    engine->applyEffects(*image, *params);
}

/**
 * 应用细节
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeApplyDetails(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr, jlong paramsPtr) {
    
    ImageProcessorEngine* engine = reinterpret_cast<ImageProcessorEngine*>(enginePtr);
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(paramsPtr);
    
    if (!engine || !image || !params) {
        LOGE("Invalid pointers in nativeApplyDetails");
        return;
    }
    
    engine->applyDetails(*image, *params);
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
 * 获取 RAW 文件的原图尺寸（不解码完整图像）
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

/**
 * BasicAdjustmentParamsNative 方法
 */

/**
 * 创建 BasicAdjustmentParams 对象
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeCreate(JNIEnv *env, jclass clazz) {
    BasicAdjustmentParams* params = new BasicAdjustmentParams();
    jlong handle = reinterpret_cast<jlong>(params);
    LOGI("nativeCreate: Created BasicAdjustmentParams at %p, handle=%lld", params, handle);
    return handle;
}

/**
 * 设置基础参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetBasicParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat exposure, jfloat contrast, jfloat saturation) {
    
    LOGI("nativeSetBasicParams: nativeHandle=%ld, exposure=%.2f, contrast=%.2f, saturation=%.2f", 
         nativeHandle, exposure, contrast, saturation);
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetBasicParams: params is NULL!");
        return;
    }
    
    LOGI("nativeSetBasicParams: params pointer=%p", params);
    params->globalExposure = exposure;
    params->contrast = contrast;
    params->saturation = saturation;
    LOGI("nativeSetBasicParams: completed");
}

/**
 * 设置色调参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetToneParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat highlights, jfloat shadows, jfloat whites, jfloat blacks) {
    
    LOGI("nativeSetToneParams: nativeHandle=%ld", nativeHandle);
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetToneParams: params is NULL!");
        return;
    }
    
    params->highlights = highlights;
    params->shadows = shadows;
    params->whites = whites;
    params->blacks = blacks;
    LOGI("nativeSetToneParams: completed");
}

/**
 * 设置存在感参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetPresenceParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat clarity, jfloat vibrance) {
    
    LOGI("nativeSetPresenceParams: nativeHandle=%ld", nativeHandle);
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetPresenceParams: params is NULL!");
        return;
    }
    
    params->clarity = clarity;
    params->vibrance = vibrance;
    LOGI("nativeSetPresenceParams: completed");
}

/**
 * 设置颜色参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetColorParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat temperature, jfloat tint) {
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetColorParams: params is NULL!");
        return;
    }
    
    params->temperature = temperature;
    params->tint = tint;
}

/**
 * 设置效果参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetEffectsParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat texture, jfloat dehaze, jfloat vignette, jfloat grain) {
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetEffectsParams: params is NULL!");
        return;
    }
    
    params->texture = texture;
    params->dehaze = dehaze;
    params->vignette = vignette;
    params->grain = grain;
}

/**
 * 设置细节参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetDetailParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat sharpening, jfloat noiseReduction) {
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetDetailParams: params is NULL!");
        return;
    }
    
    params->sharpening = sharpening;
    params->noiseReduction = noiseReduction;
}

/**
 * 设置色调曲线（动态控制点）
 * @param channel 通道：0=RGB, 1=Red, 2=Green, 3=Blue
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetToneCurve(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jint channel, jboolean enable, jfloatArray xCoords, jfloatArray yCoords) {
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetToneCurve: params is null, nativeHandle=%ld", nativeHandle);
        return;
    }
    
    LOGI("nativeSetToneCurve: nativeHandle=%ld, channel=%d, enable=%d", nativeHandle, channel, enable);
    
    // 延迟初始化 curveParams
    if (!params->curveParams) {
        params->curveParams = new ToneCurveParams();
        LOGI("nativeSetToneCurve: Created new ToneCurveParams");
    }
    
    if (!enable || xCoords == nullptr || yCoords == nullptr) {
        // 禁用该通道
        switch (channel) {
            case 0: 
                params->curveParams->rgbCurve.enabled = false; 
                LOGI("nativeSetToneCurve: RGB curve disabled");
                break;
            case 1: 
                params->curveParams->redCurve.enabled = false; 
                LOGI("nativeSetToneCurve: Red curve disabled");
                break;
            case 2: 
                params->curveParams->greenCurve.enabled = false; 
                LOGI("nativeSetToneCurve: Green curve disabled");
                break;
            case 3: 
                params->curveParams->blueCurve.enabled = false; 
                LOGI("nativeSetToneCurve: Blue curve disabled");
                break;
        }
        return;
    }
    
    jsize xLen = env->GetArrayLength(xCoords);
    jsize yLen = env->GetArrayLength(yCoords);
    
    if (xLen != yLen || xLen == 0) {
        LOGE("nativeSetToneCurve: Invalid array lengths, xLen=%d, yLen=%d", xLen, yLen);
        return;
    }
    
    LOGI("nativeSetToneCurve: Processing %d control points", xLen);
    
    jfloat* xData = env->GetFloatArrayElements(xCoords, nullptr);
    jfloat* yData = env->GetFloatArrayElements(yCoords, nullptr);
    
    if (!xData || !yData) {
        LOGE("nativeSetToneCurve: Failed to get array elements");
        if (xData) env->ReleaseFloatArrayElements(xCoords, xData, JNI_ABORT);
        if (yData) env->ReleaseFloatArrayElements(yCoords, yData, JNI_ABORT);
        return;
    }
    
    try {
        ToneCurveParams::CurveData* targetCurve = nullptr;
        
        switch (channel) {
            case 0: targetCurve = &params->curveParams->rgbCurve; break;
            case 1: targetCurve = &params->curveParams->redCurve; break;
            case 2: targetCurve = &params->curveParams->greenCurve; break;
            case 3: targetCurve = &params->curveParams->blueCurve; break;
        }
        
        if (targetCurve) {
            targetCurve->setPoints(xLen, xData, yData);
            targetCurve->enabled = true;
            
            LOGI("nativeSetToneCurve: Successfully set channel=%d, points=%d, enabled=true", channel, xLen);
            
            // 打印前几个控制点用于调试
            for (int i = 0; i < std::min(3, (int)xLen); ++i) {
                LOGI("  Point[%d]: (%.3f, %.3f)", i, xData[i], yData[i]);
            }
        } else {
            LOGE("nativeSetToneCurve: Invalid channel=%d", channel);
        }
    } catch (const std::exception& e) {
        LOGE("nativeSetToneCurve: Exception: %s", e.what());
    } catch (...) {
        LOGE("nativeSetToneCurve: Unknown exception");
    }
    
    env->ReleaseFloatArrayElements(xCoords, xData, JNI_ABORT);
    env->ReleaseFloatArrayElements(yCoords, yData, JNI_ABORT);
}

/**
 * 设置分级参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetGradingParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat highlightsTemp, jfloat highlightsTint,
    jfloat midtonesTemp, jfloat midtonesTint,
    jfloat shadowsTemp, jfloat shadowsTint,
    jfloat blending, jfloat balance) {
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetGradingParams: params is NULL!");
        return;
    }
    
    params->gradingHighlightsTemp = highlightsTemp;
    params->gradingHighlightsTint = highlightsTint;
    params->gradingMidtonesTemp = midtonesTemp;
    params->gradingMidtonesTint = midtonesTint;
    params->gradingShadowsTemp = shadowsTemp;
    params->gradingShadowsTint = shadowsTint;
    params->gradingBlending = blending;
    params->gradingBalance = balance;
    
    LOGI("nativeSetGradingParams: highlights=(%.2f,%.2f), midtones=(%.2f,%.2f), shadows=(%.2f,%.2f), blending=%.2f, balance=%.2f",
         highlightsTemp, highlightsTint, midtonesTemp, midtonesTint, shadowsTemp, shadowsTint, blending, balance);
}

/**
 * 设置 HSL 调整
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_setHSL(
    JNIEnv *env, jobject thiz,
    jboolean enableHSL, jfloatArray hueShift, jfloatArray saturation, jfloatArray luminance) {
    
    // 从 jobject 中获取 nativeHandle 字段
    jclass clazz = env->GetObjectClass(thiz);
    jfieldID fieldID = env->GetFieldID(clazz, "nativeHandle", "J");
    jlong nativeHandle = env->GetLongField(thiz, fieldID);
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("setHSL: params is NULL!");
        return;
    }
    
    LOGI("setHSL: nativeHandle=%ld, enableHSL=%d, hueShift=%p, saturation=%p, luminance=%p", 
         nativeHandle, enableHSL, hueShift, saturation, luminance);
    
    // 延迟初始化 hslParams
    if (!params->hslParams) {
        params->hslParams = new HSLParams();
        LOGI("setHSL: Created new HSLParams");
    }
    
    // 如果禁用或任何数组为空，禁用 HSL
    if (!enableHSL) {
        params->hslParams->enableHSL = false;
        LOGI("setHSL: HSL disabled by flag");
        return;
    }
    
    // 检查数组是否为 null（必须在调用 GetArrayLength 之前）
    if (hueShift == nullptr || saturation == nullptr || luminance == nullptr) {
        params->hslParams->enableHSL = false;
        LOGE("setHSL: One or more arrays are null");
        return;
    }
    
    // 现在可以安全地检查数组长度
    jsize hueLen = 0;
    jsize satLen = 0;
    jsize lumLen = 0;
    
    try {
        hueLen = env->GetArrayLength(hueShift);
        satLen = env->GetArrayLength(saturation);
        lumLen = env->GetArrayLength(luminance);
    } catch (...) {
        LOGE("setHSL: Exception getting array lengths");
        params->hslParams->enableHSL = false;
        return;
    }
    
    LOGI("setHSL: Array lengths: hue=%d, sat=%d, lum=%d", hueLen, satLen, lumLen);
    
    if (hueLen != 8 || satLen != 8 || lumLen != 8) {
        LOGE("setHSL: Invalid array lengths, hue=%d, sat=%d, lum=%d (expected 8)", hueLen, satLen, lumLen);
        params->hslParams->enableHSL = false;
        return;
    }
    
    // 获取数组数据
    jfloat* hueData = nullptr;
    jfloat* satData = nullptr;
    jfloat* lumData = nullptr;
    
    try {
        hueData = env->GetFloatArrayElements(hueShift, nullptr);
        satData = env->GetFloatArrayElements(saturation, nullptr);
        lumData = env->GetFloatArrayElements(luminance, nullptr);
    } catch (...) {
        LOGE("setHSL: Exception getting array elements");
        if (hueData) env->ReleaseFloatArrayElements(hueShift, hueData, JNI_ABORT);
        if (satData) env->ReleaseFloatArrayElements(saturation, satData, JNI_ABORT);
        if (lumData) env->ReleaseFloatArrayElements(luminance, lumData, JNI_ABORT);
        params->hslParams->enableHSL = false;
        return;
    }
    
    if (!hueData || !satData || !lumData) {
        LOGE("setHSL: Failed to get array elements (hue=%p, sat=%p, lum=%p)", hueData, satData, lumData);
        if (hueData) env->ReleaseFloatArrayElements(hueShift, hueData, JNI_ABORT);
        if (satData) env->ReleaseFloatArrayElements(saturation, satData, JNI_ABORT);
        if (lumData) env->ReleaseFloatArrayElements(luminance, lumData, JNI_ABORT);
        params->hslParams->enableHSL = false;
        return;
    }
    
    // 复制数据到 HSLParams
    try {
        for (int i = 0; i < 8; ++i) {
            params->hslParams->hueShift[i] = hueData[i];
            params->hslParams->saturation[i] = satData[i];
            params->hslParams->luminance[i] = lumData[i];
        }
        
        params->hslParams->enableHSL = true;
        
        LOGI("setHSL: Successfully set HSL parameters");
        
        // 打印前几个值用于调试
        LOGI("  Hue shifts: [%.1f, %.1f, %.1f, ...]", 
             params->hslParams->hueShift[0], 
             params->hslParams->hueShift[1], 
             params->hslParams->hueShift[2]);
    } catch (const std::exception& e) {
        LOGE("setHSL: Exception during copy: %s", e.what());
        params->hslParams->enableHSL = false;
    } catch (...) {
        LOGE("setHSL: Unknown exception during copy");
        params->hslParams->enableHSL = false;
    }
    
    // 释放数组
    env->ReleaseFloatArrayElements(hueShift, hueData, JNI_ABORT);
    env->ReleaseFloatArrayElements(saturation, satData, JNI_ABORT);
    env->ReleaseFloatArrayElements(luminance, lumData, JNI_ABORT);
}

/**
 * 释放 BasicAdjustmentParams 对象
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeRelease(
    JNIEnv *env, jobject thiz, jlong nativeHandle) {
    
    LOGI("nativeRelease: nativeHandle=%ld", nativeHandle);
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (params) {
        LOGI("nativeRelease: Deleting params at %p", params);
        delete params;
        LOGI("nativeRelease: Completed");
    } else {
        LOGE("nativeRelease: params is NULL!");
    }
}

} // extern "C"
