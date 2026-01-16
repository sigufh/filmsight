package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.native.BasicAdjustmentParamsNative
import com.filmtracker.app.native.ImageConverterNative
import com.filmtracker.app.native.ImageProcessorEngineNative

/**
 * 阶段 5：细节处理器
 * 
 * 处理参数：
 * - 锐化 (sharpening) - 需要卷积运算
 * - 降噪 (noiseReduction) - 计算最密集
 * 
 * 特点：
 * - 计算最密集的阶段
 * - **需要缓存**（约 30ms）
 * - 锐化和降噪都需要卷积运算
 */
class DetailsProcessor : BaseStageProcessor(ProcessingStage.DETAILS) {
    
    companion object {
        private const val TAG = "DetailsProcessor"
        
        // 默认值常量
        private const val DEFAULT_SHARPENING = 0.0f
        private const val DEFAULT_NOISE_REDUCTION = 0.0f
        
        // 浮点数比较容差
        private const val EPSILON = 0.001f
    }
    
    // Native 处理器（延迟初始化）
    private val processorEngine by lazy { ImageProcessorEngineNative() }
    private val imageConverter by lazy { ImageConverterNative() }
    
    override fun areParamsDefault(params: BasicAdjustmentParams): Boolean {
        return isNearDefault(params.sharpening, DEFAULT_SHARPENING) &&
               isNearDefault(params.noiseReduction, DEFAULT_NOISE_REDUCTION)
    }
    
    override fun process(input: Bitmap, params: BasicAdjustmentParams): Bitmap? {
        val startTime = System.currentTimeMillis()
        
        // 如果参数都是默认值，直接返回输入（跳过处理）
        if (areParamsDefault(params)) {
            Log.d(TAG, "Skipping DETAILS stage - all params are default")
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
            
            // 应用细节调整（锐化、降噪）
            processorEngine.applyDetails(linearImage, nativeParams)
            
            // 转换回 Bitmap
            val result = imageConverter.linearToBitmap(linearImage)
            
            val executionTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "DETAILS stage completed in ${executionTime}ms (shouldCache: $shouldCache)")
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in DETAILS processing", e)
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
        
        // 设置参数（只设置细节相关参数）
        nativeParams.setParams(
            0f, 1f, 1f,  // 曝光、对比度、饱和度（在其他阶段处理）
            0f, 0f, 0f, 0f,  // 色调调整（在 TONE_BASE 阶段处理）
            0f, 0f,  // 清晰度、自然饱和度（在其他阶段处理）
            0f, 0f,  // 色温、色调（在 COLOR 阶段处理）
            0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,  // 分级（在 COLOR 阶段处理）
            0f, 0f, 0f, 0f,  // 纹理、去雾、晕影、颗粒（在 EFFECTS 阶段处理）
            params.sharpening, params.noiseReduction
        )
        
        return nativeParams
    }
    
    /**
     * 检查浮点数是否接近默认值
     */
    private fun isNearDefault(value: Float, defaultValue: Float): Boolean {
        return kotlin.math.abs(value - defaultValue) < EPSILON
    }
    
    /**
     * 释放资源
     */
    fun release() {
        processorEngine.release()
    }
}
