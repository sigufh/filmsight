package com.filmtracker.app.data.repository

import android.graphics.Bitmap
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import com.filmtracker.app.data.source.native.NativeImageProcessor
import com.filmtracker.app.domain.model.AdjustmentParams
import com.filmtracker.app.domain.repository.ProcessingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图像处理仓储实现
 */
class ProcessingRepositoryImpl(
    private val nativeProcessor: NativeImageProcessor,
    private val mapper: AdjustmentParamsMapper
) : ProcessingRepository {
    
    override suspend fun applyAdjustments(
        image: Bitmap,
        params: AdjustmentParams
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // 转换参数
            val dataParams = mapper.toData(params)
            
            // 处理图像
            val result = nativeProcessor.process(image, dataParams)
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Image processing failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
