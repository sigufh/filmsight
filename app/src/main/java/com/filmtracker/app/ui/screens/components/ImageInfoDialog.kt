package com.filmtracker.app.ui.screens.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.exifinterface.media.ExifInterface
import com.filmtracker.app.ui.theme.ComponentSize
import com.filmtracker.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.*

/**
 * 图像信息对话框
 * 显示文件基本信息和完整的 EXIF 元数据
 */
@Composable
fun ImageInfoDialog(
    imageUri: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // 提取图像信息和 EXIF 数据
    val imageData = remember(imageUri) {
        imageUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                extractImageInfo(context, uri)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "图像信息",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            if (imageData != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = ComponentSize.panelMaxHeight + ComponentSize.cardMinHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // 基本信息
                    InfoSection(
                        title = "基本信息",
                        items = imageData.basicInfo
                    )

                    // 相机信息
                    if (imageData.cameraInfo.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoSection(
                            title = "相机信息",
                            items = imageData.cameraInfo
                        )
                    }

                    // 拍摄参数
                    if (imageData.shootingParams.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoSection(
                            title = "拍摄参数",
                            items = imageData.shootingParams
                        )
                    }

                    // 位置信息
                    if (imageData.locationInfo.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoSection(
                            title = "位置信息",
                            items = imageData.locationInfo
                        )
                    }

                    // 其他 EXIF 信息
                    if (imageData.otherExif.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoSection(
                            title = "其他信息",
                            items = imageData.otherExif
                        )
                    }
                }
            } else {
                Text(
                    text = "无法获取图像信息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun InfoSection(
    title: String,
    items: Map<String, String>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        items.forEach { (key, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 图像数据类
 */
private data class ImageData(
    val basicInfo: Map<String, String>,
    val cameraInfo: Map<String, String>,
    val shootingParams: Map<String, String>,
    val locationInfo: Map<String, String>,
    val otherExif: Map<String, String>
)

/**
 * 提取图像信息
 */
private fun extractImageInfo(context: android.content.Context, uri: Uri): ImageData? {
    try {
        val contentResolver = context.contentResolver
        
        // 基本文件信息
        val basicInfo = mutableMapOf<String, String>()
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                
                if (displayNameIndex >= 0) {
                    basicInfo["文件名"] = it.getString(displayNameIndex)
                }
                if (sizeIndex >= 0) {
                    basicInfo["文件大小"] = formatFileSize(it.getLong(sizeIndex))
                }
            }
        }
        
        // 提取 EXIF 信息（先读取，因为需要从 EXIF 获取原图尺寸）
        val exif = contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream)
        }
        
        // 图像尺寸 - 优先从 EXIF 读取原图尺寸
        val imageWidth = exif?.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) ?: 0
        val imageHeight = exif?.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0) ?: 0
        
        // 如果 EXIF 中没有尺寸信息，则从 BitmapFactory 读取
        val (finalWidth, finalHeight, mimeType) = if (imageWidth > 0 && imageHeight > 0) {
            // 从 EXIF 获取尺寸，但仍需要从 BitmapFactory 获取 MIME 类型
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            Triple(imageWidth, imageHeight, options.outMimeType)
        } else {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            Triple(options.outWidth, options.outHeight, options.outMimeType)
        }
        
        if (finalWidth > 0 && finalHeight > 0) {
            basicInfo["图像尺寸"] = "$finalWidth × $finalHeight 像素"
            basicInfo["宽高比"] = String.format("%.2f:1", finalWidth.toFloat() / finalHeight)
            basicInfo["总像素"] = formatPixelCount(finalWidth.toLong() * finalHeight)
        }
        
        // MIME 类型
        mimeType?.let { basicInfo["MIME 类型"] = it }
        
        val cameraInfo = mutableMapOf<String, String>()
        val shootingParams = mutableMapOf<String, String>()
        val locationInfo = mutableMapOf<String, String>()
        val otherExif = mutableMapOf<String, String>()
        
        exif?.let { exifInterface ->
            // 相机信息
            exifInterface.getAttribute(ExifInterface.TAG_MAKE)?.let {
                cameraInfo["制造商"] = it
            }
            exifInterface.getAttribute(ExifInterface.TAG_MODEL)?.let {
                cameraInfo["型号"] = it
            }
            exifInterface.getAttribute(ExifInterface.TAG_LENS_MAKE)?.let {
                cameraInfo["镜头制造商"] = it
            }
            exifInterface.getAttribute(ExifInterface.TAG_LENS_MODEL)?.let {
                cameraInfo["镜头型号"] = it
            }
            exifInterface.getAttribute(ExifInterface.TAG_SOFTWARE)?.let {
                cameraInfo["软件"] = it
            }
            
            // 拍摄参数
            exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
                shootingParams["拍摄时间"] = formatDateTime(it)
            }
            exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
                shootingParams["快门速度"] = formatExposureTime(it)
            }
            exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)?.let {
                shootingParams["光圈"] = "f/${it}"
            }
            exifInterface.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let {
                shootingParams["ISO"] = it
            } ?: exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let {
                shootingParams["ISO"] = it
            }
            exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                shootingParams["焦距"] = formatFocalLength(it)
            }
            exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE)?.let {
                shootingParams["曝光补偿"] = "${it} EV"
            }
            // 闪光灯 - 如果没有数据则显示"无"
            val flashValue = exifInterface.getAttribute(ExifInterface.TAG_FLASH)
            shootingParams["闪光灯"] = if (flashValue != null) {
                formatFlash(flashValue)
            } else {
                "无"
            }
            exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let {
                shootingParams["白平衡"] = formatWhiteBalance(it)
            }
            exifInterface.getAttribute(ExifInterface.TAG_METERING_MODE)?.let {
                shootingParams["测光模式"] = formatMeteringMode(it)
            }
            
            // 位置信息
            val latLong = exifInterface.latLong
            if (latLong != null) {
                locationInfo["纬度"] = String.format("%.6f°", latLong[0])
                locationInfo["经度"] = String.format("%.6f°", latLong[1])
            }
            exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.let {
                locationInfo["海拔"] = "${it}m"
            }
            
            // 其他信息
            exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)?.let {
                otherExif["方向"] = formatOrientation(it)
            }
            
            // 色彩空间 - 从 EXIF 读取原图色彩空间
            val colorSpace = exifInterface.getAttribute(ExifInterface.TAG_COLOR_SPACE)
            if (colorSpace != null) {
                otherExif["色彩空间"] = formatColorSpace(colorSpace)
            } else {
                // 如果 EXIF 中没有，尝试从其他标签读取
                exifInterface.getAttribute("ColorSpace")?.let {
                    otherExif["色彩空间"] = formatColorSpace(it)
                }
            }
            exifInterface.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.let {
                otherExif["描述"] = it
            }
            exifInterface.getAttribute(ExifInterface.TAG_ARTIST)?.let {
                otherExif["作者"] = it
            }
            exifInterface.getAttribute(ExifInterface.TAG_COPYRIGHT)?.let {
                otherExif["版权"] = it
            }
        }
        
        return ImageData(
            basicInfo = basicInfo,
            cameraInfo = cameraInfo,
            shootingParams = shootingParams,
            locationInfo = locationInfo,
            otherExif = otherExif
        )
    } catch (e: Exception) {
        return null
    }
}

// 格式化函数
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatPixelCount(pixels: Long): String {
    return when {
        pixels < 1_000_000 -> String.format("%.1f K", pixels / 1000.0)
        else -> String.format("%.1f MP", pixels / 1_000_000.0)
    }
}

private fun formatDateTime(dateTime: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateTime)
        date?.let { outputFormat.format(it) } ?: dateTime
    } catch (e: Exception) {
        dateTime
    }
}

private fun formatExposureTime(exposureTime: String): String {
    return try {
        val value = exposureTime.toDoubleOrNull()
        if (value != null && value < 1) {
            "1/${(1 / value).toInt()}s"
        } else {
            "${exposureTime}s"
        }
    } catch (e: Exception) {
        exposureTime
    }
}

private fun formatFocalLength(focalLength: String): String {
    return try {
        val parts = focalLength.split("/")
        if (parts.size == 2) {
            val value = parts[0].toDouble() / parts[1].toDouble()
            String.format("%.1fmm", value)
        } else {
            "${focalLength}mm"
        }
    } catch (e: Exception) {
        focalLength
    }
}

private fun formatFlash(flash: String): String {
    return when (flash) {
        "0" -> "未闪光"
        "1" -> "闪光"
        "5" -> "闪光，未检测到回闪"
        "7" -> "闪光，检测到回闪"
        else -> flash
    }
}

private fun formatWhiteBalance(wb: String): String {
    return when (wb) {
        "0" -> "自动"
        "1" -> "手动"
        else -> wb
    }
}

private fun formatMeteringMode(mode: String): String {
    return when (mode) {
        "0" -> "未知"
        "1" -> "平均"
        "2" -> "中央重点"
        "3" -> "点测光"
        "4" -> "多点"
        "5" -> "评价"
        "6" -> "局部"
        else -> mode
    }
}

private fun formatOrientation(orientation: String): String {
    return when (orientation) {
        "1" -> "正常"
        "3" -> "旋转180°"
        "6" -> "旋转90°顺时针"
        "8" -> "旋转90°逆时针"
        else -> orientation
    }
}

private fun formatColorSpace(colorSpace: String): String {
    return when (colorSpace) {
        "1" -> "sRGB"
        "65535" -> "未校准"
        else -> colorSpace
    }
}
