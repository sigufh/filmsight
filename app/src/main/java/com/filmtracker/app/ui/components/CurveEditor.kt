package com.filmtracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * 曲线编辑器组件
 * 支持 RGB 总曲线和单通道曲线编辑
 */
@Composable
fun CurveEditor(
    curve: FloatArray,
    onCurveChange: (FloatArray) -> Unit,
    curveColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val pointCount = curve.size
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 曲线画布
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedPointIndex = findNearestPoint(
                                    offset,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    curve
                                )
                            },
                            onDrag = { change, _ ->
                                selectedPointIndex?.let { index ->
                                    val x = change.position.x.coerceIn(0f, size.width)
                                    val y = change.position.y.coerceIn(0f, size.height)
                                    
                                    // 计算新的曲线值（Y 轴反转，因为屏幕坐标原点在左上角）
                                    val normalizedX = (x / size.width).coerceIn(0f, 1f)
                                    val normalizedY = 1f - (y / size.height).coerceIn(0f, 1f)
                                    
                                    // 更新对应控制点的 Y 值
                                    val pointX = index / (pointCount - 1f)
                                    if (kotlin.math.abs(normalizedX - pointX) < 0.1f) {
                                        val newCurve = curve.copyOf()
                                        newCurve[index] = normalizedY.coerceIn(0f, 1f)
                                        
                                        // 确保曲线单调递增（简化约束）
                                        ensureMonotonic(newCurve, index)
                                        onCurveChange(newCurve)
                                    }
                                }
                            },
                            onDragEnd = {
                                selectedPointIndex = null
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // 绘制网格
                    drawGrid(width, height)
                    
                    // 绘制曲线
                    drawCurve(curve, width, height, curveColor, selectedPointIndex)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 重置按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        // 重置为线性曲线
                        val linearCurve = FloatArray(pointCount) { it / (pointCount - 1f) }
                        onCurveChange(linearCurve)
                    }
                ) {
                    Text("重置")
                }
            }
        }
    }
}

/**
 * 查找最近的控制点
 */
private fun findNearestPoint(
    offset: Offset,
    width: Float,
    height: Float,
    curve: FloatArray
): Int? {
    val normalizedX = (offset.x / width).coerceIn(0f, 1f)
    val normalizedY = 1f - (offset.y / height).coerceIn(0f, 1f)
    
    var minDistance = Float.MAX_VALUE
    var nearestIndex: Int? = null
    
    curve.forEachIndexed { index, _ ->
        val pointX = index / (curve.size - 1f)
        val pointY = curve[index]
        
        val distance = kotlin.math.sqrt(
            (normalizedX - pointX) * (normalizedX - pointX) +
            (normalizedY - pointY) * (normalizedY - pointY)
        )
        
        if (distance < minDistance && distance < 0.1f) {
            minDistance = distance
            nearestIndex = index
        }
    }
    
    return nearestIndex
}

/**
 * 确保曲线单调递增（简化约束）
 */
private fun ensureMonotonic(curve: FloatArray, modifiedIndex: Int) {
    val pointCount = curve.size
    
    // 确保当前点不小于前一个点，不大于后一个点
    if (modifiedIndex > 0) {
        curve[modifiedIndex] = max(curve[modifiedIndex], curve[modifiedIndex - 1])
    }
    if (modifiedIndex < pointCount - 1) {
        curve[modifiedIndex] = min(curve[modifiedIndex], curve[modifiedIndex + 1])
    }
}

/**
 * 绘制网格
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    width: Float,
    height: Float
) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val gridStroke = Stroke(width = 1f)
    
    // 绘制水平和垂直网格线
    for (i in 0..4) {
        val y = height * i / 4f
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
    }
    
    for (i in 0..4) {
        val x = width * i / 4f
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
    }
}

/**
 * 绘制曲线
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurve(
    curve: FloatArray,
    width: Float,
    height: Float,
    curveColor: Color,
    selectedIndex: Int?
) {
    val pointCount = curve.size
    
                    // 绘制曲线路径（使用 Catmull-Rom 插值）
    val path = Path()
    for (i in 0 until pointCount) {
        val x = width * i / (pointCount - 1f)
        val y = height * (1f - curve[i]) // Y 轴反转
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            // 使用简单的线性插值（可以改为 Catmull-Rom）
            path.lineTo(x, y)
        }
    }
    
    // 绘制曲线
    drawPath(
        path = path,
        color = curveColor,
        style = Stroke(width = 3f)
    )
    
    // 绘制控制点
    val selectedColor = Color(0xFF6200EE) // Material Design Primary color
    for (i in 0 until pointCount) {
        val x = width * i / (pointCount - 1f)
        val y = height * (1f - curve[i])
        
        val isSelected = selectedIndex == i
        val pointColor = if (isSelected) selectedColor else curveColor
        val pointSize = if (isSelected) 12f else 8f
        
        drawCircle(
            color = pointColor,
            radius = pointSize,
            center = Offset(x, y)
        )
        
        // 绘制白色边框
        drawCircle(
            color = Color.White,
            radius = pointSize + 2f,
            style = Stroke(width = 2f)
        )
    }
}

/**
 * RGB 曲线选择器
 */
@Composable
fun CurveSelector(
    selectedCurve: CurveType,
    onCurveSelected: (CurveType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CurveType.values().forEach { curveType ->
            FilterChip(
                selected = selectedCurve == curveType,
                onClick = { onCurveSelected(curveType) },
                label = { Text(curveType.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

enum class CurveType(val label: String) {
    RGB("RGB"),
    RED("红"),
    GREEN("绿"),
    BLUE("蓝")
}
