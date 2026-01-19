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
            value = params.contrast,  // 直接使用 Adobe 标准值 (-100 到 +100)
            onValueChange = { onParamsChange(params.copy(contrast = it)) },
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
