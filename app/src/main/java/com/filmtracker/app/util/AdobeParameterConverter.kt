package com.filmtracker.app.util

import com.filmtracker.app.data.BasicAdjustmentParams
import kotlin.math.pow

/**
 * Adobe 参数转换器
 * 
 * 将 Adobe 标准参数（-100 到 +100）转换为内部处理参数
 * 确保 AI 建议的参数能正确应用到调色系统
 */
object AdobeParameterConverter {
    
    /**
     * 将 Adobe 对比度（-100 到 +100）转换为乘数（约 0.5 到 2.0）
     * 
     * Adobe 标准：
     * - -100: 最低对比度（约 0.5）
     * - 0: 不变（1.0）
     * - +100: 最高对比度（约 2.0）
     */
    fun contrastToMultiplier(adobeContrast: Float): Float {
        return when {
            adobeContrast >= 0 -> 1.0f + (adobeContrast / 100f) * 1.0f  // 0 到 +100 映射到 1.0 到 2.0
            else -> 1.0f + (adobeContrast / 100f) * 0.5f  // -100 到 0 映射到 0.5 到 1.0
        }
    }
    
    /**
     * 将乘数转换回 Adobe 对比度
     */
    fun multiplierToContrast(multiplier: Float): Float {
        return when {
            multiplier >= 1.0f -> (multiplier - 1.0f) * 100f
            else -> (multiplier - 1.0f) * 200f
        }
    }
    
    /**
     * 将 Adobe 饱和度（-100 到 +100）转换为乘数（0.0 到 2.0）
     * 
     * Adobe 标准：
     * - -100: 完全去色（0.0）
     * - 0: 不变（1.0）
     * - +100: 最高饱和度（约 2.0）
     */
    fun saturationToMultiplier(adobeSaturation: Float): Float {
        return when {
            adobeSaturation >= 0 -> 1.0f + (adobeSaturation / 100f) * 1.0f  // 0 到 +100 映射到 1.0 到 2.0
            else -> 1.0f + (adobeSaturation / 100f)  // -100 到 0 映射到 0.0 到 1.0
        }
    }
    
    /**
     * 将乘数转换回 Adobe 饱和度
     */
    fun multiplierToSaturation(multiplier: Float): Float {
        return when {
            multiplier >= 1.0f -> (multiplier - 1.0f) * 100f
            else -> (multiplier - 1.0f) * 100f
        }
    }
    
    /**
     * 将 Adobe 色温（-100 到 +100）转换为开尔文值（约 2000K 到 10000K）
     * 
     * Adobe 标准：
     * - -100: 最冷（约 2000K）
     * - 0: 中性（约 5500K）
     * - +100: 最暖（约 10000K）
     */
    fun temperatureToKelvin(adobeTemp: Float): Float {
        val neutral = 5500f
        return when {
            adobeTemp >= 0 -> neutral + (adobeTemp / 100f) * 4500f  // 5500K 到 10000K
            else -> neutral + (adobeTemp / 100f) * 3500f  // 2000K 到 5500K
        }
    }
    
    /**
     * 将开尔文值转换回 Adobe 色温
     */
    fun kelvinToTemperature(kelvin: Float): Float {
        val neutral = 5500f
        return when {
            kelvin >= neutral -> ((kelvin - neutral) / 4500f) * 100f
            else -> ((kelvin - neutral) / 3500f) * 100f
        }
    }
    
    /**
     * 验证参数是否在 Adobe 标准范围内
     */
    fun validateAdobeParameter(value: Float, min: Float = -100f, max: Float = 100f): Float {
        return value.coerceIn(min, max)
    }
    
    /**
     * 从旧格式参数转换为 Adobe 标准格式
     * 用于兼容旧版本数据
     */
    fun convertLegacyParams(params: BasicAdjustmentParams): BasicAdjustmentParams {
        return params.copy(
            contrast = if (params.contrast > 10f) {
                // 旧格式：乘数（0.5 到 2.0）
                multiplierToContrast(params.contrast)
            } else {
                // 已经是新格式
                params.contrast
            },
            saturation = if (params.saturation > 10f) {
                // 旧格式：乘数（0.0 到 2.0）
                multiplierToSaturation(params.saturation)
            } else {
                // 已经是新格式
                params.saturation
            }
        )
    }
    
    /**
     * 获取参数的显示文本（带单位）
     */
    fun getParameterDisplayText(paramName: String, value: Float): String {
        return when (paramName.lowercase()) {
            "exposure", "globalexposure" -> String.format("%.2f EV", value)
            "temperature" -> String.format("%.0f K", temperatureToKelvin(value))
            "contrast", "saturation", "highlights", "shadows", "whites", "blacks",
            "clarity", "vibrance", "tint", "texture", "dehaze", "vignette",
            "grain", "sharpening", "noisereduction" -> {
                val sign = if (value > 0) "+" else ""
                "$sign${value.toInt()}"
            }
            else -> value.toString()
        }
    }
}
