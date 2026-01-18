package com.filmtracker.app.data.mapper

import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.domain.model.AdjustmentParams

/**
 * 调整参数映射器
 * 负责领域模型和数据模型之间的转换
 */
class AdjustmentParamsMapper {
    
    /**
     * 领域模型 -> 数据模型
     */
    fun toData(domain: AdjustmentParams): BasicAdjustmentParams {
        return BasicAdjustmentParams(
            globalExposure = domain.exposure,
            contrast = domain.contrast,
            saturation = domain.saturation,
            highlights = domain.highlights,
            shadows = domain.shadows,
            whites = domain.whites,
            blacks = domain.blacks,
            clarity = domain.clarity,
            vibrance = domain.vibrance,
            temperature = domain.temperature,
            tint = domain.tint,
            gradingHighlightsTemp = domain.gradingHighlightsTemp,
            gradingHighlightsTint = domain.gradingHighlightsTint,
            gradingMidtonesTemp = domain.gradingMidtonesTemp,
            gradingMidtonesTint = domain.gradingMidtonesTint,
            gradingShadowsTemp = domain.gradingShadowsTemp,
            gradingShadowsTint = domain.gradingShadowsTint,
            gradingBlending = domain.gradingBlending,
            gradingBalance = domain.gradingBalance,
            texture = domain.texture,
            dehaze = domain.dehaze,
            vignette = domain.vignette,
            grain = domain.grain,
            sharpening = domain.sharpening,
            noiseReduction = domain.noiseReduction,
            enableRgbCurve = domain.enableRgbCurve,
            rgbCurvePoints = domain.rgbCurvePoints,
            enableRedCurve = domain.enableRedCurve,
            redCurvePoints = domain.redCurvePoints,
            enableGreenCurve = domain.enableGreenCurve,
            greenCurvePoints = domain.greenCurvePoints,
            enableBlueCurve = domain.enableBlueCurve,
            blueCurvePoints = domain.blueCurvePoints,
            enableHSL = domain.enableHSL,
            hslHueShift = domain.hslHueShift,
            hslSaturation = domain.hslSaturation,
            hslLuminance = domain.hslLuminance,
            rotation = domain.rotation,
            cropEnabled = domain.cropEnabled,
            cropLeft = domain.cropLeft,
            cropTop = domain.cropTop,
            cropRight = domain.cropRight,
            cropBottom = domain.cropBottom
        )
    }
    
    /**
     * 数据模型 -> 领域模型
     */
    fun toDomain(data: BasicAdjustmentParams): AdjustmentParams {
        return AdjustmentParams(
            exposure = data.globalExposure,
            contrast = data.contrast,
            saturation = data.saturation,
            highlights = data.highlights,
            shadows = data.shadows,
            whites = data.whites,
            blacks = data.blacks,
            clarity = data.clarity,
            vibrance = data.vibrance,
            temperature = data.temperature,
            tint = data.tint,
            gradingHighlightsTemp = data.gradingHighlightsTemp,
            gradingHighlightsTint = data.gradingHighlightsTint,
            gradingMidtonesTemp = data.gradingMidtonesTemp,
            gradingMidtonesTint = data.gradingMidtonesTint,
            gradingShadowsTemp = data.gradingShadowsTemp,
            gradingShadowsTint = data.gradingShadowsTint,
            gradingBlending = data.gradingBlending,
            gradingBalance = data.gradingBalance,
            texture = data.texture,
            dehaze = data.dehaze,
            vignette = data.vignette,
            grain = data.grain,
            sharpening = data.sharpening,
            noiseReduction = data.noiseReduction,
            enableRgbCurve = data.enableRgbCurve,
            rgbCurvePoints = data.rgbCurvePoints,
            enableRedCurve = data.enableRedCurve,
            redCurvePoints = data.redCurvePoints,
            enableGreenCurve = data.enableGreenCurve,
            greenCurvePoints = data.greenCurvePoints,
            enableBlueCurve = data.enableBlueCurve,
            blueCurvePoints = data.blueCurvePoints,
            enableHSL = data.enableHSL,
            hslHueShift = data.hslHueShift,
            hslSaturation = data.hslSaturation,
            hslLuminance = data.hslLuminance,
            rotation = data.rotation,
            cropEnabled = data.cropEnabled,
            cropLeft = data.cropLeft,
            cropTop = data.cropTop,
            cropRight = data.cropRight,
            cropBottom = data.cropBottom
        )
    }
}
