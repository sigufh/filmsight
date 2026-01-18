package com.filmtracker.app.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtracker.app.domain.model.EditSession
import com.filmtracker.app.domain.repository.SessionRepository
import com.filmtracker.app.ui.screens.ImageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 图像选择 ViewModel
 * 
 * 管理图像列表的状态和操作，包括每个图像的编辑会话
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
 * 
 * // 选择图像（自动保存当前会话并加载目标会话）
 * viewModel.selectImage(imageInfo, onSessionLoaded = { session -> ... })
 * ```
 */
class ImageSelectionViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    // 图像列表
    private val _images = MutableStateFlow<List<ImageInfo>>(emptyList())
    val images: StateFlow<List<ImageInfo>> = _images.asStateFlow()
    
    // 选中的图像
    private val _selectedImage = MutableStateFlow<ImageInfo?>(null)
    val selectedImage: StateFlow<ImageInfo?> = _selectedImage.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 每个图像的编辑会话缓存
    // Key: imageUri.toString()
    private val sessionCache = mutableMapOf<String, EditSession>()
    
    // 当前活动的会话
    private var currentSession: EditSession? = null
    
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
            
            // 从会话缓存中移除
            sessionCache.remove(imageInfo.uri)
            
            // 如果删除的是选中的图像，清除选中状态
            if (_selectedImage.value?.uri == imageInfo.uri) {
                _selectedImage.value = null
                currentSession = null
            }
        }
    }
    
    /**
     * 选择图像
     * 自动保存当前会话并加载目标图像的会话
     * 
     * @param imageInfo 要选择的图像信息
     * @param onSessionLoaded 会话加载完成的回调，参数为加载的会话（可能为 null）
     */
    fun selectImage(imageInfo: ImageInfo, onSessionLoaded: (EditSession?) -> Unit = {}) {
        viewModelScope.launch {
            // 1. 保存当前会话（如果存在）
            currentSession?.let { session ->
                saveCurrentSession(session)
            }
            
            // 2. 更新选中的图像
            _selectedImage.value = imageInfo
            
            // 3. 加载目标图像的会话
            val targetSession = loadSessionForImage(imageInfo)
            currentSession = targetSession
            
            // 4. 通知回调
            onSessionLoaded(targetSession)
        }
    }
    
    /**
     * 更新当前会话
     * 当 ProcessingViewModel 中的会话发生变化时调用
     * 
     * @param session 更新后的会话
     */
    fun updateCurrentSession(session: EditSession) {
        currentSession = session
        // 更新缓存
        sessionCache[session.imageUri.toString()] = session
    }
    
    /**
     * 保存当前会话到持久化存储
     * 
     * @param session 要保存的会话
     */
    private suspend fun saveCurrentSession(session: EditSession) {
        // 更新缓存
        sessionCache[session.imageUri.toString()] = session
        
        // 保存到持久化存储
        sessionRepository.saveSession(session).onFailure { error ->
            // 记录错误但不中断流程
            android.util.Log.e("ImageSelectionVM", "Failed to save session: ${error.message}")
        }
    }
    
    /**
     * 加载指定图像的会话
     * 
     * @param imageInfo 图像信息
     * @return 加载的会话，如果不存在则返回 null
     */
    private suspend fun loadSessionForImage(imageInfo: ImageInfo): EditSession? {
        val imageUriString = imageInfo.uri
        
        // 1. 先检查缓存
        sessionCache[imageUriString]?.let { cachedSession ->
            return cachedSession
        }
        
        // 2. 从持久化存储加载
        val imageUri = Uri.parse(imageInfo.uri)
        val sessionResult = sessionRepository.loadSessionForImage(imageUri)
        
        return sessionResult.getOrNull()?.also { session ->
            // 加载成功，更新缓存
            sessionCache[imageUriString] = session
        }
    }
    
    /**
     * 获取指定图像的会话（从缓存）
     * 
     * @param imageUri 图像 URI
     * @return 会话，如果不存在则返回 null
     */
    fun getSessionForImage(imageUri: Uri): EditSession? {
        return sessionCache[imageUri.toString()]
    }
    
    /**
     * 清除选中
     */
    fun clearSelection() {
        viewModelScope.launch {
            // 保存当前会话
            currentSession?.let { session ->
                saveCurrentSession(session)
            }
            
            _selectedImage.value = null
            currentSession = null
        }
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
