package com.filmtracker.app.ui.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingTopBar(
    onBack: () -> Unit,
    onShowImageInfo: () -> Unit,
    onExport: () -> Unit,
    onCopyParams: () -> Unit,
    onPasteParams: () -> Unit,
    onResetParams: () -> Unit,
    onCreatePreset: () -> Unit,
    onManagePresets: () -> Unit,
    canPaste: Boolean,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    isModified: Boolean = false
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    val containerColor = MaterialTheme.colorScheme.surface
    val contentColor = contentColorFor(containerColor)
    val disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    TopAppBar(
        title = {
            if (isModified) {
                Text(
                    text = "●",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = contentColor
                )
            }
        },
        actions = {
            IconButton(
                onClick = onUndo,
                enabled = canUndo
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "撤销",
                    tint = if (canUndo) contentColor else disabledContentColor
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "重做",
                    tint = if (canRedo) contentColor else disabledContentColor
                )
            }

            IconButton(onClick = onResetParams) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "复位",
                    tint = contentColor
                )
            }
            IconButton(onClick = onShowImageInfo) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "图像信息",
                    tint = contentColor
                )
            }
            Box {
                IconButton(onClick = { showDropdownMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = contentColor
                    )
                }
                DropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = { showDropdownMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("导出") },
                        onClick = {
                            showDropdownMenu = false
                            onExport()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("复制参数") },
                        onClick = {
                            showDropdownMenu = false
                            onCopyParams()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("粘贴参数") },
                        onClick = {
                            showDropdownMenu = false
                            onPasteParams()
                        },
                        enabled = canPaste,
                        leadingIcon = {
                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("创建预设") },
                        onClick = {
                            showDropdownMenu = false
                            onCreatePreset()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("管理预设") },
                        onClick = {
                            showDropdownMenu = false
                            onManagePresets()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}
