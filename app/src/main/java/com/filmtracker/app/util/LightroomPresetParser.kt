package com.filmtracker.app.util

import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import org.json.JSONObject
import java.io.File

/**
 * Lightroom 预设解析器
 * 
 * 支持的格式：
 * - .xmp (Lightroom Classic 预设)
 * - .lrtemplate (旧版 Lightroom 预设)
 * - JSON 格式的预设导出
 */
object LightroomPresetParser {
    private const val TAG = "LRPresetParser"
    
    /**
     * 从文件解析 Lightroom 预设
     */
    fun parsePresetFile(file: File): BasicAdjustmentParams? {
        return when (file.extension.lowercase()) {
            "xmp" -> parseXmpPreset(file.readText())
            "lrtemplate" -> parseLrTemplatePreset(file.readText())
            "json" -> parseJsonPreset(file.readText())
            else -> {
                Log.e(TAG, "不支持的文件格式: ${file.extension}")
                null
            }
        }
    }
    
    /**
     * 从文本内容解析预设
     */
    fun parsePresetText(text: String): BasicAdjustmentParams? {
        return when {
            text.trim().startsWith("<") -> parseXmpPreset(text)
            text.trim().startsWith("{") -> parseJsonPreset(text)
            else -> parseLrTemplatePreset(text)
        }
    }
    
    /**
     * 解析 XMP 格式预设（Lightroom Classic）
     */
    private fun parseXmpPreset(xmpContent: String): BasicAdjustmentParams? {
        try {
            val params = BasicAdjustmentParams()
            
            // 基础调整
            extractXmpValue(xmpContent, "crs:Exposure2012")?.let { 
                params.globalExposure = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:Contrast2012")?.let { 
                params.contrast = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:Saturation")?.let { 
                params.saturation = it.toFloat()
            }
            
            // 色调调整
            extractXmpValue(xmpContent, "crs:Highlights2012")?.let { 
                params.highlights = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:Shadows2012")?.let { 
                params.shadows = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:Whites2012")?.let { 
                params.whites = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:Blacks2012")?.let { 
                params.blacks = it.toFloat()
            }
            
            // 存在感
            extractXmpValue(xmpContent, "crs:Clarity2012")?.let { 
                params.clarity = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:Vibrance")?.let { 
                params.vibrance = it.toFloat()
            }
            
            // 颜色
            extractXmpValue(xmpContent, "crs:Temperature")?.let { 
                // LR 色温范围：2000-50000，转换为 -100 到 +100
                val kelvin = it.toFloat()
                params.temperature = AdobeParameterConverter.kelvinToTemperature(kelvin)
            }
            extractXmpValue(xmpContent, "crs:Tint")?.let { 
                params.tint = it.toFloat()
            }
            
            // 效果
            extractXmpValue(xmpContent, "crs:Texture")?.let { 
                params.texture = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:Dehaze")?.let { 
                params.dehaze = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:PostCropVignetteAmount")?.let { 
                params.vignette = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:GrainAmount")?.let { 
                params.grain = it.toFloat()
            }
            
            // 细节
            extractXmpValue(xmpContent, "crs:Sharpness")?.let { 
                params.sharpening = it.toFloat()
            }
            extractXmpValue(xmpContent, "crs:LuminanceSmoothing")?.let { 
                params.noiseReduction = it.toFloat()
            }
            
            // 色调曲线
            extractToneCurve(xmpContent)?.let { curve ->
                params.enableRgbCurve = true
                params.rgbCurvePoints = curve
            }
            
            // HSL 调整
            extractHSL(xmpContent)?.let { hsl ->
                params.enableHSL = true
                params.hslHueShift = hsl.hueShift
                params.hslSaturation = hsl.saturation
                params.hslLuminance = hsl.luminance
            }
            
            Log.d(TAG, "成功解析 XMP 预设")
            return params
            
        } catch (e: Exception) {
            Log.e(TAG, "解析 XMP 预设失败", e)
            return null
        }
    }
    
    /**
     * 解析 LRTemplate 格式预设（旧版）
     */
    private fun parseLrTemplatePreset(content: String): BasicAdjustmentParams? {
        try {
            // LRTemplate 是 Lua 格式，需要解析键值对
            val params = BasicAdjustmentParams()
            
            extractLuaValue(content, "Exposure2012")?.let { 
                params.globalExposure = it.toFloat()
            }
            extractLuaValue(content, "Contrast2012")?.let { 
                params.contrast = it.toFloat()
            }
            extractLuaValue(content, "Saturation")?.let { 
                params.saturation = it.toFloat()
            }
            extractLuaValue(content, "Highlights2012")?.let { 
                params.highlights = it.toFloat()
            }
            extractLuaValue(content, "Shadows2012")?.let { 
                params.shadows = it.toFloat()
            }
            extractLuaValue(content, "Whites2012")?.let { 
                params.whites = it.toFloat()
            }
            extractLuaValue(content, "Blacks2012")?.let { 
                params.blacks = it.toFloat()
            }
            extractLuaValue(content, "Clarity2012")?.let { 
                params.clarity = it.toFloat()
            }
            extractLuaValue(content, "Vibrance")?.let { 
                params.vibrance = it.toFloat()
            }
            extractLuaValue(content, "Temperature")?.let { 
                val kelvin = it.toFloat()
                params.temperature = AdobeParameterConverter.kelvinToTemperature(kelvin)
            }
            extractLuaValue(content, "Tint")?.let { 
                params.tint = it.toFloat()
            }
            
            Log.d(TAG, "成功解析 LRTemplate 预设")
            return params
            
        } catch (e: Exception) {
            Log.e(TAG, "解析 LRTemplate 预设失败", e)
            return null
        }
    }
    
    /**
     * 解析 JSON 格式预设
     */
    private fun parseJsonPreset(jsonContent: String): BasicAdjustmentParams? {
        try {
            val json = JSONObject(jsonContent)
            val params = BasicAdjustmentParams()
            
            // 基础调整
            json.optDouble("exposure", 0.0).let { 
                params.globalExposure = it.toFloat()
            }
            json.optDouble("contrast", 0.0).let { 
                params.contrast = it.toFloat()
            }
            json.optDouble("saturation", 0.0).let { 
                params.saturation = it.toFloat()
            }
            
            // 色调调整
            json.optDouble("highlights", 0.0).let { 
                params.highlights = it.toFloat()
            }
            json.optDouble("shadows", 0.0).let { 
                params.shadows = it.toFloat()
            }
            json.optDouble("whites", 0.0).let { 
                params.whites = it.toFloat()
            }
            json.optDouble("blacks", 0.0).let { 
                params.blacks = it.toFloat()
            }
            
            // 存在感
            json.optDouble("clarity", 0.0).let { 
                params.clarity = it.toFloat()
            }
            json.optDouble("vibrance", 0.0).let { 
                params.vibrance = it.toFloat()
            }
            
            // 颜色
            json.optDouble("temperature", 0.0).let { 
                params.temperature = it.toFloat()
            }
            json.optDouble("tint", 0.0).let { 
                params.tint = it.toFloat()
            }
            
            Log.d(TAG, "成功解析 JSON 预设")
            return params
            
        } catch (e: Exception) {
            Log.e(TAG, "解析 JSON 预设失败", e)
            return null
        }
    }
    
    /**
     * 从 XMP 中提取值
     */
    private fun extractXmpValue(xmp: String, key: String): String? {
        val pattern = Regex("""$key="([^"]+)"""")
        return pattern.find(xmp)?.groupValues?.get(1)
    }
    
    /**
     * 从 Lua 格式中提取值
     */
    private fun extractLuaValue(lua: String, key: String): String? {
        val pattern = Regex("""$key\s*=\s*([^,\n]+)""")
        return pattern.find(lua)?.groupValues?.get(1)?.trim()
    }
    
    /**
     * 提取色调曲线
     */
    private fun extractToneCurve(xmp: String): List<Pair<Float, Float>>? {
        try {
            val curvePattern = Regex("""crs:ToneCurvePV2012[^>]*>([^<]+)</""")
            val curveMatch = curvePattern.find(xmp) ?: return null
            val curveData = curveMatch.groupValues[1].trim()
            
            // 解析曲线点：格式如 "0, 0, 64, 60, 128, 128, 192, 196, 255, 255"
            val values = curveData.split(",").map { it.trim().toFloat() }
            val points = mutableListOf<Pair<Float, Float>>()
            
            for (i in values.indices step 2) {
                if (i + 1 < values.size) {
                    val x = values[i] / 255f  // 归一化到 0-1
                    val y = values[i + 1] / 255f
                    points.add(Pair(x, y))
                }
            }
            
            return if (points.isNotEmpty()) points else null
            
        } catch (e: Exception) {
            Log.e(TAG, "提取色调曲线失败", e)
            return null
        }
    }
    
    /**
     * 提取 HSL 调整
     */
    private fun extractHSL(xmp: String): HSLData? {
        try {
            val hueShift = FloatArray(8) { 0f }
            val saturation = FloatArray(8) { 0f }
            val luminance = FloatArray(8) { 0f }
            
            // 色相调整
            val hueKeys = listOf(
                "crs:HueAdjustmentRed", "crs:HueAdjustmentOrange", 
                "crs:HueAdjustmentYellow", "crs:HueAdjustmentGreen",
                "crs:HueAdjustmentAqua", "crs:HueAdjustmentBlue",
                "crs:HueAdjustmentPurple", "crs:HueAdjustmentMagenta"
            )
            hueKeys.forEachIndexed { index, key ->
                extractXmpValue(xmp, key)?.let { 
                    hueShift[index] = it.toFloat()
                }
            }
            
            // 饱和度调整
            val satKeys = listOf(
                "crs:SaturationAdjustmentRed", "crs:SaturationAdjustmentOrange",
                "crs:SaturationAdjustmentYellow", "crs:SaturationAdjustmentGreen",
                "crs:SaturationAdjustmentAqua", "crs:SaturationAdjustmentBlue",
                "crs:SaturationAdjustmentPurple", "crs:SaturationAdjustmentMagenta"
            )
            satKeys.forEachIndexed { index, key ->
                extractXmpValue(xmp, key)?.let { 
                    saturation[index] = it.toFloat()
                }
            }
            
            // 明度调整
            val lumKeys = listOf(
                "crs:LuminanceAdjustmentRed", "crs:LuminanceAdjustmentOrange",
                "crs:LuminanceAdjustmentYellow", "crs:LuminanceAdjustmentGreen",
                "crs:LuminanceAdjustmentAqua", "crs:LuminanceAdjustmentBlue",
                "crs:LuminanceAdjustmentPurple", "crs:LuminanceAdjustmentMagenta"
            )
            lumKeys.forEachIndexed { index, key ->
                extractXmpValue(xmp, key)?.let { 
                    luminance[index] = it.toFloat()
                }
            }
            
            // 检查是否有非零值
            val hasHSL = hueShift.any { it != 0f } || 
                        saturation.any { it != 0f } || 
                        luminance.any { it != 0f }
            
            return if (hasHSL) HSLData(hueShift, saturation, luminance) else null
            
        } catch (e: Exception) {
            Log.e(TAG, "提取 HSL 失败", e)
            return null
        }
    }
    
    /**
     * 生成预设摘要
     */
    fun generatePresetSummary(params: BasicAdjustmentParams): String {
        val changes = mutableListOf<String>()
        
        if (params.globalExposure != 0f) changes.add("曝光 ${formatValue(params.globalExposure)} EV")
        if (params.contrast != 0f) changes.add("对比度 ${formatValue(params.contrast)}")
        if (params.highlights != 0f) changes.add("高光 ${formatValue(params.highlights)}")
        if (params.shadows != 0f) changes.add("阴影 ${formatValue(params.shadows)}")
        if (params.saturation != 0f) changes.add("饱和度 ${formatValue(params.saturation)}")
        if (params.vibrance != 0f) changes.add("自然饱和度 ${formatValue(params.vibrance)}")
        if (params.clarity != 0f) changes.add("清晰度 ${formatValue(params.clarity)}")
        if (params.temperature != 0f) changes.add("色温 ${formatValue(params.temperature)}")
        
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
    
    /**
     * HSL 数据
     */
    private data class HSLData(
        val hueShift: FloatArray,
        val saturation: FloatArray,
        val luminance: FloatArray
    )
}

/**
 * Lightroom 预设数据类（包含元数据）
 */
data class LightroomPreset(
    val name: String,
    val nameEn: String = "",
    val description: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val parameters: BasicAdjustmentParams
)

/**
 * 从 Assets 解析预设（包含元数据）
 */
fun parsePresetFromAssets(context: android.content.Context, assetPath: String): LightroomPreset? {
    return try {
        val jsonContent = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        parsePresetWithMetadata(jsonContent)
    } catch (e: Exception) {
        Log.e("LRPresetParser", "从 Assets 加载预设失败: $assetPath", e)
        null
    }
}

/**
 * 解析包含元数据的 JSON 预设
 */
fun parsePresetWithMetadata(jsonContent: String): LightroomPreset? {
    try {
        val json = JSONObject(jsonContent)
        
        // 解析元数据
        val name = json.optString("name", "未命名预设")
        val nameEn = json.optString("nameEn", "")
        val description = json.optString("description", "")
        val category = json.optString("category", "")
        
        // 解析标签
        val tags = mutableListOf<String>()
        val tagsArray = json.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }
        
        // 解析参数
        val paramsJson = json.optJSONObject("parameters") ?: json
        val params = parseJsonParameters(paramsJson)
        
        return LightroomPreset(
            name = name,
            nameEn = nameEn,
            description = description,
            category = category,
            tags = tags,
            parameters = params
        )
        
    } catch (e: Exception) {
        Log.e("LRPresetParser", "解析预设元数据失败", e)
        return null
    }
}

/**
 * 解析 JSON 参数对象
 */
private fun parseJsonParameters(json: JSONObject): BasicAdjustmentParams {
    val params = BasicAdjustmentParams()
    
    // 基础调整
    json.optDouble("exposure", 0.0).let { 
        params.globalExposure = it.toFloat()
    }
    json.optDouble("contrast", 0.0).let { 
        params.contrast = it.toFloat()
    }
    json.optDouble("saturation", 0.0).let { 
        params.saturation = it.toFloat()
    }
    
    // 色调调整
    json.optDouble("highlights", 0.0).let { 
        params.highlights = it.toFloat()
    }
    json.optDouble("shadows", 0.0).let { 
        params.shadows = it.toFloat()
    }
    json.optDouble("whites", 0.0).let { 
        params.whites = it.toFloat()
    }
    json.optDouble("blacks", 0.0).let { 
        params.blacks = it.toFloat()
    }
    
    // 存在感
    json.optDouble("clarity", 0.0).let { 
        params.clarity = it.toFloat()
    }
    json.optDouble("vibrance", 0.0).let { 
        params.vibrance = it.toFloat()
    }
    
    // 颜色
    json.optDouble("temperature", 0.0).let { 
        params.temperature = it.toFloat()
    }
    json.optDouble("tint", 0.0).let { 
        params.tint = it.toFloat()
    }
    
    // 效果
    json.optDouble("texture", 0.0).let { 
        params.texture = it.toFloat()
    }
    json.optDouble("dehaze", 0.0).let { 
        params.dehaze = it.toFloat()
    }
    json.optDouble("vignette", 0.0).let { 
        params.vignette = it.toFloat()
    }
    json.optDouble("grain", 0.0).let { 
        params.grain = it.toFloat()
    }
    
    // 细节
    json.optDouble("sharpening", 0.0).let { 
        params.sharpening = it.toFloat()
    }
    json.optDouble("noiseReduction", 0.0).let { 
        params.noiseReduction = it.toFloat()
    }
    
    // HSL 调整
    val hslAdjustments = json.optJSONObject("hslAdjustments")
    if (hslAdjustments != null) {
        params.enableHSL = true
        
        // 饱和度调整
        val saturation = hslAdjustments.optJSONObject("saturation")
        if (saturation != null) {
            val colorNames = listOf("red", "orange", "yellow", "green", "aqua", "blue", "purple", "magenta")
            colorNames.forEachIndexed { index, colorName ->
                params.hslSaturation[index] = saturation.optDouble(colorName, 0.0).toFloat()
            }
        }
        
        // 明度调整
        val luminance = hslAdjustments.optJSONObject("luminance")
        if (luminance != null) {
            val colorNames = listOf("red", "orange", "yellow", "green", "aqua", "blue", "purple", "magenta")
            colorNames.forEachIndexed { index, colorName ->
                params.hslLuminance[index] = luminance.optDouble(colorName, 0.0).toFloat()
            }
        }
        
        // 色相调整
        val hue = hslAdjustments.optJSONObject("hue")
        if (hue != null) {
            val colorNames = listOf("red", "orange", "yellow", "green", "aqua", "blue", "purple", "magenta")
            colorNames.forEachIndexed { index, colorName ->
                params.hslHueShift[index] = hue.optDouble(colorName, 0.0).toFloat()
            }
        }
    }
    
    // 分离色调
    val splitToning = json.optJSONObject("splitToning")
    if (splitToning != null) {
        // TODO: 实现分离色调支持
        // 目前 BasicAdjustmentParams 没有分离色调字段
        Log.d("LRPresetParser", "检测到分离色调参数，但当前版本暂不支持")
    }
    
    return params
}
