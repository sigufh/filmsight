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

@Composable
fun CreativeFilterPanel(
    currentParams: BasicAdjustmentParams,
    onApplyPreset: (BasicAdjustmentParams) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(PresetCategory.CREATIVE) }
    
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
        val presets = remember(selectedCategory) {
            if (selectedCategory == PresetCategory.CREATIVE) {
                BuiltInPresets.getAll()
            } else {
                BuiltInPresets.getByCategory(selectedCategory)
            }
        }
        
        if (presets.isEmpty()) {
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
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(presets) { preset ->
                    PresetCard(
                        preset = preset,
                        onClick = { onApplyPreset(preset.params) }
                    )
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
        PresetCategory.BLACKWHITE to "黑白",
        PresetCategory.VINTAGE to "复古",
        PresetCategory.CINEMATIC to "电影",
        PresetCategory.PORTRAIT to "人像",
        PresetCategory.LANDSCAPE to "风景"
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI 协助功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun CropRotatePanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Create,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "裁剪旋转功能开发中",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
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
