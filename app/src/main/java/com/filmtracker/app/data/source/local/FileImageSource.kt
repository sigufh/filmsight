package com.filmtracker.app.data.source.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 本地文件图像数据源
 */
class FileImageSource(private val context: Context) {
    
    /**
     * 从 URI 加载图像
     */
    suspend fun loadImage(uri: Uri, previewMode: Boolean = true): Bitmap? = withContext(Dispatchers.IO) {
        try {
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
    
    /**
     * 检查是否是 RAW 文件
     */
    fun isRawFile(uri: Uri): Boolean {
        val fileName = getFileName(uri)?.lowercase() ?: return false
        val rawExtensions = listOf(".arw", ".cr2", ".cr3", ".nef", ".raf", ".orf", ".rw2", ".pef", ".srw", ".dng", ".raw")
        return rawExtensions.any { fileName.endsWith(it) }
    }
    
    /**
     * 获取文件路径
     */
    suspend fun getFilePath(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file") return@withContext uri.path
            
            val fileName = getFileName(uri) ?: "temp_file"
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
    
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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
}
