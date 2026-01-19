package com.filmtracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.filmtracker.app.ai.AISettingsManager

/**
 * AI助手ViewModel工厂
 */
class AIAssistantViewModelFactory(
    private val settingsManager: AISettingsManager
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIAssistantViewModel::class.java)) {
            return AIAssistantViewModel(settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
