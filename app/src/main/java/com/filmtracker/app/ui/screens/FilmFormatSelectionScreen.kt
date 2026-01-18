package com.filmtracker.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
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
import com.filmtracker.app.domain.model.FilmFormat
import com.filmtracker.app.domain.model.FilmStock
import com.filmtracker.app.domain.model.FilmType
import com.filmtracker.app.ui.screens.components.AIDialogPanel
import com.filmtracker.app.ui.theme.*

/**
 * 画幅选择页（胶卷仿拍流程第一步）
 * 
 * 功能：
 * - 选择 135 或 120 胶卷
 * - 120 胶卷可展开选择具体画幅（6x6/645/6x7/6x9）
 * - 选择胶卷型号（负片/反转片/电影卷）
 * 
 * 设计风格：
 * - Ins 风格轻复古
 * - 焦糖橘主色调
 * - 胶片压纹质感
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmFormatSelectionScreen(
    onFormatSelected: (FilmFormat, FilmStock?) -> Unit,
    onBack: (() -> Unit)? = null,  // 返回回调
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf<FilmFormat?>(null) }
    var selectedFilmStock by remember { mutableStateOf<FilmStock?>(null) }
    var show120Expansion by remember { mutableStateOf(false) }
    var showFilmStockDropdown by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            // 磨砂玻璃效果导航栏
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 品牌 logo（简约胶片图标）
                        Text(
                            text = "🎞",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "FilmSight",
                            fontWeight = FontWeight.Light,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    // 返回按钮（如果提供了 onBack 回调）
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = FilmInkBlack
                            )
                        }
                    }
                },
                actions = {
                    // 帮助按钮
                    IconButton(onClick = { /* TODO: 显示帮助 */ }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "帮助",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FilmWhiteGlass,
                    titleContentColor = FilmInkBlack
                )
            )
        },
        containerColor = FilmWarmBeige
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 背景胶片齿孔纹理
            FilmSprocketBackground()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // 标题
                Text(
                    text = "选择画幅",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FilmInkBlack,
                    fontWeight = FontWeight.Light
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 135 和 120 画幅选择（左右排列）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 135 胶卷卡片
                    FilmFormatCompactCard(
                        format = FilmFormat.Film135,
                        isSelected = selectedFormat == FilmFormat.Film135,
                        onClick = {
                            selectedFormat = FilmFormat.Film135
                            show120Expansion = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 120 胶卷卡片
                    FilmFormatCompactCard(
                        format = FilmFormat.Film120_6x6,
                        isSelected = selectedFormat in FilmFormat.get120Formats(),
                        onClick = {
                            show120Expansion = !show120Expansion
                        },
                        subtitle = "点击展开",
                        displayText = "120",  // 只显示 "120"
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 120 画幅展开选项
                if (show120Expansion) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilmFormat.get120Formats().forEach { format ->
                            Film120SubFormatOption(
                                format = format,
                                isSelected = selectedFormat == format,
                                onClick = { selectedFormat = format }
                            )
                        }
                    }
                }
                
                // 胶卷型号选择
                FilmStockSelector(
                    selectedFilmStock = selectedFilmStock,
                    onFilmStockSelected = { selectedFilmStock = it },
                    expanded = showFilmStockDropdown,
                    onExpandedChange = { showFilmStockDropdown = it }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 下一步按钮
                Button(
                    onClick = {
                        selectedFormat?.let { format ->
                            onFormatSelected(format, selectedFilmStock)
                        }
                    },
                    enabled = selectedFormat != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FilmCaramelOrange,
                        contentColor = FilmWhite,
                        disabledContainerColor = FilmLightGray,
                        disabledContentColor = FilmDarkGray
                    )
                ) {
                    Text(
                        text = "下一步 · 选择张数",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // AI 助手对话框（屏幕下半部分）
                AIDialogPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    showQuickActions = false
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 紧凑版胶卷画幅卡片（用于左右排列）
 */
@Composable
private fun FilmFormatCompactCard(
    format: FilmFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    displayText: String? = null  // 可选的自定义显示文字
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .height(100.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FilmCaramelOrange else FilmWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayText ?: format.displayName,  // 使用自定义文字或默认名称
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) FilmWhite else FilmInkBlack
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        color = if (isSelected) FilmWhite.copy(alpha = 0.9f) else FilmDarkGray
                    )
                }
                
                // 选中指示器
                if (isSelected) {
                    Text(
                        text = "✓",
                        fontSize = 20.sp,
                        color = FilmWhite
                    )
                }
            }
        }
    }
}

/**
 * 胶卷画幅卡片
 */
@Composable
private fun FilmFormatCard(
    format: FilmFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        label = "card_scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FilmCaramelOrange else FilmWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = format.displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) FilmWhite else FilmInkBlack
                )
                Text(
                    text = subtitle ?: format.description,
                    fontSize = 14.sp,
                    color = if (isSelected) FilmWhite.copy(alpha = 0.9f) else FilmDarkGray
                )
            }
            
            // 选中指示器
            if (isSelected) {
                Text(
                    text = "✓",
                    fontSize = 32.sp,
                    color = FilmWhite,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

/**
 * 120 画幅子选项
 */
@Composable
private fun Film120SubFormatOption(
    format: FilmFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) FilmCaramelOrange else FilmWhite)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = format.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) FilmWhite else FilmInkBlack
            )
            Text(
                text = format.description,
                fontSize = 11.sp,
                color = if (isSelected) FilmWhite.copy(alpha = 0.9f) else FilmDarkGray
            )
        }
        
        // 单选按钮
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = FilmWhite,
                unselectedColor = FilmDarkGray
            )
        )
    }
}

/**
 * 胶卷型号选择器（左右滑动选择）
 */
@Composable
private fun FilmStockSelector(
    selectedFilmStock: FilmStock?,
    onFilmStockSelected: (FilmStock) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "胶卷型号（可选）",
            fontSize = 14.sp,
            color = FilmDarkGray,
            fontWeight = FontWeight.Medium
        )
        
        // 使用 HorizontalPager 实现左右滑动
        val filmStocks = FilmStock.getAllFilms()
        val selectedIndex = filmStocks.indexOf(selectedFilmStock).takeIf { it >= 0 } ?: -1
        
        // 滑动选择器
        FilmStockCarousel(
            filmStocks = filmStocks,
            selectedFilmStock = selectedFilmStock,
            onFilmStockSelected = onFilmStockSelected
        )
    }
}

/**
 * 胶卷型号轮播选择器
 */
@Composable
private fun FilmStockCarousel(
    filmStocks: List<FilmStock>,
    selectedFilmStock: FilmStock?,
    onFilmStockSelected: (FilmStock) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 横向滚动的胶卷图标
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            // 所有胶卷型号
            items(filmStocks.size) { index ->
                FilmStockIcon(
                    filmStock = filmStocks[index],
                    isSelected = selectedFilmStock == filmStocks[index],
                    onClick = { onFilmStockSelected(filmStocks[index]) }
                )
            }
        }
        
        // 显示当前选中的胶卷信息
        if (selectedFilmStock != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = FilmWhite
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedFilmStock.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = FilmInkBlack
                        )
                        
                        // 类型标签
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (selectedFilmStock.type) {
                                FilmType.NEGATIVE -> Color(0xFFFFB74D)
                                FilmType.REVERSAL -> Color(0xFF64B5F6)
                                FilmType.CINEMA -> Color(0xFFBA68C8)
                            }
                        ) {
                            Text(
                                text = selectedFilmStock.type.displayName,
                                fontSize = 11.sp,
                                color = FilmWhite,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = selectedFilmStock.englishName,
                        fontSize = 12.sp,
                        color = FilmDarkGray
                    )
                    
                    Text(
                        text = selectedFilmStock.description,
                        fontSize = 13.sp,
                        color = FilmDarkGray.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            // 未选择时的提示
            Text(
                text = "← 左右滑动选择胶卷型号",
                fontSize = 13.sp,
                color = FilmDarkGray.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 胶卷图标（用于轮播选择）
 */
@Composable
private fun FilmStockIcon(
    filmStock: FilmStock,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        label = "icon_scale"
    )
    
    Column(
        modifier = modifier
            .width(100.dp)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 胶卷图标
        Card(
            modifier = Modifier
                .size(80.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) FilmCaramelOrange else FilmWhite
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 6.dp else 2.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 胶卷图标（根据类型显示不同的 emoji）
                Text(
                    text = when (filmStock.type) {
                        FilmType.NEGATIVE -> "📷"
                        FilmType.REVERSAL -> "🎞"
                        FilmType.CINEMA -> "🎬"
                    },
                    fontSize = 36.sp
                )
            }
        }
        
        // 胶卷名称（简短版）
        Text(
            text = filmStock.displayName.take(6),
            fontSize = 11.sp,
            color = if (isSelected) FilmCaramelOrange else FilmDarkGray,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * 胶片齿孔背景纹理
 */
@Composable
private fun FilmSprocketBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FilmWarmBeige,
                        FilmWarmBeige.copy(alpha = 0.95f)
                    )
                )
            )
    )
}
