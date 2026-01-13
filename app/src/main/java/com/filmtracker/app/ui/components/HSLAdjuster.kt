package com.filmtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * HSL 调整器组件
 * 支持 8 个色相段的独立调整：红、橙、黄、绿、青、蓝、紫、品红
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
        // 色相段列表
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
                    .padding(vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 重置所有按钮
        TextButton(
            onClick = {
                // 重置所有 HSL 参数
                HSLHueSegment.values().forEachIndexed { index, _ ->
                    onHueShiftChange(index, 0f)
                    onSaturationChange(index, 0f)
                    onLuminanceChange(index, 0f)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重置所有")
        }
    }
}

/**
 * 单个色相段的调整卡片
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
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 色相段标题（带颜色指示）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 颜色指示器
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = segment.color,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Text(
                        text = segment.label,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 色相偏移滑块
            ParameterSlider(
                label = "色相",
                value = hueShift,
                onValueChange = onHueShiftChange,
                valueRange = -180f..180f,
                steps = 36,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 饱和度滑块
            ParameterSlider(
                label = "饱和度",
                value = saturation,
                onValueChange = onSaturationChange,
                valueRange = -100f..100f,
                steps = 20,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 亮度滑块
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
 * HSL 色相段枚举
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
