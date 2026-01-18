package com.filmtracker.app.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtracker.app.domain.model.AdjustmentParams
import com.filmtracker.app.domain.model.EditSession
import com.filmtracker.app.domain.model.ParameterChange
import com.filmtracker.app.domain.model.ParameterMetadata
import com.filmtracker.app.domain.model.ProcessingResult
import com.filmtracker.app.domain.model.SerializableAdjustmentParams
import com.filmtracker.app.domain.repository.MetadataRepository
import com.filmtracker.app.domain.repository.SessionRepository
import com.filmtracker.app.domain.usecase.ApplyAdjustmentsUseCase
import com.filmtracker.app.domain.usecase.ExportImageUseCase
import com.filmtracker.app.processing.ExportRenderingPipeline
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
    private val exportImageUseCase: ExportImageUseCase,
    private val metadataRepository: MetadataRepository,
    private val sessionRepository: SessionRepository,
    private val exportRenderingPipeline: ExportRenderingPipeline
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
    
    // 导出结果
    private val _exportResult = MutableStateFlow<ExportRenderingPipeline.ExportResult?>(null)
    val exportResult: StateFlow<ExportRenderingPipeline.ExportResult?> = _exportResult.asStateFlow()
    
    // 编辑会话
    private val _editSession = MutableStateFlow<EditSession?>(null)
    val editSession: StateFlow<EditSession?> = _editSession.asStateFlow()
    
    // 是否可以撤销
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    
    // 是否可以重做
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    
    // 当前图像路径（用于元数据保存）
    private var currentImagePath: String? = null
    private var currentImageUri: Uri? = null
    
    init {
        // 尝试恢复上次的编辑会话
        restoreLastSession()
    }
    
    /**
     * 恢复上次的编辑会话
     */
    private fun restoreLastSession() {
        viewModelScope.launch {
            val sessionResult = sessionRepository.loadLastSession()
            
            sessionResult.onSuccess { session ->
                if (session != null) {
                    _editSession.value = session
                    _adjustmentParams.value = session.currentParams
                    currentImageUri = session.imageUri
                    currentImagePath = session.imagePath
                    updateUndoRedoState()
                    
                    // 注意：这里不加载图像位图，因为需要由 UI 层触发
                    // UI 层应该检测到会话恢复并加载对应的图像
                }
            }
        }
    }
    
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
     * 加载图像（带元数据加载）
     * 
     * @param uri 图像 URI
     * @param path 图像文件路径
     * @param bitmap 图像位图
     */
    fun loadImage(uri: Uri, path: String, bitmap: Bitmap) {
        currentImageUri = uri
        currentImagePath = path
        
        _originalImage.value = bitmap
        _processedImage.value = bitmap
        
        // 尝试加载元数据
        viewModelScope.launch {
            val metadataResult = metadataRepository.loadMetadata(path)
            
            metadataResult.onSuccess { metadata ->
                if (metadata != null) {
                    // 加载已保存的参数（转换为领域模型）
                    val params = metadata.parameters.toAdjustmentParams()
                    _adjustmentParams.value = params
                    
                    // 创建或恢复编辑会话
                    val session = EditSession.create(uri, path).copy(
                        currentParams = params,
                        isModified = false
                    )
                    _editSession.value = session
                    updateUndoRedoState()
                    
                    // 应用加载的参数
                    applyAdjustments()
                } else {
                    // 没有元数据，创建新会话
                    val session = EditSession.create(uri, path)
                    _editSession.value = session
                    _adjustmentParams.value = AdjustmentParams.default()
                    updateUndoRedoState()
                }
            }.onFailure {
                // 加载失败，使用默认参数
                val session = EditSession.create(uri, path)
                _editSession.value = session
                _adjustmentParams.value = AdjustmentParams.default()
                updateUndoRedoState()
            }
        }
    }
    
    /**
     * 更新曝光
     */
    fun updateExposure(value: Float) {
        updateParameterWithHistory(ParameterChange.ExposureChange(value))
    }
    
    /**
     * 更新对比度
     */
    fun updateContrast(value: Float) {
        updateParameterWithHistory(ParameterChange.ContrastChange(value))
    }
    
    /**
     * 更新饱和度
     */
    fun updateSaturation(value: Float) {
        updateParameterWithHistory(ParameterChange.SaturationChange(value))
    }
    
    /**
     * 更新高光
     */
    fun updateHighlights(value: Float) {
        updateParameterWithHistory(ParameterChange.HighlightsChange(value))
    }
    
    /**
     * 更新阴影
     */
    fun updateShadows(value: Float) {
        updateParameterWithHistory(ParameterChange.ShadowsChange(value))
    }
    
    /**
     * 更新白场
     */
    fun updateWhites(value: Float) {
        updateParameterWithHistory(ParameterChange.WhitesChange(value))
    }
    
    /**
     * 更新黑场
     */
    fun updateBlacks(value: Float) {
        updateParameterWithHistory(ParameterChange.BlacksChange(value))
    }
    
    /**
     * 更新清晰度
     */
    fun updateClarity(value: Float) {
        updateParameterWithHistory(ParameterChange.ClarityChange(value))
    }
    
    /**
     * 更新自然饱和度
     */
    fun updateVibrance(value: Float) {
        updateParameterWithHistory(ParameterChange.VibranceChange(value))
    }
    
    /**
     * 更新色温
     */
    fun updateTemperature(value: Float) {
        updateParameterWithHistory(ParameterChange.TemperatureChange(value))
    }
    
    /**
     * 更新色调
     */
    fun updateTint(value: Float) {
        updateParameterWithHistory(ParameterChange.TintChange(value))
    }
    
    /**
     * 更新纹理
     */
    fun updateTexture(value: Float) {
        updateParameterWithHistory(ParameterChange.TextureChange(value))
    }
    
    /**
     * 更新去雾
     */
    fun updateDehaze(value: Float) {
        updateParameterWithHistory(ParameterChange.DehazeChange(value))
    }
    
    /**
     * 更新晕影
     */
    fun updateVignette(value: Float) {
        updateParameterWithHistory(ParameterChange.VignetteChange(value))
    }
    
    /**
     * 更新颗粒
     */
    fun updateGrain(value: Float) {
        updateParameterWithHistory(ParameterChange.GrainChange(value))
    }
    
    /**
     * 更新锐化
     */
    fun updateSharpening(value: Float) {
        updateParameterWithHistory(ParameterChange.SharpeningChange(value))
    }
    
    /**
     * 更新降噪
     */
    fun updateNoiseReduction(value: Float) {
        updateParameterWithHistory(ParameterChange.NoiseReductionChange(value))
    }
    
    /**
     * 撤销操作
     */
    fun undo() {
        val session = _editSession.value ?: return
        
        val newSession = session.undo()
        if (newSession != null) {
            _editSession.value = newSession
            _adjustmentParams.value = newSession.currentParams
            updateUndoRedoState()
            
            // 触发预览更新
            applyAdjustments()
            
            // 自动保存元数据
            autoSaveMetadata()
        }
    }
    
    /**
     * 重做操作
     */
    fun redo() {
        val session = _editSession.value ?: return
        
        val newSession = session.redo()
        if (newSession != null) {
            _editSession.value = newSession
            _adjustmentParams.value = newSession.currentParams
            updateUndoRedoState()
            
            // 触发预览更新
            applyAdjustments()
            
            // 自动保存元数据
            autoSaveMetadata()
        }
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
     * 更新参数并记录到历史
     * 内部辅助方法
     */
    private fun updateParameterWithHistory(change: ParameterChange) {
        val session = _editSession.value
        
        if (session != null) {
            // 使用会话管理参数变更
            val newSession = session.applyParameterChange(change)
            _editSession.value = newSession
            _adjustmentParams.value = newSession.currentParams
            updateUndoRedoState()
            
            // 自动保存元数据
            autoSaveMetadata()
        } else {
            // 没有会话，直接更新参数（向后兼容）
            val newParams = change.apply(_adjustmentParams.value)
            _adjustmentParams.value = newParams
        }
        
        applyAdjustments()
    }
    
    /**
     * 自动保存元数据
     */
    private fun autoSaveMetadata() {
        val path = currentImagePath ?: return
        val uri = currentImageUri ?: return
        val params = _adjustmentParams.value
        
        viewModelScope.launch {
            val metadata = ParameterMetadata(
                imageUri = uri.toString(),
                imagePath = path,
                imageHash = null, // 可选：计算文件哈希
                parameters = SerializableAdjustmentParams.fromAdjustmentParams(params),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                appVersion = "1.0.0" // TODO: 从 BuildConfig 获取
            )
            
            metadataRepository.saveMetadata(metadata)
        }
    }
    
    /**
     * 更新撤销/重做状态
     */
    private fun updateUndoRedoState() {
        val session = _editSession.value
        _canUndo.value = session?.canUndo() ?: false
        _canRedo.value = session?.canRedo() ?: false
    }
    
    /**
     * 导出图像（使用 ExportRenderingPipeline）
     * 
     * Requirements: 6.1, 6.5
     * 
     * @param config 导出配置
     */
    fun exportImage(config: ExportRenderingPipeline.ExportConfig) {
        val path = currentImagePath
        if (path == null) {
            _exportResult.value = ExportRenderingPipeline.ExportResult.Failure(
                error = IllegalStateException("No image loaded"),
                message = "请先加载图像"
            )
            return
        }
        
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            _exportResult.value = null
            
            try {
                // 模拟进度更新
                _exportProgress.value = 0.1f
                
                // 使用 ExportRenderingPipeline 进行完整分辨率导出
                val result = exportRenderingPipeline.export(
                    imagePath = path,
                    params = _adjustmentParams.value,
                    config = config
                )
                
                _exportProgress.value = 1.0f
                _exportResult.value = result
                
            } catch (e: Exception) {
                _exportResult.value = ExportRenderingPipeline.ExportResult.Failure(
                    error = e,
                    message = "导出失败: ${e.message}"
                )
            } finally {
                _isExporting.value = false
                // 保持进度显示一小段时间
                delay(500)
                _exportProgress.value = 0f
            }
        }
    }
    
    /**
     * 清除导出结果
     */
    fun clearExportResult() {
        _exportResult.value = null
    }
    
    /**
     * 导出图像（使用完整分辨率）
     * @deprecated 使用 exportImage(config) 代替
     */
    @Deprecated("Use exportImage(config) instead")
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
    
    /**
     * ViewModel 清理时保存会话
     */
    override fun onCleared() {
        super.onCleared()
        
        // 保存当前会话
        val session = _editSession.value
        if (session != null) {
            viewModelScope.launch {
                sessionRepository.saveSession(session)
            }
        }
    }
}
