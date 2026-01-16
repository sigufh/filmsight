package com.filmtracker.app.ui.screens.panels.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.models.CurveChannel
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun InteractiveCurveEditor(
    channel: CurveChannel,
    params: BasicAdjustmentParams,
    onParamsChange: (BasicAdjustmentParams) -> Unit
) {
    val currentCurvePoints = when (channel) {
        CurveChannel.RGB -> params.rgbCurvePoints
        CurveChannel.RED -> params.redCurvePoints
        CurveChannel.GREEN -> params.greenCurvePoints
        CurveChannel.BLUE -> params.blueCurvePoints
    }
    
    val initialPoints = remember(channel, currentCurvePoints) {
        currentCurvePoints.map { Offset(it.first, it.second) }
    }
    
    var controlPoints by remember(channel) { mutableStateOf(initialPoints) }
    var draggingPointIndex by remember { mutableStateOf<Int?>(null) }
    
    fun updateCurvePoints() {
        val newPoints = controlPoints.map { Pair(it.x, it.y) }
        val newParams = when (channel) {
            CurveChannel.RGB -> params.copy(enableRgbCurve = true, rgbCurvePoints = newPoints)
            CurveChannel.RED -> params.copy(enableRedCurve = true, redCurvePoints = newPoints)
            CurveChannel.GREEN -> params.copy(enableGreenCurve = true, greenCurvePoints = newPoints)
            CurveChannel.BLUE -> params.copy(enableBlueCurve = true, blueCurvePoints = newPoints)
        }
        onParamsChange(newParams)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(channel, controlPoints) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        val normalizedX = (offset.x / canvasWidth).coerceIn(0f, 1f)
                        val normalizedY = (1f - offset.y / canvasHeight).coerceIn(0f, 1f)
                        
                        val clickedPointIndex = controlPoints.indexOfFirst { point ->
                            val pointX = point.x * canvasWidth
                            val pointY = (1f - point.y) * canvasHeight
                            val distance = sqrt((pointX - offset.x).pow(2) + (pointY - offset.y).pow(2))
                            distance < 60f
                        }
                        
                        if (clickedPointIndex == -1) {
                            val newPoints = controlPoints.toMutableList()
                            val insertIndex = newPoints.indexOfFirst { it.x > normalizedX }
                            if (insertIndex != -1) {
                                newPoints.add(insertIndex, Offset(normalizedX, normalizedY))
                            } else {
                                newPoints.add(Offset(normalizedX, normalizedY))
                            }
                            controlPoints = newPoints
                            updateCurvePoints()
                        }
                    }
                }
                .pointerInput(channel, controlPoints) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()
                            val pointIndex = controlPoints.indexOfFirst { point ->
                                val pointX = point.x * canvasWidth
                                val pointY = (1f - point.y) * canvasHeight
                                val distance = sqrt((pointX - offset.x).pow(2) + (pointY - offset.y).pow(2))
                                distance < 60f
                            }
                            if (pointIndex != -1) draggingPointIndex = pointIndex
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            draggingPointIndex?.let { currentIndex ->
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()
                                val normalizedY = (1f - change.position.y / canvasHeight).coerceIn(0f, 1f)
                                val newPoints = controlPoints.toMutableList()
                                
                                if (currentIndex >= 0 && currentIndex < newPoints.size) {
                                    val point = newPoints[currentIndex]
                                    if (currentIndex == 0 || currentIndex == controlPoints.size - 1) {
                                        newPoints[currentIndex] = Offset(point.x, normalizedY)
                                    } else {
                                        val normalizedX = (change.position.x / canvasWidth).coerceIn(0f, 1f)
                                        val prevX = if (currentIndex > 0) newPoints[currentIndex - 1].x else 0f
                                        val nextX = if (currentIndex < newPoints.size - 1) newPoints[currentIndex + 1].x else 1f
                                        val clampedX = normalizedX.coerceIn(prevX + 0.001f, nextX - 0.001f)
                                        newPoints[currentIndex] = Offset(clampedX, normalizedY)
                                    }
                                    controlPoints = newPoints
                                    updateCurvePoints()
                                }
                            }
                        },
                        onDragEnd = { draggingPointIndex = null }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val gridColor = Color.Gray.copy(alpha = 0.3f)
            val gridLines = 4
            
            for (i in 1 until gridLines) {
                val x = canvasWidth * i / gridLines
                drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeight), 1f)
            }
            for (i in 1 until gridLines) {
                val y = canvasHeight * i / gridLines
                drawLine(gridColor, Offset(0f, y), Offset(canvasWidth, y), 1f)
            }
            
            drawLine(
                Color.Gray.copy(alpha = 0.5f),
                Offset(0f, canvasHeight),
                Offset(canvasWidth, 0f),
                2f
            )
            
            val path = Path()
            val steps = 100
            for (i in 0..steps) {
                val x = i / steps.toFloat()
                val y = interpolateSpline(controlPoints, x)
                val canvasX = x * canvasWidth
                val canvasY = (1f - y) * canvasHeight
                if (i == 0) path.moveTo(canvasX, canvasY) else path.lineTo(canvasX, canvasY)
            }
            drawPath(path, channel.color, style = Stroke(width = 5f))
            
            for ((index, point) in controlPoints.withIndex()) {
                val canvasX = point.x * canvasWidth
                val canvasY = (1f - point.y) * canvasHeight
                val isBeingDragged = draggingPointIndex == index
                val isEndpoint = index == 0 || index == controlPoints.size - 1
                
                drawCircle(
                    Color.White,
                    if (isBeingDragged) 20f else if (isEndpoint) 14f else 16f,
                    Offset(canvasX, canvasY)
                )
                drawCircle(
                    channel.color,
                    if (isBeingDragged) 16f else if (isEndpoint) 10f else 12f,
                    Offset(canvasX, canvasY)
                )
            }
        }
    }
}

fun interpolateSpline(points: List<Offset>, x: Float): Float {
    if (points.isEmpty()) return x
    if (points.size == 1) return points[0].y
    if (x <= points.first().x) return points.first().y
    if (x >= points.last().x) return points.last().y
    
    var i1 = 0
    var i2 = 1
    for (i in 0 until points.size - 1) {
        if (x >= points[i].x && x <= points[i + 1].x) {
            i1 = i
            i2 = i + 1
            break
        }
    }
    
    val p1 = points[i1]
    val p2 = points[i2]
    val dx = p2.x - p1.x
    if (dx < 0.0001f) return p1.y
    
    val t = ((x - p1.x) / dx).coerceIn(0f, 1f)
    val t2 = t * t
    val t3 = t2 * t
    
    val h00 = 2 * t3 - 3 * t2 + 1
    val h10 = t3 - 2 * t2 + t
    val h01 = -2 * t3 + 3 * t2
    val h11 = t3 - t2
    
    val m0 = if (i1 > 0) {
        val prevDx = p2.x - points[i1 - 1].x
        if (prevDx > 0.0001f) (p2.y - points[i1 - 1].y) / prevDx else (p2.y - p1.y) / dx
    } else (p2.y - p1.y) / dx
    
    val m1 = if (i2 < points.size - 1) {
        val nextDx = points[i2 + 1].x - p1.x
        if (nextDx > 0.0001f) (points[i2 + 1].y - p1.y) / nextDx else (p2.y - p1.y) / dx
    } else (p2.y - p1.y) / dx
    
    return (h00 * p1.y + h10 * dx * m0 + h01 * p2.y + h11 * dx * m1).coerceIn(0f, 1f)
}
