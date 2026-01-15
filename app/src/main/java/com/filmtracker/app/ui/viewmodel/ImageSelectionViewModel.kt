package com.filmtracker.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtracker.app.ui.screens.ImageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 图像选择 ViewModel
 * 
 * 管理图像列表的状态和操作
 * 
 * 使用示例：
 * ```kotlin
 * val viewModel: ImageSelectionViewModel = viewModel()
 * val images by viewModel.images.collectAsState()
 * 
 * // 添加图像
 * viewModel.addImage(imageInfo)
 * 
 * // 删除图像
 * viewModel.deleteImage(imageInfo)
 * ```
 */
class ImageSelectionViewModel : ViewModel() {
    
    // 图像列表
    private val _images = MutableStateFlow<List<ImageInfo>>(emptyList())
    val images: StateFlow<List<ImageInfo>> = _images.asStateFlow()
    
    // 选中的图像
    private val _selectedImage = MutableStateFlow<ImageInfo?>(null)
    val selectedImage: StateFlow<ImageInfo?> = _selectedImage.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * 设置图像列表
     */
    fun setImages(images: List<ImageInfo>) {
        _images.value = images
    }
    
    /**
     * 添加图像到列表
     */
    fun addImage(imageInfo: ImageInfo) {
        viewModelScope.launch {
            val currentImages = _images.value.toMutableList()
            // 添加到列表开头
            currentImages.add(0, imageInfo)
            // 去重并限制数量
            _images.value = currentImages
                .distinctBy { it.uri }
                .take(20)
        }
    }
    
    /**
     * 删除图像
     */
    fun deleteImage(imageInfo: ImageInfo) {
        viewModelScope.launch {
            _images.value = _images.value.filter { it.uri != imageInfo.uri }
            
            // 如果删除的是选中的图像，清除选中状态
            if (_selectedImage.value?.uri == imageInfo.uri) {
                _selectedImage.value = null
            }
        }
    }
    
    /**
     * 选择图像
     */
    fun selectImage(imageInfo: ImageInfo) {
        _selectedImage.value = imageInfo
    }
    
    /**
     * 清除选中
     */
    fun clearSelection() {
        _selectedImage.value = null
    }
    
    /**
     * 加载最近的图像列表
     * 
     * 注意：当前实现为空，实际应该从持久化存储加载
     * 在未来的版本中，可以集成 Repository 来处理持久化
     */
    fun loadRecentImages() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // TODO: 从 Repository 加载图像列表
            // val images = imageRepository.getRecentImages()
            // _images.value = images
            
            _isLoading.value = false
        }
    }
    
    /**
     * 保存图像列表到持久化存储
     * 
     * 注意：当前实现为空，实际应该保存到持久化存储
     * 在未来的版本中，可以集成 Repository 来处理持久化
     */
    fun saveImages() {
        viewModelScope.launch {
            // TODO: 保存到 Repository
            // imageRepository.saveRecentImages(_images.value)
        }
    }
}
