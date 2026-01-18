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
    // 张数由画幅决定，取最大值
    val selectedCount = filmFormat.availableCounts.maxOrNull() ?: 36
    var selectedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAnimationPlaying by remember { mutableStateOf(false) }
    var isAnimationComplete by remember { mutableStateOf(false) }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // 限制图片数量
            val limitedUris = uris.take(selectedCount).map { it.toString() }
            selectedImageUris = limitedUris
        }
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
            
            // 提示信息
            Text(
                text = "最多可选择 $selectedCount 张照片",
                fontSize = 14.sp,
                color = FilmDarkGray
            )
            
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
            
            // 已选择图片提示
            if (selectedImageUris.isNotEmpty()) {
                Text(
                    text = "已选择 ${selectedImageUris.size}/${selectedCount} 张照片",
                    fontSize = 14.sp,
                    color = FilmMintGreen,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 取景器区域（仅在选择照片后显示）
            if (selectedImageUris.isNotEmpty()) {
                ViewfinderAnimation(
                    isPlaying = isAnimationPlaying,
                    onAnimationComplete = {
                        isAnimationComplete = true
                        // 动画完成后自动进入下一步
                        onCountSelected(selectedCount, selectedImageUris)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
                
                // 提示文字
                if (isAnimationPlaying) {
                    Text(
                        text = "取景中...",
                        fontSize = 16.sp,
                        color = FilmDarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 开始拍摄按钮（仅在选择照片后显示）
            if (selectedImageUris.isNotEmpty() && !isAnimationPlaying) {
                Button(
                    onClick = {
                        isAnimationPlaying = true
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
