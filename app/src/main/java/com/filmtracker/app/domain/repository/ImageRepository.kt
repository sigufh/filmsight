package com.filmtracker.app.domain.repository

import android.graphics.Bitmap
import android.net.Uri

/**
 * 图像仓储接口
 */
interface ImageRepository {
    /**
     * 加载原始图像
     */
    suspend fun loadImage(uri: Uri, previewMode: Boolean = true): Result<Bitmap>
    
    /**
     * 加载 RAW 预览
     */
    suspend fun loadRawPreview(filePath: String): Result<Bitmap>
}
