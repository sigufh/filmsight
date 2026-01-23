package com.filmtracker.app.ui.screens.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ImagePreview(
    processedImage: Bitmap?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    cropEnabled: Boolean = false,
    cropLeft: Float = 0f,
    cropTop: Float = 0f,
    cropRight: Float = 1f,
    cropBottom: Float = 1f,
    onCropChange: ((Float, Float, Float, Float) -> Unit)? = null,
    rotation: Float = 0f,
    onRotationChange: ((Float) -> Unit)? = null,
    showCropOverlay: Boolean = false
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var dragMode by remember { mutableStateOf(CropDragMode.None) }
    
    // 内部维护裁剪状态 - 使用数组避免多个状态变量
    val internalCropState = remember { floatArrayOf(0f, 0f, 1f, 1f) } // [left, top, right, bottom]
    var cropStateVersion by remember { mutableStateOf(0) } // 用于触发重组
    
    var cropStartLeft by remember { mutableStateOf(0f) }
    var cropStartTop by remember { mutableStateOf(0f) }
    var cropStartRight by remember { mutableStateOf(1f) }
    var cropStartBottom by remember { mutableStateOf(1f) }
    var totalDx by remember { mutableStateOf(0f) }
    var totalDy by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    
    // 只在退出裁剪模式时通知外部
    LaunchedEffect(showCropOverlay) {
        if (!showCropOverlay && (internalCropState[0] != 0f || internalCropState[1] != 0f || internalCropState[2] != 1f || internalCropState[3] != 1f)) {
            // 退出裁剪模式，通知外部应用裁剪
            android.util.Log.d("ImagePreview", "✅ 退出裁剪模式，应用裁剪: l=${internalCropState[0]}, t=${internalCropState[1]}, r=${internalCropState[2]}, b=${internalCropState[3]}")
            onCropChange?.invoke(internalCropState[0], internalCropState[1], internalCropState[2], internalCropState[3])
            // 重置内部状态
            internalCropState[0] = 0f
            internalCropState[1] = 0f
            internalCropState[2] = 1f
            internalCropState[3] = 1f
        }
        if (showCropOverlay) {
            // 进入裁剪模式，重置缩放
            android.util.Log.d("ImagePreview", "✅ 进入裁剪模式")
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    LaunchedEffect(showCropOverlay) {
        if (showCropOverlay) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(modifier = modifier) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (processedImage != null && !processedImage.isRecycled) {
            // Convert bitmap to ImageBitmap to avoid recycling issues
            val imageBitmap = remember(processedImage) {
                try {
                    if (!processedImage.isRecycled) {
                        processedImage.asImageBitmap()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "处理后的图像",
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (showCropOverlay) {
                            // 裁剪模式下四周留出操作空间
                            Modifier.padding(32.dp)
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(showCropOverlay) {
                        if (showCropOverlay) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        } else {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale == 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        )
                    }
                )
            }

            if (showCropOverlay && imageBitmap != null) {
                val currentLeft = internalCropState[0].coerceIn(0f, 1f)
                val currentTop = internalCropState[1].coerceIn(0f, 1f)
                val currentRight = internalCropState[2].coerceIn(0f, 1f)
                val currentBottom = internalCropState[3].coerceIn(0f, 1f)
                
                // 裁剪模式下的padding
                val cropPadding = with(density) { 32.dp.toPx() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { containerSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val ir = computeImageRect(
                                        containerSize.width.toFloat(),
                                        containerSize.height.toFloat(),
                                        processedImage.width.toFloat(),
                                        processedImage.height.toFloat(),
                                        cropPadding,
                                        scale,
                                        offsetX,
                                        offsetY
                                    )
                                    // 直接从数组读取当前值，避免闭包捕获旧值
                                    val currentL = internalCropState[0].coerceIn(0f, 1f)
                                    val currentT = internalCropState[1].coerceIn(0f, 1f)
                                    val currentR = internalCropState[2].coerceIn(0f, 1f)
                                    val currentB = internalCropState[3].coerceIn(0f, 1f)
                                    
                                    val cropL = ir.left + currentL * ir.width
                                    val cropT = ir.top + currentT * ir.height
                                    val cropR = ir.left + currentR * ir.width
                                    val cropB = ir.top + currentB * ir.height
                                    val thr = 32f
                                    dragMode = determineDragMode(offset, cropL, cropT, cropR, cropB, thr)
                                    cropStartLeft = currentL
                                    cropStartTop = currentT
                                    cropStartRight = currentR
                                    cropStartBottom = currentB
                                    totalDx = 0f
                                    totalDy = 0f
                                    
                                    android.util.Log.d("ImagePreview", "🎯 onDragStart: l=$currentL, t=$currentT, r=$currentR, b=$currentB")
                                },
                                onDrag = { _, dragAmount ->
                                    val ir = computeImageRect(
                                        containerSize.width.toFloat(),
                                        containerSize.height.toFloat(),
                                        processedImage.width.toFloat(),
                                        processedImage.height.toFloat(),
                                        cropPadding,
                                        scale,
                                        offsetX,
                                        offsetY
                                    )
                                    
                                    // 计算新的裁剪值
                                    val (newL, newT, newR, newB) = when (dragMode) {
                                        CropDragMode.Move -> {
                                            totalDx += dragAmount.x
                                            totalDy += dragAmount.y
                                            val dx = totalDx / ir.width
                                            val dy = totalDy / ir.height
                                            var l = cropStartLeft + dx
                                            var t = cropStartTop + dy
                                            var r = cropStartRight + dx
                                            var b = cropStartBottom + dy
                                            val w = r - l
                                            val h = b - t
                                            if (l < 0f) { r -= l; l = 0f }
                                            if (t < 0f) { b -= t; t = 0f }
                                            if (r > 1f) { l -= (r - 1f); r = 1f }
                                            if (b > 1f) { t -= (b - 1f); b = 1f }
                                            l = l.coerceIn(0f, 1f - w)
                                            t = t.coerceIn(0f, 1f - h)
                                            r = l + w
                                            b = t + h
                                            listOf(l, t, r, b)
                                        }
                                        CropDragMode.Left -> {
                                            totalDx += dragAmount.x
                                            val l = (cropStartLeft + totalDx / ir.width).coerceIn(0f, cropStartRight - 0.1f)
                                            listOf(l, cropStartTop, cropStartRight, cropStartBottom)
                                        }
                                        CropDragMode.Right -> {
                                            totalDx += dragAmount.x
                                            val r = (cropStartRight + totalDx / ir.width).coerceIn(cropStartLeft + 0.1f, 1f)
                                            listOf(cropStartLeft, cropStartTop, r, cropStartBottom)
                                        }
                                        CropDragMode.Top -> {
                                            totalDy += dragAmount.y
                                            val t = (cropStartTop + totalDy / ir.height).coerceIn(0f, cropStartBottom - 0.1f)
                                            listOf(cropStartLeft, t, cropStartRight, cropStartBottom)
                                        }
                                        CropDragMode.Bottom -> {
                                            totalDy += dragAmount.y
                                            val b = (cropStartBottom + totalDy / ir.height).coerceIn(cropStartTop + 0.1f, 1f)
                                            listOf(cropStartLeft, cropStartTop, cropStartRight, b)
                                        }
                                        CropDragMode.TopLeft -> {
                                            totalDx += dragAmount.x
                                            totalDy += dragAmount.y
                                            val l = (cropStartLeft + totalDx / ir.width).coerceIn(0f, cropStartRight - 0.1f)
                                            val t = (cropStartTop + totalDy / ir.height).coerceIn(0f, cropStartBottom - 0.1f)
                                            listOf(l, t, cropStartRight, cropStartBottom)
                                        }
                                        CropDragMode.TopRight -> {
                                            totalDx += dragAmount.x
                                            totalDy += dragAmount.y
                                            val r = (cropStartRight + totalDx / ir.width).coerceIn(cropStartLeft + 0.1f, 1f)
                                            val t = (cropStartTop + totalDy / ir.height).coerceIn(0f, cropStartBottom - 0.1f)
                                            listOf(cropStartLeft, t, r, cropStartBottom)
                                        }
                                        CropDragMode.BottomLeft -> {
                                            totalDx += dragAmount.x
                                            totalDy += dragAmount.y
                                            val l = (cropStartLeft + totalDx / ir.width).coerceIn(0f, cropStartRight - 0.1f)
                                            val b = (cropStartBottom + totalDy / ir.height).coerceIn(cropStartTop + 0.1f, 1f)
                                            listOf(l, cropStartTop, cropStartRight, b)
                                        }
                                        CropDragMode.BottomRight -> {
                                            totalDx += dragAmount.x
                                            totalDy += dragAmount.y
                                            val r = (cropStartRight + totalDx / ir.width).coerceIn(cropStartLeft + 0.1f, 1f)
                                            val b = (cropStartBottom + totalDy / ir.height).coerceIn(cropStartTop + 0.1f, 1f)
                                            listOf(cropStartLeft, cropStartTop, r, b)
                                        }
                                        CropDragMode.None -> listOf(internalCropState[0], internalCropState[1], internalCropState[2], internalCropState[3])
                                    }
                                    
                                    // 更新内部状态（不通知外部）
                                    internalCropState[0] = newL
                                    internalCropState[1] = newT
                                    internalCropState[2] = newR
                                    internalCropState[3] = newB
                                    cropStateVersion++ // 触发重组
                                    
                                    android.util.Log.d("ImagePreview", "🖱️ 拖拽更新: l=$newL, t=$newT, r=$newR, b=$newB")
                                },
                                onDragEnd = {
                                    dragMode = CropDragMode.None
                                }
                            )
                        }
                ) {
                    // 使用cropStateVersion作为key确保Canvas在裁剪状态变化时重组
                    key(cropStateVersion) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val ir = computeImageRect(w, h, processedImage.width.toFloat(), processedImage.height.toFloat(), cropPadding, scale, offsetX, offsetY)
                            
                            val cropL = ir.left + currentLeft * ir.width
                            val cropT = ir.top + currentTop * ir.height
                            val cropR = ir.left + currentRight * ir.width
                            val cropB = ir.top + currentBottom * ir.height
                        
                        if (cropL > 0f) drawRect(Color(0x88000000), size = Size(cropL, h))
                        if (cropR < w) drawRect(Color(0x88000000), topLeft = Offset(cropR, 0f), size = Size(w - cropR, h))
                        if (cropT > 0f) drawRect(Color(0x88000000), topLeft = Offset(cropL, 0f), size = Size(cropR - cropL, cropT))
                        if (cropB < h) drawRect(Color(0x88000000), topLeft = Offset(cropL, cropB), size = Size(cropR - cropL, h - cropB))
                        
                        drawRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = Offset(cropL, cropT),
                            size = Size(cropR - cropL, cropB - cropT),
                            style = Stroke(width = 3f)
                        )
                        
                        val thirdW = (cropR - cropL) / 3f
                        val thirdH = (cropB - cropT) / 3f
                        for (i in 1..2) {
                            val x = cropL + thirdW * i
                            drawLine(Color.White.copy(alpha = 0.5f), start = Offset(x, cropT), end = Offset(x, cropB), strokeWidth = 1f)
                            val y = cropT + thirdH * i
                            drawLine(Color.White.copy(alpha = 0.5f), start = Offset(cropL, y), end = Offset(cropR, y), strokeWidth = 1f)
                        }
                        
                        val handleSize = 24f
                        val handleStroke = 3f
                        
                        // 绘制四个角的L形手柄
                        drawLine(Color.White, Offset(cropL, cropT), Offset(cropL + handleSize, cropT), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropL, cropT), Offset(cropL, cropT + handleSize), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropR - handleSize, cropT), Offset(cropR, cropT), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropR, cropT), Offset(cropR, cropT + handleSize), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropL, cropB - handleSize), Offset(cropL, cropB), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropL, cropB), Offset(cropL + handleSize, cropB), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropR, cropB - handleSize), Offset(cropR, cropB), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropR - handleSize, cropB), Offset(cropR, cropB), strokeWidth = handleStroke)
                        
                        // 绘制四个角的圆形拖拽手柄（更明显）
                        val handleRadius = 12f
                        drawCircle(
                            color = Color.White,
                            radius = handleRadius,
                            center = Offset(cropL, cropT)
                        )
                        drawCircle(
                            color = Color(0xFF1C1C1E),
                            radius = handleRadius - 3f,
                            center = Offset(cropL, cropT)
                        )
                        
                        drawCircle(
                            color = Color.White,
                            radius = handleRadius,
                            center = Offset(cropR, cropT)
                        )
                        drawCircle(
                            color = Color(0xFF1C1C1E),
                            radius = handleRadius - 3f,
                            center = Offset(cropR, cropT)
                        )
                        
                        drawCircle(
                            color = Color.White,
                            radius = handleRadius,
                            center = Offset(cropL, cropB)
                        )
                        drawCircle(
                            color = Color(0xFF1C1C1E),
                            radius = handleRadius - 3f,
                            center = Offset(cropL, cropB)
                        )
                        
                        drawCircle(
                            color = Color.White,
                            radius = handleRadius,
                            center = Offset(cropR, cropB)
                        )
                        drawCircle(
                            color = Color(0xFF1C1C1E),
                            radius = handleRadius - 3f,
                            center = Offset(cropR, cropB)
                        )
                        
                        // 绘制四条边中点的拖拽手柄
                        val edgeHandleWidth = 40f
                        val edgeHandleHeight = 6f
                        
                        // 上边中点
                        val topMidX = (cropL + cropR) / 2f
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(topMidX - edgeHandleWidth / 2f, cropT - edgeHandleHeight / 2f),
                            size = Size(edgeHandleWidth, edgeHandleHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                        
                        // 下边中点
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(topMidX - edgeHandleWidth / 2f, cropB - edgeHandleHeight / 2f),
                            size = Size(edgeHandleWidth, edgeHandleHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                        
                        // 左边中点
                        val leftMidY = (cropT + cropB) / 2f
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(cropL - edgeHandleHeight / 2f, leftMidY - edgeHandleWidth / 2f),
                            size = Size(edgeHandleHeight, edgeHandleWidth),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                        
                        // 右边中点
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(cropR - edgeHandleHeight / 2f, leftMidY - edgeHandleWidth / 2f),
                            size = Size(edgeHandleHeight, edgeHandleWidth),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                    }
                    }
                }
            }
        }
    }
}

private fun computeImageRect(
    containerW: Float,
    containerH: Float,
    imageW: Float,
    imageH: Float,
    paddingPx: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Rect {
    if (containerW <= 0f || containerH <= 0f || imageW <= 0f || imageH <= 0f) {
        return Rect(0f, 0f, containerW, containerH)
    }
    
    // 考虑padding，计算可用空间
    val availableW = containerW - paddingPx * 2
    val availableH = containerH - paddingPx * 2
    
    // 计算图片在可用空间内的显示尺寸
    val baseScale = kotlin.math.min(availableW / imageW, availableH / imageH)
    val baseW = imageW * baseScale
    val baseH = imageH * baseScale
    
    val scaledW = baseW * scale
    val scaledH = baseH * scale
    
    // 图片中心位置（考虑padding偏移）
    val centerX = containerW / 2f + offsetX
    val centerY = containerH / 2f + offsetY
    
    val left = centerX - scaledW / 2f
    val top = centerY - scaledH / 2f
    
    return Rect(left, top, left + scaledW, top + scaledH)
}

private fun normalizeRotation(deg: Float): Float {
    var r = deg % 360f
    if (r > 180f) r -= 360f
    if (r < -180f) r += 360f
    return r
}

private fun snapRotation(deg: Float, threshold: Float = 2f): Float {
    val targets = floatArrayOf(-90f, -45f, 0f, 45f, 90f)
    var closest = deg
    var min = Float.MAX_VALUE
    for (t in targets) {
        val d = kotlin.math.abs(deg - t)
        if (d < min) { min = d; closest = t }
    }
    return if (min <= threshold) closest else deg
}

private fun formatAngle(deg: Float): String {
    return String.format("%.1f°", deg)
}

private enum class CropDragMode {
    None, Move, Left, Right, Top, Bottom, TopLeft, TopRight, BottomLeft, BottomRight
}

private fun determineDragMode(point: Offset, l: Float, t: Float, r: Float, b: Float, thr: Float): CropDragMode {
    val nearLeft = kotlin.math.abs(point.x - l) <= thr && point.y >= t - thr && point.y <= b + thr
    val nearRight = kotlin.math.abs(point.x - r) <= thr && point.y >= t - thr && point.y <= b + thr
    val nearTop = kotlin.math.abs(point.y - t) <= thr && point.x >= l - thr && point.x <= r + thr
    val nearBottom = kotlin.math.abs(point.y - b) <= thr && point.x >= l - thr && point.x <= r + thr
    val nearTL = kotlin.math.abs(point.x - l) <= thr && kotlin.math.abs(point.y - t) <= thr
    val nearTR = kotlin.math.abs(point.x - r) <= thr && kotlin.math.abs(point.y - t) <= thr
    val nearBL = kotlin.math.abs(point.x - l) <= thr && kotlin.math.abs(point.y - b) <= thr
    val nearBR = kotlin.math.abs(point.x - r) <= thr && kotlin.math.abs(point.y - b) <= thr
    val inside = point.x >= l && point.x <= r && point.y >= t && point.y <= b
    
    return when {
        nearTL -> CropDragMode.TopLeft
        nearTR -> CropDragMode.TopRight
        nearBL -> CropDragMode.BottomLeft
        nearBR -> CropDragMode.BottomRight
        nearLeft -> CropDragMode.Left
        nearRight -> CropDragMode.Right
        nearTop -> CropDragMode.Top
        nearBottom -> CropDragMode.Bottom
        inside -> CropDragMode.Move
        else -> CropDragMode.None
    }
}
