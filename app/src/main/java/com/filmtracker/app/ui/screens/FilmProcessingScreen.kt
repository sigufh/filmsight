package com.filmtracker.app.ui.screens

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import com.filmtracker.app.ui.viewmodel.ProcessingViewModel
import com.filmtracker.app.ui.viewmodel.ViewModelFactory

/**
 * 胶卷风格图像编辑屏幕（包装器）
 * 
 * 这个屏幕直接复用 ProcessingScreen 的所有功能，
 * 只是作为胶卷工作流的一部分被调用。
 * 
 * AI 助手对话框会在点击"AI调色"工具时显示在右侧面板中。
 */
@Composable
fun FilmProcessingScreen(
    imageUri: String?,
    initialParams: BasicAdjustmentParams? = null,  // 初始调色参数（已废弃，使用 MetadataRepository）
    onBack: (BasicAdjustmentParams?, android.graphics.Bitmap?) -> Unit,  // 返回时传递参数和缩略图
    modifier: Modifier = Modifier,
    useVintageTheme: Boolean = true  // 是否使用胶卷风格主题（暂时未使用）
) {
    val context = LocalContext.current
    
    // 获取 ViewModel
    val viewModel: ProcessingViewModel = viewModel(
        factory = ViewModelFactory.getInstance(context)
    )
    
    // 观察当前参数
    val domainParams by viewModel.adjustmentParams.collectAsState()
    val processedImage by viewModel.processedImage.collectAsState()
    val mapper = remember { AdjustmentParamsMapper() }
    val currentParams = remember(domainParams) {
        mapper.toData(domainParams)
    }
    
    // 生成处理后的缩略图（用于胶卷预览）
    val processedThumbnail = remember(processedImage) {
        processedImage?.let { bitmap ->
            // 生成缩略图（最大 400px）
            val maxSize = 400
            val scale = minOf(
                maxSize.toFloat() / bitmap.width,
                maxSize.toFloat() / bitmap.height,
                1f
            )
            if (scale < 1f) {
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }
        }
    }
    
    // 调试日志
    LaunchedEffect(imageUri) {
        Log.d("FilmProcessingScreen", "Received imageUri: $imageUri")
    }
    
    // 加载图像并自动恢复参数（通过 ProcessingViewModel.loadImage）
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            val imageProcessor = com.filmtracker.app.util.ImageProcessor(context)
            val loadedImage = imageProcessor.loadOriginalImage(imageUri, previewMode = true)
            if (loadedImage != null) {
                // 使用 loadImage 而不是 setOriginalImage，这样会自动加载元数据
                val uri = android.net.Uri.parse(imageUri)
                viewModel.loadImage(uri, imageUri, loadedImage)  // 使用 imageUri 作为 path
                Log.d("FilmProcessingScreen", "Image loaded and metadata restored")
            } else {
                Log.e("FilmProcessingScreen", "Failed to load image")
            }
        }
    }
    
    // 直接调用 ProcessingScreen，复用所有专业调色功能
    ProcessingScreen(
        imageUri = imageUri,
        onSelectImage = { 
            // 返回时传递当前参数和缩略图
            Log.d("FilmProcessingScreen", "Returning with params: exposure=${currentParams.globalExposure}")
            onBack(currentParams, processedThumbnail)
        },
        modifier = modifier
    )
}
