package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.components.AdjustmentSlider

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { subScreen = "grading" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C2C2E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "分级",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分级", color = Color.White)
                }
                
                Button(
                    onClick = { subScreen = "mixer" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C2C2E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "混合",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("混合", color = Color.White)
                }
            }
        }
    }
}
