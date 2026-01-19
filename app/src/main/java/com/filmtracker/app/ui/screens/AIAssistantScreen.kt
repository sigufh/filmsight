package com.filmtracker.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmtracker.app.ai.ChatMessage
import com.filmtracker.app.ai.ColorGradingSuggestion
import com.filmtracker.app.ui.theme.*
import com.filmtracker.app.ui.components.MarkdownText
import kotlinx.coroutines.launch

/**
 * AI助手全屏对话界面
 * 设计风格：Ins风格轻复古，焦糖橘主色调，胶片压纹质感
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    viewModel: com.filmtracker.app.ui.viewmodel.AIAssistantViewModel,
    onBack: () -> Unit,
    onApplySuggestion: (ColorGradingSuggestion) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentSuggestion by viewModel.currentSuggestion.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // 加载图片
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                selectedImage = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch (e: Exception) {
                android.util.Log.e("AIAssistantScreen", "Failed to load image", e)
            }
        }
    }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("AI调色助手", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = FilmInkBlack)
                            Text("专业摄影后期顾问", fontSize = 12.sp, color = FilmDarkGray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = FilmInkBlack)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "设置", tint = FilmInkBlack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FilmWhiteGlass,
                    titleContentColor = FilmInkBlack
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                selectedImage = selectedImage,
                onImageSelect = { imagePickerLauncher.launch("image/*") },
                onImageRemove = { 
                    selectedImage = null
                    selectedImageUri = null
                },
                onSend = {
                    if ((inputText.isNotBlank() || selectedImage != null) && !isLoading) {
                        viewModel.sendMessage(
                            message = inputText,
                            image = selectedImage,
                            imageUri = selectedImageUri,
                            context = context
                        )
                        inputText = ""
                        selectedImage = null
                        selectedImageUri = null
                        
                        scope.launch {
                            listState.animateScrollToItem(messages.size)
                        }
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = FilmWarmBeige
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 背景胶片齿孔纹理
            FilmSprocketBackground()
            if (messages.isEmpty()) {
                WelcomeScreen()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(
                            message = message,
                            onApplySuggestion = { suggestion ->
                                onApplySuggestion(suggestion)
                            }
                        )
                    }
                    
                    if (isLoading) {
                        item {
                            LoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 欢迎界面 - Ins风格轻复古
 */
@Composable
private fun WelcomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✨", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "AI调色助手",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = FilmInkBlack
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "上传照片，获取专业调色建议",
            fontSize = 14.sp,
            color = FilmDarkGray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 配置提示卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = FilmCaramelOrange.copy(alpha = 0.15f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "💡 首次使用提示",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FilmInkBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "请先点击右上角设置按钮，配置 AI API 密钥和模型信息",
                    fontSize = 14.sp,
                    color = FilmDarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        QuickActionGrid()
    }
}

/**
 * 快捷操作网格
 */
@Composable
private fun QuickActionGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                icon = "🎨",
                title = "分析照片",
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = "🎞",
                title = "胶片风格",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                icon = "👤",
                title = "人像调色",
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = "🌄",
                title = "风光调色",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 快捷操作卡片 - 焦糖橘主色调
 */
@Composable
private fun QuickActionCard(
    icon: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FilmCaramelOrange.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Medium,
                color = FilmInkBlack
            )
        }
    }
}

/**
 * 聊天气泡 - 胶片质感 + Markdown 支持 + 图片显示 + 应用参数按钮
 */
@Composable
private fun ChatBubble(
    message: ChatMessage,
    onApplySuggestion: ((ColorGradingSuggestion) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = FilmCaramelOrange.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✨", fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(modifier = Modifier.widthIn(max = 280.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (message.isUser) 
                    FilmMilkyBlue.copy(alpha = 0.3f) 
                else 
                    FilmWhiteGlass,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 显示图片（如果有）
                    message.imageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "消息图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (message.content.isNotBlank() && message.content != "[图片]") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // 显示文字内容
                    if (message.content.isNotBlank() && message.content != "[图片]") {
                        if (message.isUser) {
                            // 用户消息使用普通文本
                            Text(
                                text = message.content,
                                fontSize = 14.sp,
                                color = FilmInkBlack
                            )
                        } else {
                            // AI 消息使用 Markdown 渲染
                            if (message.content.isNotEmpty()) {
                                MarkdownText(
                                    markdown = message.content,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        color = FilmInkBlack
                                    )
                                )
                            } else {
                                // 空消息显示光标
                                Text(
                                    text = "▋",
                                    fontSize = 14.sp,
                                    color = FilmCaramelOrange
                                )
                            }
                        }
                    }
                }
            }
            
            // AI 消息底部显示"应用参数"按钮
            if (!message.isUser && message.suggestion != null && onApplySuggestion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onApplySuggestion(message.suggestion) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FilmCaramelOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = FilmWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("应用到调色界面", color = FilmWhite, fontSize = 13.sp)
                }
            }
        }
        
        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = FilmMilkyBlue.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = FilmInkBlack)
                }
            }
        }
    }
}

/**
 * 输入栏 - 磨砂玻璃效果
 */
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    selectedImage: Bitmap?,
    onImageSelect: () -> Unit,
    onImageRemove: () -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = FilmWhiteGlass,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 图片预览
            selectedImage?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "选中的图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        IconButton(
                            onClick = onImageRemove,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "移除图片",
                                    tint = Color.White,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图片选择按钮
                IconButton(onClick = onImageSelect) {
                    Icon(
                        Icons.Default.Add,
                        "添加图片",
                        tint = FilmCaramelOrange
                    )
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("描述你的照片或提出问题...", color = FilmDarkGray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FilmCaramelOrange,
                        unfocusedBorderColor = FilmLightGray,
                        focusedTextColor = FilmInkBlack,
                        unfocusedTextColor = FilmInkBlack
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = onSend,
                    containerColor = FilmCaramelOrange,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = FilmWhite
                        )
                    } else {
                        Icon(Icons.Default.Send, "发送", tint = FilmWhite)
                    }
                }
            }
        }
    }
}

/**
 * 加载指示器 - 胶片质感
 */
@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = FilmCaramelOrange.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("✨", fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FilmWhiteGlass,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FilmCaramelOrange.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

/**
 * 胶片齿孔背景纹理
 */
@Composable
private fun FilmSprocketBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FilmSprocketGray)
    )
}
