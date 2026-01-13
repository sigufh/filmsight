#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include "raw_processor.h"
#include "film_engine.h"
#include "image_converter.h"
#include "film_params.h"
#include <string>
#include <vector>

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
    
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    RawMetadata metadata;
    
    try {
        LinearImage image = processor->loadRaw(path, metadata);
        env->ReleaseStringUTFChars(filePath, path);
        
        // 将图像数据存储在堆上，返回指针
        LinearImage* imagePtr = new LinearImage(std::move(image));
        return reinterpret_cast<jlong>(imagePtr);
    } catch (...) {
        env->ReleaseStringUTFChars(filePath, path);
        LOGE("Failed to load RAW image");
        return 0;
    }
}

/**
 * 初始化胶片引擎
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_FilmEngineNative_nativeInit(JNIEnv *env, jobject thiz) {
    FilmEngine* engine = new FilmEngine();
    return reinterpret_cast<jlong>(engine);
}

/**
 * 创建胶片参数对象
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_FilmParamsNative_nativeCreate(JNIEnv *env, jobject thiz) {
    FilmParams* params = new FilmParams();
    return reinterpret_cast<jlong>(params);
}

/**
 * 设置基础参数值
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_FilmParamsNative_nativeSetParams(
    JNIEnv *env, jobject thiz, jlong paramsPtr,
    jfloat globalExposure, jfloat contrast, jfloat saturation,
    jfloat highlights, jfloat shadows, jfloat whites, jfloat blacks,
    jfloat clarity, jfloat vibrance) {
    
    FilmParams* params = reinterpret_cast<FilmParams*>(paramsPtr);
    if (params) {
        params->globalExposure = globalExposure;
        params->contrast = contrast;
        params->saturation = saturation;

        // 基础色调
        params->basicTone.highlights = highlights;
        params->basicTone.shadows    = shadows;
        params->basicTone.whites     = whites;
        params->basicTone.blacks     = blacks;
        params->basicTone.clarity    = clarity;
        params->basicTone.vibrance   = vibrance;
    }
}

/**
 * 设置色调曲线参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_FilmParamsNative_nativeSetToneCurves(
    JNIEnv *env, jobject thiz, jlong paramsPtr,
    jboolean enableRgb, jfloatArray rgbCurve,
    jboolean enableRed, jfloatArray redCurve,
    jboolean enableGreen, jfloatArray greenCurve,
    jboolean enableBlue, jfloatArray blueCurve) {
    
    FilmParams* params = reinterpret_cast<FilmParams*>(paramsPtr);
    if (!params) return;
    
    params->toneCurve.enableRgbCurve = enableRgb;
    params->toneCurve.enableRedCurve = enableRed;
    params->toneCurve.enableGreenCurve = enableGreen;
    params->toneCurve.enableBlueCurve = enableBlue;
    
    jfloat* rgbData = nullptr;
    jfloat* redData = nullptr;
    jfloat* greenData = nullptr;
    jfloat* blueData = nullptr;
    
    if (rgbCurve) {
        rgbData = env->GetFloatArrayElements(rgbCurve, nullptr);
        if (rgbData) {
            for (int i = 0; i < 16; ++i) {
                params->toneCurve.rgbCurve[i] = rgbData[i];
            }
            env->ReleaseFloatArrayElements(rgbCurve, rgbData, JNI_ABORT);
        }
    }
    
    if (redCurve) {
        redData = env->GetFloatArrayElements(redCurve, nullptr);
        if (redData) {
            for (int i = 0; i < 16; ++i) {
                params->toneCurve.redCurve[i] = redData[i];
            }
            env->ReleaseFloatArrayElements(redCurve, redData, JNI_ABORT);
        }
    }
    
    if (greenCurve) {
        greenData = env->GetFloatArrayElements(greenCurve, nullptr);
        if (greenData) {
            for (int i = 0; i < 16; ++i) {
                params->toneCurve.greenCurve[i] = greenData[i];
            }
            env->ReleaseFloatArrayElements(greenCurve, greenData, JNI_ABORT);
        }
    }
    
    if (blueCurve) {
        blueData = env->GetFloatArrayElements(blueCurve, nullptr);
        if (blueData) {
            for (int i = 0; i < 16; ++i) {
                params->toneCurve.blueCurve[i] = blueData[i];
            }
            env->ReleaseFloatArrayElements(blueCurve, blueData, JNI_ABORT);
        }
    }
}

/**
 * 设置 HSL 参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_FilmParamsNative_nativeSetHSL(
    JNIEnv *env, jobject thiz, jlong paramsPtr,
    jboolean enableHSL,
    jfloatArray hueShift, jfloatArray saturation, jfloatArray luminance) {
    
    FilmParams* params = reinterpret_cast<FilmParams*>(paramsPtr);
    if (!params) return;
    
    params->hsl.enableHSL = enableHSL;
    
    jfloat* hueData = nullptr;
    jfloat* satData = nullptr;
    jfloat* lumData = nullptr;
    
    if (hueShift) {
        hueData = env->GetFloatArrayElements(hueShift, nullptr);
        if (hueData) {
            for (int i = 0; i < 8; ++i) {
                params->hsl.hueShift[i] = hueData[i];
            }
            env->ReleaseFloatArrayElements(hueShift, hueData, JNI_ABORT);
        }
    }
    
    if (saturation) {
        satData = env->GetFloatArrayElements(saturation, nullptr);
        if (satData) {
            for (int i = 0; i < 8; ++i) {
                params->hsl.saturation[i] = satData[i];
            }
            env->ReleaseFloatArrayElements(saturation, satData, JNI_ABORT);
        }
    }
    
    if (luminance) {
        lumData = env->GetFloatArrayElements(luminance, nullptr);
        if (lumData) {
            for (int i = 0; i < 8; ++i) {
                params->hsl.luminance[i] = lumData[i];
            }
            env->ReleaseFloatArrayElements(luminance, lumData, JNI_ABORT);
        }
    }
}

/**
 * 处理图像（应用胶片模拟）
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_FilmEngineNative_nativeProcess(
    JNIEnv *env, jobject thiz, jlong enginePtr, jlong imagePtr, jlong paramsPtr) {
    
    FilmEngine* engine = reinterpret_cast<FilmEngine*>(enginePtr);
    LinearImage* input = reinterpret_cast<LinearImage*>(imagePtr);
    FilmParams* params = reinterpret_cast<FilmParams*>(paramsPtr);
    
    if (!engine || !input || !params) {
        LOGE("Invalid pointers");
        return 0;
    }
    
    // 创建元数据（简化版，实际应从 RAW 文件读取）
    RawMetadata metadata;
    metadata.iso = 400.0f;
    metadata.exposureTime = 1.0f / 125.0f;
    
    try {
        LinearImage output = engine->process(*input, *params, metadata);
        LinearImage* outputPtr = new LinearImage(std::move(output));
        return reinterpret_cast<jlong>(outputPtr);
    } catch (...) {
        LOGE("Failed to process image");
        return 0;
    }
}

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
    
    LinearImage* image = reinterpret_cast<LinearImage*>(imagePtr);
    if (!image) {
        return nullptr;
    }
    
    try {
        OutputImage output = ImageConverter::linearToSRGB(*image);
        
        jbyteArray result = env->NewByteArray(output.data.size());
        env->SetByteArrayRegion(result, 0, output.data.size(), 
                               reinterpret_cast<const jbyte*>(output.data.data()));
        
        return result;
    } catch (...) {
        LOGE("Failed to convert image");
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
    
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to get bitmap info");
        return 0;
    }
    
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Unsupported bitmap format");
        return 0;
    }
    
    void* pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("Failed to lock bitmap pixels");
        return 0;
    }
    
    try {
        LinearImage linear = ImageConverter::sRGBToLinear(
            reinterpret_cast<const uint8_t*>(pixels),
            info.width,
            info.height
        );
        
        AndroidBitmap_unlockPixels(env, bitmap);
        
        LinearImage* linearPtr = new LinearImage(std::move(linear));
        return reinterpret_cast<jlong>(linearPtr);
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("Failed to convert bitmap to linear");
        return 0;
    }
}

} // extern "C"
