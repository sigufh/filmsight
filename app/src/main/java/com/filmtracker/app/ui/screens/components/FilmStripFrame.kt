package com.filmtracker.app.ui.screens.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmtracker.app.ui.screens.ImageInfo
import com.filmtracker.app.ui.theme.*

/**
 * 胶卷条纹框架组件
 * 
 * 模拟真实反转片的视觉效果：
 * - 上下胶片齿孔
 * - 黑色边框
 * - 图片白边
 * - 帧编号标记
 * - 反转片色调（高饱和、低对比）
 * - 按画幅比例居中裁剪图片
 * - 显示修改指示器
 */
@Composable
fun FilmStripFrame(
    bitmap: Bitmap?,
    frameNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.5f,  // 画幅比例（宽/高）
    frameWidth: Dp = 280.dp,
    isModified: Boolean = false  // 是否已修改
) {
    // 根据画幅比例计算帧高度
    val frameHeight = frameWidth / aspectRatio
    
    Box(
        modifier = modifier
            .width(frameWidth)
            .height(frameHeight + 60.dp)  // 额外空间给齿孔
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 上方齿孔条
            FilmSprocketStrip(
                width = frameWidth,
                isTop = true
            )
            
            // 图片帧区域
            Box(
                modifier = Modifier
                    .width(frameWidth)
                    .height(frameHeight)
                    .background(Color.Black)  // 胶片黑边
                    .padding(8.dp)  // 黑边宽度
            ) {
                // 白色内边框（反转片特征）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FilmWhite)
                        .padding(4.dp)  // 白边宽度
                ) {
                    // 图片内容
                    if (bitmap != null && !bitmap.isRecycled) {
                        val imageBitmap = remember(bitmap) {
                            try {
                                if (!bitmap.isRecycled) {
                                    bitmap.asImageBitmap()
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
                                contentDescription = "Frame $frameNumber",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(2.dp)),
                                contentScale = ContentScale.Crop  // 居中裁剪
                            )
                            
                            // 反转片色调叠加层（轻微增强饱和度）
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(FilmCaramelOrange.copy(alpha = 0.05f))
                            )
                        }
                    } else {
                        // 占位符
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(FilmLightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Frame $frameNumber",
                                color = FilmDarkGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    // 选中指示器
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 3.dp,
                                    color = FilmCaramelOrange,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    
                    // 修改指示器（右上角）
                    if (isModified) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            color = FilmCaramelOrange,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "已修改",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                
                // 帧编号标记（左下角）
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = String.format("%02d", frameNumber),
                        color = FilmCaramelOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // 下方齿孔条
            FilmSprocketStrip(
                width = frameWidth,
                isTop = false
            )
        }
    }
}

/**
 * 胶片齿孔条
 */
@Composable
private fun FilmSprocketStrip(
    width: Dp,
    isTop: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(width)
            .height(30.dp)
            .background(Color.Black)
    ) {
        val sprocketWidth = 20.dp.toPx()
        val sprocketHeight = 16.dp.toPx()
        val sprocketSpacing = 30.dp.toPx()
        val numSprockets = (size.width / sprocketSpacing).toInt()
        
        // 绘制齿孔
        for (i in 0 until numSprockets) {
            val x = i * sprocketSpacing + sprocketSpacing / 2 - sprocketWidth / 2
            val y = if (isTop) size.height - sprocketHeight - 4.dp.toPx() else 4.dp.toPx()
            
            // 齿孔矩形（带圆角）
            drawRoundRect(
                color = Color(0xFF2A2A2A),  // 深灰色齿孔
                topLeft = Offset(x, y),
                size = Size(sprocketWidth, sprocketHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
        
        // 绘制边缘线（模拟胶片边缘）
        drawLine(
            color = Color(0xFF1A1A1A),
            start = Offset(0f, if (isTop) size.height else 0f),
            end = Offset(size.width, if (isTop) size.height else 0f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * 胶卷信息标记（侧边文字）
 */
@Composable
fun FilmStripInfoMarker(
    filmStock: String,
    frameCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(30.dp)
            .height(200.dp)
            .background(Color.Black)
    ) {
        // 绘制胶卷型号文字（竖向）
        // 注：实际实现需要旋转文字，这里简化为装饰线条
        
        // 顶部装饰线
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(size.width / 2, 10.dp.toPx()),
            end = Offset(size.width / 2, 30.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
        
        // 中间点
        repeat(5) { i ->
            val y = 50.dp.toPx() + i * 20.dp.toPx()
            drawCircle(
                color = FilmCaramelOrange.copy(alpha = 0.5f),
                radius = 2.dp.toPx(),
                center = Offset(size.width / 2, y)
            )
        }
        
        // 底部装饰线
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(size.width / 2, size.height - 30.dp.toPx()),
            end = Offset(size.width / 2, size.height - 10.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * 胶卷端部（卷轴效果）
 */
@Composable
fun FilmStripEnd(
    isStart: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(60.dp)
            .height(260.dp)
            .background(
                color = Color(0xFF1A1A1A),
                shape = if (isStart) {
                    RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp)
                } else {
                    RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // 卷轴纹理
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = 8.dp.toPx()
            val numLines = (size.height / lineSpacing).toInt()
            
            for (i in 0 until numLines) {
                val y = i * lineSpacing
                val alpha = if (i % 2 == 0) 0.3f else 0.1f
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        
        // 中心圆（卷轴孔）
        Canvas(modifier = Modifier.size(30.dp)) {
            drawCircle(
                color = Color.Black,
                radius = 15.dp.toPx()
            )
            drawCircle(
                color = Color(0xFF2A2A2A),
                radius = 12.dp.toPx()
            )
        }
    }
}
