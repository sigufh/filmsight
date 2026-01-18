package com.filmtracker.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 预设数据类
 * 保存调整参数的快照，可以跨图像应用
 * 
 * @param parameterMask 可选的参数掩码，指示哪些参数包含在预设中
 *                      如果为 null，则应用所有参数（完整预设）
 *                      如果不为 null，则仅应用掩码中指定的参数（部分预设）
 */
@Serializable
data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: PresetCategory = PresetCategory.USER,
    val params: BasicAdjustmentParams,
    val parameterMask: ParameterMask? = null,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    /**
     * 检查是否为部分预设
     */
    fun isPartialPreset(): Boolean = parameterMask != null
    
    /**
     * 检查是否为完整预设
     */
    fun isFullPreset(): Boolean = parameterMask == null
}

/**
 * 预设分类
 */
@Serializable
enum class PresetCategory {
    USER,           // 用户创建
    CREATIVE,       // 创意滤镜
    PORTRAIT,       // 人像
    LANDSCAPE,      // 风景
    BLACKWHITE,     // 黑白
    VINTAGE,        // 复古
    CINEMATIC       // 电影
}

/**
 * 内置创意滤镜预设
 */
object BuiltInPresets {
    
    /**
     * 获取所有内置预设
     */
    fun getAll(): List<Preset> = listOf(
        // 黑白系列
        createPreset(
            name = "经典黑白",
            category = PresetCategory.BLACKWHITE,
            params = BasicAdjustmentParams(
                saturation = 0f,
                contrast = 1.15f,
                clarity = 15f
            )
        ),
        createPreset(
            name = "高对比黑白",
            category = PresetCategory.BLACKWHITE,
            params = BasicAdjustmentParams(
                saturation = 0f,
                contrast = 1.35f,
                blacks = -20f,
                whites = 20f,
                clarity = 25f
            )
        ),
        createPreset(
            name = "柔和黑白",
            category = PresetCategory.BLACKWHITE,
            params = BasicAdjustmentParams(
                saturation = 0f,
                contrast = 0.9f,
                shadows = 15f,
                clarity = -10f
            )
        ),
        
        // 复古系列
        createPreset(
            name = "复古胶片",
            category = PresetCategory.VINTAGE,
            params = BasicAdjustmentParams(
                temperature = 15f,
                tint = -5f,
                saturation = 0.85f,
                contrast = 1.1f,
                highlights = -10f,
                shadows = 10f,
                grain = 15f,
                vignette = -20f
            )
        ),
        createPreset(
            name = "褪色记忆",
            category = PresetCategory.VINTAGE,
            params = BasicAdjustmentParams(
                temperature = 10f,
                saturation = 0.7f,
                contrast = 0.85f,
                highlights = 15f,
                blacks = 15f,
                grain = 20f,
                vignette = -15f
            )
        ),
        createPreset(
            name = "宝丽来",
            category = PresetCategory.VINTAGE,
            params = BasicAdjustmentParams(
                temperature = 20f,
                tint = 10f,
                saturation = 1.15f,
                contrast = 1.2f,
                shadows = 20f,
                vignette = -25f
            )
        ),
        
        // 电影系列
        createPreset(
            name = "电影蓝调",
            category = PresetCategory.CINEMATIC,
            params = BasicAdjustmentParams(
                temperature = -15f,
                tint = -10f,
                saturation = 0.9f,
                contrast = 1.15f,
                shadows = 10f,
                vignette = -15f,
                gradingShadowsTemp = -20f,
                gradingHighlightsTemp = 10f
            )
        ),
        createPreset(
            name = "暖色电影",
            category = PresetCategory.CINEMATIC,
            params = BasicAdjustmentParams(
                temperature = 25f,
                tint = 5f,
                saturation = 1.05f,
                contrast = 1.1f,
                highlights = -10f,
                vignette = -20f,
                gradingShadowsTemp = 15f,
                gradingHighlightsTemp = -5f
            )
        ),
        createPreset(
            name = "青橙电影",
            category = PresetCategory.CINEMATIC,
            params = BasicAdjustmentParams(
                temperature = 10f,
                saturation = 1.1f,
                contrast = 1.15f,
                vignette = -15f,
                gradingShadowsTemp = -25f,
                gradingShadowsTint = 10f,
                gradingHighlightsTemp = 20f,
                gradingHighlightsTint = -10f
            )
        ),
        
        // 人像系列
        createPreset(
            name = "自然人像",
            category = PresetCategory.PORTRAIT,
            params = BasicAdjustmentParams(
                temperature = 5f,
                tint = 2f,
                globalExposure = 0.1f,
                contrast = 1.05f,
                highlights = -5f,
                shadows = 10f,
                clarity = -5f,
                vibrance = 10f
            )
        ),
        createPreset(
            name = "柔光人像",
            category = PresetCategory.PORTRAIT,
            params = BasicAdjustmentParams(
                temperature = 8f,
                tint = 3f,
                globalExposure = 0.15f,
                contrast = 0.95f,
                highlights = -10f,
                shadows = 15f,
                clarity = -15f,
                vibrance = 5f
            )
        ),
        createPreset(
            name = "清新人像",
            category = PresetCategory.PORTRAIT,
            params = BasicAdjustmentParams(
                temperature = -5f,
                globalExposure = 0.2f,
                contrast = 1.1f,
                highlights = -10f,
                shadows = 5f,
                vibrance = 15f,
                clarity = 5f
            )
        ),
        
        // 风景系列
        createPreset(
            name = "鲜艳风景",
            category = PresetCategory.LANDSCAPE,
            params = BasicAdjustmentParams(
                saturation = 1.2f,
                vibrance = 20f,
                contrast = 1.15f,
                clarity = 20f,
                dehaze = 15f
            )
        ),
        createPreset(
            name = "戏剧风景",
            category = PresetCategory.LANDSCAPE,
            params = BasicAdjustmentParams(
                contrast = 1.3f,
                highlights = -20f,
                shadows = 20f,
                clarity = 30f,
                saturation = 1.1f,
                dehaze = 25f
            )
        ),
        createPreset(
            name = "柔和风景",
            category = PresetCategory.LANDSCAPE,
            params = BasicAdjustmentParams(
                globalExposure = 0.15f,
                contrast = 0.95f,
                highlights = -10f,
                shadows = 10f,
                saturation = 1.05f,
                clarity = -5f
            )
        ),
        
        // 胶片色彩系列（基于真实胶卷特性）
        createPreset(
            name = "柯达 Portra 400",
            category = PresetCategory.CREATIVE,
            params = com.filmtracker.app.domain.model.FilmStock.KodakPortra400.getPreset()
        ),
        createPreset(
            name = "富士 Pro 400H",
            category = PresetCategory.CREATIVE,
            params = com.filmtracker.app.domain.model.FilmStock.FujiPro400H.getPreset()
        ),
        createPreset(
            name = "柯达 E100",
            category = PresetCategory.CREATIVE,
            params = com.filmtracker.app.domain.model.FilmStock.KodakE100.getPreset()
        ),
        createPreset(
            name = "富士 Velvia 50",
            category = PresetCategory.CREATIVE,
            params = com.filmtracker.app.domain.model.FilmStock.FujiVelvia50.getPreset()
        ),
        createPreset(
            name = "爱克发 Chrome",
            category = PresetCategory.CREATIVE,
            params = com.filmtracker.app.domain.model.FilmStock.AgfaChrome.getPreset()
        ),
        createPreset(
            name = "柯达 Vision3 500T",
            category = PresetCategory.CREATIVE,
            params = com.filmtracker.app.domain.model.FilmStock.KodakVision3.getPreset()
        )
    )
    
    /**
     * 按分类获取预设
     */
    fun getByCategory(category: PresetCategory): List<Preset> {
        return getAll().filter { it.category == category }
    }
    
    private fun createPreset(
        name: String,
        category: PresetCategory,
        params: BasicAdjustmentParams
    ): Preset {
        return Preset(
            id = "builtin_${name.hashCode()}",
            name = name,
            category = category,
            params = params
        )
    }
}


/**
 * 参数掩码
 * 指示预设中包含哪些参数
 * 用于实现部分预设功能（例如，仅应用色彩参数或仅应用色调参数）
 */
@Serializable
data class ParameterMask(
    // 基础调整
    val exposure: Boolean = false,
    val contrast: Boolean = false,
    val saturation: Boolean = false,
    
    // 色调调整
    val highlights: Boolean = false,
    val shadows: Boolean = false,
    val whites: Boolean = false,
    val blacks: Boolean = false,
    
    // 存在感
    val clarity: Boolean = false,
    val vibrance: Boolean = false,
    
    // 颜色
    val temperature: Boolean = false,
    val tint: Boolean = false,
    
    // 分级
    val gradingHighlightsTemp: Boolean = false,
    val gradingHighlightsTint: Boolean = false,
    val gradingMidtonesTemp: Boolean = false,
    val gradingMidtonesTint: Boolean = false,
    val gradingShadowsTemp: Boolean = false,
    val gradingShadowsTint: Boolean = false,
    val gradingBlending: Boolean = false,
    val gradingBalance: Boolean = false,
    
    // 效果
    val texture: Boolean = false,
    val dehaze: Boolean = false,
    val vignette: Boolean = false,
    val grain: Boolean = false,
    
    // 细节
    val sharpening: Boolean = false,
    val noiseReduction: Boolean = false,
    
    // 曲线
    val enableRgbCurve: Boolean = false,
    val rgbCurvePoints: Boolean = false,
    val enableRedCurve: Boolean = false,
    val redCurvePoints: Boolean = false,
    val enableGreenCurve: Boolean = false,
    val greenCurvePoints: Boolean = false,
    val enableBlueCurve: Boolean = false,
    val blueCurvePoints: Boolean = false,
    
    // HSL
    val enableHSL: Boolean = false,
    val hslHueShift: Boolean = false,
    val hslSaturation: Boolean = false,
    val hslLuminance: Boolean = false,
    
    // 几何
    val rotation: Boolean = false,
    val cropEnabled: Boolean = false,
    val cropLeft: Boolean = false,
    val cropTop: Boolean = false,
    val cropRight: Boolean = false,
    val cropBottom: Boolean = false
) {
    companion object {
        /**
         * 创建包含所有参数的完整掩码
         */
        fun all() = ParameterMask(
            exposure = true,
            contrast = true,
            saturation = true,
            highlights = true,
            shadows = true,
            whites = true,
            blacks = true,
            clarity = true,
            vibrance = true,
            temperature = true,
            tint = true,
            gradingHighlightsTemp = true,
            gradingHighlightsTint = true,
            gradingMidtonesTemp = true,
            gradingMidtonesTint = true,
            gradingShadowsTemp = true,
            gradingShadowsTint = true,
            gradingBlending = true,
            gradingBalance = true,
            texture = true,
            dehaze = true,
            vignette = true,
            grain = true,
            sharpening = true,
            noiseReduction = true,
            enableRgbCurve = true,
            rgbCurvePoints = true,
            enableRedCurve = true,
            redCurvePoints = true,
            enableGreenCurve = true,
            greenCurvePoints = true,
            enableBlueCurve = true,
            blueCurvePoints = true,
            enableHSL = true,
            hslHueShift = true,
            hslSaturation = true,
            hslLuminance = true,
            rotation = true,
            cropEnabled = true,
            cropLeft = true,
            cropTop = true,
            cropRight = true,
            cropBottom = true
        )
        
        /**
         * 创建仅包含色彩参数的掩码
         */
        fun colorOnly() = ParameterMask(
            temperature = true,
            tint = true,
            saturation = true,
            vibrance = true,
            gradingHighlightsTemp = true,
            gradingHighlightsTint = true,
            gradingMidtonesTemp = true,
            gradingMidtonesTint = true,
            gradingShadowsTemp = true,
            gradingShadowsTint = true,
            gradingBlending = true,
            gradingBalance = true,
            enableHSL = true,
            hslHueShift = true,
            hslSaturation = true,
            hslLuminance = true
        )
        
        /**
         * 创建仅包含色调参数的掩码
         */
        fun toneOnly() = ParameterMask(
            exposure = true,
            contrast = true,
            highlights = true,
            shadows = true,
            whites = true,
            blacks = true,
            clarity = true,
            enableRgbCurve = true,
            rgbCurvePoints = true,
            enableRedCurve = true,
            redCurvePoints = true,
            enableGreenCurve = true,
            greenCurvePoints = true,
            enableBlueCurve = true,
            blueCurvePoints = true
        )
        
        /**
         * 创建仅包含效果参数的掩码
         */
        fun effectsOnly() = ParameterMask(
            texture = true,
            dehaze = true,
            vignette = true,
            grain = true
        )
        
        /**
         * 创建仅包含细节参数的掩码
         */
        fun detailsOnly() = ParameterMask(
            sharpening = true,
            noiseReduction = true
        )
        
        /**
         * 创建仅包含几何参数的掩码
         */
        fun geometryOnly() = ParameterMask(
            rotation = true,
            cropEnabled = true,
            cropLeft = true,
            cropTop = true,
            cropRight = true,
            cropBottom = true
        )
    }
}
