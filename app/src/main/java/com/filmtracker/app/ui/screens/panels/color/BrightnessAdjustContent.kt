package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.components.AdjustmentSlider
import kotlin.math.pow

@Composable
fun BrightnessAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onOpenCurveEditor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdjustmentSlider(
            label = "曝光度",
            value = params.globalExposure * 20f,
            onValueChange = { onParamsChange(params.copy(globalExposure = it / 20f)) },
            valueRange = -100f..100f
        )
        
        AdjustmentSlider(
            label = "对比度",
            value = run {
                val c = params.contrast
                when {
                    c >= 1f -> {
                        val normalized = (c - 1f) / 0.3f
                        (normalized.toDouble().pow(1.0/3.0).toFloat() * 100f).coerceIn(0f, 100f)
                    }
                    else -> ((c - 1f) / 0.4f * 100f).coerceIn(-100f, 0f)
                }
            },
            onValueChange = { sliderValue ->
                val contrast = when {
                    sliderValue >= 0 -> {
                        val normalized = sliderValue / 100f
                        1f + normalized.toDouble().pow(3.0).toFloat() * 0.3f
                    }
                    else -> 1f + sliderValue / 100f * 0.4f
                }
                onParamsChange(params.copy(contrast = contrast))
            },
            valueRange = -100f..100f
        )
        
        AdjustmentSlider(
            label = "高光",
            value = params.highlights,
            onValueChange = { onParamsChange(params.copy(highlights = it)) },
            valueRange = -100f..100f
        )
        
        AdjustmentSlider(
            label = "阴影",
            value = params.shadows,
            onValueChange = { onParamsChange(params.copy(shadows = it)) },
            valueRange = -100f..100f
        )
        
        AdjustmentSlider(
            label = "白场",
            value = params.whites,
            onValueChange = { onParamsChange(params.copy(whites = it)) },
            valueRange = -100f..100f
        )
        
        AdjustmentSlider(
            label = "黑场",
            value = params.blacks,
            onValueChange = { onParamsChange(params.copy(blacks = it)) },
            valueRange = -100f..100f
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onOpenCurveEditor,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2C2C2E)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "曲线",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("曲线", color = Color.White)
        }
    }
}
