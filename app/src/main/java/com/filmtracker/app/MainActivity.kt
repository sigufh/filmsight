package com.filmtracker.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.*
import com.filmtracker.app.ui.navigation.FilmWorkflowNavigation
import com.filmtracker.app.ai.ColorGradingSuggestion
import com.filmtracker.app.ui.theme.FilmTrackerTheme
import com.filmtracker.app.util.ImageExporter
import com.filmtracker.app.util.ImageProcessor
import com.filmtracker.app.native.RawProcessorNative
import com.filmtracker.app.native.BilateralFilterNative
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filmtracker.app.ui.viewmodel.AIAssistantViewModel
import com.filmtracker.app.ui.viewmodel.AIAssistantViewModelFactory
import com.filmtracker.app.ai.AISettingsManager

class MainActivity : ComponentActivity() {
    
    private val recentImagesKey = "recent_images"
    private val maxRecentImages = 20
    
    // Activity 级别的缓存，避免重复加载
    private var cachedRecentImages: List<ImageInfo>? = null
    private var isLoadingImages = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化双边滤波器配置
        initializeBilateralFilterConfig()
        
        setContent {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
            var aiSuggestedImageUri by remember { mutableStateOf<String?>(null) }
            var aiSuggestion by remember { mutableStateOf<ColorGradingSuggestion?>(null) }
            
            when (currentScreen) {
                Screen.Home -> {
                    // 首页
                    FilmTrackerTheme(useVintageTheme = true) {
                        HomeScreen(
                            onFilmModeClick = {
                                currentScreen = Screen.FilmWorkflow
                            },
                            onProModeClick = {
                                currentScreen = Screen.ProMode
                            },
                            onAIColorClick = {
                                Toast.makeText(
                                    this@MainActivity,
                                    "AI 仿色功能即将推出",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onAIAssistantClick = {
                                currentScreen = Screen.AIAssistant
                            }
                        )
                    }
                }
                Screen.FilmWorkflow -> {
                    // 胶卷工作流
                    FilmWorkflowNavigation(
                        startDestination = "filmFormat",
                        onExit = {
                            currentScreen = Screen.Home
                        }
                    )
                }
                Screen.ProMode -> {
                    // 专业修图模式（原有界面）
                    renderProMode(
                        onBack = { currentScreen = Screen.Home },
                        initialImageUri = aiSuggestedImageUri,
                        initialSuggestion = aiSuggestion,
                        onImageApplied = {
                            // 清除 AI 建议状态
                            aiSuggestedImageUri = null
                            aiSuggestion = null
                        }
                    )
                }
                Screen.AIAssistant -> {
                    // AI助手
                    var showSettings by remember { mutableStateOf(false) }
                    val settingsManager = remember { AISettingsManager(this@MainActivity) }
                    val aiViewModel: AIAssistantViewModel = viewModel(
                        factory = AIAssistantViewModelFactory(settingsManager)
                    )
                    
                    FilmTrackerTheme {
                        if (showSettings) {
                            AISettingsScreen(
                                viewModel = aiViewModel,
                                onBack = { showSettings = false }
                            )
                        } else {
                            AIAssistantScreen(
                                viewModel = aiViewModel,
                                onBack = { currentScreen = Screen.Home },
                                onApplySuggestion = { suggestion ->
                                    // 获取当前对话中最后一条用户消息的图片
                                    val messages = aiViewModel.messages.value
                                    val lastUserMessageWithImage = messages
                                        .filter { it.isUser && it.imageBitmap != null }
                                        .lastOrNull()
                                    
                                    if (lastUserMessageWithImage != null) {
                                        // 保存图片到临时文件并获取 URI
                                        lifecycleScope.launch {
                                            val uri = saveBitmapToTempFile(lastUserMessageWithImage.imageBitmap!!)
                                            if (uri != null) {
                                                aiSuggestedImageUri = uri.toString()
                                                aiSuggestion = suggestion
                                                currentScreen = Screen.ProMode
                                            } else {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "无法保存图片",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "请先上传图片",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onSettings = { showSettings = true }
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 屏幕枚举
     */
    private enum class Screen {
        Home,           // 首页
        FilmWorkflow,   // 胶卷工作流
        ProMode,        // 专业修图模式
        AIAssistant     // AI助手
    }
    
    @Composable
    private fun renderProMode(
        onBack: () -> Unit,
        initialImageUri: String? = null,
        initialSuggestion: ColorGradingSuggestion? = null,
        onImageApplied: () -> Unit = {}
    ) {
        var selectedImageUri by remember { mutableStateOf(initialImageUri) }
        var showImportScreen by remember { mutableStateOf(initialImageUri == null) }
        var appliedSuggestion by remember { mutableStateOf(initialSuggestion) }
        
        // 使用缓存的图片列表作为初始值，避免闪烁
        var recentImages by remember { mutableStateOf(cachedRecentImages ?: emptyList()) }
        
        // 当有初始图片时，标记已应用
        LaunchedEffect(initialImageUri) {
            if (initialImageUri != null) {
                onImageApplied()
            }
        }
        
        // 异步加载图片列表（如果缓存为空或需要刷新）
        LaunchedEffect(Unit) {
            if (cachedRecentImages == null && !isLoadingImages) {
                isLoadingImages = true
                val loaded = loadRecentImages()
                cachedRecentImages = loaded
                recentImages = loaded
                isLoadingImages = false
            }
        }
        
        // 重新注册以访问状态
        val imagePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                lifecycleScope.launch {
                    val imageInfo = createImageInfo(uri)
                    if (imageInfo != null) {
                        // 添加到最近列表
                        val updatedList = (listOf(imageInfo) + recentImages)
                            .distinctBy { it.uri }
                            .take(maxRecentImages)
                        recentImages = updatedList
                        
                        // 同时更新缓存
                        cachedRecentImages = updatedList
                        saveRecentImages(updatedList)
                        
                        // 选择这张图片
                        selectedImageUri = imageInfo.uri
                        showImportScreen = false
                    }
                }
            }
        }
        
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                imagePickerLauncher.launch("image/*")
            }
        }
        
        FilmTrackerTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                android.util.Log.d("MainActivity", "Rendering UI, showImportScreen=$showImportScreen, selectedImageUri=$selectedImageUri")
                
                if (showImportScreen) {
                    android.util.Log.d("MainActivity", "Showing ImageImportScreen with ${recentImages.size} recent images")
                    ImageImportScreen(
                        recentImages = recentImages,
                        onSelectImage = {
                            android.util.Log.d("MainActivity", "onSelectImage clicked")
                            checkPermissionsAndOpenPicker(requestPermissionLauncher, imagePickerLauncher)
                        },
                        onImageSelected = { imageInfo ->
                            android.util.Log.d("MainActivity", "Image selected: ${imageInfo.fileName}")
                            selectedImageUri = imageInfo.uri
                            showImportScreen = false
                        },
                        onDeleteImage = { imageInfo ->
                            android.util.Log.d("MainActivity", "Deleting image: ${imageInfo.fileName}")
                            val updatedList = recentImages.filter { it.uri != imageInfo.uri }
                            recentImages = updatedList
                            
                            // 同时更新缓存
                            cachedRecentImages = updatedList
                            lifecycleScope.launch {
                                saveRecentImages(updatedList)
                            }
                        },
                        onBack = onBack  // 添加返回回调
                    )
                } else {
                    android.util.Log.d("MainActivity", "Showing ProcessingScreen")
                    ProcessingScreen(
                        imageUri = selectedImageUri,
                        aiSuggestion = appliedSuggestion,
                        onSelectImage = {
                            android.util.Log.d("MainActivity", "Returning to import screen")
                            showImportScreen = true
                            appliedSuggestion = null // 清除建议
                        }
                    )
                }
            }
        }
    }
    
    private suspend fun createImageInfo(uri: Uri): ImageInfo? = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri) ?: "unknown"
            val isRaw = isRawFile(fileName)
            
            // 获取文件路径
            val filePath = getFilePathFromUri(uri)
            
            // 获取图片尺寸和预览
            var width = 0
            var height = 0
            var previewBitmap: Bitmap? = null
            
            if (isRaw && filePath != null) {
                // RAW 文件：获取原图尺寸和提取内嵌预览图
                val rawProcessor = RawProcessorNative()
                
                // 获取真实的原图尺寸（从 RAW 文件元数据）
                val rawSize = rawProcessor.getRawImageSize(filePath)
                if (rawSize != null) {
                    width = rawSize.first
                    height = rawSize.second
                }
                
                // 直接使用 RAW 内嵌的预览图，不做额外处理
                previewBitmap = rawProcessor.extractPreview(filePath)
            } else {
                // 普通图片：解码获取真实尺寸
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                    width = options.outWidth
                    height = options.outHeight
                }
                
                // 生成更高质量的缩略图（400x400）
                val thumbOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(width, height, 400, 400)
                }
                contentResolver.openInputStream(uri)?.use { stream ->
                    previewBitmap = BitmapFactory.decodeStream(stream, null, thumbOptions)
                }
            }
            
            ImageInfo(
                uri = uri.toString(),
                fileName = fileName,
                width = width,
                height = height,
                isRaw = isRaw,
                previewBitmap = previewBitmap,
                filePath = filePath
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error creating image info", e)
            null
        }
    }
    
    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) it.substring(cut + 1) else it
            }
        }
        return result
    }
    
    private fun isRawFile(fileName: String): Boolean {
        val rawExtensions = listOf(
            ".arw", ".cr2", ".cr3", ".nef", ".raf", ".orf", ".rw2",
            ".pef", ".srw", ".dng", ".raw"
        )
        return rawExtensions.any { fileName.lowercase().endsWith(it) }
    }
    
    private suspend fun getFilePathFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            if (uri.scheme == "file") {
                return@withContext uri.path
            }
            
            val fileName = getFileName(uri) ?: "temp_file"
            val tempFile = java.io.File(cacheDir, fileName)
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to get file path", e)
            null
        }
    }
    
    private suspend fun loadRecentImages(): List<ImageInfo> = withContext(Dispatchers.IO) {
        try {
            val prefs = getSharedPreferences("filmsight", Context.MODE_PRIVATE)
            val uriListJson = prefs.getString(recentImagesKey, null) ?: return@withContext emptyList()
            
            // 解析保存的 URI 列表
            val uriList = uriListJson.split("|").filter { it.isNotBlank() }
            
            // 为每个 URI 重新创建 ImageInfo
            val imageInfoList = mutableListOf<ImageInfo>()
            for (uriString in uriList) {
                try {
                    val uri = Uri.parse(uriString)
                    val imageInfo = createImageInfo(uri)
                    if (imageInfo != null) {
                        imageInfoList.add(imageInfo)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error loading image: $uriString", e)
                }
            }
            
            imageInfoList
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading recent images", e)
            emptyList()
        }
    }
    
    private suspend fun saveRecentImages(images: List<ImageInfo>) = withContext(Dispatchers.IO) {
        try {
            val prefs = getSharedPreferences("filmsight", Context.MODE_PRIVATE)
            // 只保存 URI 列表，用 | 分隔
            val uriListJson = images.joinToString("|") { it.uri }
            prefs.edit().putString(recentImagesKey, uriListJson).apply()
            android.util.Log.d("MainActivity", "Saved ${images.size} recent images")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error saving recent images", e)
        }
    }
    
    private fun checkPermissionsAndOpenPicker(
        permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
        imageLauncher: androidx.activity.result.ActivityResultLauncher<String>
    ) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                imageLauncher.launch("image/*")
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }
    
    private fun exportImage(imageUri: String?, params: BasicAdjustmentParams) {
        if (imageUri == null) {
            Toast.makeText(this, "请先选择图像", Toast.LENGTH_SHORT).show()
            return
        }
        
        val imageProcessor = ImageProcessor(this)
        val imageExporter = ImageExporter(this)
        
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "正在处理图像...", Toast.LENGTH_SHORT).show()
            
            // 加载原始图像（全分辨率）
            val originalBitmap = imageProcessor.loadOriginalImage(imageUri, previewMode = false)
            
            if (originalBitmap != null) {
                // 应用基础调整
                val processedBitmap = imageProcessor.applyBasicAdjustmentsToOriginal(originalBitmap, params)
                
                if (processedBitmap != null) {
                    // 导出到相册
                    val success = imageExporter.exportToGallery(processedBitmap)
                    
                    if (success) {
                        Toast.makeText(this@MainActivity, "导出成功！", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "导出失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "图像处理失败", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "加载图像失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 将 Bitmap 保存到临时文件并返回 URI
     */
    private suspend fun saveBitmapToTempFile(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val tempFile = java.io.File(cacheDir, "ai_temp_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
            outputStream.close()
            Uri.fromFile(tempFile)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to save bitmap to temp file", e)
            null
        }
    }
    
    /**
     * 初始化双边滤波器配置
     * 在应用启动时调用，启用所有性能优化功能
     */
    private fun initializeBilateralFilterConfig() {
        try {
            android.util.Log.d("MainActivity", "Initializing bilateral filter configuration...")
            
            // 初始化默认配置（启用所有优化）
            BilateralFilterNative.initializeDefaults()
            
            // 记录默认配置值（无需调用 nativeGetConfig，避免 JNI 复杂性）
            android.util.Log.i("MainActivity", "Bilateral filter configuration initialized with defaults:")
            android.util.Log.i("MainActivity", "  - Cache enabled: true")
            android.util.Log.i("MainActivity", "  - Fast approximation enabled: true")
            android.util.Log.i("MainActivity", "  - GPU enabled: true")
            android.util.Log.i("MainActivity", "  - Fast approx threshold: 4.5")
            android.util.Log.i("MainActivity", "  - GPU threshold pixels: 1500000")
            android.util.Log.i("MainActivity", "  - Max cache size: 100")
            android.util.Log.i("MainActivity", "  - Max cache memory: 512 MB")
            
            android.util.Log.d("MainActivity", "Bilateral filter configuration initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize bilateral filter configuration", e)
            // 不抛出异常，允许应用继续运行
        }
    }
    
}
