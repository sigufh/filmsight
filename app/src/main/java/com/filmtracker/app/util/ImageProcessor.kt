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
        try {
            val linearImage = imageConverter.bitmapToLinear(originalBitmap) ?: return@withContext null
            
            processorEngine.applyBasicAdjustments(linearImage, params.globalExposure, params.contrast, params.saturation)
            processorEngine.applyToneAdjustments(linearImage, params.highlights, params.shadows, params.whites, params.blacks)
            processorEngine.applyPresence(linearImage, params.clarity, params.vibrance)
            
            if (params.enableRgbCurve || params.enableRedCurve || params.enableGreenCurve || params.enableBlueCurve) {
                val nativeParams = convertToNativeParams(params)
                processorEngine.applyToneCurves(linearImage, nativeParams)
            }
            
            if (params.enableHSL) {
                val nativeParams = convertToNativeParams(params)
                processorEngine.applyHSL(linearImage, nativeParams)
            }
            
            val result = imageConverter.linearToBitmap(linearImage)
            imageConverter.release(linearImage)
            result
        } catch (e: Exception) {
            null
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
                        if (previewMode && (previewBitmap.width > 1200 || previewBitmap.height > 1200)) {
                            val scale = minOf(1200f / previewBitmap.width, 1200f / previewBitmap.height)
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
            
            if (previewMode && (rgbaBitmap.width > 1200 || rgbaBitmap.height > 1200)) {
                val scale = minOf(1200f / rgbaBitmap.width, 1200f / rgbaBitmap.height)
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
            params.clarity, params.vibrance
        )
        nativeParams.setToneCurves(
            params.enableRgbCurve, params.rgbCurve,
            params.enableRedCurve, params.redCurve,
            params.enableGreenCurve, params.greenCurve,
            params.enableBlueCurve, params.blueCurve
        )
        nativeParams.setHSL(params.enableHSL, params.hslHueShift, params.hslSaturation, params.hslLuminance)
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
