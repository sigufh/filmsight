package com.filmtracker.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmtracker.app.ui.theme.*

/**
 * 首页 - 像素蛋糕风格
 * 
 * 三大功能模块：
 * 1. 数字暗房（胶卷模式）
 * 2. 专业修图（原有功能）
 * 3. AI 仿色（待实现）
 * 
 * 设计特点：
 * - 大卡片布局
 * - 渐变背景
 * - 微动画效果
 * - 统一配色方案
 */
@Composable
fun HomeScreen(
    onFilmModeClick: () -> Unit,
    onProModeClick: () -> Unit,
    onAIColorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FilmWarmBeige,
                        Color(0xFFF8F4EC),
                        FilmWarmBeige.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // 品牌标题
            BrandHeader()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 功能卡片
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 数字暗房（胶卷模式）
                FeatureCard(
                    title = "数字暗房",
                    subtitle = "胶卷仿拍 · 反转片质感",
                    icon = "🎞",
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            FilmCaramelOrange.copy(alpha = 0.9f),
                            FilmCaramelOrange.copy(alpha = 0.7f)
                        )
                    ),
                    onClick = onFilmModeClick
                )
                
                // 专业修图
                FeatureCard(
                    title = "专业修图",
                    subtitle = "RAW 处理 · 完整调色",
                    icon = "🎨",
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            FilmMilkyBlue.copy(alpha = 0.9f),
                            FilmMilkyBlue.copy(alpha = 0.7f)
                        )
                    ),
                    onClick = onProModeClick
                )
                
                // AI 仿色（待实现）
                FeatureCard(
                    title = "AI 仿色",
                    subtitle = "智能分析 · 一键调色",
                    icon = "✨",
                    gradient = Brush.linearGradient(
                        colors = listOf(
                            FilmMintGreen.copy(alpha = 0.9f),
                            FilmMintGreen.copy(alpha = 0.7f)
                        )
                    ),
                    onClick = onAIColorClick,
                    comingSoon = true
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 底部信息
            FooterInfo()
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 品牌标题
 */
@Composable
private fun BrandHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Logo 动画
        val infiniteTransition = rememberInfiniteTransition(label = "logo")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logo_scale"
        )
        
        Text(
            text = "🎞",
            fontSize = 64.sp,
            modifier = Modifier.scale(scale)
        )
        
        Text(
            text = "FilmSight",
            fontSize = 36.sp,
            fontWeight = FontWeight.Light,
            color = FilmInkBlack,
            letterSpacing = 2.sp
        )
        
        Text(
            text = "数字暗房 · 胶片美学",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = FilmDarkGray,
            letterSpacing = 1.sp
        )
    }
}

/**
 * 功能卡片
 */
@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: String,
    gradient: Brush,
    onClick: () -> Unit,
    comingSoon: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .scale(scale)
            .clickable(
                enabled = !comingSoon,
                onClick = {
                    if (!comingSoon) {
                        onClick()
                    }
                }
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧文字
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = FilmWhite
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        color = FilmWhite.copy(alpha = 0.9f),
                        letterSpacing = 0.5.sp
                    )
                }
                
                // 右侧图标
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (comingSoon) {
                        // 即将推出标签
                        Surface(
                            color = FilmWhite.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "即将推出",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = FilmWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Text(
                            text = icon,
                            fontSize = 48.sp
                        )
                    }
                }
            }
            
            // 装饰性渐变叠加
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * 底部信息
 */
@Composable
private fun FooterInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "专业级 RAW 图像处理",
            fontSize = 12.sp,
            color = FilmDarkGray.copy(alpha = 0.6f),
            fontWeight = FontWeight.Light
        )
        Text(
            text = "非破坏性编辑 · 胶片银盐模拟",
            fontSize = 12.sp,
            color = FilmDarkGray.copy(alpha = 0.6f),
            fontWeight = FontWeight.Light
        )
    }
}
