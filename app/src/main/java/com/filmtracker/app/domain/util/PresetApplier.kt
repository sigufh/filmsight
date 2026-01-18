package com.filmtracker.app.domain.util

import com.filmtracker.app.data.Preset
import com.filmtracker.app.domain.model.AdjustmentParams

/**
 * 预设应用工具
 * 负责将预设应用到调整参数，支持部分预设
 */
object PresetApplier {
    
    /**
     * 应用预设到当前参数
     * 如果预设包含参数掩码（部分预设），则仅应用掩码中指定的参数
     * 如果预设不包含参数掩码（完整预设），则应用所有参数
     * 
     * @param preset 要应用的预设
     * @param currentParams 当前的调整参数（领域模型）
     * @return 应用预设后的新参数
     */
    fun applyPreset(preset: Preset, currentParams: AdjustmentParams): AdjustmentParams {
        val mask = preset.parameterMask
        val presetParams = preset.params
        
        // 如果没有掩码，应用所有参数（完整预设）
        if (mask == null) {
            return AdjustmentParams(
                exposure = presetParams.globalExposure,
                contrast = presetParams.contrast,
                saturation = presetParams.saturation,
                highlights = presetParams.highlights,
                shadows = presetParams.shadows,
                whites = presetParams.whites,
                blacks = presetParams.blacks,
                clarity = presetParams.clarity,
                vibrance = presetParams.vibrance,
                temperature = presetParams.temperature,
                tint = presetParams.tint,
                gradingHighlightsTemp = presetParams.gradingHighlightsTemp,
                gradingHighlightsTint = presetParams.gradingHighlightsTint,
                gradingMidtonesTemp = presetParams.gradingMidtonesTemp,
                gradingMidtonesTint = presetParams.gradingMidtonesTint,
                gradingShadowsTemp = presetParams.gradingShadowsTemp,
                gradingShadowsTint = presetParams.gradingShadowsTint,
                gradingBlending = presetParams.gradingBlending,
                gradingBalance = presetParams.gradingBalance,
                texture = presetParams.texture,
                dehaze = presetParams.dehaze,
                vignette = presetParams.vignette,
                grain = presetParams.grain,
                sharpening = presetParams.sharpening,
                noiseReduction = presetParams.noiseReduction,
                enableRgbCurve = presetParams.enableRgbCurve,
                rgbCurvePoints = presetParams.rgbCurvePoints,
                enableRedCurve = presetParams.enableRedCurve,
                redCurvePoints = presetParams.redCurvePoints,
                enableGreenCurve = presetParams.enableGreenCurve,
                greenCurvePoints = presetParams.greenCurvePoints,
                enableBlueCurve = presetParams.enableBlueCurve,
                blueCurvePoints = presetParams.blueCurvePoints,
                enableHSL = presetParams.enableHSL,
                hslHueShift = presetParams.hslHueShift,
                hslSaturation = presetParams.hslSaturation,
                hslLuminance = presetParams.hslLuminance,
                rotation = presetParams.rotation,
                cropEnabled = presetParams.cropEnabled,
                cropLeft = presetParams.cropLeft,
                cropTop = presetParams.cropTop,
                cropRight = presetParams.cropRight,
                cropBottom = presetParams.cropBottom
            )
        }
        
        // 如果有掩码，仅应用掩码中指定的参数（部分预设）
        return currentParams.copy(
            // 基础调整
            exposure = if (mask.exposure) presetParams.globalExposure else currentParams.exposure,
            contrast = if (mask.contrast) presetParams.contrast else currentParams.contrast,
            saturation = if (mask.saturation) presetParams.saturation else currentParams.saturation,
            
            // 色调调整
            highlights = if (mask.highlights) presetParams.highlights else currentParams.highlights,
            shadows = if (mask.shadows) presetParams.shadows else currentParams.shadows,
            whites = if (mask.whites) presetParams.whites else currentParams.whites,
            blacks = if (mask.blacks) presetParams.blacks else currentParams.blacks,
            
            // 存在感
            clarity = if (mask.clarity) presetParams.clarity else currentParams.clarity,
            vibrance = if (mask.vibrance) presetParams.vibrance else currentParams.vibrance,
            
            // 颜色
            temperature = if (mask.temperature) presetParams.temperature else currentParams.temperature,
            tint = if (mask.tint) presetParams.tint else currentParams.tint,
            
            // 分级
            gradingHighlightsTemp = if (mask.gradingHighlightsTemp) presetParams.gradingHighlightsTemp else currentParams.gradingHighlightsTemp,
            gradingHighlightsTint = if (mask.gradingHighlightsTint) presetParams.gradingHighlightsTint else currentParams.gradingHighlightsTint,
            gradingMidtonesTemp = if (mask.gradingMidtonesTemp) presetParams.gradingMidtonesTemp else currentParams.gradingMidtonesTemp,
            gradingMidtonesTint = if (mask.gradingMidtonesTint) presetParams.gradingMidtonesTint else currentParams.gradingMidtonesTint,
            gradingShadowsTemp = if (mask.gradingShadowsTemp) presetParams.gradingShadowsTemp else currentParams.gradingShadowsTemp,
            gradingShadowsTint = if (mask.gradingShadowsTint) presetParams.gradingShadowsTint else currentParams.gradingShadowsTint,
            gradingBlending = if (mask.gradingBlending) presetParams.gradingBlending else currentParams.gradingBlending,
            gradingBalance = if (mask.gradingBalance) presetParams.gradingBalance else currentParams.gradingBalance,
            
            // 效果
            texture = if (mask.texture) presetParams.texture else currentParams.texture,
            dehaze = if (mask.dehaze) presetParams.dehaze else currentParams.dehaze,
            vignette = if (mask.vignette) presetParams.vignette else currentParams.vignette,
            grain = if (mask.grain) presetParams.grain else currentParams.grain,
            
            // 细节
            sharpening = if (mask.sharpening) presetParams.sharpening else currentParams.sharpening,
            noiseReduction = if (mask.noiseReduction) presetParams.noiseReduction else currentParams.noiseReduction,
            
            // 曲线
            enableRgbCurve = if (mask.enableRgbCurve) presetParams.enableRgbCurve else currentParams.enableRgbCurve,
            rgbCurvePoints = if (mask.rgbCurvePoints) presetParams.rgbCurvePoints else currentParams.rgbCurvePoints,
            enableRedCurve = if (mask.enableRedCurve) presetParams.enableRedCurve else currentParams.enableRedCurve,
            redCurvePoints = if (mask.redCurvePoints) presetParams.redCurvePoints else currentParams.redCurvePoints,
            enableGreenCurve = if (mask.enableGreenCurve) presetParams.enableGreenCurve else currentParams.enableGreenCurve,
            greenCurvePoints = if (mask.greenCurvePoints) presetParams.greenCurvePoints else currentParams.greenCurvePoints,
            enableBlueCurve = if (mask.enableBlueCurve) presetParams.enableBlueCurve else currentParams.enableBlueCurve,
            blueCurvePoints = if (mask.blueCurvePoints) presetParams.blueCurvePoints else currentParams.blueCurvePoints,
            
            // HSL
            enableHSL = if (mask.enableHSL) presetParams.enableHSL else currentParams.enableHSL,
            hslHueShift = if (mask.hslHueShift) presetParams.hslHueShift else currentParams.hslHueShift,
            hslSaturation = if (mask.hslSaturation) presetParams.hslSaturation else currentParams.hslSaturation,
            hslLuminance = if (mask.hslLuminance) presetParams.hslLuminance else currentParams.hslLuminance,
            
            // 几何
            rotation = if (mask.rotation) presetParams.rotation else currentParams.rotation,
            cropEnabled = if (mask.cropEnabled) presetParams.cropEnabled else currentParams.cropEnabled,
            cropLeft = if (mask.cropLeft) presetParams.cropLeft else currentParams.cropLeft,
            cropTop = if (mask.cropTop) presetParams.cropTop else currentParams.cropTop,
            cropRight = if (mask.cropRight) presetParams.cropRight else currentParams.cropRight,
            cropBottom = if (mask.cropBottom) presetParams.cropBottom else currentParams.cropBottom
        )
    }
}
