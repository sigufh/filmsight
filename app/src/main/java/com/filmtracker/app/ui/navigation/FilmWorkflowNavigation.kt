package com.filmtracker.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.filmtracker.app.domain.model.FilmFormat
import com.filmtracker.app.domain.model.FilmStock
import com.filmtracker.app.ui.screens.FilmFormatSelectionScreen
import com.filmtracker.app.ui.screens.FilmCountSelectionScreen
import com.filmtracker.app.ui.screens.FilmGridPreviewScreen
import com.filmtracker.app.ui.screens.FilmProcessingScreen
import com.filmtracker.app.ui.screens.ImageInfo
import com.filmtracker.app.ui.theme.FilmTrackerTheme
import com.filmtracker.app.ui.viewmodel.FilmWorkflowViewModel
import com.filmtracker.app.ui.viewmodel.ViewModelFactory
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 胶卷工作流导航
 * 
 * 页面流程：
 * 1. filmFormat - 画幅选择页
 * 2. filmCount - 张数选择 + 取景动画页
 * 3. filmGrid - 胶卷滚动预览页
 * 4. filmProcessing - 详细调色页
 */
@Composable
fun FilmWorkflowNavigation(
    startDestination: String = "filmFormat",
    navController: NavHostController = rememberNavController(),
    onExit: (() -> Unit)? = null  // 退出胶卷工作流的回调
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 工作流 ViewModel（跨页面共享状态）- 使用 ViewModelFactory
    val workflowViewModel: FilmWorkflowViewModel = viewModel(
        factory = ViewModelFactory.getInstance(context)
    )
    
    // 观察状态
    val selectedFormat by workflowViewModel.selectedFormat.collectAsState()
    val selectedFilmStock by workflowViewModel.selectedFilmStock.collectAsState()
    val selectedCount by workflowViewModel.selectedCount.collectAsState()
    val filmImages by workflowViewModel.filmImages.collectAsState()
    
    // 使用胶卷风格主题
    FilmTrackerTheme(useVintageTheme = true) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // 1. 画幅选择页
            composable("filmFormat") {
                FilmFormatSelectionScreen(
                    onFormatSelected = { format, filmStock ->
                        workflowViewModel.selectFormat(format)
                        filmStock?.let { workflowViewModel.selectFilmStock(it) }
                        navController.navigate("filmCount")
                    },
                    onBack = onExit  // 返回到首页
                )
            }
            
            // 2. 张数选择 + 取景动画页
            composable("filmCount") {
                val format = selectedFormat ?: FilmFormat.Film135
                
                FilmCountSelectionScreen(
                    filmFormat = format,
                    filmStock = selectedFilmStock,
                    onBack = {
                        navController.popBackStack()
                    },
                    onCountSelected = { count, imageUris ->
                        workflowViewModel.selectCount(count)
                        
                        // 将 URI 转换为 ImageInfo 并加载预览图
                        val images = imageUris.mapIndexed { index, uri ->
                            // 加载预览图
                            val bitmap = try {
                                val inputStream = navController.context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                                android.graphics.BitmapFactory.decodeStream(inputStream)?.also {
                                    inputStream?.close()
                                }
                            } catch (e: Exception) {
                                null
                            }
                            
                            ImageInfo(
                                uri = uri,
                                fileName = "Frame ${index + 1}",
                                width = bitmap?.width ?: 0,
                                height = bitmap?.height ?: 0,
                                isRaw = false,
                                previewBitmap = bitmap,
                                filePath = uri
                            )
                        }
                        workflowViewModel.addImages(images)
                        
                        navController.navigate("filmGrid")
                    }
                )
            }
            
            // 3. 胶卷滚动预览页
            composable("filmGrid") {
                val format = selectedFormat ?: FilmFormat.Film135
                
                FilmGridPreviewScreen(
                    filmFormat = format,
                    filmStock = selectedFilmStock,
                    images = filmImages,
                    onBack = {
                        navController.popBackStack()
                    },
                    onImageClick = { imageInfo ->
                        // 直接使用 URI，不进行编码（Navigation 会自动处理）
                        // 使用 Base64 编码避免特殊字符问题
                        val encodedUri = android.util.Base64.encodeToString(
                            imageInfo.uri.toByteArray(StandardCharsets.UTF_8),
                            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                        )
                        navController.navigate("filmProcessing/$encodedUri")
                    },
                    onAddMoreImages = {
                        // 返回张数选择页添加更多图片
                        navController.popBackStack()
                    }
                )
            }
            
            // 4. 详细调色页（胶卷风格）
            composable(
                route = "filmProcessing/{imageUri}",
                arguments = listOf(
                    navArgument("imageUri") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("imageUri")
                val imageUri = encodedUri?.let {
                    // 使用 Base64 解码
                    try {
                        val decodedBytes = android.util.Base64.decode(it, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                        String(decodedBytes, StandardCharsets.UTF_8)
                    } catch (e: Exception) {
                        android.util.Log.e("FilmWorkflowNav", "Failed to decode URI", e)
                        null
                    }
                }
                
                FilmProcessingScreen(
                    imageUri = imageUri,
                    onBack = { params, thumbnail ->
                        // 保存编辑结果到 FilmWorkflowViewModel（用于显示修改指示器和更新预览图）
                        if (params != null && imageUri != null) {
                            workflowViewModel.updateImageEdits(
                                imageUri = imageUri,
                                params = params,
                                processedBitmap = thumbnail
                            )
                        }
                        navController.popBackStack()
                    },
                    useVintageTheme = true
                )
            }
        }
    }
}

/**
 * 导航路由常量
 */
object FilmWorkflowRoutes {
    const val FORMAT_SELECTION = "filmFormat"
    const val COUNT_SELECTION = "filmCount"
    const val GRID_PREVIEW = "filmGrid"
    const val PROCESSING = "filmProcessing/{imageUri}"
    
    fun processingRoute(imageUri: String) = "filmProcessing/$imageUri"
}

/**
 * 导航扩展函数
 */
fun NavHostController.navigateToFilmProcessing(imageUri: String) {
    navigate(FilmWorkflowRoutes.processingRoute(imageUri))
}

fun NavHostController.navigateToFilmGrid() {
    navigate(FilmWorkflowRoutes.GRID_PREVIEW)
}

fun NavHostController.navigateToFilmCount() {
    navigate(FilmWorkflowRoutes.COUNT_SELECTION)
}

fun NavHostController.navigateToFilmFormat() {
    navigate(FilmWorkflowRoutes.FORMAT_SELECTION) {
        // 清空返回栈，回到起始页
        popUpTo(FilmWorkflowRoutes.FORMAT_SELECTION) {
            inclusive = true
        }
    }
}
