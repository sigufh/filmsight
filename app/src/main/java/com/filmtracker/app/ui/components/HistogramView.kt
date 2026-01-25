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
import com.filmtracker.app.ui.theme.CornerRadius
import com.filmtracker.app.ui.theme.Spacing
import com.filmtracker.app.util.HistogramInfo

/**
 * Histogram channel colors - these are semantic colors for RGB channels
 * that need to remain fixed regardless of theme
 */
private object HistogramColors {
    val red = Color(0xFFFF5555)
    val green = Color(0xFF55FF55)
    val blue = Color(0xFF5555FF)
}

/**
 * Histogram display component
 */
@Composable
fun HistogramView(
    histogramInfo: HistogramInfo?,
    modifier: Modifier = Modifier,
    showChannels: Boolean = true
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = surfaceColor,
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .padding(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "直方图",
                color = onSurfaceColor,
                style = MaterialTheme.typography.titleSmall
            )

            if (showChannels) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ChannelIndicator("R", HistogramColors.red)
                    ChannelIndicator("G", HistogramColors.green)
                    ChannelIndicator("B", HistogramColors.blue)
                    ChannelIndicator("L", onSurfaceColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        if (histogramInfo != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.histogramHeight)
                    .background(surfaceContainerColor, RoundedCornerShape(CornerRadius.sm))
            ) {
                val width = size.width
                val height = size.height

                // Draw grid lines
                drawGrid(width, height, onSurfaceVariantColor)

                // Draw histogram channels
                if (showChannels) {
                    drawHistogramChannel(
                        histogramInfo.redHistogram,
                        HistogramColors.red.copy(alpha = 0.5f),
                        width,
                        height
                    )
                    drawHistogramChannel(
                        histogramInfo.greenHistogram,
                        HistogramColors.green.copy(alpha = 0.5f),
                        width,
                        height
                    )
                    drawHistogramChannel(
                        histogramInfo.blueHistogram,
                        HistogramColors.blue.copy(alpha = 0.5f),
                        width,
                        height
                    )
                }

                // Draw luminance histogram (top layer)
                drawHistogramChannel(
                    histogramInfo.luminanceHistogram,
                    onSurfaceColor.copy(alpha = 0.7f),
                    width,
                    height
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Display analysis info
            Text(
                text = histogramInfo.analyze(),
                color = onSurfaceVariantColor,
                style = MaterialTheme.typography.labelSmall
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ComponentSize.histogramHeight)
                    .background(surfaceContainerColor, RoundedCornerShape(CornerRadius.sm)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "无直方图数据",
                    color = onSurfaceVariantColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Component size constants specific to HistogramView
 */
private object ComponentSize {
    val histogramHeight = 120.dp
}

/**
 * Draw grid lines
 */
private fun DrawScope.drawGrid(width: Float, height: Float, gridBaseColor: Color) {
    val gridColor = gridBaseColor.copy(alpha = 0.1f)

    // Vertical grid lines (4 divisions)
    for (i in 1..3) {
        val x = width * i / 4
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
    }

    // Horizontal grid lines (3 divisions)
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
 * Draw a single channel histogram
 */
private fun DrawScope.drawHistogramChannel(
    histogram: IntArray,
    color: Color,
    width: Float,
    height: Float
) {
    if (histogram.isEmpty()) return

    // Find max value for normalization
    val maxValue = histogram.maxOrNull()?.toFloat() ?: 1f
    if (maxValue == 0f) return

    val path = Path()
    val barWidth = width / 256f

    // Start from bottom
    path.moveTo(0f, height)

    // Draw histogram curve
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

    // Close path at bottom
    path.lineTo(width, height)
    path.close()

    // Fill path
    drawPath(
        path = path,
        color = color
    )
}

/**
 * Channel indicator
 */
@Composable
private fun ChannelIndicator(label: String, color: Color) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.sm)
                .background(color, RoundedCornerShape(CornerRadius.xs))
        )
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
