package com.filmtracker.app.domain.model

import android.graphics.Bitmap

/**
 * 图像处理结果
 */
sealed class ProcessingResult {
    data class Success(val bitmap: Bitmap) : ProcessingResult()
    data class Error(val exception: Exception) : ProcessingResult()
    object Loading : ProcessingResult()
}
