package com.filmtracker.app.domain.model

/**
 * 参数变更
 * 表示单个参数的修改,用于历史记录追踪
 */
sealed class ParameterChange {
    abstract val timestamp: Long
    abstract val description: String
    abstract fun apply(params: AdjustmentParams): AdjustmentParams
    
    // 基础调整
    data class ExposureChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "曝光"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(exposure = newValue)
    }
    
    data class ContrastChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "对比度"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(contrast = newValue)
    }
    
    data class SaturationChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "饱和度"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(saturation = newValue)
    }
    
    // 色调调整
    data class HighlightsChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "高光"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(highlights = newValue)
    }
    
    data class ShadowsChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "阴影"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(shadows = newValue)
    }
    
    data class WhitesChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "白色"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(whites = newValue)
    }
    
    data class BlacksChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "黑色"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(blacks = newValue)
    }
    
    // 存在感
    data class ClarityChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "清晰度"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(clarity = newValue)
    }
    
    data class VibranceChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "自然饱和度"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(vibrance = newValue)
    }
    
    // 颜色
    data class TemperatureChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "色温"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(temperature = newValue)
    }
    
    data class TintChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "色调"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(tint = newValue)
    }
    
    // 分级
    data class GradingHighlightsTempChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "高光色温"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(gradingHighlightsTemp = newValue)
    }
    
    data class GradingHighlightsTintChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "高光色调"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(gradingHighlightsTint = newValue)
    }
    
    data class GradingMidtonestempChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "中间调色温"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(gradingMidtonesTemp = newValue)
    }
    
    data class GradingMidtonestintChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "中间调色调"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(gradingMidtonesTint = newValue)
    }
    
    data class GradingShadowsTempChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "阴影色温"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(gradingShadowsTemp = newValue)
    }
    
    data class GradingShadowsTintChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "阴影色调"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(gradingShadowsTint = newValue)
    }
    
    data class GradingBlendingChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "混合"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(gradingBlending = newValue)
    }
    
    data class GradingBalanceChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "平衡"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(gradingBalance = newValue)
    }
    
    // 效果
    data class TextureChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "纹理"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(texture = newValue)
    }
    
    data class DehazeChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "去雾"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(dehaze = newValue)
    }
    
    data class VignetteChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "晕影"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(vignette = newValue)
    }
    
    data class GrainChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "颗粒"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(grain = newValue)
    }
    
    // 细节
    data class SharpeningChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "锐化"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(sharpening = newValue)
    }
    
    data class NoiseReductionChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "降噪"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(noiseReduction = newValue)
    }
    
    // 曲线
    data class RgbCurveChange(
        val enabled: Boolean,
        val points: List<Pair<Float, Float>>,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "RGB曲线"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(
            enableRgbCurve = enabled,
            rgbCurvePoints = points
        )
    }
    
    data class RedCurveChange(
        val enabled: Boolean,
        val points: List<Pair<Float, Float>>,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "红色曲线"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(
            enableRedCurve = enabled,
            redCurvePoints = points
        )
    }
    
    data class GreenCurveChange(
        val enabled: Boolean,
        val points: List<Pair<Float, Float>>,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "绿色曲线"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(
            enableGreenCurve = enabled,
            greenCurvePoints = points
        )
    }
    
    data class BlueCurveChange(
        val enabled: Boolean,
        val points: List<Pair<Float, Float>>,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "蓝色曲线"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(
            enableBlueCurve = enabled,
            blueCurvePoints = points
        )
    }
    
    // HSL
    data class HSLChange(
        val enabled: Boolean,
        val hueShift: FloatArray,
        val saturation: FloatArray,
        val luminance: FloatArray,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "HSL调整"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(
            enableHSL = enabled,
            hslHueShift = hueShift,
            hslSaturation = saturation,
            hslLuminance = luminance
        )
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as HSLChange
            
            if (enabled != other.enabled) return false
            if (!hueShift.contentEquals(other.hueShift)) return false
            if (!saturation.contentEquals(other.saturation)) return false
            if (!luminance.contentEquals(other.luminance)) return false
            if (timestamp != other.timestamp) return false
            if (description != other.description) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = enabled.hashCode()
            result = 31 * result + hueShift.contentHashCode()
            result = 31 * result + saturation.contentHashCode()
            result = 31 * result + luminance.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + description.hashCode()
            return result
        }
    }
    
    // 几何
    data class RotationChange(
        val newValue: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "旋转"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(rotation = newValue)
    }
    
    data class CropChange(
        val enabled: Boolean,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "裁剪"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams) = params.copy(
            cropEnabled = enabled,
            cropLeft = left,
            cropTop = top,
            cropRight = right,
            cropBottom = bottom
        )
    }
    
    // 多个变更
    data class MultipleChanges(
        val changes: List<ParameterChange>,
        override val timestamp: Long = System.currentTimeMillis(),
        override val description: String = "多项调整"
    ) : ParameterChange() {
        override fun apply(params: AdjustmentParams): AdjustmentParams {
            return changes.fold(params) { acc, change -> change.apply(acc) }
        }
    }
}
