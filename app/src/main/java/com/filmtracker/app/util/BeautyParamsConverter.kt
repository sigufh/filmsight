package com.filmtracker.app.util

import com.filmtracker.app.ai.BeautyParams
import com.filmtracker.app.ai.BeautySuggestion
import com.filmtracker.app.data.FilmParams

/**
 * 美颜参数转换器
 * 
 * 将 AI 美颜建议转换为 FilmParams 和局部调整配置
 */
object BeautyParamsConverter {
    
    /**
     * 将美颜建议转换为 FilmParams 调整
     * 
     * @param currentParams 当前参数
     * @param suggestion 美颜建议
     * @return 调整后的参数
     */
    fun applyBeautySuggestion(
        currentParams: FilmParams,
        suggestion: BeautySuggestion
    ): FilmParams {
        var params = currentParams.copy()
        val beautyParams = suggestion.params
        
        // 1. 整体人像调整
        params.globalExposure += beautyParams.portraitExposure
        params.contrast = params.contrast * (1.0f + beautyParams.portraitContrast)
        params.vibrance += beautyParams.portraitVibrance
        
        // 2. 皮肤平滑（通过降低清晰度实现）
        // 注意：这是全局调整，理想情况下应该在局部掩膜区域应用
        params.clarity -= beautyParams.skinSmoothing * 0.5f
        
        // 3. 肤色修正（通过白平衡微调）
        // 简化实现：调整整体色温倾向
        // 实际应通过 HSL 调整肤色色相段
        if (beautyParams.skinToneWarmth != 0.0f) {
            // 暖色：增加红色/橙色饱和度，降低蓝色
            params.hslSaturation[0] += beautyParams.skinToneWarmth * 0.2f // 红
            params.hslSaturation[1] += beautyParams.skinToneWarmth * 0.15f // 橙
            params.hslSaturation[5] -= beautyParams.skinToneWarmth * 0.1f // 蓝
            params.enableHSL = true
        }
        
        // 4. 肤色饱和度
        if (beautyParams.skinToneSaturation != 0.0f) {
            params.hslSaturation[0] += beautyParams.skinToneSaturation // 红
            params.hslSaturation[1] += beautyParams.skinToneSaturation * 0.8f // 橙
            params.enableHSL = true
        }
        
        // 5. 眼部增强（通过提高亮度实现）
        // 注意：这是全局调整，理想情况下应该在眼部掩膜区域应用
        params.highlights += beautyParams.eyeBrightness * 0.3f
        
        // 6. 嘴唇增强
        if (beautyParams.lipSaturation != 0.0f || beautyParams.lipBrightness != 0.0f) {
            // 调整红色/品红色相段
            params.hslSaturation[0] += beautyParams.lipSaturation * 0.3f // 红
            params.hslSaturation[7] += beautyParams.lipSaturation * 0.3f // 品红
            params.hslLuminance[0] += beautyParams.lipBrightness * 0.2f // 红
            params.hslLuminance[7] += beautyParams.lipBrightness * 0.2f // 品红
            params.enableHSL = true
        }
        
        // 限制参数范围
        params.clarity = params.clarity.coerceIn(-1.0f, 1.0f)
        params.vibrance = params.vibrance.coerceIn(-1.0f, 1.0f)
        params.highlights = params.highlights.coerceIn(-1.0f, 1.0f)
        
        for (i in 0 until 8) {
            params.hslSaturation[i] = params.hslSaturation[i].coerceIn(-100.0f, 100.0f)
            params.hslLuminance[i] = params.hslLuminance[i].coerceIn(-100.0f, 100.0f)
        }
        
        return params
    }
    
    /**
     * 创建局部调整配置（用于未来实现）
     * 
     * 当前返回空列表，未来可以实现基于掩膜的局部调整
     */
    fun createLocalAdjustments(suggestion: BeautySuggestion): List<LocalAdjustment> {
        // TODO: 基于 faceRegions 和 skinRegions 创建局部调整
        return emptyList()
    }
}

/**
 * 局部调整配置（未来扩展）
 */
data class LocalAdjustment(
    val maskRegion: android.graphics.RectF,
    val adjustmentType: AdjustmentType,
    val value: Float
)

enum class AdjustmentType {
    EXPOSURE,
    CONTRAST,
    CLARITY,
    SATURATION
}
