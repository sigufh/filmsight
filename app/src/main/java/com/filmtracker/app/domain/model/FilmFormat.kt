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
     * 柯达 Portra 400（负片）
     */
    object KodakPortra400 : FilmStock(
        displayName = "柯达 Portra 400",
        englishName = "Kodak Portra 400",
        type = FilmType.NEGATIVE,
        description = "人像经典 温暖肤色"
    )
    
    /**
     * 富士 Pro 400H（负片）
     */
    object FujiPro400H : FilmStock(
        displayName = "富士 Pro 400H",
        englishName = "Fujifilm Pro 400H",
        type = FilmType.NEGATIVE,
        description = "清新通透 柔和色调"
    )
    
    /**
     * 柯达 E100（反转片）
     */
    object KodakE100 : FilmStock(
        displayName = "柯达 E100",
        englishName = "Kodak Ektachrome E100",
        type = FilmType.REVERSAL,
        description = "反转片经典 高饱和度"
    )
    
    /**
     * 富士 Velvia 50（反转片）
     */
    object FujiVelvia50 : FilmStock(
        displayName = "富士 Velvia 50",
        englishName = "Fujifilm Velvia 50",
        type = FilmType.REVERSAL,
        description = "风光之王 极致色彩"
    )
    
    /**
     * 爱克发 Chrome（反转片）
     */
    object AgfaChrome : FilmStock(
        displayName = "爱克发 Chrome",
        englishName = "Agfa Chrome",
        type = FilmType.REVERSAL,
        description = "欧系风格 中性色调"
    )
    
    /**
     * 柯达 Vision3 500T（电影卷）
     */
    object KodakVision3 : FilmStock(
        displayName = "柯达 Vision3 500T",
        englishName = "Kodak Vision3 500T",
        type = FilmType.CINEMA,
        description = "电影质感 宽容度高"
    )
    
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
