package com.filmtracker.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 胶卷画幅格式
 * 
 * 定义不同胶卷类型的画幅规格和可拍摄张数
 */
sealed class FilmFormat(
    val displayName: String,
    val description: String,
    val aspectRatio: Float,
    val availableCounts: List<Int>
) {
    /**
     * 135 胶卷（35mm 全画幅）
     */
    object Film135 : FilmFormat(
        displayName = "135 胶卷",
        description = "36 张 / 卷 经典全画幅",
        aspectRatio = 3f / 2f,  // 3:2
        availableCounts = listOf(24, 36)
    )
    
    /**
     * 120 胶卷 - 6x6 画幅（正方形）
     */
    object Film120_6x6 : FilmFormat(
        displayName = "120 胶卷 - 6×6",
        description = "12 张 / 卷 正方形画幅",
        aspectRatio = 1f,  // 1:1
        availableCounts = listOf(10, 12)
    )
    
    /**
     * 120 胶卷 - 645 画幅
     */
    object Film120_645 : FilmFormat(
        displayName = "120 胶卷 - 645",
        description = "16 张 / 卷 中画幅",
        aspectRatio = 4f / 3f,  // 4:3
        availableCounts = listOf(15, 16)
    )
    
    /**
     * 120 胶卷 - 6x7 画幅
     */
    object Film120_6x7 : FilmFormat(
        displayName = "120 胶卷 - 6×7",
        description = "10 张 / 卷 理想画幅",
        aspectRatio = 7f / 6f,  // 7:6
        availableCounts = listOf(10)
    )
    
    /**
     * 120 胶卷 - 6x9 画幅
     */
    object Film120_6x9 : FilmFormat(
        displayName = "120 胶卷 - 6×9",
        description = "8 张 / 卷 宽幅画幅",
        aspectRatio = 3f / 2f,  // 3:2
        availableCounts = listOf(8)
    )
    
    companion object {
        /**
         * 获取所有 135 画幅
         */
        fun get135Formats(): List<FilmFormat> = listOf(Film135)
        
        /**
         * 获取所有 120 画幅
         */
        fun get120Formats(): List<FilmFormat> = listOf(
            Film120_6x6,
            Film120_645,
            Film120_6x7,
            Film120_6x9
        )
        
        /**
         * 获取所有画幅
         */
        fun getAllFormats(): List<FilmFormat> = get135Formats() + get120Formats()
    }
}

/**
 * 胶卷型号（负片/反转片/电影卷）
 */
sealed class FilmStock(
    val displayName: String,
    val englishName: String,
    val type: FilmType,
    val description: String,
    val icon: ImageVector  // 图标
) {
    /**
     * 获取该胶卷型号的预设参数
     */
    abstract fun getPreset(): com.filmtracker.app.data.BasicAdjustmentParams
    
    /**
     * 柯达 Portra 400（负片）
     * 特点：温暖肤色、柔和对比、自然饱和度
     */
    object KodakPortra400 : FilmStock(
        displayName = "柯达 Portra 400",
        englishName = "Kodak Portra 400",
        type = FilmType.NEGATIVE,
        description = "人像经典 温暖肤色",
        icon = Icons.Default.PhotoCamera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 8f,
            tint = 2f,
            globalExposure = 0.1f,
            contrast = 1.05f,
            saturation = 0.95f,
            vibrance = 10f,
            highlights = -5f,
            shadows = 10f,
            clarity = -5f,
            grain = 8f,
            // 色彩分级 - 温暖肤色
            gradingShadowsTemp = 5f,
            gradingMidtonesTemp = 8f,
            gradingHighlightsTemp = 3f,
            gradingBlending = 60f,
            // HSL调整 - 优化肤色
            enableHSL = true,
            hslSaturation = floatArrayOf(
                5f,    // 红色：轻微增强
                15f,   // 橙色：增强肤色
                8f,    // 黄色：温暖感
                -5f,   // 绿色：降低
                -8f,   // 青色：降低
                -3f,   // 蓝色：轻微降低
                0f,    // 紫色
                5f     // 品红：轻微增强
            ),
            hslLuminance = floatArrayOf(
                8f,    // 红色：提亮
                20f,   // 橙色：提亮肤色
                15f,   // 黄色：提亮
                0f,    // 绿色
                0f,    // 青色
                -5f,   // 蓝色：压暗
                0f,    // 紫色
                0f     // 品红
            )
        )
    }
    
    /**
     * 富士 Pro 400H（负片）
     * 特点：清新通透、柔和色调、绿色偏移
     */
    object FujiPro400H : FilmStock(
        displayName = "富士 Pro 400H",
        englishName = "Fujifilm Pro 400H",
        type = FilmType.NEGATIVE,
        description = "清新通透 柔和色调",
        icon = Icons.Default.FilterVintage
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = -5f,
            tint = -8f,
            globalExposure = 0.15f,
            contrast = 0.95f,
            saturation = 0.9f,
            vibrance = 15f,
            highlights = -10f,
            shadows = 15f,
            clarity = -10f,
            grain = 10f,
            // 色彩分级 - 冷色调配合绿色偏移
            gradingShadowsTemp = -8f,
            gradingShadowsTint = -10f,
            gradingMidtonesTemp = -5f,
            gradingMidtonesTint = -8f,
            gradingHighlightsTemp = -3f,
            gradingBlending = 55f,
            // HSL调整 - 富士绿色特性
            enableHSL = true,
            hslSaturation = floatArrayOf(
                -5f,   // 红色：降低
                10f,   // 橙色：保持肤色
                5f,    // 黄色：轻微增强
                15f,   // 绿色：增强（富士特色）
                12f,   // 青色：增强
                8f,    // 蓝色：增强
                -8f,   // 紫色：降低
                -10f   // 品红：降低
            ),
            hslLuminance = floatArrayOf(
                5f,    // 红色
                18f,   // 橙色：提亮肤色
                12f,   // 黄色
                8f,    // 绿色：提亮
                10f,   // 青色：提亮
                5f,    // 蓝色
                0f,    // 紫色
                -5f    // 品红
            )
        )
    }
    
    /**
     * 柯达 E100（反转片）
     * 特点：高饱和度、中性色温、高对比度
     */
    object KodakE100 : FilmStock(
        displayName = "柯达 E100",
        englishName = "Kodak Ektachrome E100",
        type = FilmType.REVERSAL,
        description = "反转片经典 高饱和度",
        icon = Icons.Default.PhotoCamera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 1f,
            tint = 1f,
            globalExposure = -0.15f,
            contrast = 1.25f,
            saturation = 1.18f,
            vibrance = 25f,
            highlights = -20f,
            shadows = -15f,
            whites = 15f,
            blacks = -20f,
            clarity = 20f,
            sharpening = 25f,
            dehaze = 10f,
            grain = 4f,
            // 色彩分级 - 中性但鲜艳
            gradingShadowsTemp = -2f,
            gradingMidtonesTemp = 0f,
            gradingHighlightsTemp = 2f,
            gradingBlending = 70f,
            // HSL调整 - 全面增强饱和度
            enableHSL = true,
            hslSaturation = floatArrayOf(
                20f,   // 红色：强力增强
                18f,   // 橙色：增强
                22f,   // 黄色：强力增强
                18f,   // 绿色：增强
                20f,   // 青色：强力增强
                25f,   // 蓝色：极致增强
                15f,   // 紫色：增强
                18f    // 品红：增强
            ),
            hslLuminance = floatArrayOf(
                -5f,   // 红色：压暗
                0f,    // 橙色
                5f,    // 黄色：提亮
                0f,    // 绿色
                5f,    // 青色：提亮
                -8f,   // 蓝色：压暗（深邃天空）
                -5f,   // 紫色：压暗
                0f     // 品红
            )
        )
    }
    
    /**
     * 富士 Velvia 50（反转片）
     * 特点：极致色彩、超高饱和度、风光专用
     */
    object FujiVelvia50 : FilmStock(
        displayName = "富士 Velvia 50",
        englishName = "Fujifilm Velvia 50",
        type = FilmType.REVERSAL,
        description = "风光之王 极致色彩",
        icon = Icons.Default.FilterVintage
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = -3f,
            tint = -5f,
            globalExposure = -0.15f,
            contrast = 1.3f,
            saturation = 1.3f,
            vibrance = 30f,
            highlights = -20f,
            shadows = -15f,
            whites = 15f,
            blacks = -20f,
            clarity = 25f,
            dehaze = 20f,
            grain = 3f,
            // 色彩分级 - 冷色调配合绿色
            gradingShadowsTemp = -5f,
            gradingShadowsTint = -8f,
            gradingMidtonesTemp = -3f,
            gradingMidtonesTint = -5f,
            gradingHighlightsTemp = 0f,
            gradingBlending = 75f,
            // HSL调整 - 极致饱和度（风光专用）
            enableHSL = true,
            hslSaturation = floatArrayOf(
                30f,   // 红色：极致增强
                25f,   // 橙色：强力增强
                35f,   // 黄色：极致增强
                40f,   // 绿色：极致增强（风光）
                35f,   // 青色：极致增强
                40f,   // 蓝色：极致增强（天空）
                25f,   // 紫色：强力增强
                30f    // 品红：极致增强
            ),
            hslLuminance = floatArrayOf(
                -10f,  // 红色：压暗（深邃）
                0f,    // 橙色
                10f,   // 黄色：提亮
                5f,    // 绿色：轻微提亮
                8f,    // 青色：提亮
                -15f,  // 蓝色：强力压暗（深邃天空）
                -10f,  // 紫色：压暗
                -5f    // 品红：压暗
            )
        )
    }
    
    /**
     * 爱克发 Chrome（反转片）
     * 特点：中性色调、欧系风格、平衡色彩
     */
    object AgfaChrome : FilmStock(
        displayName = "爱克发 Chrome",
        englishName = "Agfa Chrome",
        type = FilmType.REVERSAL,
        description = "欧系风格 中性色调",
        icon = Icons.Default.Camera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 3f,
            tint = 2f,
            globalExposure = -0.12f,
            contrast = 1.22f,
            saturation = 1.12f,
            vibrance = 12f,
            highlights = -18f,
            shadows = -12f,
            whites = 5f,
            blacks = -18f,
            clarity = 8f,
            dehaze = 5f,
            grain = 8f,
            vignette = -5f,
            // 色彩分级 - 欧系暖色调
            gradingShadowsTemp = 5f,
            gradingShadowsTint = 3f,
            gradingMidtonesTemp = 3f,
            gradingMidtonesTint = 2f,
            gradingHighlightsTemp = 2f,
            gradingBlending = 65f,
            // HSL调整 - 平衡但略微增强
            enableHSL = true,
            hslSaturation = floatArrayOf(
                15f,   // 红色：增强
                12f,   // 橙色：增强
                15f,   // 黄色：增强
                10f,   // 绿色：适度增强
                12f,   // 青色：增强
                18f,   // 蓝色：强力增强
                10f,   // 紫色：适度增强
                12f    // 品红：增强
            ),
            hslLuminance = floatArrayOf(
                -3f,   // 红色：轻微压暗
                5f,    // 橙色：提亮
                8f,    // 黄色：提亮
                0f,    // 绿色
                3f,    // 青色：轻微提亮
                -8f,   // 蓝色：压暗
                -5f,   // 紫色：压暗
                0f     // 品红
            )
        )
    }
    
    /**
     * 柯达 Vision3 500T（电影卷）
     * 特点：宽容度高、电影质感、暖色调
     */
    object KodakVision3 : FilmStock(
        displayName = "柯达 Vision3 500T",
        englishName = "Kodak Vision3 500T",
        type = FilmType.CINEMA,
        description = "电影质感 宽容度高",
        icon = Icons.Default.PhotoCamera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 15f,
            tint = 5f,
            globalExposure = 0.05f,
            contrast = 1.1f,
            saturation = 1.05f,
            vibrance = 10f,
            highlights = -15f,
            shadows = 20f,
            whites = -5f,
            blacks = 10f,
            clarity = 5f,
            vignette = -15f,
            grain = 12f,
            // 色彩分级 - 电影色调分离（橙蓝）
            gradingShadowsTemp = 15f,
            gradingShadowsTint = 5f,
            gradingMidtonesTemp = 8f,
            gradingHighlightsTemp = -8f,
            gradingHighlightsTint = -5f,
            gradingBlending = 80f,
            gradingBalance = 10f,
            // HSL调整 - 电影感色彩
            enableHSL = true,
            hslSaturation = floatArrayOf(
                10f,   // 红色：增强
                20f,   // 橙色：强力增强（肤色）
                15f,   // 黄色：增强
                -10f,  // 绿色：降低
                -15f,  // 青色：降低
                25f,   // 蓝色：强力增强（电影感）
                5f,    // 紫色：轻微增强
                10f    // 品红：增强
            ),
            hslLuminance = floatArrayOf(
                5f,    // 红色：提亮
                15f,   // 橙色：提亮肤色
                10f,   // 黄色：提亮
                -5f,   // 绿色：压暗
                -8f,   // 青色：压暗
                -12f,  // 蓝色：压暗（深邃）
                -5f,   // 紫色：压暗
                0f     // 品红
            )
        )
    }
    
    /**
     * 柯达 Gold 200（负片）
     * 特点：经典暖色、怀旧感、日常记录
     */
    object KodakGold200 : FilmStock(
        displayName = "柯达 Gold 200",
        englishName = "Kodak Gold 200",
        type = FilmType.NEGATIVE,
        description = "经典暖色 怀旧日常",
        icon = Icons.Default.PhotoCamera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 12f,
            tint = 3f,
            globalExposure = 0.2f,
            contrast = 1.0f,
            saturation = 1.05f,
            vibrance = 12f,
            highlights = -8f,
            shadows = 12f,
            clarity = 0f,
            grain = 15f,
            vignette = -8f,
            // 色彩分级 - 经典暖黄色调
            gradingShadowsTemp = 15f,
            gradingShadowsTint = 5f,
            gradingMidtonesTemp = 12f,
            gradingMidtonesTint = 3f,
            gradingHighlightsTemp = 10f,
            gradingBlending = 70f,
            // HSL调整 - 怀旧暖色
            enableHSL = true,
            hslSaturation = floatArrayOf(
                15f,   // 红色：增强
                20f,   // 橙色：强力增强（暖色）
                25f,   // 黄色：极致增强（金色）
                -5f,   // 绿色：降低
                -10f,  // 青色：降低
                -8f,   // 蓝色：降低
                5f,    // 紫色：轻微增强
                10f    // 品红：增强
            ),
            hslLuminance = floatArrayOf(
                10f,   // 红色：提亮
                18f,   // 橙色：提亮
                20f,   // 黄色：强力提亮（金色）
                -5f,   // 绿色：压暗
                -8f,   // 青色：压暗
                -10f,  // 蓝色：压暗
                0f,    // 紫色
                5f     // 品红：提亮
            )
        )
    }
    
    /**
     * 柯达 Ektar 100（负片）
     * 特点：极致锐利、高饱和度、风光利器
     */
    object KodakEktar100 : FilmStock(
        displayName = "柯达 Ektar 100",
        englishName = "Kodak Ektar 100",
        type = FilmType.NEGATIVE,
        description = "极致锐利 高饱和度",
        icon = Icons.Default.PhotoCamera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 5f,
            tint = 0f,
            globalExposure = 0.0f,
            contrast = 1.15f,
            saturation = 1.2f,
            vibrance = 25f,
            highlights = -10f,
            shadows = 5f,
            whites = 10f,
            blacks = -10f,
            clarity = 20f,
            sharpening = 30f,
            grain = 5f,
            // 色彩分级 - 轻微暖色
            gradingShadowsTemp = 3f,
            gradingMidtonesTemp = 5f,
            gradingHighlightsTemp = 2f,
            gradingBlending = 60f,
            // HSL调整 - 风光色彩增强
            enableHSL = true,
            hslSaturation = floatArrayOf(
                25f,   // 红色：强力增强
                20f,   // 橙色：增强
                28f,   // 黄色：极致增强
                30f,   // 绿色：极致增强（风光）
                25f,   // 青色：强力增强
                30f,   // 蓝色：极致增强（天空）
                20f,   // 紫色：增强
                22f    // 品红：增强
            ),
            hslLuminance = floatArrayOf(
                -5f,   // 红色：压暗
                5f,    // 橙色：提亮
                10f,   // 黄色：提亮
                5f,    // 绿色：提亮
                8f,    // 青色：提亮
                -10f,  // 蓝色：压暗（深邃天空）
                -5f,   // 紫色：压暗
                0f     // 品红
            )
        )
    }
    
    /**
     * 富士 Superia 400（负片）
     * 特点：清新色彩、日系风格、平衡表现
     */
    object FujiSuperia400 : FilmStock(
        displayName = "富士 Superia 400",
        englishName = "Fujifilm Superia 400",
        type = FilmType.NEGATIVE,
        description = "日系清新 平衡表现",
        icon = Icons.Default.FilterVintage
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = -4f,
            tint = -7f,
            globalExposure = 0.08f,
            contrast = 1.08f,
            saturation = 1.05f,
            vibrance = 20f,
            highlights = -10f,
            shadows = 8f,
            whites = 5f,
            blacks = -5f,
            clarity = 8f,
            sharpening = 15f,
            grain = 10f,
            // 色彩分级 - 日系冷色调
            gradingShadowsTemp = -6f,
            gradingShadowsTint = -8f,
            gradingMidtonesTemp = -4f,
            gradingMidtonesTint = -7f,
            gradingHighlightsTemp = -2f,
            gradingBlending = 65f,
            // HSL调整 - 日系色彩
            enableHSL = true,
            hslSaturation = floatArrayOf(
                5f,    // 红色：轻微增强
                12f,   // 橙色：增强
                10f,   // 黄色：增强
                18f,   // 绿色：强力增强（日系）
                15f,   // 青色：增强
                12f,   // 蓝色：增强
                -5f,   // 紫色：降低
                -8f    // 品红：降低
            ),
            hslLuminance = floatArrayOf(
                5f,    // 红色：提亮
                15f,   // 橙色：提亮
                12f,   // 黄色：提亮
                10f,   // 绿色：提亮
                12f,   // 青色：提亮
                5f,    // 蓝色：提亮
                0f,    // 紫色
                -5f    // 品红：压暗
            )
        )
    }
    
    /**
     * 富士 C200（负片）
     * 特点：经济实惠、清新通透、入门首选
     */
    object FujiC200 : FilmStock(
        displayName = "富士 C200",
        englishName = "Fujifilm C200",
        type = FilmType.NEGATIVE,
        description = "清新通透 入门首选",
        icon = Icons.Default.FilterVintage
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 0f,
            tint = -3f,
            globalExposure = 0.2f,
            contrast = 0.95f,
            saturation = 0.92f,
            vibrance = 18f,
            highlights = -3f,
            shadows = 20f,
            whites = -5f,
            blacks = 8f,
            clarity = -5f,
            grain = 22f,
            dehaze = -8f,
            // 色彩分级 - 清新通透
            gradingShadowsTemp = -2f,
            gradingShadowsTint = -5f,
            gradingMidtonesTemp = 0f,
            gradingMidtonesTint = -3f,
            gradingHighlightsTemp = 2f,
            gradingBlending = 50f,
            // HSL调整 - 清新柔和
            enableHSL = true,
            hslSaturation = floatArrayOf(
                -5f,   // 红色：降低
                8f,    // 橙色：轻微增强
                5f,    // 黄色：轻微增强
                12f,   // 绿色：增强
                10f,   // 青色：增强
                8f,    // 蓝色：轻微增强
                -10f,  // 紫色：降低
                -12f   // 品红：降低
            ),
            hslLuminance = floatArrayOf(
                10f,   // 红色：提亮
                18f,   // 橙色：提亮
                15f,   // 黄色：提亮
                12f,   // 绿色：提亮
                15f,   // 青色：提亮
                10f,   // 蓝色：提亮
                5f,    // 紫色：提亮
                0f     // 品红
            )
        )
    }
    
    /**
     * 柯达 Tri-X 400（黑白负片）
     * 特点：经典黑白、高对比度、街拍经典
     */
    object KodakTriX400 : FilmStock(
        displayName = "柯达 Tri-X 400",
        englishName = "Kodak Tri-X 400",
        type = FilmType.NEGATIVE,
        description = "经典黑白 街拍利器",
        icon = Icons.Default.PhotoCamera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 0f,
            tint = 0f,
            globalExposure = 0.0f,
            contrast = 1.25f,
            saturation = 0.0f,
            vibrance = 0f,
            highlights = -15f,
            shadows = -10f,
            whites = 15f,
            blacks = -20f,
            clarity = 15f,
            grain = 25f,
            vignette = -10f,
            // 色彩分级 - 黑白不需要
            gradingBlending = 0f,
            // HSL调整 - 黑白混合器（控制不同颜色转换为灰度的亮度）
            enableHSL = true,
            hslSaturation = floatArrayOf(
                -100f, // 红色：完全去饱和
                -100f, // 橙色：完全去饱和
                -100f, // 黄色：完全去饱和
                -100f, // 绿色：完全去饱和
                -100f, // 青色：完全去饱和
                -100f, // 蓝色：完全去饱和
                -100f, // 紫色：完全去饱和
                -100f  // 品红：完全去饱和
            ),
            hslLuminance = floatArrayOf(
                15f,   // 红色：提亮（经典黑白滤镜效果）
                20f,   // 橙色：提亮（肤色）
                25f,   // 黄色：提亮
                -10f,  // 绿色：压暗
                -15f,  // 青色：压暗
                -20f,  // 蓝色：强力压暗（天空）
                -10f,  // 紫色：压暗
                0f     // 品红
            )
        )
    }
    
    /**
     * 富士 Provia 100F（反转片）
     * 特点：中性色彩、精准还原、专业标准
     */
    object FujiProvia100F : FilmStock(
        displayName = "富士 Provia 100F",
        englishName = "Fujifilm Provia 100F",
        type = FilmType.REVERSAL,
        description = "中性色彩 精准还原",
        icon = Icons.Default.FilterVintage
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = -1f,
            tint = -2f,
            globalExposure = -0.08f,
            contrast = 1.18f,
            saturation = 1.08f,
            vibrance = 15f,
            highlights = -10f,
            shadows = -5f,
            whites = 12f,
            blacks = -10f,
            clarity = 18f,
            sharpening = 20f,
            grain = 2f,
            // 色彩分级 - 极轻微富士绿
            gradingShadowsTemp = -1f,
            gradingShadowsTint = -3f,
            gradingMidtonesTemp = -1f,
            gradingMidtonesTint = -2f,
            gradingHighlightsTemp = 0f,
            gradingBlending = 50f,
            // HSL调整 - 精准平衡
            enableHSL = true,
            hslSaturation = floatArrayOf(
                12f,   // 红色：适度增强
                10f,   // 橙色：适度增强
                12f,   // 黄色：适度增强
                15f,   // 绿色：增强（富士特色）
                12f,   // 青色：适度增强
                15f,   // 蓝色：增强
                10f,   // 紫色：适度增强
                12f    // 品红：适度增强
            ),
            hslLuminance = floatArrayOf(
                0f,    // 红色：中性
                5f,    // 橙色：轻微提亮
                8f,    // 黄色：提亮
                3f,    // 绿色：轻微提亮
                5f,    // 青色：轻微提亮
                -5f,   // 蓝色：轻微压暗
                -3f,   // 紫色：轻微压暗
                0f     // 品红：中性
            )
        )
    }
    
    /**
     * 柯达 Portra 160（负片）
     * 特点：细腻肤色、低颗粒、婚礼首选
     */
    object KodakPortra160 : FilmStock(
        displayName = "柯达 Portra 160",
        englishName = "Kodak Portra 160",
        type = FilmType.NEGATIVE,
        description = "细腻肤色 婚礼首选",
        icon = Icons.Default.PhotoCamera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 4f,
            tint = 0f,
            globalExposure = 0.25f,
            contrast = 0.92f,
            saturation = 0.88f,
            vibrance = 5f,
            highlights = -12f,
            shadows = 18f,
            whites = 8f,
            blacks = 5f,
            clarity = -12f,
            texture = -8f,
            grain = 3f,
            vignette = -3f,
            // 色彩分级 - 极柔和暖色
            gradingShadowsTemp = 3f,
            gradingMidtonesTemp = 4f,
            gradingHighlightsTemp = 2f,
            gradingBlending = 55f,
            // HSL调整 - 极致肤色优化
            enableHSL = true,
            hslSaturation = floatArrayOf(
                3f,    // 红色：极轻微增强
                18f,   // 橙色：强力增强（肤色）
                10f,   // 黄色：增强
                -8f,   // 绿色：降低
                -12f,  // 青色：降低
                -5f,   // 蓝色：降低
                0f,    // 紫色
                3f     // 品红：极轻微增强
            ),
            hslLuminance = floatArrayOf(
                10f,   // 红色：提亮
                25f,   // 橙色：强力提亮（肤色）
                20f,   // 黄色：提亮
                0f,    // 绿色
                0f,    // 青色
                -8f,   // 蓝色：压暗
                0f,    // 紫色
                0f     // 品红
            )
        )
    }
    
    /**
     * 柯达 Portra 800（负片）
     * 特点：高感光度、暖色调、弱光利器
     */
    object KodakPortra800 : FilmStock(
        displayName = "柯达 Portra 800",
        englishName = "Kodak Portra 800",
        type = FilmType.NEGATIVE,
        description = "高感光度 弱光利器",
        icon = Icons.Default.PhotoCamera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 10f,
            tint = 3f,
            globalExposure = 0.2f,
            contrast = 1.0f,
            saturation = 0.98f,
            vibrance = 12f,
            highlights = -10f,
            shadows = 15f,
            clarity = -3f,
            grain = 20f,
            noiseReduction = -10f,
            // 色彩分级 - 暖色调
            gradingShadowsTemp = 12f,
            gradingShadowsTint = 5f,
            gradingMidtonesTemp = 10f,
            gradingMidtonesTint = 3f,
            gradingHighlightsTemp = 8f,
            gradingBlending = 65f,
            // HSL调整 - 高感暖色
            enableHSL = true,
            hslSaturation = floatArrayOf(
                8f,    // 红色：增强
                18f,   // 橙色：强力增强（肤色）
                12f,   // 黄色：增强
                -8f,   // 绿色：降低
                -12f,  // 青色：降低
                -5f,   // 蓝色：降低
                3f,    // 紫色：轻微增强
                8f     // 品红：增强
            ),
            hslLuminance = floatArrayOf(
                10f,   // 红色：提亮
                22f,   // 橙色：强力提亮（肤色）
                18f,   // 黄色：提亮
                -3f,   // 绿色：轻微压暗
                -5f,   // 青色：压暗
                -8f,   // 蓝色：压暗
                0f,    // 紫色
                5f     // 品红：提亮
            )
        )
    }
    
    /**
     * 爱克发 Vista 400（负片）
     * 特点：复古色调、欧系风格、独特氛围
     */
    object AgfaVista400 : FilmStock(
        displayName = "爱克发 Vista 400",
        englishName = "Agfa Vista 400",
        type = FilmType.NEGATIVE,
        description = "复古色调 欧系风格",
        icon = Icons.Default.Camera
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 8f,
            tint = 0f,
            globalExposure = 0.1f,
            contrast = 1.1f,
            saturation = 1.08f,
            vibrance = 15f,
            highlights = -10f,
            shadows = 8f,
            clarity = 5f,
            grain = 16f,
            vignette = -12f,
            // 色彩分级 - 复古欧系
            gradingShadowsTemp = 10f,
            gradingShadowsTint = 3f,
            gradingMidtonesTemp = 8f,
            gradingMidtonesTint = 2f,
            gradingHighlightsTemp = 5f,
            gradingBlending = 70f,
            // HSL调整 - 复古色彩
            enableHSL = true,
            hslSaturation = floatArrayOf(
                18f,   // 红色：强力增强
                15f,   // 橙色：增强
                20f,   // 黄色：强力增强
                8f,    // 绿色：轻微增强
                5f,    // 青色：轻微增强
                12f,   // 蓝色：增强
                10f,   // 紫色：增强
                15f    // 品红：增强
            ),
            hslLuminance = floatArrayOf(
                -5f,   // 红色：压暗（复古）
                10f,   // 橙色：提亮
                12f,   // 黄色：提亮
                -3f,   // 绿色：轻微压暗
                0f,    // 青色
                -8f,   // 蓝色：压暗
                -5f,   // 紫色：压暗
                0f     // 品红
            )
        )
    }
    
    companion object {
        /**
         * 获取所有负片
         */
        fun getNegativeFilms(): List<FilmStock> = listOf(
            KodakPortra400,
            KodakPortra160,
            KodakPortra800,
            KodakGold200,
            KodakEktar100,
            KodakTriX400,
            FujiPro400H,
            FujiSuperia400,
            FujiC200,
            AgfaVista400
        )
        
        /**
         * 获取所有反转片
         */
        fun getReversalFilms(): List<FilmStock> = listOf(
            KodakE100,
            FujiVelvia50,
            FujiProvia100F,
            AgfaChrome
        )
        
        /**
         * 获取所有电影卷
         */
        fun getCinemaFilms(): List<FilmStock> = listOf(
            KodakVision3
        )
        
        /**
         * 获取所有胶卷型号
         */
        fun getAllFilms(): List<FilmStock> = 
            getNegativeFilms() + getReversalFilms() + getCinemaFilms()
    }
}

/**
 * 胶卷类型
 */
enum class FilmType(val displayName: String) {
    NEGATIVE("负片"),      // 彩色负片
    REVERSAL("反转片"),    // 彩色反转片
    CINEMA("电影卷")       // 电影胶片
}
