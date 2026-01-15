package com.filmtracker.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filmtracker.app.ui.viewmodel.ImageSelectionViewModel
import com.filmtracker.app.ui.viewmodel.ViewModelFactory

/**
 * 图片信息数据类
 */
data class ImageInfo(
    val uri: String,
    val fileName: String,
    val width: Int,
    val height: Int,
    val isRaw: Boolean,
    val previewBitmap: Bitmap?,
    val filePath: String?
)

/**
 * 图像导入界面（类似 Lightroom 的导入界面）
 * 
 * 注意：这是旧版本，直接接收 recentImages 参数
 * 新版本使用 ViewModel 管理状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageImportScreen(
    recentImages: List<ImageInfo>,
    onSelectImage: () -> Unit,
    onImageSelected: (ImageInfo) -> Unit,
    onDeleteImage: (ImageInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("所有照片") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSelectImage,
                containerColor = Color(0xFF0A84FF)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "选择图片",
                    tint = Color.White
                )
            }
        },
        containerColor = Color.Black
    ) { padding ->
        if (recentImages.isEmpty()) {
            // 空状态
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "还没有导入图片",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSelectImage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0A84FF)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择图片")
                    }
                }
            }
        } else {
            // 图片网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recentImages) { imageInfo ->
                    ImageGridItem(
                        imageInfo = imageInfo,
                        onClick = { onImageSelected(imageInfo) },
                        onDelete = { onDeleteImage(imageInfo) }
                    )
                }
            }
        }
    }
}

/**
 * 图像选择界面（使用 ViewModel）
 * 
 * 这是新版本，使用 ViewModel 管理状态
 * 
 * 使用示例：
 * ```kotlin
 * ImageSelectionScreen(
 *     onSelectImage = { /* 打开图片选择器 */ },
 *     onImageSelected = { imageInfo -> /* 导航到处理界面 */ }
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSelectionScreen(
    onSelectImage: () -> Unit,
    onImageSelected: (ImageInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 获取 ViewModel
    val viewModel: ImageSelectionViewModel = viewModel(
        factory = ViewModelFactory.getInstance(context)
    )
    
    // 观察状态
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("所有照片") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSelectImage,
                containerColor = Color(0xFF0A84FF)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "选择图片",
                    tint = Color.White
                )
            }
        },
        containerColor = Color.Black
    ) { padding ->
        when {
            isLoading -> {
                // 加载状态
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            images.isEmpty() -> {
                // 空状态
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "还没有导入图片",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSelectImage,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0A84FF)
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("选择图片")
                        }
                    }
                }
            }
            else -> {
                // 图片网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(images) { imageInfo ->
                        ImageGridItem(
                            imageInfo = imageInfo,
                            onClick = {
                                viewModel.selectImage(imageInfo)
                                onImageSelected(imageInfo)
                            },
                            onDelete = {
                                viewModel.deleteImage(imageInfo)
                                viewModel.saveImages()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 图片网格项
 */
@Composable
fun ImageGridItem(
    imageInfo: ImageInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        // 显示缩略图
        if (imageInfo.previewBitmap != null) {
            Image(
                bitmap = imageInfo.previewBitmap.asImageBitmap(),
                contentDescription = imageInfo.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 占位符
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = imageInfo.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        // 图片信息叠加层
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = "${imageInfo.width} × ${imageInfo.height}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            if (imageInfo.isRaw) {
                Text(
                    text = "RAW",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0A84FF)
                )
            }
        }
        
        // 删除按钮
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = Color.White
            )
        }
    }
}

