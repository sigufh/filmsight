package com.filmtracker.app.domain.repository

import android.graphics.Bitmap
import com.filmtracker.app.domain.model.AdjustmentParams

/**
 * 图像处理仓储接口
 */
interface ProcessingRepository {
    /**
     * 应用调整参数到图像
     */
    suspend fun applyAdjustments(
        image: Bitmap,
        params: AdjustmentParams
    ): Result<Bitmap>
}
