package com.filmtracker.app.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.filmtracker.app.data.source.local.FileImageSource
import com.filmtracker.app.data.source.native.NativeRawProcessor
import com.filmtracker.app.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图像仓储实现
 */
class ImageRepositoryImpl(
    private val fileSource: FileImageSource,
    private val rawProcessor: NativeRawProcessor
) : ImageRepository {
    
    override suspend fun loadImage(uri: Uri, previewMode: Boolean): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            // 检查是否是 RAW 文件
            if (fileSource.isRawFile(uri)) {
                val filePath = fileSource.getFilePath(uri)
                if (filePath != null) {
                    val preview = rawProcessor.extractPreview(filePath)
                    if (preview != null) {
                        var scaledPreview = preview
                        if (previewMode && (preview.width > 1920 || preview.height > 1920)) {
                            val scale = minOf(1920f / preview.width, 1920f / preview.height)
                            val scaledWidth = (preview.width * scale).toInt()
                            val scaledHeight = (preview.height * scale).toInt()
                            scaledPreview = Bitmap.createScaledBitmap(preview, scaledWidth, scaledHeight, true)
                        }
                        return@withContext Result.success(scaledPreview)
                    }
                }
            }
            
            // 加载普通图像
            val bitmap = fileSource.loadImage(uri, previewMode)
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(Exception("Failed to load image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loadRawPreview(filePath: String): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val preview = rawProcessor.extractPreview(filePath)
            if (preview != null) {
                Result.success(preview)
            } else {
                Result.failure(Exception("Failed to extract RAW preview"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
