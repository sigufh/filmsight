package com.filmtracker.app.ai

import com.filmtracker.app.data.FilmParams
import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/**
 * AI 参数建议模块
 * 
 * 核心原则：AI 不直接生成图像，只输出参数建议
 * 
 * 这是一个轻量级示例实现，实际应使用训练好的模型
 */
class AIParameterAdvisor {
    
    /**
     * 分析图像并给出参数建议
     * 
     * @param image 低分辨率预览图像（线性域或已转换）
     * @param metadata 图像元数据
     * @return 参数建议和风险提示
     */
    fun analyzeAndSuggest(
        image: Bitmap,
        iso: Float,
        exposureTime: Float
    ): ParameterSuggestion {
        
        // 简化实现：基于图像统计特征给出建议
        // 实际应使用轻量 CNN 模型（<5MB）
        
        val stats = analyzeImageStatistics(image)
        
        return ParameterSuggestion(
            // 曝光建议（基于直方图）
            suggestedExposure = suggestExposure(stats, iso, exposureTime),
            
            // 对比度建议（基于动态范围）
            suggestedContrast = suggestContrast(stats),
            
            // 饱和度建议（基于颜色分布）
            suggestedSaturation = suggestSaturation(stats),
            
            // 风险提示
            risks = detectRisks(stats),
            
            // 推荐胶片预设
            recommendedPreset = recommendPreset(stats, iso)
        )
    }
    
    /**
     * 分析图像统计特征
     */
    private fun analyzeImageStatistics(image: Bitmap): ImageStatistics {
        var totalBrightness = 0.0
        var totalSaturation = 0.0
        var pixelCount = 0
        var minBrightness = Float.MAX_VALUE
        var maxBrightness = Float.MIN_VALUE
        
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            val brightness = (r + g + b) / 3.0 / 255.0
            val max = maxOf(r, g, b) / 255.0
            val min = minOf(r, g, b) / 255.0
            val saturation = if (max > 0) (max - min) / max else 0.0
            
            totalBrightness += brightness
            totalSaturation += saturation
            minBrightness = min(minBrightness, brightness.toFloat())
            maxBrightness = max(maxBrightness, brightness.toFloat())
            pixelCount++
        }
        
        return ImageStatistics(
            averageBrightness = (totalBrightness / pixelCount).toFloat(),
            averageSaturation = (totalSaturation / pixelCount).toFloat(),
            minBrightness = minBrightness,
            maxBrightness = maxBrightness,
            dynamicRange = maxBrightness - minBrightness
        )
    }
    
    /**
     * 建议曝光值
     */
    private fun suggestExposure(
        stats: ImageStatistics,
        iso: Float,
        exposureTime: Float
    ): Float {
        // 如果平均亮度偏低，建议增加曝光
        val targetBrightness = 0.5f
        val brightnessDiff = targetBrightness - stats.averageBrightness
        
        // 转换为 EV 调整
        return brightnessDiff * 2.0f
    }
    
    /**
     * 建议对比度
     */
    private fun suggestContrast(stats: ImageStatistics): Float {
        // 动态范围小 -> 增加对比度
        if (stats.dynamicRange < 0.3f) {
            return 1.2f
        } else if (stats.dynamicRange > 0.8f) {
            return 0.9f
        }
        return 1.0f
    }
    
    /**
     * 建议饱和度
     */
    private fun suggestSaturation(stats: ImageStatistics): Float {
        // 饱和度低 -> 建议增加
        if (stats.averageSaturation < 0.2f) {
            return 1.2f
        } else if (stats.averageSaturation > 0.6f) {
            return 0.9f
        }
        return 1.0f
    }
    
    /**
     * 检测风险（高光溢出、色偏等）
     */
    private fun detectRisks(stats: ImageStatistics): List<RiskWarning> {
        val risks = mutableListOf<RiskWarning>()
        
        if (stats.maxBrightness > 0.95f) {
            risks.add(RiskWarning.HIGHLIGHT_CLIPPING)
        }
        
        if (stats.minBrightness < 0.05f) {
            risks.add(RiskWarning.SHADOW_CLIPPING)
        }
        
        if (stats.dynamicRange < 0.2f) {
            risks.add(RiskWarning.LOW_DYNAMIC_RANGE)
        }
        
        return risks
    }
    
    /**
     * 推荐胶片预设
     */
    private fun recommendPreset(stats: ImageStatistics, iso: Float): String {
        // 简化逻辑：基于 ISO 和亮度特征
        if (iso < 200) {
            return "Kodak Gold 200"
        } else if (stats.averageBrightness > 0.6f) {
            return "Fuji Superia 400"
        } else {
            return "Portra 400"
        }
    }
}

/**
 * 图像统计特征
 */
data class ImageStatistics(
    val averageBrightness: Float,
    val averageSaturation: Float,
    val minBrightness: Float,
    val maxBrightness: Float,
    val dynamicRange: Float
)

/**
 * 参数建议
 */
data class ParameterSuggestion(
    val suggestedExposure: Float,
    val suggestedContrast: Float,
    val suggestedSaturation: Float,
    val risks: List<RiskWarning>,
    val recommendedPreset: String
)

/**
 * 风险警告
 */
enum class RiskWarning {
    HIGHLIGHT_CLIPPING,    // 高光溢出
    SHADOW_CLIPPING,       // 暗部丢失
    LOW_DYNAMIC_RANGE,     // 动态范围不足
    COLOR_CAST             // 色偏风险
}
