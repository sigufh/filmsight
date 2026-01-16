package com.filmtracker.app.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.data.Preset
import com.filmtracker.app.data.PresetCategory

/**
 * 创建预设对话框
 */
@Composable
fun CreatePresetDialog(
    currentParams: BasicAdjustmentParams,
    onDismiss: () -> Unit,
    onConfirm: (String, PresetCategory) -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PresetCategory.USER) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建预设") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 预设名称
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("预设名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 分类选择
                Text(
                    text = "分类",
                    style = MaterialTheme.typography.labelMedium
                )
                
                val categories = listOf(
                    PresetCategory.USER to "用户",
                    PresetCategory.PORTRAIT to "人像",
                    PresetCategory.LANDSCAPE to "风景",
                    PresetCategory.BLACKWHITE to "黑白",
                    PresetCategory.VINTAGE to "复古",
                    PresetCategory.CINEMATIC to "电影"
                )
                
                categories.forEach { (category, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (presetName.isNotBlank()) {
                        onConfirm(presetName.trim(), selectedCategory)
                    }
                },
                enabled = presetName.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 预设管理对话框
 */
@Composable
fun PresetManagementDialog(
    presets: List<Preset>,
    onDismiss: () -> Unit,
    onApplyPreset: (Preset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onRenamePreset: (String, String) -> Unit
) {
    var presetToRename by remember { mutableStateOf<Preset?>(null) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("预设管理") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (presets.isEmpty()) {
                    Text(
                        text = "暂无预设",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    presets.forEach { preset ->
                        PresetItem(
                            preset = preset,
                            onApply = { onApplyPreset(preset) },
                            onRename = { presetToRename = preset },
                            onDelete = { presetToDelete = preset }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
    
    // 重命名对话框
    presetToRename?.let { preset ->
        RenamePresetDialog(
            currentName = preset.name,
            onDismiss = { presetToRename = null },
            onConfirm = { newName ->
                onRenamePreset(preset.id, newName)
                presetToRename = null
            }
        )
    }
    
    // 删除确认对话框
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text("删除预设") },
            text = { Text("确定要删除预设「${preset.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePreset(preset.id)
                        presetToDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PresetItem(
    preset: Preset,
    onApply: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = preset.category.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onApply) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "应用"
                    )
                }
                
                if (!preset.id.startsWith("builtin_")) {
                    IconButton(onClick = onRename) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "重命名"
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除"
                        )
                    }
                }
            }
        }
    }
}

/**
 * 重命名预设对话框
 */
@Composable
private fun RenamePresetDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名预设") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("新名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName.trim()) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
