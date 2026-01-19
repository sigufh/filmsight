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
    onAIAssistantClick: () -> Unit,
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
            
            // 功能卡片 - 田字格布局
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 第一行：数字暗房 + 专业修图
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 数字暗房
                    FeatureCardCompact(
                        title = "数字暗房",
                        subtitle = "胶卷仿拍",
                        icon = "🎞",
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                FilmCaramelOrange.copy(alpha = 0.9f),
                                FilmCaramelOrange.copy(alpha = 0.7f)
                            )
                        ),
                        onClick = onFilmModeClick,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 专业修图
                    FeatureCardCompact(
                        title = "专业修图",
                        subtitle = "RAW 处理",
                        icon = "🎨",
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                FilmMilkyBlue.copy(alpha = 0.9f),
                                FilmMilkyBlue.copy(alpha = 0.7f)
                            )
                        ),
                        onClick = onProModeClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 第二行：AI助手 + AI仿色
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // AI助手
                    FeatureCardCompact(
                        title = "AI 助手",
                        subtitle = "智能对话",
                        icon = "✨",
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                FilmMintGreen.copy(alpha = 0.9f),
                                FilmMintGreen.copy(alpha = 0.7f)
                            )
                        ),
                        onClick = onAIAssistantClick,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // AI仿色
                    FeatureCardCompact(
                        title = "AI 仿色",
                        subtitle = "一键调色",
                        icon = "🎯",
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE8B4D9).copy(alpha = 0.9f),
                                Color(0xFFE8B4D9).copy(alpha = 0.7f)
                            )
                        ),
                        onClick = onAIColorClick,
                        comingSoon = true,
                        modifier = Modifier.weight(1f)
                    )
                }
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
 * 功能卡片 - 紧凑版（田字格布局）
 */
@Composable
private fun FeatureCardCompact(
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
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                enabled = !comingSoon,
                onClick = {
                    if (!comingSoon) {
                        onClick()
                    }
                }
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 3.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 图标
                Text(
                    text = icon,
                    fontSize = 40.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 标题
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FilmWhite
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 副标题或即将推出标签
                if (comingSoon) {
                    Surface(
                        color = FilmWhite.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "即将推出",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = FilmWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = FilmWhite.copy(alpha = 0.9f)
                    )
                }
            }
            
            // 装饰性渐变
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
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
 * 功能卡片 - 原版（保留用于其他地方）
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
