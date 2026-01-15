package com.filmtracker.app.data.source.native

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.native.BasicAdjustmentParamsNative
import com.filmtracker.app.native.ImageConverterNative
import com.filmtracker.app.native.ImageProcessorEngineNative
import com.filmtracker.app.native.LinearImageNative

/**
 * Native 图像处理器
 * 封装所有 Native 调用，提供简洁的接口
 */
class NativeImageProcessor {
    
    private val processorEngine = ImageProcessorEngineNative()
    private val imageConverter = ImageConverterNative()
    
    /**
     * 处理图像
     */
    fun process(
        originalBitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        var nativeParams: BasicAdjustmentParamsNative? = null
        
        try {
            // 转换到线性空间
            val linearImage = imageConverter.bitmapToLinear(originalBitmap) 
                ?: return null
            
            // 应用基础调整
            applyBasicAdjustments(linearImage, params)
            
            // 应用高级调整（需要 native params）
            if (needsNativeParams(params)) {
                nativeParams = convertToNativeParams(params)
                applyAdvancedAdjustments(linearImage, params, nativeParams)
            }
            
            // 转换回 sRGB
            val result = imageConverter.linearToBitmap(linearImage)
            imageConverter.release(linearImage)
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            return null
        } finally {
            nativeParams?.release()
        }
    }
    
    private fun applyBasicAdjustments(
        linearImage: LinearImageNative,
        params: BasicAdjustmentParams
    ) {
        processorEngine.applyBasicAdjustments(
            linearImage,
            params.globalExposure,
            params.contrast,
            params.saturation
        )
        
        processorEngine.applyToneAdjustments(
            linearImage,
            params.highlights,
            params.shadows,
            params.whites,
            params.blacks
        )
        
        processorEngine.applyPresence(
            linearImage,
            params.clarity,
            params.vibrance
        )
    }
    
    private fun applyAdvancedAdjustments(
        linearImage: LinearImageNative,
        params: BasicAdjustmentParams,
        nativeParams: BasicAdjustmentParamsNative
    ) {
        // 曲线
        if (params.enableRgbCurve || params.enableRedCurve || 
            params.enableGreenCurve || params.enableBlueCurve) {
            processorEngine.applyToneCurves(linearImage, nativeParams)
        }
        
        // HSL
        if (params.enableHSL) {
            processorEngine.applyHSL(linearImage, nativeParams)
        }
        
        // 颜色调整
        processorEngine.applyColorAdjustments(linearImage, nativeParams)
        
        // 效果
        processorEngine.applyEffects(linearImage, nativeParams)
        
        // 细节
        processorEngine.applyDetails(linearImage, nativeParams)
    }
    
    private fun needsNativeParams(params: BasicAdjustmentParams): Boolean {
        return params.enableRgbCurve || params.enableRedCurve || 
               params.enableGreenCurve || params.enableBlueCurve ||
               params.enableHSL ||
               params.temperature != 0f || params.tint != 0f ||
               params.gradingHighlightsTemp != 0f || params.gradingHighlightsTint != 0f ||
               params.gradingMidtonesTemp != 0f || params.gradingMidtonesTint != 0f ||
               params.gradingShadowsTemp != 0f || params.gradingShadowsTint != 0f ||
               params.texture != 0f || params.dehaze != 0f || 
               params.vignette != 0f || params.grain != 0f ||
               params.sharpening != 0f || params.noiseReduction != 0f
    }
    
    private fun convertToNativeParams(params: BasicAdjustmentParams): BasicAdjustmentParamsNative {
        val nativeParams = BasicAdjustmentParamsNative.create()
        
        nativeParams.setParams(
            params.globalExposure, params.contrast, params.saturation,
            params.highlights, params.shadows, params.whites, params.blacks,
            params.clarity, params.vibrance,
            params.temperature, params.tint,
            params.gradingHighlightsTemp, params.gradingHighlightsTint,
            params.gradingMidtonesTemp, params.gradingMidtonesTint,
            params.gradingShadowsTemp, params.gradingShadowsTint,
            params.gradingBlending, params.gradingBalance,
            params.texture, params.dehaze, params.vignette, params.grain,
            params.sharpening, params.noiseReduction
        )
        
        nativeParams.setAllToneCurves(
            params.enableRgbCurve, params.rgbCurvePoints,
            params.enableRedCurve, params.redCurvePoints,
            params.enableGreenCurve, params.greenCurvePoints,
            params.enableBlueCurve, params.blueCurvePoints
        )
        
        // 安全设置 HSL
        try {
            val hueShift = if (params.hslHueShift.size == 8) params.hslHueShift 
                          else FloatArray(8) { 0f }
            val saturation = if (params.hslSaturation.size == 8) params.hslSaturation 
                            else FloatArray(8) { 0f }
            val luminance = if (params.hslLuminance.size == 8) params.hslLuminance 
                           else FloatArray(8) { 0f }
            
            nativeParams.setHSL(params.enableHSL, hueShift, saturation, luminance)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting HSL parameters", e)
            nativeParams.setHSL(false, FloatArray(8) { 0f }, FloatArray(8) { 0f }, FloatArray(8) { 0f })
        }
        
        return nativeParams
    }
    
    companion object {
        private const val TAG = "NativeImageProcessor"
    }
}
