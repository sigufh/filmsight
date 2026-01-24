package com.filmtracker.app.ai

/**
 * AI配置
 */
data class AIConfig(
    val provider: AIProvider,
    val apiKey: String,
    val model: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2000
)

/**
 * AI提供商
 */
enum class AIProvider {
    OPENAI, CLAUDE, QWEN, GLM
}

/**
 * 聊天消息
 */
data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBitmap: android.graphics.Bitmap? = null,
    val suggestion: ColorGradingSuggestion? = null
)

/**
 * AI响应
 */
data class AIResponse(
    val message: String,
    val suggestions: List<AdjustmentSuggestion>,
    val error: String? = null
)

/**
 * 调整建议
 */
data class AdjustmentSuggestion(
    val parameters: com.filmtracker.app.data.BasicAdjustmentParams? = null,
    val description: String = "",
    val confidence: Float = 1.0f,
    // 兼容旧格式
    val parameterName: String = "",
    val value: Float = 0f,
    val reason: String = ""
)
