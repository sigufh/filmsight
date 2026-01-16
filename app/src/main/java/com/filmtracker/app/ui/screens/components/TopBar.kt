package com.filmtracker.app.ui.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.domain.model.AdjustmentParams

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
    canPaste: Boolean
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onResetParams) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "复位",
                    tint = Color.White
                )
            }
            IconButton(onClick = onShowImageInfo) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "图像信息",
                    tint = Color.White
                )
            }
            Box {
                IconButton(onClick = { showDropdownMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = Color.White
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
                    Divider()
                    DropdownMenuItem(
                        text = { Text("复制参数") },
                        onClick = {
                            showDropdownMenu = false
                            onCopyParams()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null)
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
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    )
                    Divider()
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
            containerColor = Color.Black
        )
    )
}
