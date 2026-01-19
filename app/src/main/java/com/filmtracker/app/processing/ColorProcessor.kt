package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.native.BasicAdjustmentParamsNative
import com.filmtracker.app.native.ImageConverterNative
import com.filmtracker.app.native.ImageProcessorEngineNative

/**
 * 阶段 3：色彩处理器
 * 
 * 处理参数：
 * - 色温 (temperature)
 * - 色调 (tint)
 * - 饱和度 (saturation)
 * - 自然饱和度 (vibrance)
 * - HSL 调整 (enableHSL, hslHueShift, hslSaturation, hslLuminance)
 * - 色彩分级 (gradingHighlightsTemp/Tint, gradingMidtonesTemp/Tint, gradingShadowsTemp/Tint, gradingBlending, gradingBalance)
 * 
 * 特点：
 * - 使用现有的颜色处理代码
 * - 不缓存（简单的颜色变换，< 8ms）
 */
class ColorProcessor : BaseStageProcessor(ProcessingStage.COLOR) {
    
    companion object {
        private const val TAG = "ColorProcessor"
        
        // 默认值常量（Adobe 标准）
        private const val DEFAULT_TEMPERATURE = 0.0f
        private const val DEFAULT_TINT = 0.0f
        private const val DEFAULT_SATURATION = 0.0f  // Adobe 标准：0 为不变
        private const val DEFAULT_VIBRANCE = 0.0f
        private const val DEFAULT_GRADING_TEMP = 0.0f
        private const val DEFAULT_GRADING_TINT = 0.0f
        private const val DEFAULT_GRADING_BLENDING = 50.0f
        private const val DEFAULT_GRADING_BALANCE = 0.0f
        
        // 浮点数比较容差
        private const val EPSILON = 0.001f
    }
    
    // Native 处理器（延迟初始化）
    private val processorEngine by lazy { ImageProcessorEngineNative() }
    private val imageConverter by lazy { ImageConverterNative() }
    
    override fun areParamsDefault(params: BasicAdjustmentParams): Boolean {
        // 检查基础色彩参数
        if (!isNearDefault(params.temperature, DEFAULT_TEMPERATURE) ||
            !isNearDefault(params.tint, DEFAULT_TINT) ||
            !isNearDefault(params.saturation, DEFAULT_SATURATION) ||
            !isNearDefault(params.vibrance, DEFAULT_VIBRANCE)) {
            return false
        }
        
        // 检查 HSL 参数
        if (params.enableHSL) {
            if (!isArrayAllZero(params.hslHueShift) ||
                !isArrayAllZero(params.hslSaturation) ||
                !isArrayAllZero(params.hslLuminance)) {
                return false
            }
        }
        
        // 检查色彩分级参数
        if (!isNearDefault(params.gradingHighlightsTemp, DEFAULT_GRADING_TEMP) ||
            !isNearDefault(params.gradingHighlightsTint, DEFAULT_GRADING_TINT) ||
            !isNearDefault(params.gradingMidtonesTemp, DEFAULT_GRADING_TEMP) ||
            !isNearDefault(params.gradingMidtonesTint, DEFAULT_GRADING_TINT) ||
            !isNearDefault(params.gradingShadowsTemp, DEFAULT_GRADING_TEMP) ||
            !isNearDefault(params.gradingShadowsTint, DEFAULT_GRADING_TINT)) {
            return false
        }
        
        return true
    }
    
    override fun process(input: Bitmap, params: BasicAdjustmentParams): Bitmap? {
        val startTime = System.currentTimeMillis()
        
        // 如果参数都是默认值，直接返回输入（跳过处理）
        if (areParamsDefault(params)) {
            Log.d(TAG, "Skipping COLOR stage - all params are default")
            return input.copy(input.config, true)
        }
        
        var linearImage: com.filmtracker.app.native.LinearImageNative? = null
        var nativeParams: BasicAdjustmentParamsNative? = null
        
        try {
            // 转换到线性空间
            linearImage = imageConverter.bitmapToLinear(input)
            if (linearImage == null) {
                Log.e(TAG, "Failed to convert bitmap to linear")
                return null
            }
            
            // 创建 Native 参数
            nativeParams = createNativeParams(params)
            
            // 应用自然饱和度（清晰度在 EFFECTS 阶段处理）
            if (!isNearDefault(params.vibrance, DEFAULT_VIBRANCE)) {
                processorEngine.applyPresence(linearImage, 0f, params.vibrance)
            }
            
            // 应用 HSL 调整
            if (params.enableHSL) {
                processorEngine.applyHSL(linearImage, nativeParams)
            }
            
            // 应用颜色调整（色温、色调、饱和度、分级）
            processorEngine.applyColorAdjustments(linearImage, nativeParams)
            
            // 转换回 Bitmap
            val result = imageConverter.linearToBitmap(linearImage)
            
            val executionTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "COLOR stage completed in ${executionTime}ms")
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in COLOR processing", e)
            return null
        } finally {
            linearImage?.let { imageConverter.release(it) }
            nativeParams?.release()
        }
    }
    
    /**
     * 创建 Native 参数对象
     */
    private fun createNativeParams(params: BasicAdjustmentParams): BasicAdjustmentParamsNative {
        val nativeParams = BasicAdjustmentParamsNative.create()
        
        // 将 Adobe 标准参数转换为 Native 层需要的乘数
        val saturationMultiplier = com.filmtracker.app.util.AdobeParameterConverter.saturationToMultiplier(params.saturation)
        
        // 设置参数
        nativeParams.setParams(
            0f, 1f, saturationMultiplier,  // 曝光、对比度、饱和度（使用转换后的乘数）
            0f, 0f, 0f, 0f,  // 色调调整（在 TONE_BASE 阶段处理）
            0f, params.vibrance,  // 清晰度（在 EFFECTS 阶段处理）、自然饱和度
            params.temperature, params.tint,  // 色温、色调
            params.gradingHighlightsTemp, params.gradingHighlightsTint,
            params.gradingMidtonesTemp, params.gradingMidtonesTint,
            params.gradingShadowsTemp, params.gradingShadowsTint,
            params.gradingBlending, params.gradingBalance,
            0f, 0f, 0f, 0f, 0f, 0f  // 效果和细节（在其他阶段处理）
        )
        
        // 设置 HSL 参数
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
    
    /**
     * 检查浮点数是否接近默认值
     */
    private fun isNearDefault(value: Float, defaultValue: Float): Boolean {
        return kotlin.math.abs(value - defaultValue) < EPSILON
    }
    
    /**
     * 检查数组是否全为零
     */
    private fun isArrayAllZero(array: FloatArray): Boolean {
        return array.all { kotlin.math.abs(it) < EPSILON }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        processorEngine.release()
    }
}
