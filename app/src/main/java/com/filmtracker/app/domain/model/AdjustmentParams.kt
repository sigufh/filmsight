package com.filmtracker.app.domain.model

/**
 * 领域层调整参数模型
 * 独立于具体实现，只包含业务逻辑需要的数据
 */
data class AdjustmentParams(
    // 基础调整
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    
    // 色调调整
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    
    // 存在感
    val clarity: Float = 0f,
    val vibrance: Float = 0f,
    
    // 颜色
    val temperature: Float = 0f,
    val tint: Float = 0f,
    
    // 分级
    val gradingHighlightsTemp: Float = 0f,
    val gradingHighlightsTint: Float = 0f,
    val gradingMidtonesTemp: Float = 0f,
    val gradingMidtonesTint: Float = 0f,
    val gradingShadowsTemp: Float = 0f,
    val gradingShadowsTint: Float = 0f,
    val gradingBlending: Float = 50f,
    val gradingBalance: Float = 0f,
    
    // 效果
    val texture: Float = 0f,
    val dehaze: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    
    // 细节
    val sharpening: Float = 0f,
    val noiseReduction: Float = 0f,
    
    // 曲线
    val enableRgbCurve: Boolean = false,
    val rgbCurvePoints: List<Pair<Float, Float>> = listOf(Pair(0f, 0f), Pair(1f, 1f)),
    val enableRedCurve: Boolean = false,
    val redCurvePoints: List<Pair<Float, Float>> = listOf(Pair(0f, 0f), Pair(1f, 1f)),
    val enableGreenCurve: Boolean = false,
    val greenCurvePoints: List<Pair<Float, Float>> = listOf(Pair(0f, 0f), Pair(1f, 1f)),
    val enableBlueCurve: Boolean = false,
    val blueCurvePoints: List<Pair<Float, Float>> = listOf(Pair(0f, 0f), Pair(1f, 1f)),
    
    // HSL
    val enableHSL: Boolean = false,
    val hslHueShift: FloatArray = FloatArray(8) { 0f },
    val hslSaturation: FloatArray = FloatArray(8) { 0f },
    val hslLuminance: FloatArray = FloatArray(8) { 0f },
    
    // 几何（旋转、裁剪）
    val rotation: Float = 0f,
    val cropEnabled: Boolean = false,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
) {
    companion object {
        fun default() = AdjustmentParams()
        
        fun neutral() = AdjustmentParams()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AdjustmentParams
        
        if (exposure != other.exposure) return false
        if (contrast != other.contrast) return false
        if (saturation != other.saturation) return false
        if (highlights != other.highlights) return false
        if (shadows != other.shadows) return false
        if (whites != other.whites) return false
        if (blacks != other.blacks) return false
        if (clarity != other.clarity) return false
        if (vibrance != other.vibrance) return false
        if (temperature != other.temperature) return false
        if (tint != other.tint) return false
        if (gradingHighlightsTemp != other.gradingHighlightsTemp) return false
        if (gradingHighlightsTint != other.gradingHighlightsTint) return false
        if (gradingMidtonesTemp != other.gradingMidtonesTemp) return false
        if (gradingMidtonesTint != other.gradingMidtonesTint) return false
        if (gradingShadowsTemp != other.gradingShadowsTemp) return false
        if (gradingShadowsTint != other.gradingShadowsTint) return false
        if (gradingBlending != other.gradingBlending) return false
        if (gradingBalance != other.gradingBalance) return false
        if (texture != other.texture) return false
        if (dehaze != other.dehaze) return false
        if (vignette != other.vignette) return false
        if (grain != other.grain) return false
        if (sharpening != other.sharpening) return false
        if (noiseReduction != other.noiseReduction) return false
        if (enableRgbCurve != other.enableRgbCurve) return false
        if (rgbCurvePoints != other.rgbCurvePoints) return false
        if (enableRedCurve != other.enableRedCurve) return false
        if (redCurvePoints != other.redCurvePoints) return false
        if (enableGreenCurve != other.enableGreenCurve) return false
        if (greenCurvePoints != other.greenCurvePoints) return false
        if (enableBlueCurve != other.enableBlueCurve) return false
        if (blueCurvePoints != other.blueCurvePoints) return false
        if (enableHSL != other.enableHSL) return false
        if (!hslHueShift.contentEquals(other.hslHueShift)) return false
        if (!hslSaturation.contentEquals(other.hslSaturation)) return false
        if (!hslLuminance.contentEquals(other.hslLuminance)) return false
        if (rotation != other.rotation) return false
        if (cropEnabled != other.cropEnabled) return false
        if (cropLeft != other.cropLeft) return false
        if (cropTop != other.cropTop) return false
        if (cropRight != other.cropRight) return false
        if (cropBottom != other.cropBottom) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = exposure.hashCode()
        result = 31 * result + contrast.hashCode()
        result = 31 * result + saturation.hashCode()
        result = 31 * result + highlights.hashCode()
        result = 31 * result + shadows.hashCode()
        result = 31 * result + whites.hashCode()
        result = 31 * result + blacks.hashCode()
        result = 31 * result + clarity.hashCode()
        result = 31 * result + vibrance.hashCode()
        result = 31 * result + temperature.hashCode()
        result = 31 * result + tint.hashCode()
        result = 31 * result + gradingHighlightsTemp.hashCode()
        result = 31 * result + gradingHighlightsTint.hashCode()
        result = 31 * result + gradingMidtonesTemp.hashCode()
        result = 31 * result + gradingMidtonesTint.hashCode()
        result = 31 * result + gradingShadowsTemp.hashCode()
        result = 31 * result + gradingShadowsTint.hashCode()
        result = 31 * result + gradingBlending.hashCode()
        result = 31 * result + gradingBalance.hashCode()
        result = 31 * result + texture.hashCode()
        result = 31 * result + dehaze.hashCode()
        result = 31 * result + vignette.hashCode()
        result = 31 * result + grain.hashCode()
        result = 31 * result + sharpening.hashCode()
        result = 31 * result + noiseReduction.hashCode()
        result = 31 * result + enableRgbCurve.hashCode()
        result = 31 * result + rgbCurvePoints.hashCode()
        result = 31 * result + enableRedCurve.hashCode()
        result = 31 * result + redCurvePoints.hashCode()
        result = 31 * result + enableGreenCurve.hashCode()
        result = 31 * result + greenCurvePoints.hashCode()
        result = 31 * result + enableBlueCurve.hashCode()
        result = 31 * result + blueCurvePoints.hashCode()
        result = 31 * result + enableHSL.hashCode()
        result = 31 * result + hslHueShift.contentHashCode()
        result = 31 * result + hslSaturation.contentHashCode()
        result = 31 * result + hslLuminance.contentHashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + cropEnabled.hashCode()
        result = 31 * result + cropLeft.hashCode()
        result = 31 * result + cropTop.hashCode()
        result = 31 * result + cropRight.hashCode()
        result = 31 * result + cropBottom.hashCode()
        return result
    }
}
