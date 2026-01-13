package com.filmtracker.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.filmtracker.app.data.FilmParams

/**
 * 响应曲线控制组件
 */
@Composable
fun ResponseCurveControls(
    filmParams: FilmParams,
    onParamsChange: (FilmParams) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedChannel by remember { 
        mutableStateOf<String?>(null) 
    }
    
    Column(modifier = modifier) {
        // Red Channel
        ExpandableChannelControl(
            channelName = "Red",
            expanded = expandedChannel == "Red",
            onExpandedChange = { 
                expandedChannel = if (expandedChannel == "Red") null else "Red" 
            }
        ) {
            ChannelResponseControls(
                params = filmParams,
                channelPrefix = "red",
                onParamsChange = onParamsChange
            )
        }
        
        // Green Channel
        ExpandableChannelControl(
            channelName = "Green",
            expanded = expandedChannel == "Green",
            onExpandedChange = { 
                expandedChannel = if (expandedChannel == "Green") null else "Green" 
            }
        ) {
            ChannelResponseControls(
                params = filmParams,
                channelPrefix = "green",
                onParamsChange = onParamsChange
            )
        }
        
        // Blue Channel
        ExpandableChannelControl(
            channelName = "Blue",
            expanded = expandedChannel == "Blue",
            onExpandedChange = { 
                expandedChannel = if (expandedChannel == "Blue") null else "Blue" 
            }
        ) {
            ChannelResponseControls(
                params = filmParams,
                channelPrefix = "blue",
                onParamsChange = onParamsChange
            )
        }
    }
}

@Composable
fun ExpandableChannelControl(
    channelName: String,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onExpandedChange
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = channelName,
                style = MaterialTheme.typography.labelLarge,
                color = when (channelName) {
                    "Red" -> Color(0xFFFF5252)
                    "Green" -> Color(0xFF4CAF50)
                    "Blue" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Icon(
                imageVector = if (expanded) 
                    Icons.Default.ExpandLess 
                else 
                    Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
        
        if (expanded) {
            content()
        }
    }
}

@Composable
fun ChannelResponseControls(
    params: FilmParams,
    channelPrefix: String,
    onParamsChange: (FilmParams) -> Unit
) {
    // 简化实现：实际应根据 channelPrefix 动态访问属性
    // 这里使用反射或 when 表达式
    when (channelPrefix) {
        "red" -> {
            ParameterSlider(
                label = "Toe Strength",
                value = params.redToeStrength,
                onValueChange = { onParamsChange(params.copy(redToeStrength = it)) },
                valueRange = 0.0f..0.5f
            )
            ParameterSlider(
                label = "Shoulder Strength",
                value = params.redShoulderStrength,
                onValueChange = { onParamsChange(params.copy(redShoulderStrength = it)) },
                valueRange = 0.0f..1.0f
            )
        }
        "green" -> {
            ParameterSlider(
                label = "Toe Strength",
                value = params.greenToeStrength,
                onValueChange = { onParamsChange(params.copy(greenToeStrength = it)) },
                valueRange = 0.0f..0.5f
            )
            ParameterSlider(
                label = "Shoulder Strength",
                value = params.greenShoulderStrength,
                onValueChange = { onParamsChange(params.copy(greenShoulderStrength = it)) },
                valueRange = 0.0f..1.0f
            )
        }
        "blue" -> {
            ParameterSlider(
                label = "Toe Strength",
                value = params.blueToeStrength,
                onValueChange = { onParamsChange(params.copy(blueToeStrength = it)) },
                valueRange = 0.0f..0.5f
            )
            ParameterSlider(
                label = "Shoulder Strength",
                value = params.blueShoulderStrength,
                onValueChange = { onParamsChange(params.copy(blueShoulderStrength = it)) },
                valueRange = 0.0f..1.0f
            )
        }
    }
}
