package com.filmtracker.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 预设管理器
 * 负责预设的保存、加载、删除等操作
 */
class PresetManager(private val context: Context) {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val presetsDir: File
        get() = File(context.filesDir, "presets").apply {
            if (!exists()) mkdirs()
        }
    
    /**
     * 获取所有预设（内置 + 用户）
     */
    suspend fun getAllPresets(): List<Preset> = withContext(Dispatchers.IO) {
        val userPresets = loadUserPresets()
        val builtInPresets = BuiltInPresets.getAll()
        builtInPresets + userPresets
    }
    
    /**
     * 按分类获取预设
     */
    suspend fun getPresetsByCategory(category: PresetCategory): List<Preset> = withContext(Dispatchers.IO) {
        getAllPresets().filter { it.category == category }
    }
    
    /**
     * 保存预设
     */
    suspend fun savePreset(preset: Preset): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(presetsDir, "${preset.id}.json")
            val jsonString = json.encodeToString(preset)
            file.writeText(jsonString)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除预设
     */
    suspend fun deletePreset(presetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 不能删除内置预设
            if (presetId.startsWith("builtin_")) {
                return@withContext Result.failure(IllegalArgumentException("Cannot delete built-in preset"))
            }
            
            val file = File(presetsDir, "$presetId.json")
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 重命名预设
     */
    suspend fun renamePreset(presetId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 不能重命名内置预设
            if (presetId.startsWith("builtin_")) {
                return@withContext Result.failure(IllegalArgumentException("Cannot rename built-in preset"))
            }
            
            val file = File(presetsDir, "$presetId.json")
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Preset not found"))
            }
            
            val jsonString = file.readText()
            val preset = json.decodeFromString<Preset>(jsonString)
            val updatedPreset = preset.copy(
                name = newName,
                modifiedAt = System.currentTimeMillis()
            )
            
            file.writeText(json.encodeToString(updatedPreset))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 应用预设到当前参数
     * 如果预设包含参数掩码（部分预设），则仅应用掩码中指定的参数
     * 如果预设不包含参数掩码（完整预设），则应用所有参数
     * 
     * @param preset 要应用的预设
     * @param currentParams 当前的调整参数
     * @return 应用预设后的新参数
     */
    fun applyPreset(preset: Preset, currentParams: BasicAdjustmentParams): BasicAdjustmentParams {
        val mask = preset.parameterMask
        
        // 如果没有掩码，应用所有参数（完整预设）
        if (mask == null) {
            return preset.params
        }
        
        // 如果有掩码，仅应用掩码中指定的参数（部分预设）
        return currentParams.copy(
            // 基础调整
            globalExposure = if (mask.exposure) preset.params.globalExposure else currentParams.globalExposure,
            contrast = if (mask.contrast) preset.params.contrast else currentParams.contrast,
            saturation = if (mask.saturation) preset.params.saturation else currentParams.saturation,
            
            // 色调调整
            highlights = if (mask.highlights) preset.params.highlights else currentParams.highlights,
            shadows = if (mask.shadows) preset.params.shadows else currentParams.shadows,
            whites = if (mask.whites) preset.params.whites else currentParams.whites,
            blacks = if (mask.blacks) preset.params.blacks else currentParams.blacks,
            
            // 存在感
            clarity = if (mask.clarity) preset.params.clarity else currentParams.clarity,
            vibrance = if (mask.vibrance) preset.params.vibrance else currentParams.vibrance,
            
            // 颜色
            temperature = if (mask.temperature) preset.params.temperature else currentParams.temperature,
            tint = if (mask.tint) preset.params.tint else currentParams.tint,
            
            // 分级
            gradingHighlightsTemp = if (mask.gradingHighlightsTemp) preset.params.gradingHighlightsTemp else currentParams.gradingHighlightsTemp,
            gradingHighlightsTint = if (mask.gradingHighlightsTint) preset.params.gradingHighlightsTint else currentParams.gradingHighlightsTint,
            gradingMidtonesTemp = if (mask.gradingMidtonesTemp) preset.params.gradingMidtonesTemp else currentParams.gradingMidtonesTemp,
            gradingMidtonesTint = if (mask.gradingMidtonesTint) preset.params.gradingMidtonesTint else currentParams.gradingMidtonesTint,
            gradingShadowsTemp = if (mask.gradingShadowsTemp) preset.params.gradingShadowsTemp else currentParams.gradingShadowsTemp,
            gradingShadowsTint = if (mask.gradingShadowsTint) preset.params.gradingShadowsTint else currentParams.gradingShadowsTint,
            gradingBlending = if (mask.gradingBlending) preset.params.gradingBlending else currentParams.gradingBlending,
            gradingBalance = if (mask.gradingBalance) preset.params.gradingBalance else currentParams.gradingBalance,
            
            // 效果
            texture = if (mask.texture) preset.params.texture else currentParams.texture,
            dehaze = if (mask.dehaze) preset.params.dehaze else currentParams.dehaze,
            vignette = if (mask.vignette) preset.params.vignette else currentParams.vignette,
            grain = if (mask.grain) preset.params.grain else currentParams.grain,
            
            // 细节
            sharpening = if (mask.sharpening) preset.params.sharpening else currentParams.sharpening,
            noiseReduction = if (mask.noiseReduction) preset.params.noiseReduction else currentParams.noiseReduction,
            
            // 曲线
            enableRgbCurve = if (mask.enableRgbCurve) preset.params.enableRgbCurve else currentParams.enableRgbCurve,
            rgbCurvePoints = if (mask.rgbCurvePoints) preset.params.rgbCurvePoints else currentParams.rgbCurvePoints,
            enableRedCurve = if (mask.enableRedCurve) preset.params.enableRedCurve else currentParams.enableRedCurve,
            redCurvePoints = if (mask.redCurvePoints) preset.params.redCurvePoints else currentParams.redCurvePoints,
            enableGreenCurve = if (mask.enableGreenCurve) preset.params.enableGreenCurve else currentParams.enableGreenCurve,
            greenCurvePoints = if (mask.greenCurvePoints) preset.params.greenCurvePoints else currentParams.greenCurvePoints,
            enableBlueCurve = if (mask.enableBlueCurve) preset.params.enableBlueCurve else currentParams.enableBlueCurve,
            blueCurvePoints = if (mask.blueCurvePoints) preset.params.blueCurvePoints else currentParams.blueCurvePoints,
            
            // HSL
            enableHSL = if (mask.enableHSL) preset.params.enableHSL else currentParams.enableHSL,
            hslHueShift = if (mask.hslHueShift) preset.params.hslHueShift else currentParams.hslHueShift,
            hslSaturation = if (mask.hslSaturation) preset.params.hslSaturation else currentParams.hslSaturation,
            hslLuminance = if (mask.hslLuminance) preset.params.hslLuminance else currentParams.hslLuminance,
            
            // 几何
            rotation = if (mask.rotation) preset.params.rotation else currentParams.rotation,
            cropEnabled = if (mask.cropEnabled) preset.params.cropEnabled else currentParams.cropEnabled,
            cropLeft = if (mask.cropLeft) preset.params.cropLeft else currentParams.cropLeft,
            cropTop = if (mask.cropTop) preset.params.cropTop else currentParams.cropTop,
            cropRight = if (mask.cropRight) preset.params.cropRight else currentParams.cropRight,
            cropBottom = if (mask.cropBottom) preset.params.cropBottom else currentParams.cropBottom
        )
    }
    
    /**
     * 加载用户预设
     */
    private fun loadUserPresets(): List<Preset> {
        return try {
            presetsDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.mapNotNull { file ->
                    try {
                        val jsonString = file.readText()
                        json.decodeFromString<Preset>(jsonString)
                    } catch (e: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.modifiedAt }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
