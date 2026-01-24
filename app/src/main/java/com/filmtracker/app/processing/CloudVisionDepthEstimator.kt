package com.filmtracker.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.ai.AIAssistantService
import com.filmtracker.app.ai.AIConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.min

/**
 * 使用视觉大模型（Claude/GPT-4V/Gemini）进行深度估计
 * 
 * 直接复用现有的 AIAssistantService，无需额外配置
 * 
 * 优势：
 * - 无需部署本地模型
 * - 理解能力强
 * - 效果好
 * - 开发简单
 * - 复用现有 AI 配置
 */
class CloudVisionDepthEstimator(
    private val context: Context,
    private val aiConfig: AIConfig  // 使用现有的 AI 配置
) {
    private val aiService = AIAssistantService(aiConfig)
    
    @Serializable
    data class DepthAnalysis(
        val depth_grid: List<List<Int>>,  // 深度网格 (0-255)，16x16
        val subject_mask: List<List<Int>>  // 主体蒙版网格 (0-255)，32x32
    )
    
    @Serializable
    data class DepthGridResponse(
        val depth_grid: String  // 紧凑格式：逗号分隔的字符串 "150,150,150,..."
    )
    
    @Serializable
    data class MaskGridResponse(
        val mask: String  // 紧凑格式：逗号分隔的字符串（缩短字段名）
    )
    
    companion object {
        private const val TAG = "CloudVisionDepthEstimator"
        private const val DEPTH_GRID_SIZE = 8    // 降低到 8x8 = 64 个值（避免截断）
        private const val MASK_GRID_SIZE = 8     // 蒙版也降低到 8x8
    }
    
    /**
     * 使用 AI 分析深度（两次请求：深度 + 蒙版）
     */
    suspend fun analyzeDepth(bitmap: Bitmap): DepthAnalysis = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== STARTING AI DEPTH ANALYSIS (2-STEP) ===")
            Log.d(TAG, "Image size: ${bitmap.width}x${bitmap.height}")
            
            // 第一步：获取深度网格
            Log.d(TAG, "Step 1: Requesting depth grid...")
            val depthGrid = requestDepthGrid(bitmap)
            
            // 第二步：获取蒙版网格
            Log.d(TAG, "Step 2: Requesting subject mask...")
            val subjectMask = requestSubjectMask(bitmap, depthGrid)
            
            val analysis = DepthAnalysis(
                depth_grid = depthGrid,
                subject_mask = subjectMask
            )
            
            Log.d(TAG, "=== AI DEPTH ANALYSIS COMPLETED SUCCESSFULLY ===")
            return@withContext analysis
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ AI depth analysis failed: ${e.message}", e)
            Log.w(TAG, "Falling back to default depth analysis (radial gradient)")
            
            // 返回默认值
            return@withContext generateDefaultDepthAnalysis()
        }
    }
    
    /**
     * 第一步：请求深度网格
     */
    private suspend fun requestDepthGrid(bitmap: Bitmap): List<List<Int>> {
        val prompt = buildDepthPrompt()
        Log.d(TAG, "Depth prompt length: ${prompt.length} chars")
        
        // Token 消耗分析（详见 docs/AI_MODEL_TOKEN_LIMITS.md）：
        // - 输入：图片 ~2400 + Prompt ~200 = ~2600 tokens
        // - 输出：8x8=64 个值的紧凑格式 ~200-300 tokens
        // - 总计：~2900 tokens
        // 
        // 设置 6000 tokens 确保所有模型都能完整返回（增加余量）
        val depthConfig = aiConfig.copy(maxTokens = 6000)
        val depthService = AIAssistantService(depthConfig)
        
        // 最多重试 2 次
        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                Log.d(TAG, "Calling AI service for depth grid (attempt ${attempt + 1}/2, max_tokens=6000)...")
                val response = depthService.sendMessage(
                    message = prompt,
                    conversationHistory = emptyList(),
                    imageBitmap = bitmap,
                    userPreferences = null,
                    onChunk = null
                )
                
                Log.d(TAG, "Depth grid response received")
                Log.d(TAG, "Response message length: ${response.message.length} chars")
                
                if (response.message.isEmpty()) {
                    Log.e(TAG, "❌ Depth grid response is empty!")
                    throw Exception("Depth grid response is empty: ${response.error ?: "no error provided"}")
                }
                
                // 解析深度网格
                val depthResponse = parseDepthGridResponse(response.message)
                val values = depthResponse.depth_grid.split(",").mapNotNull { it.trim().toIntOrNull() }
                
                Log.d(TAG, "Parsed ${values.size} depth values (expected 64)")
                
                // 验证是否有完整的 64 个值
                if (values.size < 64) {
                    Log.w(TAG, "⚠️ Incomplete depth grid: got ${values.size}/64 values")
                    throw Exception("Incomplete depth grid: got ${values.size}/64 values")
                }
                
                val grid = parseCompactDepthGrid(depthResponse.depth_grid)
                Log.d(TAG, "✅ Depth grid received successfully (${values.size} values)")
                return grid
                
            } catch (e: Exception) {
                Log.e(TAG, "Depth grid request failed (attempt ${attempt + 1}/2): ${e.message}")
                lastError = e
                if (attempt == 0) {
                    // 第一次失败，等待后重试
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
        
        // 所有重试都失败
        throw lastError ?: Exception("Failed to get depth grid after 2 attempts")
    }
    
    /**
     * 第二步：请求主体蒙版
     */
    private suspend fun requestSubjectMask(
        bitmap: Bitmap,
        depthGrid: List<List<Int>>
    ): List<List<Int>> {
        val prompt = buildMaskPrompt(depthGrid)
        Log.d(TAG, "Mask prompt length: ${prompt.length} chars")
        
        // 设置 6000 tokens 确保所有模型都能完整返回（增加余量）
        val maskConfig = aiConfig.copy(maxTokens = 6000)
        val maskService = AIAssistantService(maskConfig)
        
        // 最多重试 2 次
        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                Log.d(TAG, "Calling AI service for subject mask (attempt ${attempt + 1}/2, max_tokens=6000)...")
                val response = maskService.sendMessage(
                    message = prompt,
                    conversationHistory = emptyList(),
                    imageBitmap = bitmap,
                    userPreferences = null,
                    onChunk = null
                )
                
                Log.d(TAG, "Subject mask response length: ${response.message.length} chars")
                
                if (response.message.isEmpty()) {
                    Log.e(TAG, "❌ Subject mask response is empty!")
                    throw Exception("Subject mask response is empty: ${response.error ?: "no error provided"}")
                }
                
                // 解析蒙版网格
                val maskResponse = parseMaskGridResponse(response.message)
                val values = maskResponse.mask.split(",").mapNotNull { it.trim().toIntOrNull() }
                
                Log.d(TAG, "Parsed ${values.size} mask values (expected 64)")
                
                // 验证是否有完整的 64 个值
                if (values.size < 64) {
                    Log.w(TAG, "⚠️ Incomplete mask grid: got ${values.size}/64 values")
                    throw Exception("Incomplete mask grid: got ${values.size}/64 values")
                }
                
                val grid = parseCompactMaskGrid(maskResponse.mask)
                Log.d(TAG, "✅ Subject mask received successfully (${values.size} values)")
                return grid
                
            } catch (e: Exception) {
                Log.e(TAG, "Subject mask request failed (attempt ${attempt + 1}/2): ${e.message}")
                lastError = e
                if (attempt == 0) {
                    // 第一次失败，等待后重试
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
        
        // 所有重试都失败
        throw lastError ?: Exception("Failed to get subject mask after 2 attempts")
    }
    
    /**
     * 构建深度分析 Prompt（第一步）
     */
    private fun buildDepthPrompt(): String {
        return """
分析图片深度，生成 8x8 网格（共 64 个深度值）。

深度值范围：
- 0-50: 主体（最近）
- 50-100: 近景
- 100-150: 中景
- 150-200: 远景
- 200-255: 背景（最远）

输出格式要求：
1. 返回纯 JSON 对象（不要用 markdown 代码块包裹）
2. JSON 结构：{"depth_grid": "v1,v2,v3,...,v64"}
3. depth_grid 字段是一个字符串，包含 64 个整数值
4. 值之间用逗号分隔，不要有空格
5. 按从左到右、从上到下的顺序排列（第1行8个值，第2行8个值，...，第8行8个值）
6. 必须包含完整的 64 个值，不能省略或截断

重要：这是一个 8x8 的完整网格，你必须生成所有 64 个位置的深度值。每一行有 8 个值，共 8 行。
""".trimIndent()
    }
    
    /**
     * 构建蒙版分析 Prompt（第二步）
     */
    private fun buildMaskPrompt(depthGrid: List<List<Int>>): String {
        // 找到最小深度值（主体位置）
        val minDepth = depthGrid.flatten().minOrNull() ?: 50
        val threshold = minDepth + 50
        
        return """
基于深度信息，生成主体蒙版的 8x8 网格（共 64 个蒙版值）。

蒙版规则：
- 深度值 < $threshold 的区域为主体
- 主体区域：255（完全不透明）
- 主体边缘：200-254（半透明过渡）
- 背景区域：0（完全透明）

输出格式要求：
1. 返回纯 JSON 对象（不要用 markdown 代码块包裹）
2. JSON 结构：{"mask": "v1,v2,v3,...,v64"}
3. mask 字段是一个字符串，包含 64 个整数值（0-255）
4. 值之间用逗号分隔，不要有空格
5. 按从左到右、从上到下的顺序排列（第1行8个值，第2行8个值，...，第8行8个值）
6. 必须包含完整的 64 个值，不能省略或截断

重要：这是一个 8x8 的完整网格，你必须生成所有 64 个位置的蒙版值。每一行有 8 个值，共 8 行。
""".trimIndent()
    }
    
    /**
     * 解析深度网格响应
     */
    private fun parseDepthGridResponse(response: String): DepthGridResponse {
        try {
            Log.d(TAG, "=== PARSING DEPTH GRID RESPONSE ===")
            Log.d(TAG, "Response length: ${response.length}")
            
            val jsonText = extractJson(response)
            Log.d(TAG, "Extracted JSON length: ${jsonText.length}")
            
            val json = Json { 
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            
            val depthResponse = json.decodeFromString<DepthGridResponse>(jsonText)
            
            // 解析紧凑格式的字符串
            val values = depthResponse.depth_grid.split(",").mapNotNull { it.trim().toIntOrNull() }
            
            Log.d(TAG, "Parsed ${values.size} depth values (expected ${DEPTH_GRID_SIZE * DEPTH_GRID_SIZE})")
            
            // 转换为 2D 网格
            val grid = mutableListOf<List<Int>>()
            for (row in 0 until DEPTH_GRID_SIZE) {
                val startIdx = row * DEPTH_GRID_SIZE
                val endIdx = startIdx + DEPTH_GRID_SIZE
                
                if (endIdx <= values.size) {
                    grid.add(values.subList(startIdx, endIdx))
                } else {
                    // 不完整的行，用默认值填充
                    val partialRow = if (startIdx < values.size) {
                        values.subList(startIdx, values.size)
                    } else {
                        emptyList()
                    }
                    val lastValue = partialRow.lastOrNull() ?: grid.lastOrNull()?.last() ?: 150
                    val completeRow = partialRow + List(DEPTH_GRID_SIZE - partialRow.size) { lastValue }
                    grid.add(completeRow)
                    Log.w(TAG, "⚠️ Padded incomplete row $row")
                }
            }
            
            // 如果行数不足，填充缺失的行
            while (grid.size < DEPTH_GRID_SIZE) {
                val lastRow = grid.lastOrNull() ?: List(DEPTH_GRID_SIZE) { 150 }
                grid.add(lastRow)
                Log.w(TAG, "⚠️ Added missing row ${grid.size - 1}")
            }
            
            // 统计深度分布
            val depths = grid.flatten()
            val minDepth = depths.minOrNull() ?: 0
            val maxDepth = depths.maxOrNull() ?: 255
            val avgDepth = depths.average().toInt()
            
            Log.d(TAG, "✅ Depth grid parsed successfully")
            Log.d(TAG, "Depth range: [$minDepth, $maxDepth], avg=$avgDepth")
            Log.d(TAG, "Sample (first 8 values): ${grid[0].take(8)}")
            
            return DepthGridResponse(depth_grid = depthResponse.depth_grid)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse depth grid: ${e.message}", e)
            Log.e(TAG, "Response was:\n$response")
            throw e
        }
    }
    
    /**
     * 从紧凑格式字符串解析深度网格
     */
    private fun parseCompactDepthGrid(compactString: String): List<List<Int>> {
        val values = compactString.split(",").mapNotNull { it.trim().toIntOrNull() }
        val grid = mutableListOf<List<Int>>()
        
        for (row in 0 until DEPTH_GRID_SIZE) {
            val startIdx = row * DEPTH_GRID_SIZE
            val endIdx = startIdx + DEPTH_GRID_SIZE
            
            if (endIdx <= values.size) {
                grid.add(values.subList(startIdx, endIdx))
            } else {
                val partialRow = if (startIdx < values.size) {
                    values.subList(startIdx, values.size)
                } else {
                    emptyList()
                }
                val lastValue = partialRow.lastOrNull() ?: grid.lastOrNull()?.last() ?: 150
                val completeRow = partialRow + List(DEPTH_GRID_SIZE - partialRow.size) { lastValue }
                grid.add(completeRow)
            }
        }
        
        while (grid.size < DEPTH_GRID_SIZE) {
            val lastRow = grid.lastOrNull() ?: List(DEPTH_GRID_SIZE) { 150 }
            grid.add(lastRow)
        }
        
        return grid
    }
    
    /**
     * 解析蒙版网格响应
     */
    private fun parseMaskGridResponse(response: String): MaskGridResponse {
        try {
            Log.d(TAG, "=== PARSING MASK GRID RESPONSE ===")
            Log.d(TAG, "Response length: ${response.length}")
            
            val jsonText = extractJson(response)
            Log.d(TAG, "Extracted JSON length: ${jsonText.length}")
            
            val json = Json { 
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }
            
            // 尝试解析新格式（mask）或旧格式（subject_mask）
            val maskString = try {
                val maskResponse = json.decodeFromString<MaskGridResponse>(jsonText)
                maskResponse.mask
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse with 'mask' field, trying 'subject_mask'...")
                // 尝试旧格式
                @Serializable
                data class OldMaskResponse(val subject_mask: String)
                val oldResponse = json.decodeFromString<OldMaskResponse>(jsonText)
                oldResponse.subject_mask
            }
            
            // 解析紧凑格式的字符串
            val values = maskString.split(",").mapNotNull { it.trim().toIntOrNull() }
            
            val expectedValues = MASK_GRID_SIZE * MASK_GRID_SIZE
            Log.d(TAG, "Parsed ${values.size} mask values (expected $expectedValues)")
            
            if (values.size < expectedValues) {
                Log.w(TAG, "⚠️ INCOMPLETE MASK DATA: only ${values.size}/$expectedValues values")
                Log.w(TAG, "⚠️ This indicates max_tokens is still too low or AI truncated response")
            }
            
            // 转换为 2D 网格
            val grid = mutableListOf<List<Int>>()
            for (row in 0 until MASK_GRID_SIZE) {
                val startIdx = row * MASK_GRID_SIZE
                val endIdx = startIdx + MASK_GRID_SIZE
                
                if (endIdx <= values.size) {
                    grid.add(values.subList(startIdx, endIdx))
                } else {
                    val partialRow = if (startIdx < values.size) {
                        values.subList(startIdx, values.size)
                    } else {
                        emptyList()
                    }
                    val lastValue = partialRow.lastOrNull() ?: grid.lastOrNull()?.last() ?: 0
                    val completeRow = partialRow + List(MASK_GRID_SIZE - partialRow.size) { lastValue }
                    grid.add(completeRow)
                    Log.w(TAG, "⚠️ Padded incomplete row $row with value $lastValue")
                }
            }
            
            while (grid.size < MASK_GRID_SIZE) {
                val lastRow = grid.lastOrNull() ?: List(MASK_GRID_SIZE) { 0 }
                grid.add(lastRow)
                Log.w(TAG, "⚠️ Added missing row ${grid.size - 1}")
            }
            
            // 统计蒙版覆盖率
            val totalPixels = MASK_GRID_SIZE * MASK_GRID_SIZE
            val subjectPixels = grid.flatten().count { it > 128 }
            val coverage = subjectPixels * 100 / totalPixels
            
            Log.d(TAG, "✅ Subject mask parsed successfully")
            Log.d(TAG, "Subject coverage: $coverage% ($subjectPixels/$totalPixels pixels)")
            Log.d(TAG, "Sample (first 8 values): ${grid[0].take(8)}")
            
            return MaskGridResponse(mask = maskString)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse subject mask: ${e.message}", e)
            Log.e(TAG, "Response was:\n$response")
            throw e
        }
    }
    
    /**
     * 从紧凑格式字符串解析蒙版网格
     */
    private fun parseCompactMaskGrid(compactString: String): List<List<Int>> {
        val values = compactString.split(",").mapNotNull { it.trim().toIntOrNull() }
        val grid = mutableListOf<List<Int>>()
        
        for (row in 0 until MASK_GRID_SIZE) {
            val startIdx = row * MASK_GRID_SIZE
            val endIdx = startIdx + MASK_GRID_SIZE
            
            if (endIdx <= values.size) {
                grid.add(values.subList(startIdx, endIdx))
            } else {
                val partialRow = if (startIdx < values.size) {
                    values.subList(startIdx, values.size)
                } else {
                    emptyList()
                }
                val lastValue = partialRow.lastOrNull() ?: grid.lastOrNull()?.last() ?: 0
                val completeRow = partialRow + List(MASK_GRID_SIZE - partialRow.size) { lastValue }
                grid.add(completeRow)
            }
        }
        
        while (grid.size < MASK_GRID_SIZE) {
            val lastRow = grid.lastOrNull() ?: List(MASK_GRID_SIZE) { 0 }
            grid.add(lastRow)
        }
        
        return grid
    }
    
    /**
     * 从响应中提取 JSON
     */
    private fun extractJson(response: String): String {
        Log.d(TAG, "Extracting JSON from response...")
        
        // 先移除可能的 markdown 代码块标记
        var cleaned = response.trim()
        
        // 移除开头的 ```json 或 ```
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trim()
            Log.d(TAG, "Removed ```json prefix")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
            Log.d(TAG, "Removed ``` prefix")
        }
        
        // 移除结尾的 ```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
            Log.d(TAG, "Removed ``` suffix")
        }
        
        // 检测并修复截断的 JSON
        cleaned = fixTruncatedJson(cleaned)
        
        // 现在尝试提取 JSON
        // 1. 如果已经是有效的 JSON 开头，直接返回
        if (cleaned.startsWith("{") || cleaned.startsWith("[")) {
            Log.d(TAG, "✅ Found valid JSON after cleaning")
            return cleaned
        }
        
        // 2. 尝试使用正则提取 JSON 代码块
        val jsonBlockPatterns = listOf(
            "```json\\s*([\\s\\S]*?)```".toRegex(),  // ```json ... ```
            "```\\s*([\\s\\S]*?)```".toRegex(),      // ``` ... ```
            "`([\\s\\S]*?)`".toRegex()               // ` ... `
        )
        
        for ((index, pattern) in jsonBlockPatterns.withIndex()) {
            val match = pattern.find(response)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                // 验证是否是有效的 JSON 开头
                if (extracted.startsWith("{") || extracted.startsWith("[")) {
                    Log.d(TAG, "✅ Extracted JSON from code block (pattern $index)")
                    return fixTruncatedJson(extracted)
                } else {
                    Log.d(TAG, "⚠️ Pattern $index matched but content doesn't start with { or [")
                }
            }
        }
        
        // 3. 尝试提取 {} 包裹的内容
        val jsonRegex = "\\{[\\s\\S]*\\}".toRegex()
        val jsonMatch = jsonRegex.find(response)
        if (jsonMatch != null) {
            Log.d(TAG, "✅ Extracted JSON from braces")
            return fixTruncatedJson(jsonMatch.value)
        }
        
        // 4. 直接返回清理后的文本
        Log.d(TAG, "⚠️ No JSON pattern matched, using cleaned response")
        return cleaned
    }
    
    /**
     * 修复截断的 JSON
     * 处理 AI 响应被截断导致的不完整 JSON
     */
    private fun fixTruncatedJson(json: String): String {
        var fixed = json.trim()
        
        // 检测是否被截断（没有正确的结束符）
        val openBraces = fixed.count { it == '{' }
        val closeBraces = fixed.count { it == '}' }
        val openBrackets = fixed.count { it == '[' }
        val closeBrackets = fixed.count { it == ']' }
        val openQuotes = fixed.count { it == '"' }
        
        val isTruncated = openBraces != closeBraces || 
                         openBrackets != closeBrackets ||
                         openQuotes % 2 != 0  // 引号必须成对
        
        if (isTruncated) {
            Log.w(TAG, "⚠️ Detected truncated JSON!")
            Log.w(TAG, "  Open braces: $openBraces, Close braces: $closeBraces")
            Log.w(TAG, "  Open brackets: $openBrackets, Close brackets: $closeBrackets")
            Log.w(TAG, "  Quotes: $openQuotes (should be even)")
            Log.w(TAG, "  Last 50 chars: ${fixed.takeLast(50)}")
            
            // 1. 如果字符串引号不成对，补全引号
            if (openQuotes % 2 != 0) {
                // 检查最后一个字符是否在字符串内
                val lastQuoteIndex = fixed.lastIndexOf('"')
                if (lastQuoteIndex >= 0) {
                    // 在字符串内被截断，移除末尾的不完整部分
                    val beforeLastQuote = fixed.substring(0, lastQuoteIndex)
                    val afterLastQuote = fixed.substring(lastQuoteIndex + 1)
                    
                    // 如果是 "key": "value 这种情况，补全引号
                    if (afterLastQuote.contains(':')) {
                        // 这是 key，保留
                        fixed = fixed + "\""
                        Log.d(TAG, "  Added closing quote for key")
                    } else {
                        // 这是 value，移除不完整的部分并补全引号
                        val lastComma = afterLastQuote.lastIndexOf(',')
                        if (lastComma >= 0) {
                            fixed = beforeLastQuote + "\"" + afterLastQuote.substring(0, lastComma) + "\""
                        } else {
                            fixed = beforeLastQuote + "\"" + afterLastQuote + "\""
                        }
                        Log.d(TAG, "  Fixed incomplete string value")
                    }
                }
            }
            
            // 2. 移除末尾的逗号和空白
            fixed = fixed.trimEnd(',', ' ', '\n', '\r', '\t')
            
            // 3. 如果末尾是不完整的数组元素，移除它
            val lastCommaIndex = fixed.lastIndexOf(',')
            if (lastCommaIndex > 0) {
                val afterComma = fixed.substring(lastCommaIndex + 1).trim()
                // 如果逗号后面只有数字或不完整的内容，移除它
                if (afterComma.matches(Regex("\\d*")) || afterComma.isEmpty()) {
                    fixed = fixed.substring(0, lastCommaIndex).trim()
                    Log.d(TAG, "  Removed incomplete element after last comma")
                }
            }
            
            // 4. 补全缺失的括号
            val missingCloseBrackets = openBrackets - closeBrackets
            val missingCloseBraces = openBraces - closeBraces
            
            if (missingCloseBrackets > 0) {
                fixed += "]".repeat(missingCloseBrackets)
                Log.d(TAG, "  Added $missingCloseBrackets closing brackets")
            }
            
            if (missingCloseBraces > 0) {
                fixed += "}".repeat(missingCloseBraces)
                Log.d(TAG, "  Added $missingCloseBraces closing braces")
            }
            
            Log.d(TAG, "✅ Fixed truncated JSON")
            Log.d(TAG, "  New last 50 chars: ${fixed.takeLast(50)}")
        }
        
        return fixed
    }
    
    /**
     * 生成默认深度分析（降级方案）
     */
    private fun generateDefaultDepthAnalysis(): DepthAnalysis {
        Log.w(TAG, "⚠️ Using default depth analysis (radial gradient fallback)")
        
        // 生成径向渐变深度图
        val depthGrid = List(DEPTH_GRID_SIZE) { y ->
            List(DEPTH_GRID_SIZE) { x ->
                val centerX = DEPTH_GRID_SIZE / 2f
                val centerY = DEPTH_GRID_SIZE / 2f
                val dx = x - centerX
                val dy = y - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
                val maxDistance = Math.sqrt((centerX * centerX + centerY * centerY).toDouble())
                (distance / maxDistance * 255).toInt().coerceIn(0, 255)
            }
        }
        
        // 生成中心圆形蒙版
        val maskGrid = List(MASK_GRID_SIZE) { y ->
            List(MASK_GRID_SIZE) { x ->
                val centerX = MASK_GRID_SIZE / 2f
                val centerY = MASK_GRID_SIZE / 2f
                val dx = x - centerX
                val dy = y - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
                val radius = MASK_GRID_SIZE / 3f
                
                if (distance < radius) {
                    255  // 主体
                } else if (distance < radius + 5) {
                    // 羽化边缘
                    ((1f - (distance - radius) / 5f) * 255).toInt().coerceIn(0, 255)
                } else {
                    0  // 背景
                }
            }
        }
        
        return DepthAnalysis(
            depth_grid = depthGrid,
            subject_mask = maskGrid
        )
    }
    
    /**
     * 获取焦点位置的深度值
     */
    fun getFocusDepth(analysis: DepthAnalysis, focusX: Float, focusY: Float): Int {
        val gridX = (focusX * DEPTH_GRID_SIZE).toInt().coerceIn(0, DEPTH_GRID_SIZE - 1)
        val gridY = (focusY * DEPTH_GRID_SIZE).toInt().coerceIn(0, DEPTH_GRID_SIZE - 1)
        return analysis.depth_grid[gridY][gridX]
    }
    
    /**
     * 获取建议的焦点位置（深度最小的位置）
     */
    fun getSuggestedFocus(analysis: DepthAnalysis): Pair<Float, Float> {
        var minDepth = 255
        var minX = DEPTH_GRID_SIZE / 2
        var minY = DEPTH_GRID_SIZE / 2
        
        analysis.depth_grid.forEachIndexed { y, row ->
            row.forEachIndexed { x, depth ->
                if (depth < minDepth) {
                    minDepth = depth
                    minX = x
                    minY = y
                }
            }
        }
        
        val focusX = (minX + 0.5f) / DEPTH_GRID_SIZE
        val focusY = (minY + 0.5f) / DEPTH_GRID_SIZE
        
        Log.d(TAG, "Suggested focus: ($focusX, $focusY), depth=$minDepth at grid[$minY][$minX]")
        
        return Pair(focusX, focusY)
    }
    
    /**
     * 将网格深度转换为完整深度图
     */
    fun generateDepthMap(
        analysis: DepthAnalysis,
        width: Int,
        height: Int
    ): Bitmap {
        Log.d(TAG, "Generating depth map: ${width}x${height}")
        
        val depthMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val cellWidth = width.toFloat() / DEPTH_GRID_SIZE
        val cellHeight = height.toFloat() / DEPTH_GRID_SIZE
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 计算所属网格
                val gx = min((x / cellWidth).toInt(), DEPTH_GRID_SIZE - 1)
                val gy = min((y / cellHeight).toInt(), DEPTH_GRID_SIZE - 1)
                
                // 获取深度值
                val depth = analysis.depth_grid[gy][gx]
                
                // 转换为灰度颜色
                val color = (0xFF shl 24) or (depth shl 16) or (depth shl 8) or depth
                depthMap.setPixel(x, y, color)
            }
        }
        
        // 可选：应用双线性插值平滑
        return smoothDepthMap(depthMap)
    }
    
    /**
     * 将蒙版网格转换为完整蒙版图
     */
    fun generateSubjectMask(
        analysis: DepthAnalysis,
        width: Int,
        height: Int
    ): Bitmap {
        Log.d(TAG, "Generating subject mask: ${width}x${height}")
        
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val cellWidth = width.toFloat() / MASK_GRID_SIZE
        val cellHeight = height.toFloat() / MASK_GRID_SIZE
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 计算所属网格
                val gx = min((x / cellWidth).toInt(), MASK_GRID_SIZE - 1)
                val gy = min((y / cellHeight).toInt(), MASK_GRID_SIZE - 1)
                
                // 获取蒙版值
                val value = analysis.subject_mask[gy][gx]
                
                // 转换为灰度颜色
                val color = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
                mask.setPixel(x, y, color)
            }
        }
        
        // 应用平滑以改善边缘
        return smoothDepthMap(mask)
    }
    
    /**
     * 平滑深度图（简单高斯模糊）
     */
    private fun smoothDepthMap(depthMap: Bitmap): Bitmap {
        // TODO: 实现高斯模糊
        // 当前直接返回原图
        return depthMap
    }
}
