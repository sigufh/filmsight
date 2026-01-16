package com.filmtracker.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtracker.app.domain.model.AdjustmentParams
import com.filmtracker.app.domain.model.ProcessingResult
import com.filmtracker.app.domain.usecase.ApplyAdjustmentsUseCase
import com.filmtracker.app.domain.usecase.ExportImageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 图像处理 ViewModel
 * 
 * 使用示例：
 * ```kotlin
 * val viewModel: ProcessingViewModel = viewModel()
 * val processedImage by viewModel.processedImage.collectAsState()
 * val params by viewModel.adjustmentParams.collectAsState()
 * 
 * // 更新参数
 * viewModel.updateExposure(1.5f)
 * 
 * // 应用调整
 * viewModel.applyAdjustments()
 * ```
 */
class ProcessingViewModel(
    private val applyAdjustmentsUseCase: ApplyAdjustmentsUseCase,
    private val exportImageUseCase: ExportImageUseCase
) : ViewModel() {
    
    // 原始图像
    private val _originalImage = MutableStateFlow<Bitmap?>(null)
    val originalImage: StateFlow<Bitmap?> = _originalImage.asStateFlow()
    
    // 处理后的图像
    private val _processedImage = MutableStateFlow<Bitmap?>(null)
    val processedImage: StateFlow<Bitmap?> = _processedImage.asStateFlow()
    
    // 调整参数
    private val _adjustmentParams = MutableStateFlow(AdjustmentParams.default())
    val adjustmentParams: StateFlow<AdjustmentParams> = _adjustmentParams.asStateFlow()
    
    // 处理状态
    private val _processingState = MutableStateFlow<ProcessingResult>(ProcessingResult.Loading)
    val processingState: StateFlow<ProcessingResult> = _processingState.asStateFlow()
    
    // 是否正在处理
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    // 是否正在导出
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    // 导出进度（0.0 - 1.0）
    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()
    
    /**
     * 设置原始图像
     * 
     * 注意：加载新图像时会自动重置所有参数为默认值
     */
    fun setOriginalImage(bitmap: Bitmap) {
        _originalImage.value = bitmap
        _processedImage.value = bitmap
        // 重置参数为默认值
        _adjustmentParams.value = AdjustmentParams.default()
    }
    
    /**
     * 更新曝光
     */
    fun updateExposure(value: Float) {
        _adjustmentParams.update { it.copy(exposure = value) }
        applyAdjustments()
    }
    
    /**
     * 更新对比度
     */
    fun updateContrast(value: Float) {
        _adjustmentParams.update { it.copy(contrast = value) }
        applyAdjustments()
    }
    
    /**
     * 更新饱和度
     */
    fun updateSaturation(value: Float) {
        _adjustmentParams.update { it.copy(saturation = value) }
        applyAdjustments()
    }
    
    /**
     * 更新高光
     */
    fun updateHighlights(value: Float) {
        _adjustmentParams.update { it.copy(highlights = value) }
        applyAdjustments()
    }
    
    /**
     * 更新阴影
     */
    fun updateShadows(value: Float) {
        _adjustmentParams.update { it.copy(shadows = value) }
        applyAdjustments()
    }
    
    /**
     * 更新白场
     */
    fun updateWhites(value: Float) {
        _adjustmentParams.update { it.copy(whites = value) }
        applyAdjustments()
    }
    
    /**
     * 更新黑场
     */
    fun updateBlacks(value: Float) {
        _adjustmentParams.update { it.copy(blacks = value) }
        applyAdjustments()
    }
    
    /**
     * 更新清晰度
     */
    fun updateClarity(value: Float) {
        _adjustmentParams.update { it.copy(clarity = value) }
        applyAdjustments()
    }
    
    /**
     * 更新自然饱和度
     */
    fun updateVibrance(value: Float) {
        _adjustmentParams.update { it.copy(vibrance = value) }
        applyAdjustments()
    }
    
    /**
     * 更新色温
     */
    fun updateTemperature(value: Float) {
        _adjustmentParams.update { it.copy(temperature = value) }
        applyAdjustments()
    }
    
    /**
     * 更新色调
     */
    fun updateTint(value: Float) {
        _adjustmentParams.update { it.copy(tint = value) }
        applyAdjustments()
    }
    
    /**
     * 更新纹理
     */
    fun updateTexture(value: Float) {
        _adjustmentParams.update { it.copy(texture = value) }
        applyAdjustments()
    }
    
    /**
     * 更新去雾
     */
    fun updateDehaze(value: Float) {
        _adjustmentParams.update { it.copy(dehaze = value) }
        applyAdjustments()
    }
    
    /**
     * 更新晕影
     */
    fun updateVignette(value: Float) {
        _adjustmentParams.update { it.copy(vignette = value) }
        applyAdjustments()
    }
    
    /**
     * 更新颗粒
     */
    fun updateGrain(value: Float) {
        _adjustmentParams.update { it.copy(grain = value) }
        applyAdjustments()
    }
    
    /**
     * 更新锐化
     */
    fun updateSharpening(value: Float) {
        _adjustmentParams.update { it.copy(sharpening = value) }
        applyAdjustments()
    }
    
    /**
     * 更新降噪
     */
    fun updateNoiseReduction(value: Float) {
        _adjustmentParams.update { it.copy(noiseReduction = value) }
        applyAdjustments()
    }
    
    /**
     * 批量更新参数
     */
    fun updateParams(params: AdjustmentParams) {
        _adjustmentParams.value = params
        applyAdjustments()
    }
    
    /**
     * 重置参数
     */
    fun resetParams() {
        _adjustmentParams.value = AdjustmentParams.default()
        _processedImage.value = _originalImage.value
    }
    
    /**
     * 导出图像（使用完整分辨率）
     */
    fun exportImage(): Bitmap? {
        val original = _originalImage.value ?: return null
        var result: Bitmap? = null
        
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            
            try {
                // 模拟进度更新
                _exportProgress.value = 0.1f
                
                // 使用完整分辨率处理
                val exportResult = exportImageUseCase(original, _adjustmentParams.value)
                
                _exportProgress.value = 0.9f
                
                exportResult.onSuccess { bitmap ->
                    result = bitmap
                    _exportProgress.value = 1.0f
                }.onFailure { exception ->
                    _processingState.value = ProcessingResult.Error(exception as Exception)
                }
            } finally {
                _isExporting.value = false
                _exportProgress.value = 0f
            }
        }
        
        return result
    }
    
    /**
     * 导出图像（挂起版本，用于协程）
     */
    suspend fun exportImageSuspend(): Result<Bitmap> {
        val original = _originalImage.value 
            ?: return Result.failure(Exception("No original image"))
        
        _isExporting.value = true
        _exportProgress.value = 0f
        
        return try {
            _exportProgress.value = 0.1f
            
            val result = exportImageUseCase(original, _adjustmentParams.value)
            
            _exportProgress.value = 0.9f
            
            result.also {
                _exportProgress.value = 1.0f
            }
        } finally {
            _isExporting.value = false
            _exportProgress.value = 0f
        }
    }
    
    /**
     * 应用调整
     */
    private fun applyAdjustments() {
        val original = _originalImage.value ?: return
        
        // 取消之前的处理任务（如果有）
        currentProcessingJob?.cancel()
        
        currentProcessingJob = viewModelScope.launch {
            _isProcessing.value = true
            _processingState.value = ProcessingResult.Loading
            
            // 使用较短的防抖延迟（增量处理更快）
            kotlinx.coroutines.delay(50)
            
            val result = applyAdjustmentsUseCase(original, _adjustmentParams.value)
            
            result.onSuccess { bitmap ->
                _processedImage.value = bitmap
                _processingState.value = ProcessingResult.Success(bitmap)
            }.onFailure { exception ->
                _processingState.value = ProcessingResult.Error(exception as Exception)
            }
            
            _isProcessing.value = false
        }
    }
    
    // 当前处理任务
    private var currentProcessingJob: kotlinx.coroutines.Job? = null
}
