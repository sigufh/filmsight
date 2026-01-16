package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.native.BasicAdjustmentParamsNative
import com.filmtracker.app.native.ImageConverterNative
import com.filmtracker.app.native.ImageProcessorEngineNative

/**
 * 阶段 2：曲线处理器
 * 
 * 处理参数：
 * - RGB 曲线 (enableRgbCurve, rgbCurvePoints)
 * - 红色通道曲线 (enableRedCurve, redCurvePoints)
 * - 绿色通道曲线 (enableGreenCurve, greenCurvePoints)
 * - 蓝色通道曲线 (enableBlueCurve, blueCurvePoints)
 * 
 * 特点：
 * - 使用 LUT 查找优化
 * - 不缓存（LUT 查找非常快，< 3ms）
 */
class CurvesProcessor : BaseStageProcessor(ProcessingStage.CURVES) {
    
    companion object {
        private const val TAG = "CurvesProcessor"
        
        // 默认曲线点（线性曲线）
        private val DEFAULT_CURVE_POINTS = listOf(Pair(0f, 0f), Pair(1f, 1f))
    }
    
    // Native 处理器（延迟初始化）
    private val processorEngine by lazy { ImageProcessorEngineNative() }
    private val imageConverter by lazy { ImageConverterNative() }
    
    override fun areParamsDefault(params: BasicAdjustmentParams): Boolean {
        // 如果所有曲线都未启用，则为默认值
        if (!params.enableRgbCurve && !params.enableRedCurve && 
            !params.enableGreenCurve && !params.enableBlueCurve) {
            return true
        }
        
        // 检查启用的曲线是否为默认曲线
        if (params.enableRgbCurve && !isDefaultCurve(params.rgbCurvePoints)) {
            return false
        }
        if (params.enableRedCurve && !isDefaultCurve(params.redCurvePoints)) {
            return false
        }
        if (params.enableGreenCurve && !isDefaultCurve(params.greenCurvePoints)) {
            return false
        }
        if (params.enableBlueCurve && !isDefaultCurve(params.blueCurvePoints)) {
            return false
        }
        
        return true
    }
    
    override fun process(input: Bitmap, params: BasicAdjustmentParams): Bitmap? {
        val startTime = System.currentTimeMillis()
        
        // 如果参数都是默认值，直接返回输入（跳过处理）
        if (areParamsDefault(params)) {
            Log.d(TAG, "Skipping CURVES stage - all params are default")
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
            
            // 应用曲线调整
            processorEngine.applyToneCurves(linearImage, nativeParams)
            
            // 转换回 Bitmap
            val result = imageConverter.linearToBitmap(linearImage)
            
            val executionTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "CURVES stage completed in ${executionTime}ms")
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in CURVES processing", e)
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
        
        // 设置基础参数为默认值（只处理曲线）
        nativeParams.setParams(
            0f, 1f, 1f,  // 曝光、对比度、饱和度
            0f, 0f, 0f, 0f,  // 色调调整
            0f, 0f,  // 清晰度、自然饱和度
            0f, 0f,  // 色温、色调
            0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,  // 分级
            0f, 0f, 0f, 0f, 0f, 0f  // 效果和细节
        )
        
        // 设置曲线参数
        nativeParams.setAllToneCurves(
            params.enableRgbCurve, params.rgbCurvePoints,
            params.enableRedCurve, params.redCurvePoints,
            params.enableGreenCurve, params.greenCurvePoints,
            params.enableBlueCurve, params.blueCurvePoints
        )
        
        return nativeParams
    }
    
    /**
     * 检查曲线是否为默认曲线（线性）
     */
    private fun isDefaultCurve(curvePoints: List<Pair<Float, Float>>): Boolean {
        if (curvePoints.size != 2) return false
        
        val first = curvePoints[0]
        val last = curvePoints[1]
        
        return isNearEqual(first.first, 0f) && isNearEqual(first.second, 0f) &&
               isNearEqual(last.first, 1f) && isNearEqual(last.second, 1f)
    }
    
    /**
     * 检查浮点数是否接近
     */
    private fun isNearEqual(a: Float, b: Float, epsilon: Float = 0.001f): Boolean {
        return kotlin.math.abs(a - b) < epsilon
    }
    
    /**
     * 释放资源
     */
    fun release() {
        processorEngine.release()
    }
}
