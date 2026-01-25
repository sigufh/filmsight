package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.components.AdjustmentSlider
import com.filmtracker.app.ui.theme.Spacing

@Composable
fun EffectsAdjustContent(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        AdjustmentSlider(
            label = "纹理",
            value = params.texture,
            onValueChange = { onParamsChange(params.copy(texture = it)) },
            valueRange = -100f..100f
        )

        AdjustmentSlider(
            label = "去雾",
            value = params.dehaze,
            onValueChange = { onParamsChange(params.copy(dehaze = it)) },
            valueRange = -100f..100f
        )

        AdjustmentSlider(
            label = "晕影",
            value = params.vignette,
            onValueChange = { onParamsChange(params.copy(vignette = it)) },
            valueRange = -100f..100f
        )

        AdjustmentSlider(
            label = "颗粒",
            value = params.grain,
            onValueChange = { onParamsChange(params.copy(grain = it)) },
            valueRange = 0f..100f
        )
    }
}
