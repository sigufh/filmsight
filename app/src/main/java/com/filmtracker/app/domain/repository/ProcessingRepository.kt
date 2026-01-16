package com.filmtracker.app.domain.repository

import android.graphics.Bitmap
import com.filmtracker.app.domain.model.AdjustmentParams

/**
 * 图像处理仓储接口
 */
interface ProcessingRepository {
    /**
     * 应用调整参数到图像（预览模式）
     */
    suspend fun applyAdjustments(
        image: Bitmap,
        params: AdjustmentParams
    ): Result<Bitmap>
    
    /**
     * 导出图像（完整分辨率）
     */
    suspend fun exportImage(
        image: Bitmap,
        params: AdjustmentParams
    ): Result<Bitmap>
}
