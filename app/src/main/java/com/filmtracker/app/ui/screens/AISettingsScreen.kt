package com.filmtracker.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.filmtracker.app.ai.AIProvider
import com.filmtracker.app.ai.ColorStyle
import com.filmtracker.app.ui.theme.CornerRadius
import com.filmtracker.app.ui.theme.Spacing

/**
 * AI Assistant Settings Screen - Material Design 3 compliant
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    viewModel: com.filmtracker.app.ui.viewmodel.AIAssistantViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPreferences by viewModel.userPreferences.collectAsState()
    val savedConfig by viewModel.apiConfig.collectAsState()

    var selectedProvider by remember { mutableStateOf(savedConfig?.provider ?: AIProvider.OPENAI) }
    var apiKey by remember { mutableStateOf(savedConfig?.apiKey ?: "") }
    var modelName by remember { mutableStateOf(savedConfig?.model ?: "gpt-4o") }
    var selectedStyle by remember { mutableStateOf(currentPreferences.colorStyle) }
    var colorTendency by remember { mutableStateOf(currentPreferences.colorTendency) }
    var contrastPref by remember { mutableStateOf(currentPreferences.contrastPreference) }
    var saturationPref by remember { mutableStateOf(currentPreferences.saturationPreference) }
    var customRules by remember { mutableStateOf(currentPreferences.customRules) }

    // Update UI when saved config changes
    LaunchedEffect(savedConfig) {
        savedConfig?.let { config ->
            selectedProvider = config.provider
            apiKey = config.apiKey
            modelName = config.model
        }
    }

    // Update default model when provider changes (only when model is empty or default)
    LaunchedEffect(selectedProvider) {
        if (modelName.isEmpty() || modelName in listOf("gpt-4o", "claude-3-5-sonnet-20241022", "qwen-plus", "qwen-turbo", "qwen-max", "qwen3-vl-30b-a3b-instruct", "glm-4v-flash", "glm-4.6v-flash")) {
            modelName = when (selectedProvider) {
                AIProvider.OPENAI -> "gpt-4o"
                AIProvider.CLAUDE -> "claude-3-5-sonnet-20241022"
                AIProvider.QWEN -> "qwen3-vl-30b-a3b-instruct"
                AIProvider.GLM -> "glm-4.6v-flash"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI助手设置",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // API Configuration
            SettingsSection(title = "API配置") {
                // Model Provider
                Text(
                    text = "模型提供商",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AIProvider.entries.forEach { provider ->
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            label = {
                                Text(
                                    text = provider.name,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = {
                        Text(
                            text = "API Key",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                // Model Name
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = {
                        Text(
                            text = "模型名称",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = when (selectedProvider) {
                                AIProvider.OPENAI -> "例如: gpt-4o"
                                AIProvider.CLAUDE -> "例如: claude-3-5-sonnet-20241022"
                                AIProvider.QWEN -> "例如: qwen3-vl-30b-a3b-instruct"
                                AIProvider.GLM -> "例如: glm-4.6v-flash"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            // Color Preferences
            SettingsSection(title = "调色偏好") {
                // Color Style
                Text(
                    text = "调色风格",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                ColorStyle.entries.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        row.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = {
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        // Fill empty space
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Color Tendency
                OutlinedTextField(
                    value = colorTendency,
                    onValueChange = { colorTendency = it },
                    label = {
                        Text(
                            text = "色彩倾向",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "例如: 偏暖色调、清新通透",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))

                // Contrast Preference
                Text(
                    text = "对比度偏好",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf("低", "适中", "高").forEach { pref ->
                        FilterChip(
                            selected = contrastPref == pref,
                            onClick = { contrastPref = pref },
                            label = {
                                Text(
                                    text = pref,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))

                // Saturation Preference
                Text(
                    text = "饱和度偏好",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf("低", "适中", "高").forEach { pref ->
                        FilterChip(
                            selected = saturationPref == pref,
                            onClick = { saturationPref = pref },
                            label = {
                                Text(
                                    text = pref,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))

                // Custom Rules
                OutlinedTextField(
                    value = customRules,
                    onValueChange = { customRules = it },
                    label = {
                        Text(
                            text = "自定义规则",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = {
                        Text(
                            text = "输入你的特殊要求，例如：\n- 保持肤色自然\n- 避免过度饱和\n- 偏好胶片质感",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            
            // Save Button
            Button(
                onClick = {
                    // Save API configuration
                    val config = com.filmtracker.app.ai.AIConfig(
                        provider = selectedProvider,
                        apiKey = apiKey,
                        model = modelName
                    )
                    viewModel.initializeAI(config)

                    // Save user preferences
                    val preferences = com.filmtracker.app.ai.UserPreferences(
                        colorStyle = selectedStyle,
                        colorTendency = colorTendency,
                        contrastPreference = contrastPref,
                        saturationPreference = saturationPref,
                        customRules = customRules
                    )
                    viewModel.updatePreferences(preferences)

                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "保存设置",
                    modifier = Modifier.padding(vertical = Spacing.sm),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Settings Section - Material Design 3 Card
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.lg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.xs)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            content()
        }
    }
}
