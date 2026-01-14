package com.filmtracker.app.data

/**
 * 基础调整参数（独立于胶片模拟）
 * 对应 Adobe Camera RAW / Lightroom 的基础面板
 */
data class BasicAdjustmentParams(
    // 全局调整
    var globalExposure: Float = 0.0f,   // 曝光（EV，-5 到 +5）
    var contrast: Float = 1.0f,         // 对比度（0.5 到 2.0，1.0 为不变）
    var saturation: Float = 1.0f,       // 饱和度（0.0 到 2.0，1.0 为不变）
    
    // 色调调整
    var highlights: Float = 0.0f,       // 高光（-100 到 +100）
    var shadows: Float = 0.0f,          // 阴影（-100 到 +100）
    var whites: Float = 0.0f,           // 白场（-100 到 +100）
    var blacks: Float = 0.0f,           // 黑场（-100 到 +100）
    
    // 存在感调整
    var clarity: Float = 0.0f,          // 清晰度（-100 到 +100）
    var vibrance: Float = 0.0f,         // 自然饱和度（-100 到 +100）
    
    // 色调曲线（16个控制点，0.0-1.0）
    var enableRgbCurve: Boolean = false,
    var rgbCurve: FloatArray = FloatArray(16) { it / 15.0f },
    var enableRedCurve: Boolean = false,
    var redCurve: FloatArray = FloatArray(16) { it / 15.0f },
    var enableGreenCurve: Boolean = false,
    var greenCurve: FloatArray = FloatArray(16) { it / 15.0f },
    var enableBlueCurve: Boolean = false,
    var blueCurve: FloatArray = FloatArray(16) { it / 15.0f },
    
    // HSL 调整（8个色相段：红、橙、黄、绿、青、蓝、紫、品红）
    var enableHSL: Boolean = false,
    var hslHueShift: FloatArray = FloatArray(8) { 0.0f },      // [-180, 180] 度
    var hslSaturation: FloatArray = FloatArray(8) { 0.0f },   // [-100, 100] %
    var hslLuminance: FloatArray = FloatArray(8) { 0.0f }     // [-100, 100] %
) {
    companion object {
        /**
         * 创建默认的中性参数
         */
        fun neutral(): BasicAdjustmentParams {
            return BasicAdjustmentParams()
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BasicAdjustmentParams

        if (globalExposure != other.globalExposure) return false
        if (contrast != other.contrast) return false
        if (saturation != other.saturation) return false
        if (highlights != other.highlights) return false
        if (shadows != other.shadows) return false
        if (whites != other.whites) return false
        if (blacks != other.blacks) return false
        if (clarity != other.clarity) return false
        if (vibrance != other.vibrance) return false
        if (enableRgbCurve != other.enableRgbCurve) return false
        if (!rgbCurve.contentEquals(other.rgbCurve)) return false
        if (enableRedCurve != other.enableRedCurve) return false
        if (!redCurve.contentEquals(other.redCurve)) return false
        if (enableGreenCurve != other.enableGreenCurve) return false
        if (!greenCurve.contentEquals(other.greenCurve)) return false
        if (enableBlueCurve != other.enableBlueCurve) return false
        if (!blueCurve.contentEquals(other.blueCurve)) return false
        if (enableHSL != other.enableHSL) return false
        if (!hslHueShift.contentEquals(other.hslHueShift)) return false
        if (!hslSaturation.contentEquals(other.hslSaturation)) return false
        if (!hslLuminance.contentEquals(other.hslLuminance)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = globalExposure.hashCode()
        result = 31 * result + contrast.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + highlights.hashCode()
        result = 31 * result + shadows.hashCode()
        result = 31 * result + whites.hashCode()
        result = 31 * result + blacks.hashCode()
        result = 31 * result + clarity.hashCode()
        result = 31 * result + vibrance.hashCode()
        result = 31 * result + enableRgbCurve.hashCode()
        result = 31 * result + rgbCurve.contentHashCode()
        result = 31 * result + enableRedCurve.hashCode()
        result = 31 * result + redCurve.contentHashCode()
        result = 31 * result + enableGreenCurve.hashCode()
        result = 31 * result + greenCurve.contentHashCode()
        result = 31 * result + enableBlueCurve.hashCode()
        result = 31 * result + blueCurve.contentHashCode()
        result = 31 * result + enableHSL.hashCode()
        result = 31 * result + hslHueShift.contentHashCode()
        result = 31 * result + hslSaturation.contentHashCode()
        result = 31 * result + hslLuminance.contentHashCode()
        return result
    }
}
