package com.filmtracker.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.native.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ImageProcessor(private val context: Context? = null) {
    
    private val rawProcessor = RawProcessorNative()
    private val processorEngine = ImageProcessorEngineNative()
    private val imageConverter = ImageConverterNative()
    
    suspend fun loadRawPreview(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            rawProcessor.extractPreview(filePath)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun applyBasicAdjustmentsToOriginal(
        originalBitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? = withContext(Dispatchers.Default) {
        var nativeParams: BasicAdjustmentParamsNative? = null
        try {
            val linearImage = imageConverter.bitmapToLinear(originalBitmap) ?: return@withContext null
            
            // 基础调整
            processorEngine.applyBasicAdjustments(linearImage, params.globalExposure, params.contrast, params.saturation)
            processorEngine.applyToneAdjustments(linearImage, params.highlights, params.shadows, params.whites, params.blacks)
            processorEngine.applyPresence(linearImage, params.clarity, params.vibrance)
            
            // 需要 nativeParams 的调整
            val needsNativeParams = params.enableRgbCurve || params.enableRedCurve || 
                                   params.enableGreenCurve || params.enableBlueCurve ||
                                   params.enableHSL ||
                                   params.temperature != 0f || params.tint != 0f ||
                                   params.gradingHighlightsTemp != 0f || params.gradingHighlightsTint != 0f ||
                                   params.gradingMidtonesTemp != 0f || params.gradingMidtonesTint != 0f ||
                                   params.gradingShadowsTemp != 0f || params.gradingShadowsTint != 0f ||
                                   params.texture != 0f || params.dehaze != 0f || 
                                   params.vignette != 0f || params.grain != 0f ||
                                   params.sharpening != 0f || params.noiseReduction != 0f
            
            if (needsNativeParams) {
                nativeParams = convertToNativeParams(params)
                
                // 应用曲线
                if (params.enableRgbCurve || params.enableRedCurve || params.enableGreenCurve || params.enableBlueCurve) {
                    processorEngine.applyToneCurves(linearImage, nativeParams)
                }
                
                // 应用 HSL
                if (params.enableHSL) {
                    processorEngine.applyHSL(linearImage, nativeParams)
                }
                
                // 应用颜色调整
                processorEngine.applyColorAdjustments(linearImage, nativeParams)
                
                // 应用效果
                processorEngine.applyEffects(linearImage, nativeParams)
                
                // 应用细节
                processorEngine.applyDetails(linearImage, nativeParams)
            }
            
            val result = imageConverter.linearToBitmap(linearImage)
            imageConverter.release(linearImage)
            result
        } catch (e: Exception) {
            android.util.Log.e("ImageProcessor", "Error applying adjustments", e)
            null
        } finally {
            // 确保释放 native 资源
            nativeParams?.release()
        }
    }
    
    suspend fun loadOriginalImage(imageUri: String, previewMode: Boolean = true): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (context == null) return@withContext null
            
            val uri = Uri.parse(imageUri)
            val isRawFile = isRawFileFormat(uri)
            
            if (isRawFile) {
                val filePath = getFilePathFromUri(context, uri)
                if (filePath != null) {
                    val previewBitmap = rawProcessor.extractPreview(filePath)
                    if (previewBitmap != null) {
                        var scaledPreview = previewBitmap
                        // 预览模式：限制到 1920px，保持较高质量
                        // 非预览模式：使用原图全分辨率
                        if (previewMode && (previewBitmap.width > 1920 || previewBitmap.height > 1920)) {
                            val scale = minOf(1920f / previewBitmap.width, 1920f / previewBitmap.height)
                            val scaledWidth = (previewBitmap.width * scale).toInt()
                            val scaledHeight = (previewBitmap.height * scale).toInt()
                            scaledPreview = Bitmap.createScaledBitmap(previewBitmap, scaledWidth, scaledHeight, true)
                        }
                        return@withContext scaledPreview
                    }
                }
            }
            
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) return@withContext null
            
            var rgbaBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            
            // 预览模式：限制到 1920px
            // 非预览模式：使用原图全分辨率
            if (previewMode && (rgbaBitmap.width > 1920 || rgbaBitmap.height > 1920)) {
                val scale = minOf(1920f / rgbaBitmap.width, 1920f / rgbaBitmap.height)
                val scaledWidth = (rgbaBitmap.width * scale).toInt()
                val scaledHeight = (rgbaBitmap.height * scale).toInt()
                rgbaBitmap = Bitmap.createScaledBitmap(rgbaBitmap, scaledWidth, scaledHeight, true)
            }
            
            rgbaBitmap
        } catch (e: Exception) {
            null
        }
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
        
        // 安全地设置 HSL 参数
        try {
            // 确保数组不为空且长度正确
            val hueShift = if (params.hslHueShift.size == 8) params.hslHueShift else FloatArray(8) { 0.0f }
            val saturation = if (params.hslSaturation.size == 8) params.hslSaturation else FloatArray(8) { 0.0f }
            val luminance = if (params.hslLuminance.size == 8) params.hslLuminance else FloatArray(8) { 0.0f }
            
            nativeParams.setHSL(params.enableHSL, hueShift, saturation, luminance)
        } catch (e: Exception) {
            android.util.Log.e("ImageProcessor", "Error setting HSL parameters", e)
            // 如果出错，禁用 HSL
            nativeParams.setHSL(false, FloatArray(8) { 0.0f }, FloatArray(8) { 0.0f }, FloatArray(8) { 0.0f })
        }
        
        return nativeParams
    }
    
    private fun isRawFileFormat(uri: Uri): Boolean {
        val fileName = getFileName(uri)?.lowercase() ?: return false
        val rawExtensions = listOf(".arw", ".cr2", ".cr3", ".nef", ".raf", ".orf", ".rw2", ".pef", ".srw", ".dng", ".raw")
        return rawExtensions.any { fileName.endsWith(it) }
    }
    
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context?.contentResolver?.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) it.substring(cut + 1) else it
            }
        }
        return result
    }
    
    private suspend fun getFilePathFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file") return@withContext uri.path
            
            val fileName = getFileName(uri) ?: "temp_raw.raw"
            val tempFile = File(context.cacheDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            if (tempFile.exists() && tempFile.length() > 0) tempFile.absolutePath else null
        } catch (e: Exception) {
            null
        }
    }
}
