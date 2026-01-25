package com.filmtracker.app.ui.screens.panels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.models.SecondaryTool
import com.filmtracker.app.ui.screens.models.CurveChannel
import com.filmtracker.app.ui.screens.panels.color.*
import com.filmtracker.app.ui.theme.IconSize
import com.filmtracker.app.ui.theme.Spacing

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
        val tools = listOf(
            SecondaryTool.AUTO,
            SecondaryTool.BRIGHTNESS,
            SecondaryTool.COLOR_TEMP,
            SecondaryTool.EFFECTS,
            SecondaryTool.DETAIL
        )
        val selectedIndex = tools.indexOf(selectedSecondaryTool).coerceAtLeast(0)

        Column(modifier = Modifier.fillMaxSize()) {
            SecondaryToolTabRow(
                tools = tools,
                selectedIndex = selectedIndex,
                onToolSelected = onSecondaryToolSelected
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(Spacing.md)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondaryToolTabRow(
    tools: List<SecondaryTool>,
    selectedIndex: Int,
    onToolSelected: (SecondaryTool) -> Unit
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = Spacing.md
    ) {
        tools.forEachIndexed { index, tool ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onToolSelected(tool) },
                text = {
                    Text(
                        text = tool.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                icon = {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.label,
                        modifier = Modifier.size(IconSize.md)
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
