package com.filmtracker.app.data

/**
 * 胶片参数数据类（Kotlin 层）
 * 用于 UI 绑定和参数控制
 */
data class FilmParams(
    // 全局调整
    var globalExposure: Float = 0.0f,      // EV
    var contrast: Float = 1.0f,
    var saturation: Float = 1.0f,

    // 基础色调（对应 C++ BasicToneParams，范围推荐 [-1, 1]）
    var highlights: Float = 0.0f,
    var shadows: Float = 0.0f,
    var whites: Float = 0.0f,
    var blacks: Float = 0.0f,
    var clarity: Float = 0.0f,
    var vibrance: Float = 0.0f,
    
    // Red 通道响应曲线
    var redToeSlope: Float = 0.3f,
    var redToeStrength: Float = 0.15f,
    var redToePoint: Float = 0.05f,
    var redLinearSlope: Float = 1.0f,
    var redShoulderSlope: Float = 0.4f,
    var redShoulderStrength: Float = 0.8f,
    var redShoulderPoint: Float = 0.7f,
    
    // Green 通道
    var greenToeSlope: Float = 0.3f,
    var greenToeStrength: Float = 0.12f,
    var greenToePoint: Float = 0.05f,
    var greenLinearSlope: Float = 1.0f,
    var greenShoulderSlope: Float = 0.4f,
    var greenShoulderStrength: Float = 0.8f,
    var greenShoulderPoint: Float = 0.7f,
    
    // Blue 通道
    var blueToeSlope: Float = 0.3f,
    var blueToeStrength: Float = 0.15f,
    var blueToePoint: Float = 0.05f,
    var blueLinearSlope: Float = 1.0f,
    var blueShoulderSlope: Float = 0.4f,
    var blueShoulderStrength: Float = 0.75f,
    var blueShoulderPoint: Float = 0.7f,
    
    // 颜色串扰矩阵
    var crosstalkRtoR: Float = 1.0f,
    var crosstalkGtoR: Float = 0.05f,
    var crosstalkBtoR: Float = 0.0f,
    var crosstalkRtoG: Float = 0.03f,
    var crosstalkGtoG: Float = 1.0f,
    var crosstalkBtoG: Float = 0.0f,
    var crosstalkRtoB: Float = 0.0f,
    var crosstalkGtoB: Float = 0.04f,
    var crosstalkBtoB: Float = 1.0f,
    
    // 颗粒参数
    var grainEnabled: Boolean = true,
    var grainBaseDensity: Float = 0.02f,
    var grainIsoMultiplier: Float = 1.0f,
    var grainSizeVariation: Float = 0.3f,
    var grainColorCoupling: Float = 0.5f,
    
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
    /**
     * 创建预设（模拟不同胶片）
     */
    companion object {
        fun portra400(): FilmParams {
            return FilmParams(
                globalExposure = 0.0f,
                contrast = 1.0f,
                saturation = 1.1f,
                // Portra 400 特征：软肩部，轻微 Toe
                redShoulderStrength = 0.8f,
                greenShoulderStrength = 0.8f,
                blueShoulderStrength = 0.75f
            )
        }
        
        fun kodakGold200(): FilmParams {
            return FilmParams(
                globalExposure = 0.2f,
                contrast = 1.15f,
                saturation = 1.2f,
                // Gold 200 特征：较高对比度，暖色调
                redShoulderStrength = 0.7f,
                greenShoulderStrength = 0.75f,
                blueShoulderStrength = 0.65f
            )
        }
        
        fun fujiSuperia400(): FilmParams {
            return FilmParams(
                globalExposure = 0.0f,
                contrast = 1.05f,
                saturation = 1.15f,
                // Superia 特征：冷色调倾向
                crosstalkBtoG = 0.02f,
                blueShoulderStrength = 0.8f
            )
        }
    }
}
