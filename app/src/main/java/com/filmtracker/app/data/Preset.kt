package com.filmtracker.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 预设数据类
 * 保存调整参数的快照，可以跨图像应用
 */
@Serializable
data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: PresetCategory = PresetCategory.USER,
    val params: BasicAdjustmentParams,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

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
