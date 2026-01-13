package com.filmtracker.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.filmtracker.app.data.FilmParams
import com.filmtracker.app.native.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * 图像处理器
 * 协调 Native 层处理流程
 */
class ImageProcessor(private val context: Context? = null) {
    
    private val rawProcessor = RawProcessorNative()
    private val filmEngine = FilmEngineNative()
    private val imageConverter = ImageConverterNative()
    
    /**
     * 处理图像（支持RAW和普通图片）
     */
    suspend fun processImage(
        imageUri: String,
        params: FilmParams
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // 尝试作为RAW文件处理
            val linearImage = rawProcessor.loadRaw(imageUri)
            if (linearImage != null) {
                return@withContext processLinearImage(linearImage, params)
            }
            
            // 如果不是RAW，尝试作为普通图片处理
            if (context != null) {
                val uri = Uri.parse(imageUri)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bitmap != null) {
                    return@withContext processBitmap(bitmap, params)
                }
            }
            
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 处理 RAW 图像
     */
    suspend fun processRaw(
        filePath: String,
        params: FilmParams
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val linearImage = rawProcessor.loadRaw(filePath) ?: return@withContext null
            processLinearImage(linearImage, params)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 处理线性图像
     */
    private suspend fun processLinearImage(
        linearImage: LinearImageNative,
        params: FilmParams
    ): Bitmap? {
        try {
            val nativeParams = convertToNativeParams(params)
            val processedImage = filmEngine.process(linearImage, nativeParams) ?: return null
            val bitmap = imageConverter.linearToBitmap(processedImage)
            imageConverter.release(linearImage)
            imageConverter.release(processedImage)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 处理普通Bitmap（转换为LinearImage后处理）
     */
    private suspend fun processBitmap(
        bitmap: Bitmap,
        params: FilmParams
    ): Bitmap? {
        try {
            // 将Bitmap转换为LinearImage
            val linearImage = imageConverter.bitmapToLinear(bitmap) ?: return null
            val nativeParams = convertToNativeParams(params)
            val processedImage = filmEngine.process(linearImage, nativeParams) ?: return null
            val result = imageConverter.linearToBitmap(processedImage)
            imageConverter.release(linearImage)
            imageConverter.release(processedImage)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 处理普通图像（JPEG/PNG等）
     * 从URI加载，转换为LinearImage，然后处理
     */
    suspend fun processImage(
        imageUri: String,
        params: FilmParams
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            if (context == null) return@withContext null
            
            // 1. 从URI加载Bitmap
            val uri = Uri.parse(imageUri)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) return@withContext null
            
            // 2. 转换为LinearImage（简化实现：直接使用原始图像）
            // 注意：这里需要将sRGB转换为线性域，但为了简化，我们假设图像已经是线性域
            // 实际应该使用imageConverter.bitmapToLinear，但当前可能未实现
            val linearImage = imageConverter.bitmapToLinear(originalBitmap) 
                ?: return@withContext null
            
            // 3. 转换参数
            val nativeParams = convertToNativeParams(params)
            
            // 4. 应用胶片模拟
            val processedImage = filmEngine.process(linearImage, nativeParams)
                ?: return@withContext null
            
            // 5. 转换为Bitmap
            val bitmap = imageConverter.linearToBitmap(processedImage)
            
            // 6. 清理资源
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
