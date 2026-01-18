package com.filmtracker.app.domain.model

import android.net.Uri
import kotlinx.serialization.Serializable

/**
 * Represents the current editing session for an image in the non-destructive editing system.
 * 
 * This class encapsulates all the state needed for editing an image, including:
 * - Current adjustment parameters
 * - Complete edit history for undo/redo operations
 * - Modification tracking for auto-save functionality
 * - Image identification and metadata
 * 
 * The EditSession follows immutable patterns - all operations return new instances
 * rather than modifying the existing session. This ensures thread safety and
 * enables reliable undo/redo functionality.
 * 
 * Key features:
 * - Non-destructive: Original image files are never modified
 * - Complete history: Unlimited undo/redo with parameter snapshots
 * - Auto-save: Modification tracking triggers automatic metadata persistence
 * - Session persistence: Can be serialized/deserialized for app restart recovery
 * 
 * @param imageUri Android MediaStore URI for the image being edited
 * @param imagePath Absolute file system path to the original image file
 * @param currentParams Current adjustment parameters applied to the image
 * @param history Complete edit history with undo/redo stacks
 * @param lastModified Timestamp of the last modification to this session
 * @param isModified Flag indicating if the session has unsaved changes
 */
data class EditSession(
    val imageUri: Uri,
    val imagePath: String,
    val currentParams: AdjustmentParams,
    val history: EditHistory,
    val lastModified: Long,
    val isModified: Boolean
) {
    /**
     * Applies a parameter change to the current session.
     * 
     * This method:
     * 1. Applies the parameter change to create new parameters
     * 2. Records the current state in the edit history before applying the change
     * 3. Updates the modification timestamp and sets the modified flag
     * 
     * The operation is immutable - returns a new EditSession instance.
     * 
     * @param change The parameter change to apply
     * @return New EditSession with the change applied and recorded in history
     */
    fun applyParameterChange(change: ParameterChange): EditSession {
        val newParams = change.apply(currentParams)
        val newHistory = history.recordChange(currentParams, change.description)
        
        return copy(
            currentParams = newParams,
            history = newHistory,
            lastModified = System.currentTimeMillis(),
            isModified = true
        )
    }
    
    /**
     * 撤销操作
     * 返回撤销后的会话,如果无法撤销则返回 null
     */
    fun undo(): EditSession? {
        val (previousParams, newHistory) = history.undoWithCurrent(currentParams)
        
        return if (previousParams != null) {
            copy(
                currentParams = previousParams,
                history = newHistory,
                lastModified = System.currentTimeMillis(),
                isModified = true
            )
        } else {
            null
        }
    }
    
    /**
     * 重做操作
     * 返回重做后的会话,如果无法重做则返回 null
     */
    fun redo(): EditSession? {
        val (nextParams, newHistory) = history.redoWithCurrent(currentParams)
        
        return if (nextParams != null) {
            copy(
                currentParams = nextParams,
                history = newHistory,
                lastModified = System.currentTimeMillis(),
                isModified = true
            )
        } else {
            null
        }
    }
    
    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = history.canUndo()
    
    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = history.canRedo()
    
    /**
     * 重置为默认参数
     */
    fun resetToDefaults(): EditSession {
        val defaultParams = AdjustmentParams.default()
        val newHistory = history.recordChange(currentParams, "重置为默认")
        
        return copy(
            currentParams = defaultParams,
            history = newHistory,
            lastModified = System.currentTimeMillis(),
            isModified = true
        )
    }
    
    /**
     * 应用预设
     */
    fun applyPreset(preset: com.filmtracker.app.data.Preset): EditSession {
        // 将 BasicAdjustmentParams 转换为 AdjustmentParams
        // 这里需要一个映射函数
        val newParams = mergePresetParams(currentParams, preset.params)
        val newHistory = history.recordChange(currentParams, "应用预设: ${preset.name}")
        
        return copy(
            currentParams = newParams,
            history = newHistory,
            lastModified = System.currentTimeMillis(),
            isModified = true
        )
    }
    
    /**
     * 合并预设参数
     * 只更新预设中包含的参数
     */
    private fun mergePresetParams(
        current: AdjustmentParams,
        preset: com.filmtracker.app.data.BasicAdjustmentParams
    ): AdjustmentParams {
        // 简单实现:直接应用预设中的所有参数
        // 未来可以支持部分预设
        return current.copy(
            exposure = preset.globalExposure,
            contrast = preset.contrast,
            saturation = preset.saturation,
            highlights = preset.highlights,
            shadows = preset.shadows,
            whites = preset.whites,
            blacks = preset.blacks,
            clarity = preset.clarity,
            vibrance = preset.vibrance,
            temperature = preset.temperature,
            tint = preset.tint,
            gradingHighlightsTemp = preset.gradingHighlightsTemp,
            gradingHighlightsTint = preset.gradingHighlightsTint,
            gradingMidtonesTemp = preset.gradingMidtonesTemp,
            gradingMidtonesTint = preset.gradingMidtonesTint,
            gradingShadowsTemp = preset.gradingShadowsTemp,
            gradingShadowsTint = preset.gradingShadowsTint,
            gradingBlending = preset.gradingBlending,
            gradingBalance = preset.gradingBalance,
            texture = preset.texture,
            dehaze = preset.dehaze,
            vignette = preset.vignette,
            grain = preset.grain,
            sharpening = preset.sharpening,
            noiseReduction = preset.noiseReduction
        )
    }
    
    companion object {
        /**
         * 创建新的编辑会话
         */
        fun create(imageUri: Uri, imagePath: String): EditSession {
            return EditSession(
                imageUri = imageUri,
                imagePath = imagePath,
                currentParams = AdjustmentParams.default(),
                history = EditHistory(),
                lastModified = System.currentTimeMillis(),
                isModified = false
            )
        }
    }
}

/**
 * 可序列化的编辑会话
 * 用于会话持久化
 */
@Serializable
data class SerializableEditSession(
    val imageUri: String,
    val imagePath: String,
    val currentParams: SerializableAdjustmentParams,
    val history: SerializableEditHistory,
    val lastModified: Long,
    val isModified: Boolean
) {
    /**
     * 转换为领域模型
     */
    fun toEditSession(): EditSession {
        return EditSession(
            imageUri = Uri.parse(imageUri),
            imagePath = imagePath,
            currentParams = currentParams.toAdjustmentParams(),
            history = history.toEditHistory(),
            lastModified = lastModified,
            isModified = isModified
        )
    }
    
    companion object {
        /**
         * 从领域模型创建
         */
        fun fromEditSession(session: EditSession): SerializableEditSession {
            return SerializableEditSession(
                imageUri = session.imageUri.toString(),
                imagePath = session.imagePath,
                currentParams = SerializableAdjustmentParams.fromAdjustmentParams(session.currentParams),
                history = SerializableEditHistory.fromEditHistory(session.history),
                lastModified = session.lastModified,
                isModified = session.isModified
            )
        }
    }
}
