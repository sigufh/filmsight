package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.components.AdjustmentSlider
import com.filmtracker.app.ui.theme.IconSize
import com.filmtracker.app.ui.theme.Spacing

@Composable
fun ColorAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    currentBitmap: android.graphics.Bitmap? = null
) {
    var subScreen by remember { mutableStateOf<String?>(null) }
    
    when (subScreen) {
        "grading" -> ColorGradingScreen(
            params = params,
            onParamsChange = onParamsChange,
            onBack = { subScreen = null },
            currentBitmap = currentBitmap
        )
        "mixer" -> ColorMixerScreen(
            params = params,
            onParamsChange = onParamsChange,
            onBack = { subScreen = null }
        )
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                AdjustmentSlider(
                    label = "色温",
                    value = params.temperature,
                    onValueChange = { onParamsChange(params.copy(temperature = it)) },
                    valueRange = -100f..100f
                )
                
                AdjustmentSlider(
                    label = "色调",
                    value = params.tint,
                    onValueChange = { onParamsChange(params.copy(tint = it)) },
                    valueRange = -100f..100f
                )
                
                AdjustmentSlider(
                    label = "饱和度",
                    value = params.saturation,  // 直接使用 Adobe 标准值 (-100 到 +100)
                    onValueChange = { onParamsChange(params.copy(saturation = it)) },
                    valueRange = -100f..100f
                )
                
                AdjustmentSlider(
                    label = "自然饱和度",
                    value = params.vibrance,
                    onValueChange = { onParamsChange(params.copy(vibrance = it)) },
                    valueRange = -100f..100f
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                Button(
                    onClick = { subScreen = "grading" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "分级",
                        modifier = Modifier.size(IconSize.sm)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "分级",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                
                Button(
                    onClick = { subScreen = "mixer" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "混合",
                        modifier = Modifier.size(IconSize.sm)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "混合",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
