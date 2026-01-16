package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.components.AdjustmentSlider

@Composable
fun ColorMixerScreen(
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit,
    onBack: () -> Unit
) {
    var selectedChannel by remember { mutableStateOf(0) }
    
    val channelNames = listOf("红", "橙", "黄", "绿", "青", "蓝", "紫", "品红")
    val channelColors = listOf(
        Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFFF00), Color(0xFF00FF00),
        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFF8800FF), Color(0xFFFF00FF)
    )
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
            }
            Text("混合", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(channelNames.size) { index ->
                ColorChannelButton(
                    channelNames[index], channelColors[index],
                    selectedChannel == index,
                    { selectedChannel = index }
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdjustmentSlider("色相", params.hslHueShift[selectedChannel], { 
                val newArray = params.hslHueShift.copyOf()
                newArray[selectedChannel] = it
                onParamsChange(params.copy(enableHSL = true, hslHueShift = newArray))
            }, -180f..180f)
            
            AdjustmentSlider("饱和度", params.hslSaturation[selectedChannel], { 
                val newArray = params.hslSaturation.copyOf()
                newArray[selectedChannel] = it
                onParamsChange(params.copy(enableHSL = true, hslSaturation = newArray))
            }, -100f..100f)
            
            AdjustmentSlider("明度", params.hslLuminance[selectedChannel], { 
                val newArray = params.hslLuminance.copyOf()
                newArray[selectedChannel] = it
                onParamsChange(params.copy(enableHSL = true, hslLuminance = newArray))
            }, -100f..100f)
        }
    }
}

@Composable
private fun ColorChannelButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected) color else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .then(
                        if (!isSelected) {
                            Modifier.border(2.dp, color, MaterialTheme.shapes.small)
                        } else Modifier
                    )
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF0A84FF) else Color.White
        )
    }
}
