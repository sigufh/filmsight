package com.filmtracker.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.filmtracker.app.data.FilmParams
import com.filmtracker.app.native.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
     * @param previewMode 预览模式，如果为true则使用较低分辨率以提高性能
     */
    suspend fun processImage(
        imageUri: String,
        params: FilmParams,
        previewMode: Boolean = true
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            if (context == null) {
                android.util.Log.e("ImageProcessor", "Context is null")
                return@withContext null
            }
            
            val uri = Uri.parse(imageUri)
            
            // 检查文件扩展名，判断是否为RAW文件
            val isRawFile = isRawFileFormat(uri)
            
            // 如果是RAW文件，尝试作为RAW处理
            if (isRawFile) {
                try {
                    // 对于URI格式，需要先获取实际文件路径或复制到临时文件
                    android.util.Log.d("ImageProcessor", "Detected RAW file, getting file path...")
                    val filePath = getFilePathFromUri(context, uri)
                    if (filePath != null) {
                        android.util.Log.d("ImageProcessor", "Processing RAW file: $filePath")
                        val linearImage = rawProcessor.loadRaw(filePath)
                        android.util.Log.d("ImageProcessor", "RAW file loaded, processing linear image...")
                        if (linearImage != null) {
                            val result = processLinearImage(linearImage, params, previewMode)
                            android.util.Log.d("ImageProcessor", "RAW processing completed")
                            return@withContext result
                        } else {
                            android.util.Log.e("ImageProcessor", "Failed to load RAW file, linearImage is null")
                        }
                    } else {
                        android.util.Log.e("ImageProcessor", "Failed to get file path from URI")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ImageProcessor", "Failed to process RAW file", e)
                    e.printStackTrace()
                    // 继续尝试作为普通图片处理
                }
            }
            
            // 如果不是RAW或RAW处理失败，尝试作为普通图片处理
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
            var rgbaBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            
            // 预览模式：降低分辨率以提高性能
            if (previewMode && (rgbaBitmap.width > 1200 || rgbaBitmap.height > 1200)) {
                val scale = minOf(1200f / rgbaBitmap.width, 1200f / rgbaBitmap.height)
                val scaledWidth = (rgbaBitmap.width * scale).toInt()
                val scaledHeight = (rgbaBitmap.height * scale).toInt()
                rgbaBitmap = Bitmap.createScaledBitmap(rgbaBitmap, scaledWidth, scaledHeight, true)
                android.util.Log.d("ImageProcessor", "Preview mode: scaled to ${scaledWidth}x${scaledHeight}")
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
        params: FilmParams,
        previewMode: Boolean = true
    ): Bitmap? {
        try {
            val nativeParams = convertToNativeParams(params)
            val processedImage = filmEngine.process(linearImage, nativeParams) ?: return null
            var bitmap = imageConverter.linearToBitmap(processedImage)
            
            // 预览模式：如果图像太大，缩放以提高性能
            if (previewMode && bitmap != null && (bitmap.width > 1200 || bitmap.height > 1200)) {
                val scale = minOf(1200f / bitmap.width, 1200f / bitmap.height)
                val scaledWidth = (bitmap.width * scale).toInt()
                val scaledHeight = (bitmap.height * scale).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                android.util.Log.d("ImageProcessor", "Preview mode: scaled linear image to ${scaledWidth}x${scaledHeight}")
            }
            
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
    
    /**
     * 检查是否为RAW文件格式
     */
    private fun isRawFileFormat(uri: Uri): Boolean {
        val fileName = getFileName(uri)?.lowercase() ?: return false
        val rawExtensions = listOf(
            ".arw", ".cr2", ".cr3", ".nef", ".raf", ".orf", ".rw2", 
            ".pef", ".srw", ".dng", ".raw", ".3fr", ".ari", ".bay",
            ".cap", ".data", ".dcs", ".dcr", ".drf", ".eip", ".erf",
            ".fff", ".iiq", ".k25", ".kdc", ".mdc", ".mef", ".mos",
            ".mrw", ".nrw", ".obm", ".ptx", ".pxn", ".r3d", ".raf",
            ".raw", ".rwl", ".rwz", ".sr2", ".srf", ".srw", ".tif",
            ".x3f"
        )
        return rawExtensions.any { fileName.endsWith(it) }
    }
    
    /**
     * 从URI获取文件名
     */
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
                if (cut != -1) {
                    it.substring(cut + 1)
                } else {
                    it
                }
            }
        }
        return result
    }
    
    /**
     * 从URI获取文件路径（如果是file://）或复制到临时文件
     */
    private suspend fun getFilePathFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // 如果是file:// URI，直接返回路径
            if (uri.scheme == "file") {
                return@withContext uri.path
            }
            
            // 对于content:// URI，复制到临时文件
            val fileName = getFileName(uri) ?: "temp_raw.raw"
            val tempFile = File(context.cacheDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            if (tempFile.exists() && tempFile.length() > 0) {
                return@withContext tempFile.absolutePath
            }
            
            null
        } catch (e: Exception) {
            android.util.Log.e("ImageProcessor", "Failed to get file path from URI", e)
            null
        }
    }
}
