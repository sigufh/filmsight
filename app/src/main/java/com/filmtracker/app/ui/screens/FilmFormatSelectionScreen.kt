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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import com.filmtracker.app.domain.model.FilmFormat
import com.filmtracker.app.domain.model.FilmStock
import com.filmtracker.app.domain.model.FilmType
import com.filmtracker.app.ui.screens.components.AIDialogPanel
import com.filmtracker.app.ui.theme.CornerRadius
import com.filmtracker.app.ui.theme.Spacing

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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = "FilmSight",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 显示帮助 */ }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "帮助",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Spacer(modifier = Modifier.height(Spacing.md))

                // 标题
                Text(
                    text = "选择画幅",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // 135 和 120 画幅选择（左右排列）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
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
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
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
                
                Spacer(modifier = Modifier.height(Spacing.sm))

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
                        .height(Spacing.xxl + Spacing.sm),
                    shape = RoundedCornerShape(CornerRadius.xl),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "下一步 · 选择张数",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // AI 助手对话框（屏幕下半部分）
                AIDialogPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.xxl * 6),
                    showQuickActions = false
                )

                Spacer(modifier = Modifier.height(Spacing.md))
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
    displayText: String? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        label = "card_scale"
    )

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    ElevatedCard(
        modifier = modifier
            .height(Spacing.xxl * 2 + Spacing.xs)
            .scale(scale),
        onClick = onClick,
        shape = RoundedCornerShape(CornerRadius.md),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) Spacing.sm else Spacing.xs
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = displayText ?: format.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            contentColor.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选中",
                        tint = contentColor,
                        modifier = Modifier.size(Spacing.lg)
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

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.xxl * 3)
            .scale(scale),
        onClick = onClick,
        shape = RoundedCornerShape(CornerRadius.lg),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) Spacing.sm else Spacing.xs
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = format.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor
                )
                Text(
                    text = subtitle ?: format.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        contentColor.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = contentColor,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(Spacing.xl)
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
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.sm))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = format.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
            Text(
                text = format.description,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    contentColor.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = "胶卷型号（可选）",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val filmStocks = FilmStock.getAllFilms()

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
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(horizontal = Spacing.lg)
        ) {
            items(filmStocks.size) { index ->
                FilmStockIcon(
                    filmStock = filmStocks[index],
                    isSelected = selectedFilmStock == filmStocks[index],
                    onClick = { onFilmStockSelected(filmStocks[index]) }
                )
            }
        }

        if (selectedFilmStock != null) {
            FilmStockInfoCard(selectedFilmStock = selectedFilmStock)
        } else {
            Text(
                text = "← 左右滑动选择胶卷型号",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .width(Spacing.xxl * 2 + Spacing.xs)
            .scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        ElevatedCard(
            modifier = Modifier.size(Spacing.xxl + Spacing.xl),
            onClick = onClick,
            shape = RoundedCornerShape(CornerRadius.md),
            colors = CardDefaults.elevatedCardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = if (isSelected) Spacing.sm else Spacing.xs
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = filmStock.icon,
                    contentDescription = filmStock.displayName,
                    modifier = Modifier.size(Spacing.xl),
                    tint = contentColor
                )
            }
        }

        Text(
            text = filmStock.displayName.take(6),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
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
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    )
                )
            )
    )
}

/**
 * 胶卷信息卡片
 */
@Composable
private fun FilmStockInfoCard(
    selectedFilmStock: FilmStock,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        shape = RoundedCornerShape(CornerRadius.md),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFilmStock.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(CornerRadius.xs),
                    color = when (selectedFilmStock.type) {
                        FilmType.NEGATIVE -> MaterialTheme.colorScheme.tertiaryContainer
                        FilmType.REVERSAL -> MaterialTheme.colorScheme.secondaryContainer
                        FilmType.CINEMA -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = selectedFilmStock.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (selectedFilmStock.type) {
                            FilmType.NEGATIVE -> MaterialTheme.colorScheme.onTertiaryContainer
                            FilmType.REVERSAL -> MaterialTheme.colorScheme.onSecondaryContainer
                            FilmType.CINEMA -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    )
                }
            }

            Text(
                text = selectedFilmStock.englishName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = selectedFilmStock.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
