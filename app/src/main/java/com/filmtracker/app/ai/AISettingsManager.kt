package com.filmtracker.app.ai

import android.content.Context
import android.content.SharedPreferences

/**
 * AI 设置管理器 - 持久化保存配置
 */
class AISettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_COLOR_STYLE = "color_style"
        private const val KEY_COLOR_TENDENCY = "color_tendency"
        private const val KEY_CONTRAST_PREF = "contrast_pref"
        private const val KEY_SATURATION_PREF = "saturation_pref"
        private const val KEY_CUSTOM_RULES = "custom_rules"
    }
    
    /**
     * 保存 API 配置
     */
    fun saveAPIConfig(provider: AIProvider, apiKey: String, model: String) {
        prefs.edit().apply {
            putString(KEY_PROVIDER, provider.name)
            putString(KEY_API_KEY, apiKey)
            putString(KEY_MODEL, model)
            apply()
        }
    }
    
    /**
     * 获取 API 配置
     */
    fun getAPIConfig(): AIConfig? {
        val providerName = prefs.getString(KEY_PROVIDER, null) ?: return null
        val apiKey = prefs.getString(KEY_API_KEY, null) ?: return null
        val model = prefs.getString(KEY_MODEL, null) ?: return null
        
        val provider = try {
            AIProvider.valueOf(providerName)
        } catch (e: Exception) {
            return null
        }
        
        return AIConfig(provider, apiKey, model)
    }
    
    /**
     * 保存用户偏好
     */
    fun saveUserPreferences(preferences: UserPreferences) {
        prefs.edit().apply {
            putString(KEY_COLOR_STYLE, preferences.colorStyle.name)
            putString(KEY_COLOR_TENDENCY, preferences.colorTendency)
            putString(KEY_CONTRAST_PREF, preferences.contrastPreference)
            putString(KEY_SATURATION_PREF, preferences.saturationPreference)
            putString(KEY_CUSTOM_RULES, preferences.customRules)
            apply()
        }
    }
    
    /**
     * 获取用户偏好
     */
    fun getUserPreferences(): UserPreferences {
        val colorStyleName = prefs.getString(KEY_COLOR_STYLE, ColorStyle.NATURAL.name)
        val colorStyle = try {
            ColorStyle.valueOf(colorStyleName ?: ColorStyle.NATURAL.name)
        } catch (e: Exception) {
            ColorStyle.NATURAL
        }
        
        return UserPreferences(
            colorStyle = colorStyle,
            colorTendency = prefs.getString(KEY_COLOR_TENDENCY, "自然真实") ?: "自然真实",
            contrastPreference = prefs.getString(KEY_CONTRAST_PREF, "适中") ?: "适中",
            saturationPreference = prefs.getString(KEY_SATURATION_PREF, "适中") ?: "适中",
            customRules = prefs.getString(KEY_CUSTOM_RULES, "") ?: ""
        )
    }
    
    /**
     * 清除所有设置
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
