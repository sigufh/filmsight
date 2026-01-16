package com.filmtracker.app.processing

import com.filmtracker.app.data.BasicAdjustmentParams

/**
 * 参数到阶段的映射
 * 
 * 定义每个参数属于哪个处理阶段，用于：
 * 1. 确定参数变化时需要从哪个阶段开始重新计算
 * 2. 检测参数变化属于哪个阶段
 */
object StageParameterMapping {
    
    /**
     * 参数名称枚举
     */
    enum class ParameterName {
        // TONE_BASE 阶段参数
        GLOBAL_EXPOSURE,
        CONTRAST,
        HIGHLIGHTS,
        SHADOWS,
        WHITES,
        BLACKS,
        
        // CURVES 阶段参数
        ENABLE_RGB_CURVE,
        RGB_CURVE_POINTS,
        ENABLE_RED_CURVE,
        RED_CURVE_POINTS,
        ENABLE_GREEN_CURVE,
        GREEN_CURVE_POINTS,
        ENABLE_BLUE_CURVE,
        BLUE_CURVE_POINTS,
        
        // COLOR 阶段参数
        TEMPERATURE,
        TINT,
        SATURATION,
        VIBRANCE,
        ENABLE_HSL,
        HSL_HUE_SHIFT,
        HSL_SATURATION,
        HSL_LUMINANCE,
        GRADING_HIGHLIGHTS_TEMP,
        GRADING_HIGHLIGHTS_TINT,
        GRADING_MIDTONES_TEMP,
        GRADING_MIDTONES_TINT,
        GRADING_SHADOWS_TEMP,
        GRADING_SHADOWS_TINT,
        GRADING_BLENDING,
        GRADING_BALANCE,
        
        // EFFECTS 阶段参数
        CLARITY,
        TEXTURE,
        DEHAZE,
        VIGNETTE,
        GRAIN,
        
        // DETAILS 阶段参数
        SHARPENING,
        NOISE_REDUCTION
    }
    
    /**
     * 参数到阶段的映射表
     */
    private val parameterToStage: Map<ParameterName, ProcessingStage> = mapOf(
        // TONE_BASE 阶段
        ParameterName.GLOBAL_EXPOSURE to ProcessingStage.TONE_BASE,
        ParameterName.CONTRAST to ProcessingStage.TONE_BASE,
        ParameterName.HIGHLIGHTS to ProcessingStage.TONE_BASE,
        ParameterName.SHADOWS to ProcessingStage.TONE_BASE,
        ParameterName.WHITES to ProcessingStage.TONE_BASE,
        ParameterName.BLACKS to ProcessingStage.TONE_BASE,
        
        // CURVES 阶段
        ParameterName.ENABLE_RGB_CURVE to ProcessingStage.CURVES,
        ParameterName.RGB_CURVE_POINTS to ProcessingStage.CURVES,
        ParameterName.ENABLE_RED_CURVE to ProcessingStage.CURVES,
        ParameterName.RED_CURVE_POINTS to ProcessingStage.CURVES,
        ParameterName.ENABLE_GREEN_CURVE to ProcessingStage.CURVES,
        ParameterName.GREEN_CURVE_POINTS to ProcessingStage.CURVES,
        ParameterName.ENABLE_BLUE_CURVE to ProcessingStage.CURVES,
        ParameterName.BLUE_CURVE_POINTS to ProcessingStage.CURVES,
        
        // COLOR 阶段
        ParameterName.TEMPERATURE to ProcessingStage.COLOR,
        ParameterName.TINT to ProcessingStage.COLOR,
        ParameterName.SATURATION to ProcessingStage.COLOR,
        ParameterName.VIBRANCE to ProcessingStage.COLOR,
        ParameterName.ENABLE_HSL to ProcessingStage.COLOR,
        ParameterName.HSL_HUE_SHIFT to ProcessingStage.COLOR,
        ParameterName.HSL_SATURATION to ProcessingStage.COLOR,
        ParameterName.HSL_LUMINANCE to ProcessingStage.COLOR,
        ParameterName.GRADING_HIGHLIGHTS_TEMP to ProcessingStage.COLOR,
        ParameterName.GRADING_HIGHLIGHTS_TINT to ProcessingStage.COLOR,
        ParameterName.GRADING_MIDTONES_TEMP to ProcessingStage.COLOR,
        ParameterName.GRADING_MIDTONES_TINT to ProcessingStage.COLOR,
        ParameterName.GRADING_SHADOWS_TEMP to ProcessingStage.COLOR,
        ParameterName.GRADING_SHADOWS_TINT to ProcessingStage.COLOR,
        ParameterName.GRADING_BLENDING to ProcessingStage.COLOR,
        ParameterName.GRADING_BALANCE to ProcessingStage.COLOR,
        
        // EFFECTS 阶段
        ParameterName.CLARITY to ProcessingStage.EFFECTS,
        ParameterName.TEXTURE to ProcessingStage.EFFECTS,
        ParameterName.DEHAZE to ProcessingStage.EFFECTS,
        ParameterName.VIGNETTE to ProcessingStage.EFFECTS,
        ParameterName.GRAIN to ProcessingStage.EFFECTS,
        
        // DETAILS 阶段
        ParameterName.SHARPENING to ProcessingStage.DETAILS,
        ParameterName.NOISE_REDUCTION to ProcessingStage.DETAILS
    )
    
    /**
     * 阶段到参数的反向映射
     */
    private val stageToParameters: Map<ProcessingStage, Set<ParameterName>> by lazy {
        parameterToStage.entries
            .groupBy { it.value }
            .mapValues { entry -> entry.value.map { it.key }.toSet() }
    }
    
    /**
     * 获取参数所属的阶段
     */
    fun getStageForParameter(parameter: ParameterName): ProcessingStage {
        return parameterToStage[parameter] 
            ?: throw IllegalArgumentException("Unknown parameter: $parameter")
    }
    
    /**
     * 获取阶段包含的所有参数
     */
    fun getParametersForStage(stage: ProcessingStage): Set<ParameterName> {
        return stageToParameters[stage] ?: emptySet()
    }
    
    /**
     * 检查参数是否属于指定阶段
     */
    fun isParameterInStage(parameter: ParameterName, stage: ProcessingStage): Boolean {
        return parameterToStage[parameter] == stage
    }
    
    /**
     * 获取所有参数名称
     */
    fun getAllParameters(): Set<ParameterName> {
        return parameterToStage.keys
    }
    
    /**
     * 检测两组参数之间变化的参数
     * 返回变化的参数名称集合
     * 
     * 使用 epsilon 比较浮点数，避免浮点精度问题导致的误判
     */
    fun detectChangedParameters(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams
    ): Set<ParameterName> {
        if (oldParams == null) {
            return getAllParameters()
        }
        
        val changedParams = mutableSetOf<ParameterName>()
        
        // TONE_BASE 阶段参数比较（使用 epsilon 比较）
        if (!floatEquals(oldParams.globalExposure, newParams.globalExposure)) {
            changedParams.add(ParameterName.GLOBAL_EXPOSURE)
        }
        if (!floatEquals(oldParams.contrast, newParams.contrast)) {
            changedParams.add(ParameterName.CONTRAST)
        }
        if (!floatEquals(oldParams.highlights, newParams.highlights)) {
            changedParams.add(ParameterName.HIGHLIGHTS)
        }
        if (!floatEquals(oldParams.shadows, newParams.shadows)) {
            changedParams.add(ParameterName.SHADOWS)
        }
        if (!floatEquals(oldParams.whites, newParams.whites)) {
            changedParams.add(ParameterName.WHITES)
        }
        if (!floatEquals(oldParams.blacks, newParams.blacks)) {
            changedParams.add(ParameterName.BLACKS)
        }
        
        // CURVES 阶段参数比较
        if (oldParams.enableRgbCurve != newParams.enableRgbCurve) {
            changedParams.add(ParameterName.ENABLE_RGB_CURVE)
        }
        if (oldParams.rgbCurvePoints != newParams.rgbCurvePoints) {
            changedParams.add(ParameterName.RGB_CURVE_POINTS)
        }
        if (oldParams.enableRedCurve != newParams.enableRedCurve) {
            changedParams.add(ParameterName.ENABLE_RED_CURVE)
        }
        if (oldParams.redCurvePoints != newParams.redCurvePoints) {
            changedParams.add(ParameterName.RED_CURVE_POINTS)
        }
        if (oldParams.enableGreenCurve != newParams.enableGreenCurve) {
            changedParams.add(ParameterName.ENABLE_GREEN_CURVE)
        }
        if (oldParams.greenCurvePoints != newParams.greenCurvePoints) {
            changedParams.add(ParameterName.GREEN_CURVE_POINTS)
        }
        if (oldParams.enableBlueCurve != newParams.enableBlueCurve) {
            changedParams.add(ParameterName.ENABLE_BLUE_CURVE)
        }
        if (oldParams.blueCurvePoints != newParams.blueCurvePoints) {
            changedParams.add(ParameterName.BLUE_CURVE_POINTS)
        }
        
        // COLOR 阶段参数比较（使用 epsilon 比较）
        if (!floatEquals(oldParams.temperature, newParams.temperature)) {
            changedParams.add(ParameterName.TEMPERATURE)
        }
        if (!floatEquals(oldParams.tint, newParams.tint)) {
            changedParams.add(ParameterName.TINT)
        }
        if (!floatEquals(oldParams.saturation, newParams.saturation)) {
            changedParams.add(ParameterName.SATURATION)
        }
        if (!floatEquals(oldParams.vibrance, newParams.vibrance)) {
            changedParams.add(ParameterName.VIBRANCE)
        }
        if (oldParams.enableHSL != newParams.enableHSL) {
            changedParams.add(ParameterName.ENABLE_HSL)
        }
        if (!floatArrayEquals(oldParams.hslHueShift, newParams.hslHueShift)) {
            changedParams.add(ParameterName.HSL_HUE_SHIFT)
        }
        if (!floatArrayEquals(oldParams.hslSaturation, newParams.hslSaturation)) {
            changedParams.add(ParameterName.HSL_SATURATION)
        }
        if (!floatArrayEquals(oldParams.hslLuminance, newParams.hslLuminance)) {
            changedParams.add(ParameterName.HSL_LUMINANCE)
        }
        if (!floatEquals(oldParams.gradingHighlightsTemp, newParams.gradingHighlightsTemp)) {
            changedParams.add(ParameterName.GRADING_HIGHLIGHTS_TEMP)
        }
        if (!floatEquals(oldParams.gradingHighlightsTint, newParams.gradingHighlightsTint)) {
            changedParams.add(ParameterName.GRADING_HIGHLIGHTS_TINT)
        }
        if (!floatEquals(oldParams.gradingMidtonesTemp, newParams.gradingMidtonesTemp)) {
            changedParams.add(ParameterName.GRADING_MIDTONES_TEMP)
        }
        if (!floatEquals(oldParams.gradingMidtonesTint, newParams.gradingMidtonesTint)) {
            changedParams.add(ParameterName.GRADING_MIDTONES_TINT)
        }
        if (!floatEquals(oldParams.gradingShadowsTemp, newParams.gradingShadowsTemp)) {
            changedParams.add(ParameterName.GRADING_SHADOWS_TEMP)
        }
        if (!floatEquals(oldParams.gradingShadowsTint, newParams.gradingShadowsTint)) {
            changedParams.add(ParameterName.GRADING_SHADOWS_TINT)
        }
        if (!floatEquals(oldParams.gradingBlending, newParams.gradingBlending)) {
            changedParams.add(ParameterName.GRADING_BLENDING)
        }
        if (!floatEquals(oldParams.gradingBalance, newParams.gradingBalance)) {
            changedParams.add(ParameterName.GRADING_BALANCE)
        }
        
        // EFFECTS 阶段参数比较（使用 epsilon 比较）
        if (!floatEquals(oldParams.clarity, newParams.clarity)) {
            changedParams.add(ParameterName.CLARITY)
        }
        if (!floatEquals(oldParams.texture, newParams.texture)) {
            changedParams.add(ParameterName.TEXTURE)
        }
        if (!floatEquals(oldParams.dehaze, newParams.dehaze)) {
            changedParams.add(ParameterName.DEHAZE)
        }
        if (!floatEquals(oldParams.vignette, newParams.vignette)) {
            changedParams.add(ParameterName.VIGNETTE)
        }
        if (!floatEquals(oldParams.grain, newParams.grain)) {
            changedParams.add(ParameterName.GRAIN)
        }
        
        // DETAILS 阶段参数比较（使用 epsilon 比较）
        if (!floatEquals(oldParams.sharpening, newParams.sharpening)) {
            changedParams.add(ParameterName.SHARPENING)
        }
        if (!floatEquals(oldParams.noiseReduction, newParams.noiseReduction)) {
            changedParams.add(ParameterName.NOISE_REDUCTION)
        }
        
        return changedParams
    }
    
    /**
     * 获取变化参数所影响的最早阶段
     * 用于确定增量计算的起始点
     */
    fun getEarliestAffectedStage(changedParams: Set<ParameterName>): ProcessingStage? {
        if (changedParams.isEmpty()) return null
        
        return changedParams
            .map { getStageForParameter(it) }
            .minByOrNull { it.order }
    }
    
    /**
     * 获取从指定阶段开始需要重新计算的所有阶段
     */
    fun getStagesFromStage(startStage: ProcessingStage): List<ProcessingStage> {
        return ProcessingStage.getOrderedStages()
            .filter { it.order >= startStage.order }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 浮点数相等比较（使用 epsilon）
     * 
     * 使用 0.1 的 epsilon 值，对应 1 位小数精度
     * 这与 ParameterHasher 中的舍入策略一致
     */
    private fun floatEquals(a: Float, b: Float, epsilon: Float = 0.05f): Boolean {
        return Math.abs(a - b) < epsilon
    }
    
    /**
     * 浮点数组相等比较（使用 epsilon）
     */
    private fun floatArrayEquals(a: FloatArray, b: FloatArray, epsilon: Float = 0.05f): Boolean {
        if (a.size != b.size) return false
        return a.indices.all { i -> floatEquals(a[i], b[i], epsilon) }
    }
}
