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
    showRotationDial: Boolean = false,
    showCropOverlay: Boolean = false
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var dragMode by remember { mutableStateOf(CropDragMode.None) }
    var cropStartLeft by remember { mutableStateOf(0f) }
    var cropStartTop by remember { mutableStateOf(0f) }
    var cropStartRight by remember { mutableStateOf(1f) }
    var cropStartBottom by remember { mutableStateOf(1f) }
    var totalDx by remember { mutableStateOf(0f) }
    var totalDy by remember { mutableStateOf(0f) }
    var dialDragging by remember { mutableStateOf(false) }
    var dialStartRotation by remember { mutableStateOf(0f) }
    var dialAccumX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val dialHeightPx = with(density) { 120.dp.toPx() }
    val marginPx = with(density) { if (showCropOverlay) 48.dp.toPx() else 0f }

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
        } else if (processedImage != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(processedImage)
                    .build(),
                contentDescription = "处理后的图像",
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (showCropOverlay) Modifier.padding(48.dp)
                        else Modifier
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

            if (showCropOverlay) {
                val currentLeft = cropLeft.coerceIn(0f, 1f)
                val currentTop = cropTop.coerceIn(0f, 1f)
                val currentRight = cropRight.coerceIn(0f, 1f)
                val currentBottom = cropBottom.coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { containerSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val ch = containerSize.height.toFloat()
                                    if (showRotationDial && offset.y >= ch - dialHeightPx) {
                                        dialDragging = true
                                        dialStartRotation = rotation
                                        dialAccumX = 0f
                                        dragMode = CropDragMode.None
                                    } else {
                                        val ir = computeImageRect(
                                            containerSize.width.toFloat(),
                                            containerSize.height.toFloat(),
                                            processedImage.width.toFloat(),
                                            processedImage.height.toFloat(),
                                            marginPx,
                                            scale,
                                            offsetX,
                                            offsetY
                                        )
                                        val cropL = ir.left + currentLeft * ir.width
                                        val cropT = ir.top + currentTop * ir.height
                                        val cropR = ir.left + currentRight * ir.width
                                        val cropB = ir.top + currentBottom * ir.height
                                        val thr = 32f
                                        dragMode = determineDragMode(offset, cropL, cropT, cropR, cropB, thr)
                                        cropStartLeft = currentLeft
                                        cropStartTop = currentTop
                                        cropStartRight = currentRight
                                        cropStartBottom = currentBottom
                                        totalDx = 0f
                                        totalDy = 0f
                                    }
                                },
                                onDrag = { _, dragAmount ->
                                    if (dialDragging) {
                                        dialAccumX += dragAmount.x
                                        val sensitivity = 0.25f
                                        val nr = normalizeRotation(dialStartRotation + dialAccumX * sensitivity)
                                        onRotationChange?.invoke(nr)
                                    } else {
                                        val ir = computeImageRect(
                                            containerSize.width.toFloat(),
                                            containerSize.height.toFloat(),
                                            processedImage.width.toFloat(),
                                            processedImage.height.toFloat(),
                                            marginPx,
                                            scale,
                                            offsetX,
                                            offsetY
                                        )
                                        when (dragMode) {
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
                                                onCropChange?.invoke(l, t, r, b)
                                            }
                                            CropDragMode.Left -> {
                                                totalDx += dragAmount.x
                                                val l = (cropStartLeft + totalDx / ir.width).coerceIn(0f, cropStartRight - 0.1f)
                                                onCropChange?.invoke(l, cropStartTop, cropStartRight, cropStartBottom)
                                            }
                                            CropDragMode.Right -> {
                                                totalDx += dragAmount.x
                                                val r = (cropStartRight + totalDx / ir.width).coerceIn(cropStartLeft + 0.1f, 1f)
                                                onCropChange?.invoke(cropStartLeft, cropStartTop, r, cropStartBottom)
                                            }
                                            CropDragMode.Top -> {
                                                totalDy += dragAmount.y
                                                val t = (cropStartTop + totalDy / ir.height).coerceIn(0f, cropStartBottom - 0.1f)
                                                onCropChange?.invoke(cropStartLeft, t, cropStartRight, cropStartBottom)
                                            }
                                            CropDragMode.Bottom -> {
                                                totalDy += dragAmount.y
                                                val b = (cropStartBottom + totalDy / ir.height).coerceIn(cropStartTop + 0.1f, 1f)
                                                onCropChange?.invoke(cropStartLeft, cropStartTop, cropStartRight, b)
                                            }
                                            CropDragMode.TopLeft -> {
                                                totalDx += dragAmount.x
                                                totalDy += dragAmount.y
                                                val l = (cropStartLeft + totalDx / ir.width).coerceIn(0f, cropStartRight - 0.1f)
                                                val t = (cropStartTop + totalDy / ir.height).coerceIn(0f, cropStartBottom - 0.1f)
                                                onCropChange?.invoke(l, t, cropStartRight, cropStartBottom)
                                            }
                                            CropDragMode.TopRight -> {
                                                totalDx += dragAmount.x
                                                totalDy += dragAmount.y
                                                val r = (cropStartRight + totalDx / ir.width).coerceIn(cropStartLeft + 0.1f, 1f)
                                                val t = (cropStartTop + totalDy / ir.height).coerceIn(0f, cropStartBottom - 0.1f)
                                                onCropChange?.invoke(cropStartLeft, t, r, cropStartBottom)
                                            }
                                            CropDragMode.BottomLeft -> {
                                                totalDx += dragAmount.x
                                                totalDy += dragAmount.y
                                                val l = (cropStartLeft + totalDx / ir.width).coerceIn(0f, cropStartRight - 0.1f)
                                                val b = (cropStartBottom + totalDy / ir.height).coerceIn(cropStartTop + 0.1f, 1f)
                                                onCropChange?.invoke(l, cropStartTop, cropStartRight, b)
                                            }
                                            CropDragMode.BottomRight -> {
                                                totalDx += dragAmount.x
                                                totalDy += dragAmount.y
                                                val r = (cropStartRight + totalDx / ir.width).coerceIn(cropStartLeft + 0.1f, 1f)
                                                val b = (cropStartBottom + totalDy / ir.height).coerceIn(cropStartTop + 0.1f, 1f)
                                                onCropChange?.invoke(cropStartLeft, cropStartTop, r, b)
                                            }
                                            CropDragMode.None -> {}
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (dialDragging) {
                                        val snapped = snapRotation(rotation)
                                        onRotationChange?.invoke(snapped)
                                        dialDragging = false
                                    }
                                    dragMode = CropDragMode.None
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val ir = computeImageRect(w, h, processedImage.width.toFloat(), processedImage.height.toFloat(), marginPx, scale, offsetX, offsetY)
                        
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
                        drawLine(Color.White, Offset(cropL, cropT), Offset(cropL + handleSize, cropT), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropL, cropT), Offset(cropL, cropT + handleSize), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropR - handleSize, cropT), Offset(cropR, cropT), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropR, cropT), Offset(cropR, cropT + handleSize), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropL, cropB - handleSize), Offset(cropL, cropB), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropL, cropB), Offset(cropL + handleSize, cropB), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropR, cropB - handleSize), Offset(cropR, cropB), strokeWidth = handleStroke)
                        drawLine(Color.White, Offset(cropR - handleSize, cropB), Offset(cropR, cropB), strokeWidth = handleStroke)
                        
                        if (showRotationDial) {
                            val cx = w / 2f
                            val radius = kotlin.math.min(w / 2f - 24.dp.toPx(), 120.dp.toPx())
                            val cy = h - 24.dp.toPx()
                            var ang = -90
                            while (ang <= 90) {
                                val rad = Math.toRadians((ang + 90).toDouble())
                                val c = kotlin.math.cos(rad).toFloat()
                                val s = kotlin.math.sin(rad).toFloat()
                                val len = if (ang % 15 == 0) 16f else 8f
                                val px = cx + radius * c
                                val py = cy - radius * s
                                val ix = cx + (radius - len) * c
                                val iy = cy - (radius - len) * s
                                drawLine(Color.White.copy(alpha = if (ang % 15 == 0) 0.9f else 0.5f), start = Offset(ix, iy), end = Offset(px, py), strokeWidth = if (ang % 15 == 0) 2f else 1f)
                                ang += 5
                            }
                            val clamped = rotation.coerceIn(-90f, 90f)
                            val rad = Math.toRadians((clamped + 90).toDouble())
                            val c = kotlin.math.cos(rad).toFloat()
                            val s = kotlin.math.sin(rad).toFloat()
                            val px = cx + radius * c
                            val py = cy - radius * s
                            val ix = cx + (radius - 24f) * c
                            val iy = cy - (radius - 24f) * s
                            drawLine(Color.White, start = Offset(ix, iy), end = Offset(px, py), strokeWidth = 3f)
                        }
                    }
                    if (showRotationDial) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = formatAngle(rotation), color = Color.White)
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
    marginPx: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Rect {
    if (containerW <= 0f || containerH <= 0f || imageW <= 0f || imageH <= 0f) {
        return Rect(0f, 0f, containerW, containerH)
    }
    
    val availableW = containerW - marginPx * 2
    val availableH = containerH - marginPx * 2
    val baseScale = kotlin.math.min(availableW / imageW, availableH / imageH)
    val baseW = imageW * baseScale
    val baseH = imageH * baseScale
    
    val scaledW = baseW * scale
    val scaledH = baseH * scale
    
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
