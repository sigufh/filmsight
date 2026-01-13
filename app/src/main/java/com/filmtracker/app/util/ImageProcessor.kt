package com.filmtracker.app.util

import android.graphics.Bitmap
import com.filmtracker.app.data.FilmParams
import com.filmtracker.app.native.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图像处理器
 * 协调 Native 层处理流程
 */
class ImageProcessor {
    
    private val rawProcessor = RawProcessorNative()
    private val filmEngine = FilmEngineNative()
    private val imageConverter = ImageConverterNative()
    
    /**
     * 处理 RAW 图像
     */
    suspend fun processRaw(
        filePath: String,
        params: FilmParams
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // 1. 加载 RAW
            val linearImage = rawProcessor.loadRaw(filePath) ?: return@withContext null
            
            // 2. 转换参数（Kotlin -> Native）
            val nativeParams = convertToNativeParams(params)
            
            // 3. 应用胶片模拟
            val processedImage = filmEngine.process(linearImage, nativeParams) 
                ?: return@withContext null
            
            // 4. 转换为 Bitmap
            val bitmap = imageConverter.linearToBitmap(processedImage)
            
            // 5. 清理资源
            imageConverter.release(linearImage)
            imageConverter.release(processedImage)
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 转换参数到 Native 格式
     */
    private fun convertToNativeParams(params: FilmParams): FilmParamsNative {
        val nativeParams = FilmParamsNative.create()
        
        // 基础参数
        nativeParams.setParams(
            params.globalExposure,
            params.contrast,
            params.saturation,
            params.highlights,
            params.shadows,
            params.whites,
            params.blacks,
            params.clarity,
            params.vibrance
        )
        
        // 色调曲线
        nativeParams.setToneCurves(
            params.enableRgbCurve, params.rgbCurve,
            params.enableRedCurve, params.redCurve,
            params.enableGreenCurve, params.greenCurve,
            params.enableBlueCurve, params.blueCurve
        )
        
        // HSL 调整
        nativeParams.setHSL(
            params.enableHSL,
            params.hslHueShift,
            params.hslSaturation,
            params.hslLuminance
        )
        
        return nativeParams
    }
}
