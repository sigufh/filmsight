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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                        
                        // 异步加载图片，先创建占位符
                        val images = imageUris.mapIndexed { index, uri ->
                            ImageInfo(
                                uri = uri,
                                fileName = "Frame ${index + 1}",
                                width = 0,
                                height = 0,
                                isRaw = false,
                                previewBitmap = null,  // 先不加载，后台加载
                                filePath = uri
                            )
                        }
                        workflowViewModel.addImages(images)
                        
                        // 在后台线程加载预览图
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            imageUris.forEachIndexed { index, uri ->
                                try {
                                    val bitmap = navController.context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { stream ->
                                        // 降采样加载以节省内存
                                        val options = android.graphics.BitmapFactory.Options().apply {
                                            inJustDecodeBounds = true
                                        }
                                        android.graphics.BitmapFactory.decodeStream(stream, null, options)
                                        
                                        // 计算采样率
                                        val targetSize = 1024
                                        var inSampleSize = 1
                                        if (options.outHeight > targetSize || options.outWidth > targetSize) {
                                            val halfHeight = options.outHeight / 2
                                            val halfWidth = options.outWidth / 2
                                            while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                                                inSampleSize *= 2
                                            }
                                        }
                                        
                                        // 重新打开流并解码
                                        navController.context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { stream2 ->
                                            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                                                this.inSampleSize = inSampleSize
                                            }
                                            android.graphics.BitmapFactory.decodeStream(stream2, null, decodeOptions)
                                        }
                                    }
                                    
                                    // 更新图片信息
                                    if (bitmap != null) {
                                        workflowViewModel.updateImagePreview(index, bitmap)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("FilmWorkflow", "Failed to load preview for image $index", e)
                                }
                            }
                        }
                        
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
                    },
                    viewModel = workflowViewModel  // 传递 workflowViewModel
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
