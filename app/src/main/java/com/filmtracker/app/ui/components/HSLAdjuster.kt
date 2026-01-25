package com.filmtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.filmtracker.app.ui.theme.CornerRadius
import com.filmtracker.app.ui.theme.Spacing

/**
 * HSL Adjuster Component
 * Supports independent adjustment of 8 hue segments: Red, Orange, Yellow, Green, Cyan, Blue, Purple, Magenta
 */
@Composable
fun HSLAdjuster(
    hueShift: FloatArray,
    saturation: FloatArray,
    luminance: FloatArray,
    onHueShiftChange: (Int, Float) -> Unit,
    onSaturationChange: (Int, Float) -> Unit,
    onLuminanceChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Hue segment list
        HSLHueSegment.values().forEachIndexed { index, segment ->
            HSLSegmentCard(
                segment = segment,
                hueShift = hueShift[index],
                saturation = saturation[index],
                luminance = luminance[index],
                onHueShiftChange = { onHueShiftChange(index, it) },
                onSaturationChange = { onSaturationChange(index, it) },
                onLuminanceChange = { onLuminanceChange(index, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Reset all button
        TextButton(
            onClick = {
                // Reset all HSL parameters
                HSLHueSegment.values().forEachIndexed { index, _ ->
                    onHueShiftChange(index, 0f)
                    onSaturationChange(index, 0f)
                    onLuminanceChange(index, 0f)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "重置所有",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Single hue segment adjustment card
 */
@Composable
fun HSLSegmentCard(
    segment: HSLHueSegment,
    hueShift: Float,
    saturation: Float,
    luminance: Float,
    onHueShiftChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onLuminanceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(CornerRadius.sm),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            // Hue segment title (with color indicator)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // Color indicator
                    Box(
                        modifier = Modifier
                            .size(Spacing.lg)
                            .background(
                                color = segment.color,
                                shape = RoundedCornerShape(CornerRadius.xs)
                            )
                    )
                    Text(
                        text = segment.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Hue shift slider
            ParameterSlider(
                label = "色相",
                value = hueShift,
                onValueChange = onHueShiftChange,
                valueRange = -180f..180f,
                steps = 36,
                modifier = Modifier.fillMaxWidth()
            )

            // Saturation slider
            ParameterSlider(
                label = "饱和度",
                value = saturation,
                onValueChange = onSaturationChange,
                valueRange = -100f..100f,
                steps = 20,
                modifier = Modifier.fillMaxWidth()
            )

            // Luminance slider
            ParameterSlider(
                label = "亮度",
                value = luminance,
                onValueChange = onLuminanceChange,
                valueRange = -100f..100f,
                steps = 20,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * HSL Hue Segment Enum
 * Note: These colors are semantic and represent actual hue values,
 * so they remain fixed regardless of theme
 */
enum class HSLHueSegment(
    val label: String,
    val color: Color
) {
    RED("红", Color(0xFFFF0000)),
    ORANGE("橙", Color(0xFFFF8000)),
    YELLOW("黄", Color(0xFFFFFF00)),
    GREEN("绿", Color(0xFF00FF00)),
    CYAN("青", Color(0xFF00FFFF)),
    BLUE("蓝", Color(0xFF0000FF)),
    PURPLE("紫", Color(0xFF8000FF)),
    MAGENTA("品红", Color(0xFFFF00FF))
}
