#include "jni_common.h"
#include "../color/basic_adjustment_params.h"
#include <algorithm>

using namespace filmtracker;

extern "C" {

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
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetBasicParams: params is NULL!");
        return;
    }
    
    params->globalExposure = exposure;
    params->contrast = contrast;
    params->saturation = saturation;
}

/**
 * 设置色调参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetToneParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat highlights, jfloat shadows, jfloat whites, jfloat blacks) {
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetToneParams: params is NULL!");
        return;
    }
    
    params->highlights = highlights;
    params->shadows = shadows;
    params->whites = whites;
    params->blacks = blacks;
}

/**
 * 设置存在感参数
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetPresenceParams(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jfloat clarity, jfloat vibrance) {
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetPresenceParams: params is NULL!");
        return;
    }
    
    params->clarity = clarity;
    params->vibrance = vibrance;
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
 * 设置色调曲线
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_nativeSetToneCurve(
    JNIEnv *env, jobject thiz, jlong nativeHandle,
    jint channel, jboolean enable, jfloatArray xCoords, jfloatArray yCoords) {
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (!params) {
        LOGE("nativeSetToneCurve: params is null");
        return;
    }
    
    if (!params->curveParams) {
        params->curveParams = new ToneCurveParams();
    }
    
    if (!enable || xCoords == nullptr || yCoords == nullptr) {
        switch (channel) {
            case 0: params->curveParams->rgbCurve.enabled = false; break;
            case 1: params->curveParams->redCurve.enabled = false; break;
            case 2: params->curveParams->greenCurve.enabled = false; break;
            case 3: params->curveParams->blueCurve.enabled = false; break;
        }
        return;
    }
    
    jsize xLen = env->GetArrayLength(xCoords);
    jsize yLen = env->GetArrayLength(yCoords);
    
    if (xLen != yLen || xLen == 0) {
        LOGE("nativeSetToneCurve: Invalid array lengths");
        return;
    }
    
    jfloat* xData = env->GetFloatArrayElements(xCoords, nullptr);
    jfloat* yData = env->GetFloatArrayElements(yCoords, nullptr);
    
    if (!xData || !yData) {
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
        }
    } catch (...) {
        LOGE("nativeSetToneCurve: Exception");
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
}

/**
 * 设置 HSL 调整
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_BasicAdjustmentParamsNative_setHSL(
    JNIEnv *env, jobject thiz,
    jboolean enableHSL, jfloatArray hueShift, jfloatArray saturation, jfloatArray luminance) {
    
    jlong nativeHandle = jni::getNativeHandle(env, thiz);
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    
    if (!params) {
        LOGE("setHSL: params is NULL!");
        return;
    }
    
    if (!params->hslParams) {
        params->hslParams = new HSLParams();
    }
    
    if (!enableHSL) {
        params->hslParams->enableHSL = false;
        return;
    }
    
    if (hueShift == nullptr || saturation == nullptr || luminance == nullptr) {
        params->hslParams->enableHSL = false;
        LOGE("setHSL: One or more arrays are null");
        return;
    }
    
    jsize hueLen = env->GetArrayLength(hueShift);
    jsize satLen = env->GetArrayLength(saturation);
    jsize lumLen = env->GetArrayLength(luminance);
    
    if (hueLen != 8 || satLen != 8 || lumLen != 8) {
        LOGE("setHSL: Invalid array lengths");
        params->hslParams->enableHSL = false;
        return;
    }
    
    jfloat* hueData = env->GetFloatArrayElements(hueShift, nullptr);
    jfloat* satData = env->GetFloatArrayElements(saturation, nullptr);
    jfloat* lumData = env->GetFloatArrayElements(luminance, nullptr);
    
    if (!hueData || !satData || !lumData) {
        if (hueData) env->ReleaseFloatArrayElements(hueShift, hueData, JNI_ABORT);
        if (satData) env->ReleaseFloatArrayElements(saturation, satData, JNI_ABORT);
        if (lumData) env->ReleaseFloatArrayElements(luminance, lumData, JNI_ABORT);
        params->hslParams->enableHSL = false;
        return;
    }
    
    try {
        for (int i = 0; i < 8; ++i) {
            params->hslParams->hueShift[i] = hueData[i];
            params->hslParams->saturation[i] = satData[i];
            params->hslParams->luminance[i] = lumData[i];
        }
        params->hslParams->enableHSL = true;
    } catch (...) {
        LOGE("setHSL: Exception during copy");
        params->hslParams->enableHSL = false;
    }
    
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
    
    BasicAdjustmentParams* params = reinterpret_cast<BasicAdjustmentParams*>(nativeHandle);
    if (params) {
        delete params;
    }
}

} // extern "C"
