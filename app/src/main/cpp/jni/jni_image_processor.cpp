#include "jni_common.h"
#include "../include/image_processor_engine.h"
#include "../include/basic_adjustment_params.h"

using namespace filmtracker;

extern "C" {

/**
 * 初始化图像处理引擎
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_ImageProcessorEngineNative_nativeInit(JNIEnv *env, jobject thiz) {
    ImageProcessorEngine* engine = new ImageProcessorEngine();
    return reinterpret_cast<jlong>(engine);
}

/**
 * 应用基础调整
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
 * 应用色调调整
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

} // extern "C"
