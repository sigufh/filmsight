package com.filmtracker.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmtracker.app.ai.AIProvider
import com.filmtracker.app.ai.ColorStyle
import com.filmtracker.app.ui.theme.*

/**
 * AI助手设置界面 - Ins风格轻复古
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
    
    // 当保存的配置改变时更新界面
    LaunchedEffect(savedConfig) {
        savedConfig?.let { config ->
            selectedProvider = config.provider
            apiKey = config.apiKey
            modelName = config.model
        }
    }
    
    // 当提供商改变时更新默认模型（仅当模型为空或为默认值时）
    LaunchedEffect(selectedProvider) {
        if (modelName.isEmpty() || modelName in listOf("gpt-4o", "claude-3-5-sonnet-20241022", "qwen-plus", "qwen-turbo", "qwen-max", "qwen3-vl-30b-a3b-instruct")) {
            modelName = when (selectedProvider) {
                AIProvider.OPENAI -> "gpt-4o"
                AIProvider.CLAUDE -> "claude-3-5-sonnet-20241022"
                AIProvider.QWEN -> "qwen3-vl-30b-a3b-instruct"
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI助手设置", color = FilmInkBlack) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = FilmInkBlack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FilmWhiteGlass,
                    titleContentColor = FilmInkBlack
                )
            )
        },
        containerColor = FilmWarmBeige
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // API配置
            SettingsSection(title = "API配置") {
                // 模型提供商
                Text("模型提供商", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FilmInkBlack)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AIProvider.entries.forEach { provider ->
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            label = { Text(provider.name, color = if (selectedProvider == provider) FilmWhite else FilmInkBlack) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FilmCaramelOrange,
                                selectedLabelColor = FilmWhite
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key", color = FilmDarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FilmInkBlack,
                        unfocusedTextColor = FilmInkBlack,
                        focusedBorderColor = FilmCaramelOrange,
                        unfocusedBorderColor = FilmLightGray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 模型名称
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称", color = FilmDarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { 
                        Text(
                            when (selectedProvider) {
                                AIProvider.OPENAI -> "例如: gpt-4o"
                                AIProvider.CLAUDE -> "例如: claude-3-5-sonnet-20241022"
                                AIProvider.QWEN -> "例如: qwen3-vl-30b-a3b-instruct"
                            },
                            color = FilmDarkGray
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FilmInkBlack,
                        unfocusedTextColor = FilmInkBlack,
                        focusedBorderColor = FilmCaramelOrange,
                        unfocusedBorderColor = FilmLightGray
                    )
                )
            }
            
            // 调色偏好
            SettingsSection(title = "调色偏好") {
                // 调色风格
                Text("调色风格", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FilmInkBlack)
                Spacer(modifier = Modifier.height(8.dp))
                ColorStyle.entries.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(style.displayName, fontSize = 12.sp, color = if (selectedStyle == style) FilmWhite else FilmInkBlack) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FilmCaramelOrange,
                                    selectedLabelColor = FilmWhite
                                )
                            )
                        }
                        // 填充空白
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 色彩倾向
                OutlinedTextField(
                    value = colorTendency,
                    onValueChange = { colorTendency = it },
                    label = { Text("色彩倾向", color = FilmDarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如: 偏暖色调、清新通透", color = FilmDarkGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FilmInkBlack,
                        unfocusedTextColor = FilmInkBlack,
                        focusedBorderColor = FilmCaramelOrange,
                        unfocusedBorderColor = FilmLightGray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 对比度偏好
                Text("对比度偏好", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FilmInkBlack)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("低", "适中", "高").forEach { pref ->
                        FilterChip(
                            selected = contrastPref == pref,
                            onClick = { contrastPref = pref },
                            label = { Text(pref, color = if (contrastPref == pref) FilmWhite else FilmInkBlack) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FilmCaramelOrange,
                                selectedLabelColor = FilmWhite
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 饱和度偏好
                Text("饱和度偏好", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FilmInkBlack)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("低", "适中", "高").forEach { pref ->
                        FilterChip(
                            selected = saturationPref == pref,
                            onClick = { saturationPref = pref },
                            label = { Text(pref, color = if (saturationPref == pref) FilmWhite else FilmInkBlack) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FilmCaramelOrange,
                                selectedLabelColor = FilmWhite
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 自定义规则
                OutlinedTextField(
                    value = customRules,
                    onValueChange = { customRules = it },
                    label = { Text("自定义规则", color = FilmDarkGray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("输入你的特殊要求，例如：\n- 保持肤色自然\n- 避免过度饱和\n- 偏好胶片质感", color = FilmDarkGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FilmInkBlack,
                        unfocusedTextColor = FilmInkBlack,
                        focusedBorderColor = FilmCaramelOrange,
                        unfocusedBorderColor = FilmLightGray
                    )
                )
            }
            
            // 保存按钮
            Button(
                onClick = {
                    // 保存API配置
                    val config = com.filmtracker.app.ai.AIConfig(
                        provider = selectedProvider,
                        apiKey = apiKey,
                        model = modelName
                    )
                    viewModel.initializeAI(config)
                    
                    // 保存用户偏好
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
                colors = ButtonDefaults.buttonColors(containerColor = FilmCaramelOrange)
            ) {
                Text("保存设置", modifier = Modifier.padding(vertical = 8.dp), color = FilmWhite)
            }
        }
    }
}

/**
 * 设置分组 - 胶片质感卡片
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FilmWhiteGlass
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FilmInkBlack
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
