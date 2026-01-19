package com.filmtracker.app.ui.screens.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.components.AdjustmentSlider
import com.filmtracker.app.ui.screens.models.SecondaryTool
import com.filmtracker.app.ui.screens.models.CurveChannel
import com.filmtracker.app.ui.screens.panels.color.*

@Composable
fun ColorAdjustmentPanel(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    selectedSecondaryTool: SecondaryTool?,
    onSecondaryToolSelected: (SecondaryTool) -> Unit,
    currentBitmap: android.graphics.Bitmap? = null
) {
    var showCurveEditor by remember { mutableStateOf(false) }
    var selectedCurveChannel by remember { mutableStateOf(CurveChannel.RGB) }
    
    if (showCurveEditor) {
        CurveEditorFullScreen(
            selectedChannel = selectedCurveChannel,
            onChannelSelected = { selectedCurveChannel = it },
            onDismiss = { showCurveEditor = false },
            params = params,
            onParamsChange = onParamsChange
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            SecondaryToolBar(
                tools = listOf(
                    SecondaryTool.AUTO,
                    SecondaryTool.BRIGHTNESS,
                    SecondaryTool.COLOR_TEMP,
                    SecondaryTool.EFFECTS,
                    SecondaryTool.DETAIL
                ),
                selectedTool = selectedSecondaryTool,
                onToolSelected = onSecondaryToolSelected
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (selectedSecondaryTool) {
                    SecondaryTool.AUTO -> AutoAdjustContent()
                    SecondaryTool.BRIGHTNESS -> BrightnessAdjustContent(
                        params = params,
                        onParamsChange = onParamsChange,
                        onOpenCurveEditor = { showCurveEditor = true }
                    )
                    SecondaryTool.COLOR_TEMP -> ColorAdjustContent(
                        params = params,
                        onParamsChange = onParamsChange,
                        currentBitmap = currentBitmap
                    )
                    SecondaryTool.EFFECTS -> EffectsAdjustContent(params, onParamsChange)
                    SecondaryTool.DETAIL -> DetailAdjustContent(params, onParamsChange)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun SecondaryToolBar(
    tools: List<SecondaryTool>,
    selectedTool: SecondaryTool?,
    onToolSelected: (SecondaryTool) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2C2C2E))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(tools) { tool ->
            SecondaryToolButton(
                tool = tool,
                isSelected = selectedTool == tool,
                onClick = { onToolSelected(tool) }
            )
        }
    }
}

@Composable
private fun SecondaryToolButton(
    tool: SecondaryTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                tint = if (isSelected) Color(0xFF0A84FF) else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF0A84FF) else Color.White
        )
    }
}
