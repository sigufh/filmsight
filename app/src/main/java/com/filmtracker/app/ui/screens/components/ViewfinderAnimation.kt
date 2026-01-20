package com.filmtracker.app.ui.screens.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmtracker.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * 取景器动画组件
 * 
 * 模拟胶片相机取景器的视觉效果：
 * - 景色掠过动画（循环播放）
 * - 轻微胶片颗粒
 * - 边缘暗角
 * - 取景框标记
 * 
 * 动画流程：
 * 1. 显示取景框
 * 2. 播放景色掠过动画（循环）
 * 3. 外部控制停止
 */
@Composable
fun ViewfinderAnimation(
    isPlaying: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationPhase by remember { mutableStateOf(ViewfinderPhase.IDLE) }
    val infiniteTransition = rememberInfiniteTransition(label = "viewfinder")
    
    // 景色移动动画（循环播放）
    val sceneOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scene_offset"
    )
    
    // 颗粒闪烁动画
    val grainAlpha by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "grain_alpha"
    )
    
    // 控制动画流程 - 简化为只有播放和停止
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animationPhase = ViewfinderPhase.FOCUSING
            delay(300)
            animationPhase = ViewfinderPhase.ANIMATING
            // 不再自动停止，由外部控制
        } else {
            animationPhase = ViewfinderPhase.IDLE
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f)  // 135 胶卷比例
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(
                width = 3.dp,
                color = FilmCaramelOrange,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when (animationPhase) {
            ViewfinderPhase.IDLE -> {
                // 空闲状态 - 显示取景框
                ViewfinderFrame()
            }
            ViewfinderPhase.FOCUSING -> {
                // 对焦状态
                ViewfinderFrame()
                Text(
                    text = "对焦中...",
                    color = FilmCaramelOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light
                )
            }
            ViewfinderPhase.ANIMATING -> {
                // 动画播放状态 - 持续循环直到外部停止
                ViewfinderScene(
                    offset = sceneOffset,
                    grainAlpha = grainAlpha
                )
                ViewfinderFrame()
            }
        }
    }
}

/**
 * 取景器动画阶段
 */
private enum class ViewfinderPhase {
    IDLE,       // 空闲
    FOCUSING,   // 对焦中
    ANIMATING   // 动画播放（移除 COMPLETE 状态）
}

/**
 * 取景框标记
 */
@Composable
private fun BoxScope.ViewfinderFrame() {
    // 四角标记
    val cornerSize = 40.dp
    val cornerThickness = 2.dp
    
    // 左上角
    Canvas(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(16.dp)
            .size(cornerSize)
    ) {
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(0f, 0f),
            end = Offset(cornerSize.toPx(), 0f),
            strokeWidth = cornerThickness.toPx()
        )
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(0f, 0f),
            end = Offset(0f, cornerSize.toPx()),
            strokeWidth = cornerThickness.toPx()
        )
    }
    
    // 右上角
    Canvas(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)
            .size(cornerSize)
    ) {
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(0f, 0f),
            end = Offset(cornerSize.toPx(), 0f),
            strokeWidth = cornerThickness.toPx()
        )
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(cornerSize.toPx(), 0f),
            end = Offset(cornerSize.toPx(), cornerSize.toPx()),
            strokeWidth = cornerThickness.toPx()
        )
    }
    
    // 左下角
    Canvas(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp)
            .size(cornerSize)
    ) {
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(0f, cornerSize.toPx()),
            end = Offset(cornerSize.toPx(), cornerSize.toPx()),
            strokeWidth = cornerThickness.toPx()
        )
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(0f, 0f),
            end = Offset(0f, cornerSize.toPx()),
            strokeWidth = cornerThickness.toPx()
        )
    }
    
    // 右下角
    Canvas(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
            .size(cornerSize)
    ) {
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(0f, cornerSize.toPx()),
            end = Offset(cornerSize.toPx(), cornerSize.toPx()),
            strokeWidth = cornerThickness.toPx()
        )
        drawLine(
            color = FilmCaramelOrange,
            start = Offset(cornerSize.toPx(), 0f),
            end = Offset(cornerSize.toPx(), cornerSize.toPx()),
            strokeWidth = cornerThickness.toPx()
        )
    }
    
    // 中心对焦点
    Canvas(
        modifier = Modifier
            .align(Alignment.Center)
            .size(24.dp)
    ) {
        drawCircle(
            color = FilmCaramelOrange,
            radius = 12.dp.toPx(),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = FilmCaramelOrange,
            radius = 2.dp.toPx()
        )
    }
}

/**
 * 景色动画（模拟景色掠过）
 */
@Composable
private fun ViewfinderScene(
    offset: Float,
    grainAlpha: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 绘制模拟景色（渐变条纹）
        drawSceneStripes(offset)
        
        // 绘制胶片颗粒
        drawFilmGrain(grainAlpha)
        
        // 绘制边缘暗角
        drawVignette()
    }
}

/**
 * 绘制景色条纹（模拟景色移动）
 */
private fun DrawScope.drawSceneStripes(offset: Float) {
    val stripeWidth = 100f
    val numStripes = (size.width / stripeWidth).toInt() + 2
    
    for (i in 0 until numStripes) {
        val x = (i * stripeWidth - (offset % stripeWidth))
        val brightness = (sin(i * 0.5f) + 1f) / 2f
        val color = Color(
            red = 0.3f + brightness * 0.3f,
            green = 0.4f + brightness * 0.2f,
            blue = 0.2f + brightness * 0.3f
        )
        
        drawRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = androidx.compose.ui.geometry.Size(stripeWidth, size.height)
        )
    }
}

/**
 * 绘制胶片颗粒（优化版 - 减少颗粒数量）
 */
private fun DrawScope.drawFilmGrain(alpha: Float) {
    val random = Random(System.currentTimeMillis() / 100)
    val grainDensity = 0.005f  // 降低颗粒密度（从0.02改为0.005）
    val numGrains = (size.width * size.height * grainDensity).toInt()
    
    repeat(numGrains) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val grainSize = random.nextFloat() * 2f + 1f
        
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = grainSize,
            center = Offset(x, y)
        )
    }
}

/**
 * 绘制边缘暗角
 */
private fun DrawScope.drawVignette() {
    val gradient = Brush.radialGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.5f)
        ),
        center = Offset(size.width / 2, size.height / 2),
        radius = size.width * 0.8f
    )
    
    drawRect(brush = gradient)
}
