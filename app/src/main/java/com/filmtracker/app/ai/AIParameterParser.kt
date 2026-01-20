package com.filmtracker.app.ai

import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import org.json.JSONObject

/**
 * AI 参数解析器
 * 
 * 从 AI 回复中提取 JSON 格式的调色参数
 */
object AIParameterParser {
    private const val TAG = "AIParameterParser"
    
    /**
     * 从 AI 回复文本中提取参数
     * 
     * @param aiResponse AI 的完整回复文本
     * @return 解析出的参数对象，如果没有找到参数则返回 null
     */
    fun parseParameters(aiResponse: String): ParsedParameters? {
        try {
            // 查找 JSON 代码块
            val jsonPattern = Regex("""```json\s*(\{[\s\S]*?\})\s*```""")
            val match = jsonPattern.find(aiResponse)
            
            val jsonString = if (match != null) {
                // 从代码块中提取
                match.groupValues[1]
            } else {
                // 尝试直接查找 JSON 对象
                val directJsonPattern = Regex("""\{[\s\S]*"parameters"[\s\S]*\}""")
                val directMatch = directJsonPattern.find(aiResponse)
                directMatch?.value
            }
            
            if (jsonString == null) {
                Log.d(TAG, "未在回复中找到 JSON 参数")
                return null
            }
            
            Log.d(TAG, "找到 JSON: $jsonString")
            
            val json = JSONObject(jsonString)
            val params = json.optJSONObject("parameters") ?: return null
            val description = json.optString("description", "")
            
            // 创建参数对象
            val adjustmentParams = BasicAdjustmentParams().apply {
                // 基础调整
                if (params.has("exposure")) globalExposure = params.getDouble("exposure").toFloat()
                if (params.has("contrast")) contrast = params.getDouble("contrast").toFloat()
                if (params.has("saturation")) saturation = params.getDouble("saturation").toFloat()
                
                // 色调调整
                if (params.has("highlights")) highlights = params.getDouble("highlights").toFloat()
                if (params.has("shadows")) shadows = params.getDouble("shadows").toFloat()
                if (params.has("whites")) whites = params.getDouble("whites").toFloat()
                if (params.has("blacks")) blacks = params.getDouble("blacks").toFloat()
                
                // 存在感
                if (params.has("clarity")) clarity = params.getDouble("clarity").toFloat()
                if (params.has("vibrance")) vibrance = params.getDouble("vibrance").toFloat()
                
                // 颜色
                if (params.has("temperature")) temperature = params.getDouble("temperature").toFloat()
                if (params.has("tint")) tint = params.getDouble("tint").toFloat()
                
                // 色彩分级
                if (params.has("gradingHighlightsTemp")) gradingHighlightsTemp = params.getDouble("gradingHighlightsTemp").toFloat()
                if (params.has("gradingHighlightsTint")) gradingHighlightsTint = params.getDouble("gradingHighlightsTint").toFloat()
                if (params.has("gradingMidtonesTemp")) gradingMidtonesTemp = params.getDouble("gradingMidtonesTemp").toFloat()
                if (params.has("gradingMidtonesTint")) gradingMidtonesTint = params.getDouble("gradingMidtonesTint").toFloat()
                if (params.has("gradingShadowsTemp")) gradingShadowsTemp = params.getDouble("gradingShadowsTemp").toFloat()
                if (params.has("gradingShadowsTint")) gradingShadowsTint = params.getDouble("gradingShadowsTint").toFloat()
                if (params.has("gradingBlending")) gradingBlending = params.getDouble("gradingBlending").toFloat()
                if (params.has("gradingBalance")) gradingBalance = params.getDouble("gradingBalance").toFloat()
                
                // 效果
                if (params.has("texture")) texture = params.getDouble("texture").toFloat()
                if (params.has("dehaze")) dehaze = params.getDouble("dehaze").toFloat()
                if (params.has("vignette")) vignette = params.getDouble("vignette").toFloat()
                if (params.has("grain")) grain = params.getDouble("grain").toFloat()
                
                // 细节
                if (params.has("sharpening")) sharpening = params.getDouble("sharpening").toFloat()
                if (params.has("noiseReduction")) noiseReduction = params.getDouble("noiseReduction").toFloat()
            }
            
            return ParsedParameters(adjustmentParams, description)
            
        } catch (e: Exception) {
            Log.e(TAG, "解析参数失败", e)
            return null
        }
    }
    
    /**
     * 验证参数是否在有效范围内
     */
    fun validateParameters(params: BasicAdjustmentParams): BasicAdjustmentParams {
        return params.copy(
            globalExposure = params.globalExposure.coerceIn(-5f, 5f),
            contrast = params.contrast.coerceIn(-100f, 100f),
            saturation = params.saturation.coerceIn(-100f, 100f),
            highlights = params.highlights.coerceIn(-100f, 100f),
            shadows = params.shadows.coerceIn(-100f, 100f),
            whites = params.whites.coerceIn(-100f, 100f),
            blacks = params.blacks.coerceIn(-100f, 100f),
            clarity = params.clarity.coerceIn(-100f, 100f),
            vibrance = params.vibrance.coerceIn(-100f, 100f),
            temperature = params.temperature.coerceIn(-100f, 100f),
            tint = params.tint.coerceIn(-100f, 100f),
            gradingHighlightsTemp = params.gradingHighlightsTemp.coerceIn(-100f, 100f),
            gradingHighlightsTint = params.gradingHighlightsTint.coerceIn(-100f, 100f),
            gradingMidtonesTemp = params.gradingMidtonesTemp.coerceIn(-100f, 100f),
            gradingMidtonesTint = params.gradingMidtonesTint.coerceIn(-100f, 100f),
            gradingShadowsTemp = params.gradingShadowsTemp.coerceIn(-100f, 100f),
            gradingShadowsTint = params.gradingShadowsTint.coerceIn(-100f, 100f),
            gradingBlending = params.gradingBlending.coerceIn(0f, 100f),
            gradingBalance = params.gradingBalance.coerceIn(-100f, 100f),
            texture = params.texture.coerceIn(-100f, 100f),
            dehaze = params.dehaze.coerceIn(-100f, 100f),
            vignette = params.vignette.coerceIn(-100f, 100f),
            grain = params.grain.coerceIn(0f, 100f),
            sharpening = params.sharpening.coerceIn(0f, 100f),
            noiseReduction = params.noiseReduction.coerceIn(0f, 100f)
        )
    }
    
    /**
     * 生成参数摘要文本
     */
    fun generateSummary(params: BasicAdjustmentParams): String {
        val changes = mutableListOf<String>()
        
        if (params.globalExposure != 0f) {
            changes.add("曝光 ${formatValue(params.globalExposure)} EV")
        }
        if (params.contrast != 0f) {
            changes.add("对比度 ${formatValue(params.contrast)}")
        }
        if (params.highlights != 0f) {
            changes.add("高光 ${formatValue(params.highlights)}")
        }
        if (params.shadows != 0f) {
            changes.add("阴影 ${formatValue(params.shadows)}")
        }
        if (params.whites != 0f) {
            changes.add("白场 ${formatValue(params.whites)}")
        }
        if (params.blacks != 0f) {
            changes.add("黑场 ${formatValue(params.blacks)}")
        }
        if (params.saturation != 0f) {
            changes.add("饱和度 ${formatValue(params.saturation)}")
        }
        if (params.vibrance != 0f) {
            changes.add("自然饱和度 ${formatValue(params.vibrance)}")
        }
        if (params.temperature != 0f) {
            changes.add("色温 ${formatValue(params.temperature)}")
        }
        if (params.tint != 0f) {
            changes.add("色调 ${formatValue(params.tint)}")
        }
        if (params.clarity != 0f) {
            changes.add("清晰度 ${formatValue(params.clarity)}")
        }
        if (params.texture != 0f) {
            changes.add("纹理 ${formatValue(params.texture)}")
        }
        if (params.dehaze != 0f) {
            changes.add("去雾 ${formatValue(params.dehaze)}")
        }
        if (params.vignette != 0f) {
            changes.add("晕影 ${formatValue(params.vignette)}")
        }
        if (params.grain != 0f) {
            changes.add("颗粒 ${params.grain.toInt()}")
        }
        if (params.sharpening != 0f) {
            changes.add("锐化 ${params.sharpening.toInt()}")
        }
        if (params.noiseReduction != 0f) {
            changes.add("降噪 ${params.noiseReduction.toInt()}")
        }
        
        return if (changes.isEmpty()) {
            "无调整"
        } else {
            changes.joinToString("、")
        }
    }
    
    private fun formatValue(value: Float): String {
        val sign = if (value > 0) "+" else ""
        return if (value == value.toInt().toFloat()) {
            "$sign${value.toInt()}"
        } else {
            "$sign%.1f".format(value)
        }
    }
}

/**
 * 解析结果
 */
data class ParsedParameters(
    val parameters: BasicAdjustmentParams,
    val description: String
)
