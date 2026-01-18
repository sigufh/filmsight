package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.models.CurveChannel

@Composable
fun CurveEditorFullScreen(
    selectedChannel: CurveChannel,
    onChannelSelected: (CurveChannel) -> Unit,
    onDismiss: () -> Unit,
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                InteractiveCurveEditor(
                    channel = selectedChannel,
                    params = params,
                    onParamsChange = onParamsChange
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    
                    Text(
                        text = "曲线",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // 占位，保持标题居中
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CurveChannel.entries.forEach { channel ->
                        CurveChannelButton(
                            channel = channel,
                            isSelected = selectedChannel == channel,
                            onClick = { onChannelSelected(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurveChannelButton(
    channel: CurveChannel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isSelected) channel.color else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
                .then(
                    if (!isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = channel.color,
                            shape = MaterialTheme.shapes.small
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (channel == CurveChannel.RGB) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = channel.label,
                    tint = if (isSelected) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
