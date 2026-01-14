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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.ui.screens.ImageImportScreen
import com.filmtracker.app.ui.screens.ImageInfo
import com.filmtracker.app.ui.screens.ProcessingScreen
import com.filmtracker.app.ui.theme.FilmTrackerTheme
import com.filmtracker.app.util.ImageExporter
import com.filmtracker.app.util.ImageProcessor
import com.filmtracker.app.native.RawProcessorNative
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    
    private val recentImagesKey = "recent_images"
    private val maxRecentImages = 20
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var selectedImageUri by mutableStateOf<String?>(null)
        var recentImages by mutableStateOf<List<ImageInfo>>(emptyList())
        var showImportScreen by mutableStateOf(true)
        
        // 加载最近图片列表
        lifecycleScope.launch {
            recentImages = loadRecentImages()
        }
        
        // 重新注册以访问状态
        val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                lifecycleScope.launch {
                    val imageInfo = createImageInfo(uri)
                    if (imageInfo != null) {
                        // 添加到最近列表
                        recentImages = (listOf(imageInfo) + recentImages)
                            .distinctBy { it.uri }
                            .take(maxRecentImages)
                        saveRecentImages(recentImages)
                        
                        // 选择这张图片
                        selectedImageUri = imageInfo.uri
                        showImportScreen = false
                    }
                }
            }
        }
        
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                imagePickerLauncher.launch("image/*")
            }
        }
        
        setContent {
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
                                recentImages = recentImages.filter { it.uri != imageInfo.uri }
                                lifecycleScope.launch {
                                    saveRecentImages(recentImages)
                                }
                            }
                        )
                    } else {
                        android.util.Log.d("MainActivity", "Showing ProcessingScreen")
                        ProcessingScreen(
                            imageUri = selectedImageUri,
                            onSelectImage = {
                                android.util.Log.d("MainActivity", "Returning to import screen")
                                showImportScreen = true
                            },
                            onExport = { params ->
                                exportImage(selectedImageUri, params)
                            }
                        )
                    }
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
            val json = prefs.getString(recentImagesKey, null) ?: return@withContext emptyList()
            
            // 简单的 JSON 解析（实际项目中应使用 Gson 或 Kotlinx.serialization）
            // 这里暂时返回空列表，后续可以实现持久化
            emptyList()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading recent images", e)
            emptyList()
        }
    }
    
    private suspend fun saveRecentImages(images: List<ImageInfo>) = withContext(Dispatchers.IO) {
        try {
            val prefs = getSharedPreferences("filmsight", Context.MODE_PRIVATE)
            // 简单的持久化（实际项目中应使用 Gson 或 Kotlinx.serialization）
            // 这里暂时不实现，后续可以添加
            prefs.edit().apply()
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
    
}
