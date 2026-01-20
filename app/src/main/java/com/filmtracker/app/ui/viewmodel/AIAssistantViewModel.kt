package com.filmtracker.app.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmtracker.app.ai.AIAssistantService
import com.filmtracker.app.ai.AIConfig
import com.filmtracker.app.ai.AIResponse
import com.filmtracker.app.ai.AISettingsManager
import com.filmtracker.app.ai.ChatMessage
import com.filmtracker.app.ai.ColorGradingSuggestion
import com.filmtracker.app.ai.UserPreferences
import com.filmtracker.app.util.ExifHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AI助手ViewModel
 */
class AIAssistantViewModel(private val settingsManager: AISettingsManager) : ViewModel() {
    
    // 为每个图片缓存聊天记录 (imageHash -> messages)
    private val conversationCache = mutableMapOf<Int, List<ChatMessage>>()
    private var currentImageHash: Int? = null
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentSuggestion = MutableStateFlow<ColorGradingSuggestion?>(null)
    val currentSuggestion: StateFlow<ColorGradingSuggestion?> = _currentSuggestion.asStateFlow()
    
    private val _userPreferences = MutableStateFlow(settingsManager.getUserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()
    
    private val _apiConfig = MutableStateFlow(settingsManager.getAPIConfig())
    val apiConfig: StateFlow<AIConfig?> = _apiConfig.asStateFlow()
    
    private var aiService: AIAssistantService? = null
    
    init {
        // 如果有保存的配置，自动初始化 AI 服务
        _apiConfig.value?.let { config ->
            initializeAI(config)
        }
    }
    
    /**
     * 初始化AI服务
     */
    fun initializeAI(config: AIConfig) {
        aiService = AIAssistantService(config)
        _apiConfig.value = config
        settingsManager.saveAPIConfig(config.provider, config.apiKey, config.model)
    }
    
    /**
     * 发送消息（支持流式输出和图片分析）
     */
    fun sendMessage(
        message: String,
        image: Bitmap? = null,
        imageUri: Uri? = null,
        context: Context? = null
    ) {
        if ((message.isBlank() && image == null) || _isLoading.value) return
        
        viewModelScope.launch {
            try {
                // 提取 EXIF 和直方图信息
                var enhancedMessage = message.ifBlank { "请分析这张图片并提供调色建议" }
                
                if (image != null && imageUri != null && context != null) {
                    val exifInfo = ExifHelper.extractExifInfo(context, imageUri)
                    val histogramInfo = ExifHelper.calculateHistogram(image)
                    
                    // 将 EXIF 和直方图信息附加到消息中
                    val imageAnalysis = buildString {
                        appendLine()
                        appendLine("【图片技术信息】")
                        
                        exifInfo?.let {
                            appendLine()
                            appendLine("EXIF 数据:")
                            appendLine(it.toReadableString())
                        }
                        
                        appendLine()
                        appendLine("直方图分析:")
                        appendLine(histogramInfo.analyze())
                    }
                    
                    enhancedMessage += imageAnalysis
                }
                
                // 添加用户消息（包含图片）
                val userMessage = ChatMessage(
                    content = if (image != null) message.ifBlank { "[图片]" } else message,
                    isUser = true,
                    imageBitmap = image
                )
                _messages.value = _messages.value + userMessage
                
                // 检查 AI 服务是否已初始化
                if (aiService == null) {
                    val errorMessage = ChatMessage(
                        "请先在设置中配置 AI API 密钥和模型信息",
                        false
                    )
                    _messages.value = _messages.value + errorMessage
                    return@launch
                }
                
                _isLoading.value = true
                
                // 添加一个空的 AI 消息用于流式更新
                val aiMessageIndex = _messages.value.size
                _messages.value = _messages.value + ChatMessage("", false)
                
                // 调用AI服务（流式输出）
                val response = aiService?.sendMessage(
                    message = enhancedMessage,
                    conversationHistory = _messages.value.dropLast(1), // 不包含刚添加的空消息
                    imageBitmap = image,
                    userPreferences = _userPreferences.value,
                    onChunk = { chunk ->
                        // 流式更新消息内容
                        val currentMessages = _messages.value.toMutableList()
                        val currentContent = currentMessages[aiMessageIndex].content
                        currentMessages[aiMessageIndex] = ChatMessage(currentContent + chunk, false)
                        _messages.value = currentMessages
                    }
                ) ?: AIResponse("AI服务未初始化", emptyList())
                
                // 解析 AI 回复中的参数建议
                val finalContent = if (response.message.isNotEmpty()) {
                    response.message
                } else {
                    _messages.value[aiMessageIndex].content
                }
                
                val suggestion = parseColorGradingSuggestion(finalContent)
                
                // 更新最后一条消息，包含建议
                val currentMessages = _messages.value.toMutableList()
                currentMessages[aiMessageIndex] = ChatMessage(
                    content = finalContent,
                    isUser = false,
                    suggestion = suggestion
                )
                _messages.value = currentMessages
                
                // 如果有调色建议，也更新到 currentSuggestion
                if (suggestion != null) {
                    _currentSuggestion.value = suggestion
                }
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    "抱歉，发生了错误：${e.message ?: "未知错误"}\n\n请检查：\n1. 网络连接是否正常\n2. API 密钥是否正确\n3. 模型名称是否正确",
                    false
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 分析图像
     */
    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val suggestion = aiService?.analyzeImage(
                    bitmap = bitmap,
                    userPreferences = _userPreferences.value
                ) ?: ColorGradingSuggestion.empty()
                
                _currentSuggestion.value = suggestion
                
                // 添加分析结果消息
                val message = ChatMessage(
                    "我已经分析了这张照片。${suggestion.explanation}",
                    false
                )
                _messages.value = _messages.value + message
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage("图像分析失败：${e.message}", false)
                _messages.value = _messages.value + errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 从 AI 回复文本中解析调色参数建议
     * 使用新的 JSON 格式解析器
     */
    private fun parseColorGradingSuggestion(text: String): ColorGradingSuggestion? {
        try {
            // 使用新的 JSON 参数解析器
            val parsed = com.filmtracker.app.ai.AIParameterParser.parseParameters(text)
            
            if (parsed != null) {
                // 验证参数范围
                val validated = com.filmtracker.app.ai.AIParameterParser.validateParameters(parsed.parameters)
                
                // 转换为 ColorGradingSuggestion
                return ColorGradingSuggestion(
                    exposure = validated.globalExposure,
                    contrast = validated.contrast,
                    highlights = validated.highlights,
                    shadows = validated.shadows,
                    whites = validated.whites,
                    blacks = validated.blacks,
                    saturation = validated.saturation,
                    vibrance = validated.vibrance,
                    temperature = validated.temperature,
                    tint = validated.tint,
                    clarity = validated.clarity,
                    sharpness = validated.sharpening,
                    denoise = validated.noiseReduction,
                    explanation = parsed.description.ifEmpty { text }
                )
            }
            
            // 如果 JSON 解析失败，尝试旧的正则表达式方式（向后兼容）
            val exposureMatch = Regex("曝光[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val contrastMatch = Regex("对比度[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val highlightsMatch = Regex("高光[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val shadowsMatch = Regex("阴影[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val whitesMatch = Regex("白场[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val blacksMatch = Regex("黑场[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val saturationMatch = Regex("饱和度[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val vibranceMatch = Regex("自然饱和度[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val temperatureMatch = Regex("色温[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val tintMatch = Regex("色调[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val clarityMatch = Regex("清晰度[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val sharpnessMatch = Regex("锐化[：:]([-+]?\\d+\\.?\\d*)").find(text)
            val denoiseMatch = Regex("降噪[：:]([-+]?\\d+\\.?\\d*)").find(text)
            
            // 如果至少有一个参数被提取到，就创建建议
            if (exposureMatch != null || contrastMatch != null || highlightsMatch != null ||
                shadowsMatch != null || saturationMatch != null || temperatureMatch != null) {
                
                return ColorGradingSuggestion(
                    exposure = exposureMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    contrast = contrastMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    highlights = highlightsMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    shadows = shadowsMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    whites = whitesMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    blacks = blacksMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    saturation = saturationMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    vibrance = vibranceMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    temperature = temperatureMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    tint = tintMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    clarity = clarityMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    sharpness = sharpnessMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    denoise = denoiseMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f,
                    explanation = text
                )
            }
            
            return null
        } catch (e: Exception) {
            android.util.Log.e("AIAssistantViewModel", "Error parsing suggestion", e)
            return null
        }
    }
    
    /**
     * 更新用户偏好
     */
    fun updatePreferences(preferences: UserPreferences) {
        _userPreferences.value = preferences
        settingsManager.saveUserPreferences(preferences)
    }
    
    /**
     * 切换到指定图片的对话历史
     */
    fun switchToImage(imageHash: Int?) {
        // 保存当前图片的对话历史
        currentImageHash?.let { hash ->
            conversationCache[hash] = _messages.value
        }
        
        // 切换到新图片
        currentImageHash = imageHash
        
        // 加载新图片的对话历史（如果有）
        _messages.value = if (imageHash != null) {
            conversationCache[imageHash] ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 清空当前图片的对话
     */
    fun clearCurrentConversation() {
        _messages.value = emptyList()
        _currentSuggestion.value = null
        currentImageHash?.let { hash ->
            conversationCache.remove(hash)
        }
    }
    
    /**
     * 清空所有对话缓存
     */
    fun clearAllConversations() {
        conversationCache.clear()
        _messages.value = emptyList()
        _currentSuggestion.value = null
        currentImageHash = null
    }
    
    /**
     * 清空对话（保留向后兼容）
     */
    fun clearConversation() {
        clearCurrentConversation()
    }
}
