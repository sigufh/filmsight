package com.filmtracker.app.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import com.filmtracker.app.data.source.native.NativeImageProcessor
import com.filmtracker.app.domain.model.AdjustmentParams
import com.filmtracker.app.domain.repository.ProcessingRepository
import com.filmtracker.app.processing.IncrementalRenderingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图像处理仓储实现
 * 
 * 支持两种处理模式：
 * 1. 增量渲染模式（默认）：使用 IncrementalRenderingEngine，支持阶段缓存和增量计算
 * 2. 直接处理模式：直接调用 NativeImageProcessor，用于导出和特殊场景
 */
class ProcessingRepositoryImpl(
    private val nativeProcessor: NativeImageProcessor,
    private val mapper: AdjustmentParamsMapper
) : ProcessingRepository {
    
    companion object {
        private const val TAG = "ProcessingRepositoryImpl"
    }
    
    // 增量渲染引擎（单例）
    private val incrementalEngine = IncrementalRenderingEngine.getInstance()
    
    // 是否启用增量渲染（默认启用）
    private var useIncrementalRendering = true
    
    init {
        // 默认启用预览模式
        nativeProcessor.setPreviewMode(enabled = true, maxWidth = 1920, maxHeight = 1080)
        
        // 启用增量渲染
        incrementalEngine.setIncrementalEnabled(true)
        incrementalEngine.setCacheEnabled(true)
        
        Log.d(TAG, "ProcessingRepositoryImpl initialized with incremental rendering enabled")
    }
    
    override suspend fun applyAdjustments(
        image: Bitmap,
        params: AdjustmentParams
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // 转换参数
            val dataParams = mapper.toData(params)
            
            // 根据模式选择处理方式
            val result = if (useIncrementalRendering) {
                // 使用增量渲染引擎
                val processingResult = incrementalEngine.process(image, dataParams)
                
                if (processingResult.success && processingResult.output != null) {
                    // 记录性能信息
                    Log.d(TAG, "Incremental processing: ${processingResult.totalTimeMs}ms, " +
                            "executed: ${processingResult.stagesExecuted}, " +
                            "skipped: ${processingResult.stagesSkipped}, " +
                            "cache hits: ${processingResult.cacheHits}, " +
                            "cache hit rate: ${(processingResult.cacheHitRate * 100).toInt()}%")
                    processingResult.output
                } else {
                    Log.e(TAG, "Incremental processing failed: ${processingResult.errorMessage}")
                    null
                }
            } else {
                // 直接使用 Native 处理器
                nativeProcessor.setPreviewMode(enabled = true, maxWidth = 1920, maxHeight = 1080)
                nativeProcessor.process(image, dataParams)
            }
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("Image processing failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Processing error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun exportImage(
        image: Bitmap,
        params: AdjustmentParams
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            // 导出时使用完整分辨率，不使用增量渲染（确保最高质量）
            nativeProcessor.setPreviewMode(enabled = false)
            
            // 转换参数
            val dataParams = mapper.toData(params)
            
            // 处理图像（完整分辨率）
            val result = nativeProcessor.process(image, dataParams)
            
            // 恢复预览模式
            nativeProcessor.setPreviewMode(enabled = true, maxWidth = 1920, maxHeight = 1080)
            
            if (result != null) {
                Log.d(TAG, "Image exported successfully")
                Result.success(result)
            } else {
                Result.failure(Exception("Image export failed"))
            }
        } catch (e: Exception) {
            // 确保恢复预览模式
            nativeProcessor.setPreviewMode(enabled = true, maxWidth = 1920, maxHeight = 1080)
            Log.e(TAG, "Export error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 设置是否使用增量渲染
     * 
     * @param enabled true 启用增量渲染，false 使用直接处理
     */
    fun setIncrementalRenderingEnabled(enabled: Boolean) {
        useIncrementalRendering = enabled
        Log.d(TAG, "Incremental rendering ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 检查是否启用增量渲染
     */
    fun isIncrementalRenderingEnabled(): Boolean = useIncrementalRendering
    
    /**
     * 清除所有缓存
     */
    fun clearCaches() {
        incrementalEngine.invalidateAllCaches()
        Log.d(TAG, "All caches cleared")
    }
    
    /**
     * 获取缓存统计
     */
    fun getCacheStats() = incrementalEngine.getCacheStats()
    
    /**
     * 获取性能统计
     */
    fun getPerformanceStats() = incrementalEngine.getPerformanceStats()
    
    /**
     * 重置性能统计
     */
    fun resetPerformanceStats() {
        incrementalEngine.resetPerformanceStats()
        Log.d(TAG, "Performance stats reset")
    }
}
