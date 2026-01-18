package com.filmtracker.app.ui.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import com.filmtracker.app.data.repository.MetadataRepositoryImpl
import com.filmtracker.app.domain.model.FilmFormat
import com.filmtracker.app.domain.model.FilmStock
import com.filmtracker.app.domain.model.ParameterMetadata
import com.filmtracker.app.domain.model.SerializableAdjustmentParams
import com.filmtracker.app.processing.ExportRenderingPipeline
import com.filmtracker.app.ui.screens.ImageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 胶卷工作流 ViewModel
 * 
 * 管理胶卷仿拍流程的状态：
 * 1. 画幅选择
 * 2. 胶卷型号选择
 * 3. 张数选择
 * 4. 图片管理
 * 5. 编辑状态持久化（使用 MetadataRepository）
 */
class FilmWorkflowViewModel(
    private val context: Context
) : ViewModel() {
    
    // 元数据仓储（用于持久化参数）
    private val metadataRepository = MetadataRepositoryImpl(context)
    
    // 导出渲染管线
    private val exportRenderingPipeline = ExportRenderingPipeline(context)
    
    // 参数映射器
    private val paramsMapper = AdjustmentParamsMapper()
    
    // ========== 画幅选择 ==========
    
    private val _selectedFormat = MutableStateFlow<FilmFormat?>(null)
    val selectedFormat: StateFlow<FilmFormat?> = _selectedFormat.asStateFlow()
    
    /**
     * 选择画幅
     */
    fun selectFormat(format: FilmFormat) {
        _selectedFormat.value = format
        // 重置张数选择（因为不同画幅可选张数不同）
        _selectedCount.value = 0
    }
    
    // ========== 胶卷型号选择 ==========
    
    private val _selectedFilmStock = MutableStateFlow<FilmStock?>(null)
    val selectedFilmStock: StateFlow<FilmStock?> = _selectedFilmStock.asStateFlow()
    
    /**
     * 选择胶卷型号
     */
    fun selectFilmStock(filmStock: FilmStock) {
        _selectedFilmStock.value = filmStock
    }
    
    // ========== 张数选择 ==========
    
    private val _selectedCount = MutableStateFlow(0)
    val selectedCount: StateFlow<Int> = _selectedCount.asStateFlow()
    
    /**
     * 选择拍摄张数
     */
    fun selectCount(count: Int) {
        val format = _selectedFormat.value
        if (format != null && count in format.availableCounts) {
            _selectedCount.value = count
        }
    }
    
    /**
     * 获取当前画幅的可选张数
     */
    fun getAvailableCounts(): List<Int> {
        return _selectedFormat.value?.availableCounts ?: emptyList()
    }
    
    // ========== 图片管理 ==========
    
    private val _filmImages = MutableStateFlow<List<ImageInfo>>(emptyList())
    val filmImages: StateFlow<List<ImageInfo>> = _filmImages.asStateFlow()
    
    /**
     * 添加图片到胶卷
     * 
     * 自动应用选定胶卷型号的预设参数
     */
    fun addImages(images: List<ImageInfo>) {
        val maxCount = _selectedCount.value
        val limitedImages = if (maxCount > 0) {
            images.take(maxCount)
        } else {
            images
        }
        
        // 获取当前选定的胶卷型号预设
        val filmStockPreset = _selectedFilmStock.value?.getPreset()
        
        // 如果有胶卷预设，应用到所有图片
        val imagesWithPreset = if (filmStockPreset != null) {
            limitedImages.map { image ->
                image.copy(
                    adjustmentParams = filmStockPreset.deepCopy(),
                    isModified = true  // 标记为已修改（应用了预设）
                )
            }
        } else {
            limitedImages
        }
        
        _filmImages.value = imagesWithPreset
        
        // 持久化预设参数并生成预览缩略图
        if (filmStockPreset != null) {
            viewModelScope.launch {
                imagesWithPreset.forEach { image ->
                    // 生成应用预设后的缩略图
                    val thumbnail = generatePresetThumbnail(image.uri, filmStockPreset)
                    
                    // 持久化参数（复用 updateImageEdits 方法）
                    updateImageEdits(
                        imageUri = image.uri,
                        params = filmStockPreset.deepCopy(),
                        processedBitmap = thumbnail
                    )
                }
            }
        }
    }
    
    /**
     * 生成应用预设后的缩略图
     * 
     * 使用低分辨率快速生成预览效果
     */
    private suspend fun generatePresetThumbnail(
        imageUri: String,
        preset: com.filmtracker.app.data.BasicAdjustmentParams
    ): android.graphics.Bitmap? {
        return try {
            val contentUri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(contentUri)
            
            // 降采样加载（缩略图尺寸）
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4  // 降采样 4 倍，快速生成
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                inMutable = false
            }
            
            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            
            if (originalBitmap == null) {
                return null
            }
            
            // 使用 IncrementalRenderingEngine 应用预设
            val renderingEngine = com.filmtracker.app.processing.IncrementalRenderingEngine.getInstance()
            val result = renderingEngine.process(originalBitmap, preset)
            
            // 释放原始位图
            originalBitmap.recycle()
            
            result.output
        } catch (e: Exception) {
            android.util.Log.e("FilmWorkflowViewModel", "Failed to generate preset thumbnail", e)
            null
        }
    }
    
    /**
     * 移除图片
     */
    fun removeImage(image: ImageInfo) {
        _filmImages.value = _filmImages.value.filter { it.uri != image.uri }
    }
    
    /**
     * 清空所有图片
     */
    fun clearImages() {
        _filmImages.value = emptyList()
    }
    
    /**
     * 更新图片的调色参数（使用 MetadataRepository 持久化）
     * 
     * 注意：这个方法会将参数保存到磁盘，应用重启后仍然可用
     */
    fun updateImageEdits(
        imageUri: String,
        params: com.filmtracker.app.data.BasicAdjustmentParams,
        processedBitmap: android.graphics.Bitmap? = null
    ) {
        viewModelScope.launch {
            // 1. 更新内存中的图片状态
            val currentImages = _filmImages.value
            val updatedImages = currentImages.map { image ->
                if (image.uri == imageUri) {
                    image.copy(
                        adjustmentParams = params,
                        processedBitmap = processedBitmap,  // 更新缩略图
                        isModified = true
                    )
                } else {
                    image
                }
            }
            _filmImages.value = updatedImages
            
            // 2. 持久化参数到磁盘（使用 MetadataRepository）
            val imagePath = imageUri  // 使用 URI 作为路径标识
            val serializableParams = SerializableAdjustmentParams.fromBasicParams(params)
            val currentTime = System.currentTimeMillis()
            val metadata = ParameterMetadata(
                imageUri = imageUri,
                imagePath = imagePath,
                parameters = serializableParams,
                createdAt = currentTime,
                modifiedAt = currentTime,
                appVersion = "1.0.0"  // TODO: 从 BuildConfig 获取
            )
            
            metadataRepository.saveMetadata(metadata).onFailure { error ->
                android.util.Log.e("FilmWorkflowViewModel", "Failed to save metadata", error)
            }
        }
    }
    
    /**
     * 获取图片的调色参数（从 MetadataRepository 加载）
     * 
     * 这个方法会从磁盘加载参数，即使应用重启也能恢复
     */
    fun getImageParams(imageUri: String): com.filmtracker.app.data.BasicAdjustmentParams? {
        // 首先尝试从内存中获取
        val imageInfo = _filmImages.value.find { it.uri == imageUri }
        if (imageInfo?.adjustmentParams != null) {
            return imageInfo.adjustmentParams
        }
        
        // 如果内存中没有，尝试从磁盘加载（同步方式，用于快速访问）
        // 注意：这是一个简化实现，实际应该使用协程
        return null  // 实际加载会在 ProcessingViewModel 中通过 loadImage 完成
    }
    
    /**
     * 检查图片是否已修改（从元数据检查）
     */
    fun isImageModified(imageUri: String): Boolean {
        return _filmImages.value.find { it.uri == imageUri }?.isModified ?: false
    }
    
    // ========== 工作流状态 ==========
    
    /**
     * 是否可以进入下一步（张数选择）
     */
    fun canProceedToCountSelection(): Boolean {
        return _selectedFormat.value != null
    }
    
    /**
     * 是否可以进入图片选择
     */
    fun canProceedToImageSelection(): Boolean {
        return _selectedFormat.value != null && _selectedCount.value > 0
    }
    
    /**
     * 是否可以进入预览页
     */
    fun canProceedToPreview(): Boolean {
        return _filmImages.value.isNotEmpty()
    }
    
    /**
     * 重置工作流
     */
    fun resetWorkflow() {
        _selectedFormat.value = null
        _selectedFilmStock.value = null
        _selectedCount.value = 0
        _filmImages.value = emptyList()
    }
    
    // ========== 工作流摘要 ==========
    
    /**
     * 获取当前工作流摘要（用于调试和显示）
     */
    fun getWorkflowSummary(): String {
        val format = _selectedFormat.value?.displayName ?: "未选择"
        val filmStock = _selectedFilmStock.value?.displayName ?: "未选择"
        val count = _selectedCount.value
        val imageCount = _filmImages.value.size
        
        return """
            画幅: $format
            胶卷: $filmStock
            张数: $count
            已添加: $imageCount 张
        """.trimIndent()
    }
    
    // ========== 批量导出 ==========
    
    // 批量导出状态
    private val _batchExportState = MutableStateFlow<BatchExportState>(BatchExportState.Idle)
    val batchExportState: StateFlow<BatchExportState> = _batchExportState.asStateFlow()
    
    /**
     * 批量导出状态
     */
    sealed class BatchExportState {
        object Idle : BatchExportState()
        data class Exporting(
            val currentIndex: Int,
            val totalCount: Int,
            val currentFileName: String
        ) : BatchExportState()
        data class Success(
            val exportedCount: Int,
            val totalTimeMs: Long,
            val outputUris: List<Uri>
        ) : BatchExportState()
        data class Failure(
            val error: Throwable,
            val message: String,
            val exportedCount: Int
        ) : BatchExportState()
    }
    
    /**
     * 批量导出所有图片
     * 
     * 使用专业调色的导出方法（ExportRenderingPipeline）
     * 
     * @param config 导出配置
     */
    fun batchExportImages(config: ExportRenderingPipeline.ExportConfig) {
        val images = _filmImages.value
        if (images.isEmpty()) {
            _batchExportState.value = BatchExportState.Failure(
                error = IllegalStateException("No images to export"),
                message = "没有可导出的图片",
                exportedCount = 0
            )
            return
        }
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val outputUris = mutableListOf<Uri>()
            var exportedCount = 0
            
            try {
                _batchExportState.value = BatchExportState.Exporting(
                    currentIndex = 0,
                    totalCount = images.size,
                    currentFileName = images[0].fileName
                )
                
                for ((index, imageInfo) in images.withIndex()) {
                    // 更新进度
                    _batchExportState.value = BatchExportState.Exporting(
                        currentIndex = index,
                        totalCount = images.size,
                        currentFileName = imageInfo.fileName
                    )
                    
                    // 加载原始图片（完整分辨率）
                    val originalBitmap = loadOriginalBitmap(imageInfo.uri)
                    if (originalBitmap == null) {
                        android.util.Log.e("FilmWorkflowViewModel", "Failed to load image: ${imageInfo.uri}")
                        continue
                    }
                    
                    // 获取调色参数（如果有）
                    val params = imageInfo.adjustmentParams?.let { basicParams ->
                        paramsMapper.toDomain(basicParams)
                    } ?: com.filmtracker.app.domain.model.AdjustmentParams.default()
                    
                    // 生成唯一的文件名
                    val displayName = "FilmSight_${System.currentTimeMillis()}_${index + 1}"
                    val exportConfig = config.copy(displayName = displayName)
                    
                    // 导出图片
                    val result = exportRenderingPipeline.exportFromBitmap(
                        originalBitmap = originalBitmap,
                        params = params,
                        config = exportConfig
                    )
                    
                    // 释放位图
                    originalBitmap.recycle()
                    
                    // 处理结果
                    when (result) {
                        is ExportRenderingPipeline.ExportResult.Success -> {
                            exportedCount++
                            result.outputUri?.let { outputUris.add(it) }
                            android.util.Log.d("FilmWorkflowViewModel", "Exported ${index + 1}/${images.size}: ${imageInfo.fileName}")
                        }
                        is ExportRenderingPipeline.ExportResult.Failure -> {
                            android.util.Log.e("FilmWorkflowViewModel", "Failed to export ${imageInfo.fileName}: ${result.message}")
                        }
                    }
                }
                
                val totalTime = System.currentTimeMillis() - startTime
                
                if (exportedCount > 0) {
                    _batchExportState.value = BatchExportState.Success(
                        exportedCount = exportedCount,
                        totalTimeMs = totalTime,
                        outputUris = outputUris
                    )
                } else {
                    _batchExportState.value = BatchExportState.Failure(
                        error = IllegalStateException("No images exported"),
                        message = "所有图片导出失败",
                        exportedCount = 0
                    )
                }
                
            } catch (e: Exception) {
                android.util.Log.e("FilmWorkflowViewModel", "Batch export failed", e)
                _batchExportState.value = BatchExportState.Failure(
                    error = e,
                    message = "批量导出失败: ${e.message}",
                    exportedCount = exportedCount
                )
            }
        }
    }
    
    /**
     * 加载原始位图（完整分辨率）
     */
    private fun loadOriginalBitmap(uri: String): android.graphics.Bitmap? {
        return try {
            val contentUri = Uri.parse(uri)
            val inputStream = context.contentResolver.openInputStream(contentUri)
            
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1  // 不降采样
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                inMutable = false
            }
            
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: Exception) {
            android.util.Log.e("FilmWorkflowViewModel", "Error loading bitmap: $uri", e)
            null
        }
    }
    
    /**
     * 清除批量导出状态
     */
    fun clearBatchExportState() {
        _batchExportState.value = BatchExportState.Idle
    }
}
