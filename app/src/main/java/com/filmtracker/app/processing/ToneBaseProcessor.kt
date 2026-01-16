package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.native.BasicAdjustmentParamsNative
import com.filmtracker.app.native.ImageConverterNative
import com.filmtracker.app.native.ImageProcessorEngineNative
import com.filmtracker.app.native.ParallelProcessorNative

/**
 * 阶段 1：基础影调处理器
 * 
 * 处理参数：
 * - 曝光 (globalExposure)
 * - 高光 (highlights)
 * - 阴影 (shadows)
 * - 白场 (whites)
 * - 黑场 (blacks)
 * - 对比度 (contrast)
 * 
 * 特点：
 * - 使用现有的 SIMD 优化代码
 * - 不缓存（实时计算足够快，< 5ms）
 * - 支持并行处理
 */
class ToneBaseProcessor : BaseStageProcessor(ProcessingStage.TONE_BASE) {
    
    companion object {
        private const val TAG = "ToneBaseProcessor"
        
        // 默认值常量
        private const val DEFAULT_EXPOSURE = 0.0f
        private const val DEFAULT_CONTRAST = 1.0f
        private const val DEFAULT_HIGHLIGHTS = 0.0f
        private const val DEFAULT_SHADOWS = 0.0f
        private const val DEFAULT_WHITES = 0.0f
        private const val DEFAULT_BLACKS = 0.0f
        
        // 浮点数比较容差
        private const val EPSILON = 0.001f
    }
    
    // Native 处理器（延迟初始化）
    private val processorEngine by lazy { ImageProcessorEngineNative() }
    private val imageConverter by lazy { ImageConverterNative() }
    private val parallelProcessor by lazy { ParallelProcessorNative() }
    
    // 是否使用并行处理
    private var useParallelProcessing: Boolean = true
    
    /**
     * 设置是否使用并行处理
     */
    fun setParallelProcessing(enabled: Boolean) {
        useParallelProcessing = enabled
    }
    
    override fun areParamsDefault(params: BasicAdjustmentParams): Boolean {
        return isNearDefault(params.globalExposure, DEFAULT_EXPOSURE) &&
               isNearDefault(params.contrast, DEFAULT_CONTRAST) &&
               isNearDefault(params.highlights, DEFAULT_HIGHLIGHTS) &&
               isNearDefault(params.shadows, DEFAULT_SHADOWS) &&
               isNearDefault(params.whites, DEFAULT_WHITES) &&
               isNearDefault(params.blacks, DEFAULT_BLACKS)
    }
    
    override fun process(input: Bitmap, params: BasicAdjustmentParams): Bitmap? {
        val startTime = System.currentTimeMillis()
        
        // 如果参数都是默认值，直接返回输入（跳过处理）
        if (areParamsDefault(params)) {
            Log.d(TAG, "Skipping TONE_BASE stage - all params are default")
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
            
            // 应用基础影调调整
            if (useParallelProcessing) {
                applyWithParallelProcessing(linearImage, params)
            } else {
                applyWithSequentialProcessing(linearImage, params)
            }
            
            // 转换回 Bitmap
            val result = imageConverter.linearToBitmap(linearImage)
            
            val executionTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "TONE_BASE stage completed in ${executionTime}ms")
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in TONE_BASE processing", e)
            return null
        } finally {
            linearImage?.let { imageConverter.release(it) }
            nativeParams?.release()
        }
    }
    
    /**
     * 使用并行处理器处理（多线程 + SIMD）
     */
    private fun applyWithParallelProcessing(
        linearImage: com.filmtracker.app.native.LinearImageNative,
        params: BasicAdjustmentParams
    ) {
        // 创建临时参数对象
        val nativeParams = BasicAdjustmentParamsNative.create()
        try {
            // 设置基础调整参数（曝光、对比度、饱和度）
            // 注意：饱和度在 COLOR 阶段处理，这里设为 1.0
            nativeParams.setParams(
                params.globalExposure, 
                params.contrast, 
                1.0f,  // 饱和度在 COLOR 阶段处理
                0f, 0f, 0f, 0f,  // 色调调整单独处理
                0f, 0f,  // 清晰度和自然饱和度在其他阶段处理
                0f, 0f,  // 色温色调在 COLOR 阶段处理
                0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,  // 分级在 COLOR 阶段处理
                0f, 0f, 0f, 0f, 0f, 0f  // 效果和细节在其他阶段处理
            )
            
            // 使用并行处理器处理基础调整（曝光、对比度）
            parallelProcessor.process(
                linearImage.nativePtr,
                linearImage.nativePtr,  // 原地处理
                nativeParams.handle
            )
        } finally {
            nativeParams.release()
        }
        
        // 使用原引擎处理色调调整（高光、阴影、白场、黑场）
        processorEngine.applyToneAdjustments(
            linearImage,
            params.highlights,
            params.shadows,
            params.whites,
            params.blacks
        )
    }
    
    /**
     * 使用顺序处理器处理（单线程）
     */
    private fun applyWithSequentialProcessing(
        linearImage: com.filmtracker.app.native.LinearImageNative,
        params: BasicAdjustmentParams
    ) {
        // 应用基础调整（曝光、对比度）
        // 注意：饱和度在 COLOR 阶段处理
        processorEngine.applyBasicAdjustments(
            linearImage,
            params.globalExposure,
            params.contrast,
            1.0f  // 饱和度在 COLOR 阶段处理
        )
        
        // 应用色调调整（高光、阴影、白场、黑场）
        processorEngine.applyToneAdjustments(
            linearImage,
            params.highlights,
            params.shadows,
            params.whites,
            params.blacks
        )
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
        parallelProcessor.release()
        processorEngine.release()
    }
}
