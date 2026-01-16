package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.components.AdjustmentSlider

@Composable
fun DetailAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdjustmentSlider(
            label = "清晰度",
            value = params.clarity,
            onValueChange = { onParamsChange(params.copy(clarity = it)) },
            valueRange = -100f..100f
        )
        
        AdjustmentSlider(
            label = "锐化",
            value = params.sharpening,
            onValueChange = { onParamsChange(params.copy(sharpening = it)) },
            valueRange = 0f..100f
        )
        
        AdjustmentSlider(
            label = "降噪",
            value = params.noiseReduction,
            onValueChange = { onParamsChange(params.copy(noiseReduction = it)) },
            valueRange = 0f..100f
        )
    }
}
