package com.filmtracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmtracker.app.util.HistogramInfo

/**
 * 直方图显示组件
 */
@Composable
fun HistogramView(
    histogramInfo: HistogramInfo?,
    modifier: Modifier = Modifier,
    showChannels: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "直方图",
                color = Color.White,
                fontSize = 14.sp,
                style = MaterialTheme.typography.titleSmall
            )
            
            if (showChannels) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChannelIndicator("R", Color(0xFFFF5555))
                    ChannelIndicator("G", Color(0xFF55FF55))
                    ChannelIndicator("B", Color(0xFF5555FF))
                    ChannelIndicator("L", Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (histogramInfo != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
            ) {
                val width = size.width
                val height = size.height
                
                // 绘制网格线
                drawGrid(width, height)
                
                // 绘制各个通道的直方图
                if (showChannels) {
                    drawHistogramChannel(
                        histogramInfo.redHistogram,
                        Color(0xFFFF5555).copy(alpha = 0.5f),
                        width,
                        height
                    )
                    drawHistogramChannel(
                        histogramInfo.greenHistogram,
                        Color(0xFF55FF55).copy(alpha = 0.5f),
                        width,
                        height
                    )
                    drawHistogramChannel(
                        histogramInfo.blueHistogram,
                        Color(0xFF5555FF).copy(alpha = 0.5f),
                        width,
                        height
                    )
                }
                
                // 绘制亮度直方图（最上层）
                drawHistogramChannel(
                    histogramInfo.luminanceHistogram,
                    Color.White.copy(alpha = 0.7f),
                    width,
                    height
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 显示分析信息
            Text(
                text = histogramInfo.analyze(),
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "无直方图数据",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 绘制网格线
 */
private fun DrawScope.drawGrid(width: Float, height: Float) {
    val gridColor = Color.Gray.copy(alpha = 0.1f)
    
    // 垂直网格线（4等分）
    for (i in 1..3) {
        val x = width * i / 4
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
    }
    
    // 水平网格线（3等分）
    for (i in 1..2) {
        val y = height * i / 3
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
    }
}

/**
 * 绘制单个通道的直方图
 */
private fun DrawScope.drawHistogramChannel(
    histogram: IntArray,
    color: Color,
    width: Float,
    height: Float
) {
    if (histogram.isEmpty()) return
    
    // 找到最大值用于归一化
    val maxValue = histogram.maxOrNull()?.toFloat() ?: 1f
    if (maxValue == 0f) return
    
    val path = Path()
    val barWidth = width / 256f
    
    // 从底部开始
    path.moveTo(0f, height)
    
    // 绘制直方图曲线
    histogram.forEachIndexed { index, value ->
        val x = index * barWidth
        val normalizedHeight = (value.toFloat() / maxValue) * height
        val y = height - normalizedHeight
        
        if (index == 0) {
            path.lineTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    // 回到底部闭合路径
    path.lineTo(width, height)
    path.close()
    
    // 填充路径
    drawPath(
        path = path,
        color = color
    )
}

/**
 * 通道指示器
 */
@Composable
private fun ChannelIndicator(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}
