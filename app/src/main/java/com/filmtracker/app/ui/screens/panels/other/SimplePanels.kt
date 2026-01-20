@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.filmtracker.app.ui.screens.panels.other

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.data.BuiltInPresets
import com.filmtracker.app.data.Preset
import com.filmtracker.app.data.PresetCategory
import com.filmtracker.app.ui.screens.components.AIDialogPanel

@Composable
fun CreativeFilterPanel(
    currentParams: BasicAdjustmentParams,
    onApplyPreset: (BasicAdjustmentParams) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedCategory by remember { mutableStateOf(PresetCategory.CREATIVE) }
    var allPresets by remember { mutableStateOf<List<Preset>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 加载预设（内置 + Assets）
    LaunchedEffect(Unit) {
        isLoading = true
        val builtInPresets = BuiltInPresets.getAll()
        val assetPresets = try {
            com.filmtracker.app.data.AssetPresetLoader(context).loadAllPresets()
        } catch (e: Exception) {
            android.util.Log.e("CreativeFilterPanel", "Failed to load asset presets", e)
            emptyList()
        }
        allPresets = builtInPresets + assetPresets
        isLoading = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 分类选择
        CategoryTabs(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 预设网格
        val filteredPresets = remember(selectedCategory, allPresets) {
            if (selectedCategory == PresetCategory.CREATIVE) {
                allPresets
            } else {
                allPresets.filter { it.category == selectedCategory }
            }
        }
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            filteredPresets.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无预设",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPresets) { preset ->
                        PresetCard(
                            preset = preset,
                            onClick = { onApplyPreset(preset.params) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: PresetCategory,
    onCategorySelected: (PresetCategory) -> Unit
) {
    val categories = listOf(
        PresetCategory.CREATIVE to "全部",
        PresetCategory.PORTRAIT to "人像",
        PresetCategory.LANDSCAPE to "风景",
        PresetCategory.BLACKWHITE to "黑白",
        PresetCategory.FILM to "胶片",
        PresetCategory.VINTAGE to "复古",
        PresetCategory.CINEMATIC to "电影"
    )
    
    ScrollableTabRow(
        selectedTabIndex = categories.indexOfFirst { it.first == selectedCategory },
        containerColor = Color.Transparent,
        contentColor = Color.White,
        edgePadding = 0.dp
    ) {
        categories.forEach { (category, label) ->
            Tab(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: Preset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 预设图标（根据分类显示不同图标）
                Icon(
                    imageVector = when (preset.category) {
                        PresetCategory.BLACKWHITE -> Icons.Default.Face
                        PresetCategory.VINTAGE -> Icons.Default.Star
                        PresetCategory.CINEMATIC -> Icons.Default.Create
                        PresetCategory.PORTRAIT -> Icons.Default.Face
                        PresetCategory.LANDSCAPE -> Icons.Default.Star
                        else -> Icons.Default.Settings
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AIAssistPanel() {
    // 使用 AI 对话框组件
    AIDialogPanel(
        modifier = Modifier.fillMaxSize(),
        showQuickActions = true,
        onGenerateImage = {
            // TODO: AI 生图功能
        },
        onSmartColorGrade = {
            // TODO: 智能调色功能
        },
        onSendMessage = { message ->
            // TODO: 处理用户消息
        }
    )
}

@Composable
fun CropRotatePanel(
    previewBitmap: android.graphics.Bitmap?,
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    val aspectOptions = listOf(
        "自由" to null,
        "1:1" to 1f / 1f,
        "3:2" to 3f / 2f,
        "4:3" to 4f / 3f,
        "16:9" to 16f / 9f
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 旋转
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "旋转", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(text = formatAngle(params.rotation), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = params.rotation.coerceIn(-180f, 180f),
            onValueChange = { v ->
                val snapped = snapRotation(normalizeRotation(v))
                onParamsChange(params.copy(rotation = snapped))
            },
            valueRange = -180f..180f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onParamsChange(params.copy(rotation = normalizeRotation(params.rotation - 90f))) }) {
                Text(text = "-90°")
            }
            Button(onClick = { onParamsChange(params.copy(rotation = 0f)) }) {
                Text(text = "重置旋转")
            }
            Button(onClick = { onParamsChange(params.copy(rotation = normalizeRotation(params.rotation + 90f))) }) {
                Text(text = "+90°")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 裁剪
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "裁剪", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            FilterChip(
                selected = params.cropEnabled,
                onClick = { onParamsChange(params.copy(cropEnabled = !params.cropEnabled)) },
                label = { Text(text = if (params.cropEnabled) "开启" else "关闭") }
            )
            TextButton(onClick = {
                // 重置裁剪
                onParamsChange(
                    params.copy(
                        cropEnabled = false,
                        cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f
                    )
                )
            }) { Text("重置裁剪") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 常用比例
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(aspectOptions) { (label, ratio) ->
                ElevatedButton(onClick = {
                    if (ratio == null) {
                        onParamsChange(
                            params.copy(
                                cropEnabled = true,
                                cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f
                            )
                        )
                    } else {
                        val updated = computeCenteredCrop(previewBitmap, ratio, params)
                        onParamsChange(updated)
                    }
                }) {
                    Text(label)
                }
            }
        }
    }
}

private fun normalizeRotation(deg: Float): Float {
    var r = deg % 360f
    if (r > 180f) r -= 360f
    if (r < -180f) r += 360f
    return r
}

private fun snapRotation(deg: Float, threshold: Float = 2f): Float {
    val targets = floatArrayOf(-90f, -45f, 0f, 45f, 90f)
    val d = targets.minByOrNull { t -> kotlin.math.abs(deg - t) } ?: 0f
    return if (kotlin.math.abs(deg - d) <= threshold) d else deg
}

private fun formatAngle(deg: Float): String {
    return String.format("%.1f°", deg)
}

private fun computeCenteredCrop(
    previewBitmap: android.graphics.Bitmap?,
    targetRatio: Float,
    params: BasicAdjustmentParams
): BasicAdjustmentParams {
    val w = previewBitmap?.width ?: return params
    val h = previewBitmap.height
    if (w <= 0 || h <= 0) return params
    val imageRatio = w.toFloat() / h.toFloat()
    return if (imageRatio > targetRatio) {
        val widthNorm = targetRatio / imageRatio
        val left = (1f - widthNorm) / 2f
        params.copy(
            cropEnabled = true,
            cropLeft = left,
            cropTop = 0f,
            cropRight = 1f - left,
            cropBottom = 1f
        )
    } else {
        val heightNorm = imageRatio / targetRatio
        val top = (1f - heightNorm) / 2f
        params.copy(
            cropEnabled = true,
            cropLeft = 0f,
            cropTop = top,
            cropRight = 1f,
            cropBottom = 1f - top
        )
    }
}

@Composable
fun MaskPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "蒙版功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun HealPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "修补消除功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
