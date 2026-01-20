package com.filmtracker.app.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI助手服务
 */
class AIAssistantService(private val config: AIConfig) {
    private val ragKnowledgeBase = RAGKnowledgeBase()
    
    suspend fun sendMessage(
        message: String,
        conversationHistory: List<ChatMessage>,
        imageBitmap: Bitmap? = null,
        userPreferences: UserPreferences? = null,
        onChunk: ((String) -> Unit)? = null
    ): AIResponse = withContext(Dispatchers.IO) {
        try {
            val relevantKnowledge = ragKnowledgeBase.search(message)
            val systemPrompt = buildSystemPrompt(userPreferences, relevantKnowledge)
            
            when (config.provider) {
                AIProvider.OPENAI -> callOpenAI(systemPrompt, message, conversationHistory, imageBitmap, onChunk)
                AIProvider.CLAUDE -> callClaude(systemPrompt, message, conversationHistory, imageBitmap, onChunk)
                AIProvider.QWEN -> callQwen(systemPrompt, message, conversationHistory, imageBitmap, onChunk)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling AI", e)
            AIResponse("抱歉，AI助手暂时无法响应", emptyList(), e.message)
        }
    }
    
    /**
     * 将 Bitmap 转换为 Base64 字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // 压缩图片以减少传输大小
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    private fun buildSystemPrompt(preferences: UserPreferences?, knowledge: List<KnowledgeItem>): String {
        return """你是专业摄影后期调色助手，精通RAW处理、色彩理论和胶片美学。

## 参数标准（遵循 Adobe Camera RAW / Lightroom）

所有参数必须使用 Adobe 标准范围，用户界面直接使用这些数值：

### 基础调整
- **曝光 (Exposure)**: -5.0 到 +5.0 EV（步进 0.1）
- **对比度 (Contrast)**: -100 到 +100（0 = 不变）
  - 注意：使用平方曲线映射，±30 范围内保持细腻控制
  - ±10: 极微调（变化 < 1%）
  - ±20: 轻度调整（变化 < 4%）
  - ±30: 中度调整（变化 < 10%）
  - ±50: 强度调整（变化 < 25%）
- **饱和度 (Saturation)**: -100 到 +100（0 = 不变，-100 = 完全去色）
  - 注意：当前饱和度参数暂时无法使用，请使用色彩分级实现饱和度调整

### 色调调整
- **高光 (Highlights)**: -100 到 +100（负值恢复过曝）
- **阴影 (Shadows)**: -100 到 +100（正值提亮暗部）
- **白场 (Whites)**: -100 到 +100（调整最亮区域）
- **黑场 (Blacks)**: -100 到 +100（调整最暗区域）

### 存在感
- **清晰度 (Clarity)**: -100 到 +100（增强中间调对比）
- **自然饱和度 (Vibrance)**: -100 到 +100（智能提升色彩）

### 颜色
- **色温 (Temperature)**: -100 到 +100（负值偏冷，正值偏暖）
- **色调 (Tint)**: -100 到 +100（负值偏绿，正值偏紫）

### 色彩分级（Color Grading）
用于精细控制不同亮度区域的色彩，也可用于实现饱和度调整：

- **高光色温 (GradingHighlightsTemp)**: -100 到 +100
- **高光色调 (GradingHighlightsTint)**: -100 到 +100
- **中间调色温 (GradingMidtonesTemp)**: -100 到 +100
- **中间调色调 (GradingMidtonesTint)**: -100 到 +100
- **阴影色温 (GradingShadowsTemp)**: -100 到 +100
- **阴影色调 (GradingShadowsTint)**: -100 到 +100
- **混合 (GradingBlending)**: 0 到 100（分级效果强度）
- **平衡 (GradingBalance)**: -100 到 +100（阴影/高光平衡）

**黑白效果实现**：将 saturation 设为 -100

### 效果
- **纹理 (Texture)**: -100 到 +100（增强细节纹理）
- **去雾 (Dehaze)**: -100 到 +100（去除雾霾）
- **晕影 (Vignette)**: -100 到 +100（负值暗角）
- **颗粒 (Grain)**: 0 到 100（胶片颗粒感）

### 细节
- **锐化 (Sharpening)**: 0 到 100
- **降噪 (NoiseReduction)**: 0 到 100

## 重要规则

1. **参数格式要求**：
   - 必须使用上述 Adobe 标准范围
   - 不要使用乘数（如 1.2x）或百分比符号
   - 使用整数或一位小数（曝光可用两位小数）
   
2. **建议格式**：
   当提供调色建议时，必须在回复末尾输出 JSON 格式的参数：
   
   ```json
   {
     "parameters": {
       "exposure": 0.5,
       "contrast": 15,
       "highlights": -30,
       "shadows": 25,
       "whites": -10,
       "blacks": 5,
       "saturation": -100,
       "vibrance": 20,
       "temperature": 15,
       "tint": 0,
       "clarity": 10,
       "texture": 0,
       "dehaze": 0,
       "vignette": 0,
       "grain": 0,
       "sharpening": 0,
       "noiseReduction": 0,
       "gradingHighlightsTemp": 0,
       "gradingHighlightsTint": 0,
       "gradingMidtonesTemp": 0,
       "gradingMidtonesTint": 0,
       "gradingShadowsTemp": 0,
       "gradingShadowsTint": 0,
       "gradingBlending": 50,
       "gradingBalance": 0
     },
     "description": "简短的调整说明"
   }
   ```
   
   注意：
   - JSON 必须放在回复的最后
   - 只包含需要调整的参数（值为 0 的可以省略，但黑白效果必须包含 saturation: -100）
   - 参数名使用驼峰命名法
   - 数值不带单位符号
   - 色彩分级参数默认可省略，需要时再添加

3. **参数解释**：
   - 提供参数后，简要说明调整原因
   - 说明预期效果
   - 如有多种方案，可提供对比

4. **常见场景参考**（基于新的平方曲线）：
   - 日系清新：曝光 +0.3 到 +0.7，对比度 -15，高光 -20，阴影 +30，自然饱和度 +20，gradingMidtonesTemp: -10
   - 电影感：对比度 +25，高光 -40，阴影 +20，清晰度 +15，gradingShadowsTemp: -30, gradingHighlightsTemp: 20, gradingBlending: 60
   - 胶片复古：曝光 +0.2，对比度 -20，高光 -25，阴影 +15，颗粒 +30 到 +50，gradingShadowsTemp: 15
   - 人像柔和：清晰度 -20 到 -30，对比度 -12，高光 -15，阴影 +20，gradingHighlightsTemp: 10
   - 黑白效果：saturation: -100，对比度 +20 到 +40，清晰度 +15

${preferences?.let { "\n## 用户偏好\n- 调色风格：${it.colorStyle.displayName}\n- 色彩倾向：${it.colorTendency}\n- 对比度偏好：${it.contrastPreference}\n- 饱和度偏好：${it.saturationPreference}" } ?: ""}
${if (knowledge.isNotEmpty()) "\n## 相关知识\n" + knowledge.joinToString("\n") { "- ${it.content}" } else ""}

记住：用户会直接将你建议的数值输入到调色界面，所以必须使用 Adobe 标准范围！对比度使用平方曲线，±30 范围内保持细腻。""".trimIndent()
    }
    
    private fun callOpenAI(system: String, msg: String, history: List<ChatMessage>, img: Bitmap?, onChunk: ((String) -> Unit)?): AIResponse {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        conn.doOutput = true
        
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", system))
            history.forEach { 
                put(JSONObject()
                    .put("role", if (it.isUser) "user" else "assistant")
                    .put("content", it.content))
            }
            
            // 添加用户消息（可能包含图片）
            if (img != null) {
                val base64Image = bitmapToBase64(img)
                val content = JSONArray().apply {
                    put(JSONObject().put("type", "text").put("text", msg))
                    put(JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject()
                            .put("url", "data:image/jpeg;base64,$base64Image")))
                }
                put(JSONObject().put("role", "user").put("content", content))
            } else {
                put(JSONObject().put("role", "user").put("content", msg))
            }
        }
        
        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("temperature", config.temperature)
            .put("max_tokens", config.maxTokens)
            .put("stream", onChunk != null)
        
        Log.d(TAG, "OpenAI request (image: ${img != null})")
        
        OutputStreamWriter(conn.outputStream).use { 
            it.write(body.toString())
            it.flush()
        }
        
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            if (onChunk != null) {
                // 流式响应
                return handleStreamResponse(conn.inputStream, onChunk)
            } else {
                // 非流式响应
                val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val content = JSONObject(response)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                return AIResponse(content, parseSuggestions(content))
            }
        }
        
        // 读取错误响应
        val errorResponse = try {
            BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
        } catch (e: Exception) {
            "无法读取错误详情"
        }
        throw Exception("OpenAI API Error: ${conn.responseCode}\n详情: $errorResponse")
    }
    
    private fun handleStreamResponse(inputStream: java.io.InputStream, onChunk: (String) -> Unit): AIResponse {
        val fullContent = StringBuilder()
        
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val data = line ?: continue
                if (data.startsWith("data: ")) {
                    val jsonStr = data.substring(6).trim()
                    if (jsonStr == "[DONE]") break
                    
                    try {
                        val json = JSONObject(jsonStr)
                        val delta = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta")
                        
                        if (delta.has("content")) {
                            val chunk = delta.getString("content")
                            fullContent.append(chunk)
                            onChunk(chunk)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing stream chunk", e)
                    }
                }
            }
        }
        
        val content = fullContent.toString()
        return AIResponse(content, parseSuggestions(content))
    }
    
    private fun callClaude(system: String, msg: String, history: List<ChatMessage>, img: Bitmap?, onChunk: ((String) -> Unit)?): AIResponse {
        val url = URL("https://api.anthropic.com/v1/messages")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-api-key", config.apiKey)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.doOutput = true
        
        val messages = JSONArray().apply {
            history.forEach {
                put(JSONObject()
                    .put("role", if (it.isUser) "user" else "assistant")
                    .put("content", it.content))
            }
            
            // 添加用户消息（可能包含图片）
            if (img != null) {
                val base64Image = bitmapToBase64(img)
                val content = JSONArray().apply {
                    put(JSONObject()
                        .put("type", "image")
                        .put("source", JSONObject()
                            .put("type", "base64")
                            .put("media_type", "image/jpeg")
                            .put("data", base64Image)))
                    put(JSONObject().put("type", "text").put("text", msg))
                }
                put(JSONObject().put("role", "user").put("content", content))
            } else {
                put(JSONObject().put("role", "user").put("content", msg))
            }
        }
        
        val body = JSONObject()
            .put("model", config.model)
            .put("system", system)
            .put("messages", messages)
            .put("temperature", config.temperature)
            .put("max_tokens", config.maxTokens)
            .put("stream", onChunk != null)
        
        Log.d(TAG, "Claude request (image: ${img != null})")
        
        OutputStreamWriter(conn.outputStream).use {
            it.write(body.toString())
            it.flush()
        }
        
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            if (onChunk != null) {
                // 流式响应
                return handleClaudeStreamResponse(conn.inputStream, onChunk)
            } else {
                // 非流式响应
                val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val content = JSONObject(response)
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                return AIResponse(content, parseSuggestions(content))
            }
        }
        
        // 读取错误响应
        val errorResponse = try {
            BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
        } catch (e: Exception) {
            "无法读取错误详情"
        }
        throw Exception("Claude API Error: ${conn.responseCode}\n详情: $errorResponse")
    }
    
    private fun handleClaudeStreamResponse(inputStream: java.io.InputStream, onChunk: (String) -> Unit): AIResponse {
        val fullContent = StringBuilder()
        
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val data = line ?: continue
                
                // Claude 使用 SSE 格式: "event: xxx" 和 "data: xxx"
                if (data.startsWith("data: ")) {
                    val jsonStr = data.substring(6).trim()
                    
                    try {
                        val json = JSONObject(jsonStr)
                        val type = json.optString("type", "")
                        
                        when (type) {
                            "content_block_delta" -> {
                                val delta = json.getJSONObject("delta")
                                if (delta.has("text")) {
                                    val chunk = delta.getString("text")
                                    fullContent.append(chunk)
                                    onChunk(chunk)
                                }
                            }
                            "message_stop" -> break
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Claude stream chunk", e)
                    }
                }
            }
        }
        
        val content = fullContent.toString()
        return AIResponse(content, parseSuggestions(content))
    }
    
    private fun callQwen(system: String, msg: String, history: List<ChatMessage>, img: Bitmap?, onChunk: ((String) -> Unit)?): AIResponse {
        // 通义千问使用不同的 API 端点
        val url = URL("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        conn.doOutput = true
        
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", system))
            history.forEach {
                put(JSONObject()
                    .put("role", if (it.isUser) "user" else "assistant")
                    .put("content", it.content))
            }
            
            // 添加用户消息（可能包含图片）
            if (img != null) {
                val base64Image = bitmapToBase64(img)
                val content = JSONArray().apply {
                    put(JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject()
                            .put("url", "data:image/jpeg;base64,$base64Image")))
                    put(JSONObject().put("type", "text").put("text", msg))
                }
                put(JSONObject().put("role", "user").put("content", content))
            } else {
                put(JSONObject().put("role", "user").put("content", msg))
            }
        }
        
        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("stream", onChunk != null)
        
        Log.d(TAG, "Qwen request (image: ${img != null})")
        
        OutputStreamWriter(conn.outputStream).use {
            it.write(body.toString())
            it.flush()
        }
        
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            if (onChunk != null) {
                // 流式响应（使用 OpenAI 兼容格式）
                return handleStreamResponse(conn.inputStream, onChunk)
            } else {
                // 非流式响应
                val response = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                Log.d(TAG, "Qwen response: $response")
                
                val jsonResponse = JSONObject(response)
                // 通义千问兼容模式使用 OpenAI 格式的响应
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                return AIResponse(content, parseSuggestions(content))
            }
        }
        
        // 读取错误响应
        val errorResponse = try {
            BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).readText()
        } catch (e: Exception) {
            "无法读取错误详情"
        }
        Log.e(TAG, "Qwen API Error: ${conn.responseCode}, Response: $errorResponse")
        throw Exception("Qwen API Error: ${conn.responseCode}\n详情: $errorResponse")
    }
    
    suspend fun analyzeImage(
        bitmap: Bitmap,
        userPreferences: UserPreferences
    ): ColorGradingSuggestion = withContext(Dispatchers.IO) {
        try {
            val message = "请分析这张照片并提供调色建议"
            val response = sendMessage(message, emptyList(), bitmap, userPreferences)
            
            // 从响应中提取建议
            if (response.suggestions.isNotEmpty()) {
                val suggestion = response.suggestions.first()
                ColorGradingSuggestion(
                    explanation = response.message
                )
            } else {
                ColorGradingSuggestion(explanation = response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image", e)
            ColorGradingSuggestion(explanation = "图像分析失败：${e.message}")
        }
    }
    
    private fun parseSuggestions(content: String): List<AdjustmentSuggestion> {
        val suggestions = mutableListOf<AdjustmentSuggestion>()
        
        // 尝试解析 JSON 参数
        val parsed = AIParameterParser.parseParameters(content)
        if (parsed != null) {
            val summary = AIParameterParser.generateSummary(parsed.parameters)
            suggestions.add(
                AdjustmentSuggestion(
                    parameters = parsed.parameters,
                    description = parsed.description.ifEmpty { summary },
                    confidence = 0.9f
                )
            )
            Log.d(TAG, "成功解析参数: $summary")
        }
        
        return suggestions
    }
    
    companion object {
        private const val TAG = "AIAssistantService"
    }
}
