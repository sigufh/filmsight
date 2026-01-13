package com.filmtracker.app

import android.Manifest
import android.content.pm.PackageManager
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
import com.filmtracker.app.data.FilmParams
import com.filmtracker.app.ui.screens.ProcessingScreen
import com.filmtracker.app.ui.theme.FilmTrackerTheme
import com.filmtracker.app.util.ImageExporter
import com.filmtracker.app.util.ImageProcessor
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    
    // 这些 launcher 已移动到 onCreate 中，使用局部变量
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var selectedImageUri by mutableStateOf<String?>(null)
        
        // 重新注册以访问状态
        val imagePickerLauncher2 = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedImageUri = uri?.toString()
        }
        
        val requestPermissionLauncher2 = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                imagePickerLauncher2.launch("image/*")
            }
        }
        
        setContent {
            FilmTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProcessingScreen(
                        imageUri = selectedImageUri,
                        onSelectImage = {
                            checkPermissionsAndOpenPicker(requestPermissionLauncher2, imagePickerLauncher2)
                        },
                        onExport = { params ->
                            exportImage(selectedImageUri, params)
                        }
                    )
                }
            }
        }
        
        // 首次打开时请求权限（延迟执行，避免在 setContent 中调用）
        lifecycleScope.launch {
            checkPermissionsAndOpenPicker(requestPermissionLauncher2, imagePickerLauncher2)
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
    
    private fun exportImage(imageUri: String?, params: FilmParams) {
        if (imageUri == null) {
            Toast.makeText(this, "请先选择图像", Toast.LENGTH_SHORT).show()
            return
        }
        
        val imageProcessor = ImageProcessor(this)
        val imageExporter = ImageExporter(this)
        
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "正在处理图像...", Toast.LENGTH_SHORT).show()
            
            // 处理图像（支持RAW和普通图片）
            val bitmap = imageProcessor.processImage(imageUri, params)
            
            if (bitmap != null) {
                // 导出到相册
                val success = imageExporter.exportToGallery(bitmap)
                
                if (success) {
                    Toast.makeText(this@MainActivity, "导出成功！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "导出失败", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "图像处理失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
}
