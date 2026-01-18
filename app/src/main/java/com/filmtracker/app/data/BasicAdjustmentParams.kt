package com.filmtracker.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * FloatArray 序列化器
 */
object FloatArraySerializer : KSerializer<FloatArray> {
    private val listSerializer = ListSerializer(Float.serializer())
    
    override val descriptor: SerialDescriptor = listSerializer.descriptor
    
    override fun serialize(encoder: Encoder, value: FloatArray) {
        encoder.encodeSerializableValue(listSerializer, value.toList())
    }
    
    override fun deserialize(decoder: Decoder): FloatArray {
        return decoder.decodeSerializableValue(listSerializer).toFloatArray()
    }
}

/**
 * 基础调整参数（独立于胶片模拟）
 * 对应 Adobe Camera RAW / Lightroom 的基础面板
 */
@Serializable
data class BasicAdjustmentParams(
    // 全局调整
    var globalExposure: Float = 0.0f,   // 曝光（EV，-5 到 +5）
    var contrast: Float = 1.0f,         // 对比度（推荐 0.6 到 1.3，1.0 为不变）
    var saturation: Float = 1.0f,       // 饱和度（0.0 到 2.0，1.0 为不变）
    
    // 色调调整
    var highlights: Float = 0.0f,       // 高光（-100 到 +100）
    var shadows: Float = 0.0f,          // 阴影（-100 到 +100）
    var whites: Float = 0.0f,           // 白场（-100 到 +100）
    var blacks: Float = 0.0f,           // 黑场（-100 到 +100）
    
    // 存在感调整
    var clarity: Float = 0.0f,          // 清晰度（-100 到 +100）
    var vibrance: Float = 0.0f,         // 自然饱和度（-100 到 +100）
    
    // 颜色调整
    var temperature: Float = 0.0f,      // 色温（-100 到 +100）
    var tint: Float = 0.0f,             // 色调（-100 到 +100）
    
    // 分级调整（Color Grading）
    var gradingHighlightsTemp: Float = 0.0f,    // 高光色温（-100 到 +100）
    var gradingHighlightsTint: Float = 0.0f,    // 高光色调（-100 到 +100）
    var gradingMidtonesTemp: Float = 0.0f,      // 中间调色温（-100 到 +100）
    var gradingMidtonesTint: Float = 0.0f,      // 中间调色调（-100 到 +100）
    var gradingShadowsTemp: Float = 0.0f,       // 阴影色温（-100 到 +100）
    var gradingShadowsTint: Float = 0.0f,       // 阴影色调（-100 到 +100）
    var gradingBlending: Float = 50.0f,         // 混合（0 到 100）
    var gradingBalance: Float = 0.0f,           // 平衡（-100 到 +100）
    
    // 效果调整
    var texture: Float = 0.0f,          // 纹理（-100 到 +100）
    var dehaze: Float = 0.0f,           // 去雾（-100 到 +100）
    var vignette: Float = 0.0f,         // 晕影（-100 到 +100）
    var grain: Float = 0.0f,            // 颗粒（0 到 100）
    
    // 细节调整
    var sharpening: Float = 0.0f,       // 锐化（0 到 100）
    var noiseReduction: Float = 0.0f,   // 降噪（0 到 100）
    
    // 色调曲线（动态控制点列表，每个点为 (x, y) 坐标，范围 0.0-1.0）
    var enableRgbCurve: Boolean = false,
    var rgbCurvePoints: List<Pair<Float, Float>> = listOf(Pair(0f, 0f), Pair(1f, 1f)),
    var enableRedCurve: Boolean = false,
    var redCurvePoints: List<Pair<Float, Float>> = listOf(Pair(0f, 0f), Pair(1f, 1f)),
    var enableGreenCurve: Boolean = false,
    var greenCurvePoints: List<Pair<Float, Float>> = listOf(Pair(0f, 0f), Pair(1f, 1f)),
    var enableBlueCurve: Boolean = false,
    var blueCurvePoints: List<Pair<Float, Float>> = listOf(Pair(0f, 0f), Pair(1f, 1f)),
    
    // HSL 调整（8个色相段：红、橙、黄、绿、青、蓝、紫、品红）
    var enableHSL: Boolean = false,
    @Serializable(with = FloatArraySerializer::class)
    var hslHueShift: FloatArray = FloatArray(8) { 0.0f },      // [-180, 180] 度
    @Serializable(with = FloatArraySerializer::class)
    var hslSaturation: FloatArray = FloatArray(8) { 0.0f },   // [-100, 100] %
    @Serializable(with = FloatArraySerializer::class)
    var hslLuminance: FloatArray = FloatArray(8) { 0.0f },    // [-100, 100] %
    
    // 几何调整
    var rotation: Float = 0.0f,         // 旋转角度（度）
    var cropEnabled: Boolean = false,   // 是否启用裁剪
    var cropLeft: Float = 0.0f,         // 裁剪左（0-1，归一化）
    var cropTop: Float = 0.0f,          // 裁剪上（0-1，归一化）
    var cropRight: Float = 1.0f,        // 裁剪右（0-1，归一化）
    var cropBottom: Float = 1.0f        // 裁剪下（0-1，归一化）
) {
    companion object {
        /**
         * 创建默认的中性参数
         */
        fun neutral(): BasicAdjustmentParams {
            return BasicAdjustmentParams()
        }
    }
    
    /**
     * 深拷贝方法
     * 
     * 注意：Kotlin 的 data class 自动生成的 copy() 方法对数组是浅拷贝，
     * 这会导致增量渲染的参数比较失败。因此需要手动深拷贝数组字段。
     */
    fun deepCopy(): BasicAdjustmentParams {
        return this.copy(
            hslHueShift = this.hslHueShift.copyOf(),
            hslSaturation = this.hslSaturation.copyOf(),
            hslLuminance = this.hslLuminance.copyOf()
        )
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
        var result = globalExposure.hashCode()
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
