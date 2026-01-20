package com.filmtracker.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmtracker.app.domain.model.FilmFormat
import com.filmtracker.app.domain.model.FilmStock
import com.filmtracker.app.ui.screens.components.AIDialogPanel
import com.filmtracker.app.ui.screens.components.ViewfinderAnimation
import com.filmtracker.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 照片选择页（胶卷仿拍流程第二步）
 * 
 * 功能：
 * - 选择图片（从相册）- 张数由画幅决定
 * - 播放取景动画
 * 
 * 流程：
 * 1. 选择照片（从相册）
 * 2. 播放取景动画
 * 3. 进入预览页
 * 
 * 注意：AI 助手不在此页面显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmCountSelectionScreen(
    filmFormat: FilmFormat,
    filmStock: FilmStock?,
    onBack: () -> Unit,
    onCountSelected: (Int, List<String>) -> Unit,  // 张数 + 图片URI列表
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 张数由画幅决定，取最大值
    val selectedCount = filmFormat.availableCounts.maxOrNull() ?: 36
    var selectedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAnimationPlaying by remember { mutableStateOf(false) }
    var showLimitWarning by remember { mutableStateOf(false) }
    var originalSelectionCount by remember { mutableStateOf(0) }
    
    // 加载状态
    var isLoadingImages by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var loadedImagesCount by remember { mutableStateOf(0) }
    var isReadyToNavigate by remember { mutableStateOf(false) }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            originalSelectionCount = uris.size
            
            // 限制图片数量
            val limitedUris = uris.take(selectedCount).map { it.toString() }
            selectedImageUris = limitedUris
            
            // 如果用户选择的图片超过限制，显示警告
            if (uris.size > selectedCount) {
                showLimitWarning = true
            }
        }
    }
    
    // 预加载图片的函数（在后台线程执行）
    fun preloadImages() {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                isLoadingImages = true
                loadedImagesCount = 0
                loadingProgress = 0f
            }
            
            try {
                // 在IO线程加载每张图片
                selectedImageUris.forEachIndexed { index, uriString ->
                    val uri = android.net.Uri.parse(uriString)
                    
                    // 验证图片可以打开（在IO线程）
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            // 验证图片可以打开
                            val options = android.graphics.BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            android.graphics.BitmapFactory.decodeStream(stream, null, options)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FilmCountSelection", "Failed to validate image: $uriString", e)
                    }
                    
                    // 更新UI（切换到主线程）
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        loadedImagesCount = index + 1
                        loadingProgress = (index + 1).toFloat() / selectedImageUris.size
                    }
                }
                
                // 所有图片加载完成（切换到主线程）
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoadingImages = false
                    isReadyToNavigate = true
                }
                
            } catch (e: Exception) {
                android.util.Log.e("FilmCountSelection", "Failed to preload images", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoadingImages = false
                    // 即使失败也允许导航
                    isReadyToNavigate = true
                }
            }
        }
    }
    
    // 监听加载完成，触发导航
    LaunchedEffect(isReadyToNavigate) {
        if (isReadyToNavigate) {
            // 确保动画至少播放了最小时长
            kotlinx.coroutines.delay(300)
            onCountSelected(selectedCount, selectedImageUris)
        }
    }
    
    // 限制警告对话框
    if (showLimitWarning) {
        AlertDialog(
            onDismissRequest = { showLimitWarning = false },
            title = {
                Text(
                    text = "⚠️ 照片数量限制",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "您选择了 $originalSelectionCount 张照片，但 ${filmFormat.displayName} 最多只能拍摄 $selectedCount 张。",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "已自动保留前 $selectedCount 张照片。",
                        fontSize = 14.sp,
                        color = FilmMintGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLimitWarning = false }
                ) {
                    Text("知道了", color = FilmCaramelOrange)
                }
            },
            containerColor = FilmWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = filmFormat.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        filmStock?.let {
                            Text(
                                text = it.displayName,
                                fontSize = 12.sp,
                                color = FilmDarkGray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = FilmInkBlack
                        )
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = "选择照片",
                style = MaterialTheme.typography.headlineMedium,
                color = FilmInkBlack,
                fontWeight = FontWeight.Light
            )
            
            // 提示信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = FilmMilkyBlue.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ℹ️",
                        fontSize = 24.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "照片数量限制",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FilmInkBlack
                        )
                        Text(
                            text = "${filmFormat.displayName} 最多可选择 $selectedCount 张照片",
                            fontSize = 13.sp,
                            color = FilmDarkGray
                        )
                        if (selectedImageUris.isNotEmpty()) {
                            Text(
                                text = "已选择：${selectedImageUris.size}/$selectedCount 张",
                                fontSize = 12.sp,
                                color = FilmMintGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 选择照片按钮
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FilmCaramelOrange,
                    contentColor = FilmWhite
                )
            ) {
                Text(
                    text = if (selectedImageUris.isEmpty()) {
                        "📷 选择照片"
                    } else {
                        "📷 重新选择照片"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 取景器区域（仅在选择照片后显示）
            if (selectedImageUris.isNotEmpty() && !isReadyToNavigate) {
                ViewfinderAnimation(
                    isPlaying = isAnimationPlaying,
                    onAnimationComplete = {
                        // 动画完成回调 - 但如果图片还在加载，动画会继续循环
                        // 实际导航由 isReadyToNavigate 控制
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
                
                // 提示文字 - 根据状态显示不同信息
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoadingImages) {
                        Text(
                            text = "正在加载照片...",
                            fontSize = 16.sp,
                            color = FilmCaramelOrange,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$loadedImagesCount / ${selectedImageUris.size}",
                            fontSize = 14.sp,
                            color = FilmDarkGray
                        )
                        // 进度条
                        LinearProgressIndicator(
                            progress = loadingProgress,
                            modifier = Modifier
                                .width(200.dp)
                                .padding(top = 8.dp),
                            color = FilmCaramelOrange,
                            trackColor = FilmLightGray
                        )
                    } else if (isAnimationPlaying) {
                        Text(
                            text = "取景中...",
                            fontSize = 16.sp,
                            color = FilmDarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 开始拍摄按钮（仅在选择照片后显示）
            if (selectedImageUris.isNotEmpty() && !isAnimationPlaying && !isLoadingImages) {
                Button(
                    onClick = {
                        // 同时启动动画和图片加载
                        isAnimationPlaying = true
                        preloadImages()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FilmMintGreen,
                        contentColor = FilmWhite
                    )
                ) {
                    Text(
                        text = "🎬 开始拍摄",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 张数选项按钮
 */
@Composable
private fun CountOption(
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) FilmCaramelOrange else FilmWhite
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) FilmWhite else FilmInkBlack
        )
    }
}
