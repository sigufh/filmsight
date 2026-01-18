package com.filmtracker.app.domain.model

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
    val description: String
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
        description = "人像经典 温暖肤色"
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 8f,           // 温暖色温
            tint = 2f,                  // 轻微品红偏移
            globalExposure = 0.1f,      // 轻微提亮
            contrast = 1.05f,           // 柔和对比
            saturation = 0.95f,         // 略微降低饱和度
            vibrance = 10f,             // 增强自然饱和度
            highlights = -5f,           // 保护高光
            shadows = 10f,              // 提亮阴影
            clarity = -5f,              // 柔化皮肤
            grain = 8f                  // 轻微胶片颗粒
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
        description = "清新通透 柔和色调"
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = -5f,          // 冷色调
            tint = -8f,                 // 绿色偏移（富士特色）
            globalExposure = 0.15f,     // 明亮通透
            contrast = 0.95f,           // 低对比度
            saturation = 0.9f,          // 柔和饱和度
            vibrance = 15f,             // 自然色彩
            highlights = -10f,          // 柔和高光
            shadows = 15f,              // 明亮阴影
            clarity = -10f,             // 柔和质感
            grain = 10f                 // 胶片颗粒
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
        description = "反转片经典 高饱和度"
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 0f,           // 中性色温
            tint = 0f,                  // 中性色调
            globalExposure = -0.1f,     // 略微压暗（反转片特性）
            contrast = 1.2f,            // 高对比度
            saturation = 1.15f,         // 高饱和度
            vibrance = 20f,             // 鲜艳色彩
            highlights = -15f,          // 压制高光
            shadows = -10f,             // 加深阴影
            whites = 10f,               // 提亮白场
            blacks = -15f,              // 加深黑场
            clarity = 15f,              // 清晰锐利
            grain = 5f                  // 细腻颗粒
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
        description = "风光之王 极致色彩"
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = -3f,          // 略微冷色调
            tint = -5f,                 // 轻微绿色偏移
            globalExposure = -0.15f,    // 压暗（反转片特性）
            contrast = 1.3f,            // 极高对比度
            saturation = 1.3f,          // 极高饱和度
            vibrance = 30f,             // 极致鲜艳
            highlights = -20f,          // 强力压制高光
            shadows = -15f,             // 深邃阴影
            whites = 15f,               // 明亮白场
            blacks = -20f,              // 深黑场
            clarity = 25f,              // 极致清晰
            dehaze = 20f,               // 去雾增强
            grain = 3f                  // 极细颗粒
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
        description = "欧系风格 中性色调"
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 2f,           // 略微暖色
            tint = 0f,                  // 中性色调
            globalExposure = -0.05f,    // 轻微压暗
            contrast = 1.15f,           // 中等对比度
            saturation = 1.1f,          // 适度饱和度
            vibrance = 15f,             // 自然鲜艳
            highlights = -12f,          // 控制高光
            shadows = -8f,              // 适度阴影
            whites = 8f,                // 提亮白场
            blacks = -12f,              // 加深黑场
            clarity = 10f,              // 清晰度
            grain = 6f                  // 中等颗粒
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
        description = "电影质感 宽容度高"
    ) {
        override fun getPreset() = com.filmtracker.app.data.BasicAdjustmentParams(
            temperature = 15f,          // 暖色调（钨丝灯平衡）
            tint = 5f,                  // 轻微品红
            globalExposure = 0.05f,     // 轻微提亮
            contrast = 1.1f,            // 适度对比
            saturation = 1.05f,         // 略微增强饱和度
            vibrance = 10f,             // 自然色彩
            highlights = -15f,          // 保护高光（宽容度）
            shadows = 20f,              // 提亮阴影（宽容度）
            whites = -5f,               // 柔和白场
            blacks = 10f,               // 提亮黑场
            clarity = 5f,               // 轻微清晰
            vignette = -15f,            // 电影暗角
            grain = 12f,                // 电影颗粒
            gradingShadowsTemp = 10f,   // 阴影暖色
            gradingHighlightsTemp = -5f // 高光冷色（电影色调分离）
        )
    }
    
    companion object {
        /**
         * 获取所有负片
         */
        fun getNegativeFilms(): List<FilmStock> = listOf(
            KodakPortra400,
            FujiPro400H
        )
        
        /**
         * 获取所有反转片
         */
        fun getReversalFilms(): List<FilmStock> = listOf(
            KodakE100,
            FujiVelvia50,
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
