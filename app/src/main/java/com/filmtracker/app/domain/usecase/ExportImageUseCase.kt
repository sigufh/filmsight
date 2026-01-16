package com.filmtracker.app.domain.usecase

import android.graphics.Bitmap
import com.filmtracker.app.domain.model.AdjustmentParams
import com.filmtracker.app.domain.repository.ProcessingRepository

/**
 * 导出图像用例
 * 使用完整分辨率处理图像
 */
class ExportImageUseCase(
    private val repository: ProcessingRepository
) {
    suspend operator fun invoke(
        image: Bitmap,
        params: AdjustmentParams
    ): Result<Bitmap> {
        return repository.exportImage(image, params)
    }
}
