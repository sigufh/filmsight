package com.filmtracker.app.domain.usecase

import android.graphics.Bitmap
import com.filmtracker.app.domain.model.AdjustmentParams
import com.filmtracker.app.domain.repository.ProcessingRepository

/**
 * 应用调整参数用例
 */
class ApplyAdjustmentsUseCase(
    private val repository: ProcessingRepository
) {
    suspend operator fun invoke(
        image: Bitmap,
        params: AdjustmentParams
    ): Result<Bitmap> {
        return repository.applyAdjustments(image, params)
    }
}
