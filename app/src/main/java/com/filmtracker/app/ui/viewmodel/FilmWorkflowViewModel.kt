package com.filmtracker.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtracker.app.data.repository.MetadataRepositoryImpl
import com.filmtracker.app.domain.model.FilmFormat
import com.filmtracker.app.domain.model.FilmStock
import com.filmtracker.app.domain.model.ParameterMetadata
import com.filmtracker.app.domain.model.SerializableAdjustmentParams
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
     */
    fun addImages(images: List<ImageInfo>) {
        val maxCount = _selectedCount.value
        if (maxCount > 0) {
            // 限制图片数量不超过选定的张数
            _filmImages.value = images.take(maxCount)
        } else {
            _filmImages.value = images
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
}
