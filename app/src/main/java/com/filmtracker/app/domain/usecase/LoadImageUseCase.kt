package com.filmtracker.app.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.filmtracker.app.domain.repository.ImageRepository

/**
 * 加载图像用例
 */
class LoadImageUseCase(
    private val repository: ImageRepository
) {
    suspend operator fun invoke(
        uri: Uri,
        previewMode: Boolean = true
    ): Result<Bitmap> {
        return repository.loadImage(uri, previewMode)
    }
}
