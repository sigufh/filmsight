package com.filmtracker.app.ai

/**
 * 用户偏好设置
 */
data class UserPreferences(
    val colorStyle: ColorStyle = ColorStyle.NATURAL,
    val colorTendency: String = "自然真实",
    val contrastPreference: String = "适中",
    val saturationPreference: String = "适中",
    val customRules: String = ""
)

/**
 * 调色风格
 */
enum class ColorStyle(val displayName: String) {
    NATURAL("自然真实"),
    FILM_VINTAGE("胶片复古"),
    FRESH_JAPANESE("日系清新"),
    CINEMATIC("电影感"),
    HIGH_CONTRAST("高对比"),
    LOW_SATURATION("低饱和"),
    WARM_TONE("暖色调"),
    COOL_TONE("冷色调"),
    BLACK_WHITE("黑白")
}

/**
 * 调色建议
 */
data class ColorGradingSuggestion(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val temperature: Float = 5500f,
    val tint: Float = 0f,
    val clarity: Float = 0f,
    val sharpness: Float = 0f,
    val denoise: Float = 0f,
    val explanation: String = ""
) {
    companion object {
        fun empty() = ColorGradingSuggestion()
    }
}
