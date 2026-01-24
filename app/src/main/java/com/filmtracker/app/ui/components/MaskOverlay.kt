package com.filmtracker.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 蒙版叠加层
 * 用于在图像预览上显示 AI 识别的主体范围
 */
@Composable
fun MaskOverlay(
    mask: Bitmap?,
    imageSize: Size,
    modifier: Modifier = Modifier,
    overlayColor: Color = Color(0x8000FF00), // 半透明绿色
    showBoundingBox: Boolean = true
) {
    if (mask == null) return
    
    Canvas(modifier = modifier.fillMaxSize()) {
        // 绘制半透明蒙版
        drawMaskOverlay(
            mask = mask,
            imageSize = imageSize,
            canvasSize = size,
            overlayColor = overlayColor
        )
        
        // 绘制边界框
        if (showBoundingBox) {
            val bounds = calculateMaskBounds(mask)
            if (bounds != null) {
                drawBoundingBox(
                    bounds = bounds,
                    imageSize = imageSize,
                    canvasSize = size
                )
            }
        }
    }
}

/**
 * 绘制蒙版叠加
 */
private fun DrawScope.drawMaskOverlay(
    mask: Bitmap,
    imageSize: Size,
    canvasSize: Size,
    overlayColor: Color
) {
    // 计算缩放比例和偏移
    val scaleX = canvasSize.width / imageSize.width
    val scaleY = canvasSize.height / imageSize.height
    val scale = minOf(scaleX, scaleY)
    
    val scaledWidth = imageSize.width * scale
    val scaledHeight = imageSize.height * scale
    val offsetX = (canvasSize.width - scaledWidth) / 2
    val offsetY = (canvasSize.height - scaledHeight) / 2
    
    // 绘制蒙版（采样以提高性能）
    val sampleRate = 4 // 每4个像素采样一次
    val maskWidth = mask.width
    val maskHeight = mask.height
    
    for (y in 0 until maskHeight step sampleRate) {
        for (x in 0 until maskWidth step sampleRate) {
            val pixel = mask.getPixel(x, y)
            val maskValue = (pixel and 0xFF) / 255f
            
            if (maskValue > 0.5f) {
                // 计算在画布上的位置
                val canvasX = offsetX + (x.toFloat() / maskWidth) * scaledWidth
                val canvasY = offsetY + (y.toFloat() / maskHeight) * scaledHeight
                val pixelSize = (scaledWidth / maskWidth) * sampleRate
                
                drawRect(
                    color = overlayColor.copy(alpha = overlayColor.alpha * maskValue),
                    topLeft = Offset(canvasX, canvasY),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }
}

/**
 * 计算蒙版的边界框
 */
private fun calculateMaskBounds(mask: Bitmap): MaskBounds? {
    var minX = mask.width
    var minY = mask.height
    var maxX = 0
    var maxY = 0
    var found = false
    
    for (y in 0 until mask.height) {
        for (x in 0 until mask.width) {
            val pixel = mask.getPixel(x, y)
            val maskValue = (pixel and 0xFF) / 255f
            
            if (maskValue > 0.5f) {
                found = true
                minX = minOf(minX, x)
                minY = minOf(minY, y)
                maxX = maxOf(maxX, x)
                maxY = maxOf(maxY, y)
            }
        }
    }
    
    return if (found) {
        MaskBounds(
            left = minX.toFloat() / mask.width,
            top = minY.toFloat() / mask.height,
            right = maxX.toFloat() / mask.width,
            bottom = maxY.toFloat() / mask.height
        )
    } else {
        null
    }
}

/**
 * 绘制边界框
 */
private fun DrawScope.drawBoundingBox(
    bounds: MaskBounds,
    imageSize: Size,
    canvasSize: Size
) {
    // 计算缩放比例和偏移
    val scaleX = canvasSize.width / imageSize.width
    val scaleY = canvasSize.height / imageSize.height
    val scale = minOf(scaleX, scaleY)
    
    val scaledWidth = imageSize.width * scale
    val scaledHeight = imageSize.height * scale
    val offsetX = (canvasSize.width - scaledWidth) / 2
    val offsetY = (canvasSize.height - scaledHeight) / 2
    
    // 计算边界框在画布上的位置
    val left = offsetX + bounds.left * scaledWidth
    val top = offsetY + bounds.top * scaledHeight
    val right = offsetX + bounds.right * scaledWidth
    val bottom = offsetY + bounds.bottom * scaledHeight
    
    // 绘制边界框
    drawRect(
        color = Color(0xFF00FF00), // 绿色
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        style = Stroke(width = 3f)
    )
    
    // 绘制角点标记
    val cornerSize = 20f
    val corners = listOf(
        Pair(left, top),
        Pair(right, top),
        Pair(left, bottom),
        Pair(right, bottom)
    )
    
    corners.forEach { (x, y) ->
        // 水平线
        drawLine(
            color = Color(0xFF00FF00),
            start = Offset(x - cornerSize / 2, y),
            end = Offset(x + cornerSize / 2, y),
            strokeWidth = 3f
        )
        // 垂直线
        drawLine(
            color = Color(0xFF00FF00),
            start = Offset(x, y - cornerSize / 2),
            end = Offset(x, y + cornerSize / 2),
            strokeWidth = 3f
        )
    }
}

/**
 * 蒙版边界数据类
 */
private data class MaskBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
