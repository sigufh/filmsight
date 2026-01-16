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
