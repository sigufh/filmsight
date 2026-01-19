package com.filmtracker.app.data

import android.content.Context
import com.filmtracker.app.util.parsePresetFromAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 从 Assets 加载预设
 */
class AssetPresetLoader(private val context: Context) {
    
    /**
     * 加载所有 assets/presets 目录下的预设
     */
    suspend fun loadAllPresets(): List<Preset> = withContext(Dispatchers.IO) {
        val presets = mutableListOf<Preset>()
        
        try {
            val assetManager = context.assets
            val presetFiles = assetManager.list("presets") ?: emptyArray()
            
            presetFiles.filter { it.endsWith(".json") }.forEach { fileName ->
                try {
                    val preset = parsePresetFromAssets(context, "presets/$fileName")
                    preset?.let { lightroomPreset ->
                        // 将 LightroomPreset 转换为 Preset
                        val category = mapCategoryFromTags(lightroomPreset.tags)
                        presets.add(
                            Preset(
                                id = "asset_${fileName.removeSuffix(".json")}",
                                name = lightroomPreset.name,
                                category = category,
                                params = lightroomPreset.parameters
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AssetPresetLoader", "Failed to load preset: $fileName", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AssetPresetLoader", "Failed to list preset files", e)
        }
        
        presets
    }
    
    /**
     * 根据标签映射到预设分类
     */
    private fun mapCategoryFromTags(tags: List<String>): PresetCategory {
        return when {
            tags.any { it.contains("人像", ignoreCase = true) || it.contains("portrait", ignoreCase = true) } -> 
                PresetCategory.PORTRAIT
            tags.any { it.contains("风光", ignoreCase = true) || it.contains("landscape", ignoreCase = true) } -> 
                PresetCategory.LANDSCAPE
            tags.any { it.contains("黑白", ignoreCase = true) || it.contains("bw", ignoreCase = true) } -> 
                PresetCategory.BLACKWHITE
            tags.any { it.contains("复古", ignoreCase = true) || it.contains("vintage", ignoreCase = true) } -> 
                PresetCategory.VINTAGE
            tags.any { it.contains("电影", ignoreCase = true) || it.contains("cinematic", ignoreCase = true) } -> 
                PresetCategory.CINEMATIC
            tags.any { it.contains("胶片", ignoreCase = true) || it.contains("film", ignoreCase = true) } -> 
                PresetCategory.CREATIVE
            tags.any { it.contains("街拍", ignoreCase = true) || it.contains("street", ignoreCase = true) } -> 
                PresetCategory.CREATIVE
            tags.any { it.contains("日系", ignoreCase = true) || it.contains("japanese", ignoreCase = true) } -> 
                PresetCategory.CREATIVE
            tags.any { it.contains("建筑", ignoreCase = true) || it.contains("architecture", ignoreCase = true) } -> 
                PresetCategory.LANDSCAPE
            else -> PresetCategory.CREATIVE
        }
    }
}
