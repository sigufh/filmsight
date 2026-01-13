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
            if (context == null) {
                android.util.Log.e("ImageProcessor", "Context is null")
                return@withContext null
            }
            
            // 尝试作为RAW文件处理
            try {
                val linearImage = rawProcessor.loadRaw(imageUri)
                if (linearImage != null) {
                    return@withContext processLinearImage(linearImage, params)
                }
            } catch (e: Exception) {
                android.util.Log.d("ImageProcessor", "Not a RAW file, trying as regular image")
            }
            
            // 如果不是RAW，尝试作为普通图片处理
            val uri = Uri.parse(imageUri)
            val inputStream: InputStream? = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                android.util.Log.e("ImageProcessor", "Failed to open input stream", e)
                null
            }
            
            if (inputStream == null) {
                android.util.Log.e("ImageProcessor", "Input stream is null")
                return@withContext null
            }
            
            val bitmap = try {
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                android.util.Log.e("ImageProcessor", "Failed to decode bitmap", e)
                null
            } finally {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    android.util.Log.e("ImageProcessor", "Failed to close stream", e)
                }
            }
            
            if (bitmap == null) {
                android.util.Log.e("ImageProcessor", "Decoded bitmap is null")
                return@withContext null
            }
            
            // 确保 Bitmap 格式正确
            val rgbaBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            
            return@withContext processBitmap(rgbaBitmap, params)
        } catch (e: Exception) {
            android.util.Log.e("ImageProcessor", "Error processing image", e)
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
