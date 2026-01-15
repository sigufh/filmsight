#include "jni_common.h"
#include "../include/image_converter.h"
#include <android/bitmap.h>
#include <vector>

using namespace filmtracker;

extern "C" {

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
        
        if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
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

} // extern "C"
