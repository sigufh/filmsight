package com.filmtracker.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

data class ExifInfo(
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val iso: String? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val dateTime: String? = null,
    val whiteBalance: String? = null,
    val flash: String? = null,
    val width: Int? = null,
    val height: Int? = null
) {
    fun toReadableString(): String {
        val parts = mutableListOf<String>()
        
        cameraMake?.let { make ->
            cameraModel?.let { model ->
                parts.add("相机: $make $model")
            }
        }
        
        lensModel?.let { parts.add("镜头: $it") }
        
        val exposureInfo = mutableListOf<String>()
        iso?.let { exposureInfo.add("ISO $it") }
        fNumber?.let { exposureInfo.add("f/$it") }
        exposureTime?.let { exposureInfo.add("${it}s") }
        focalLength?.let { exposureInfo.add("${it}mm") }
        
        if (exposureInfo.isNotEmpty()) {
            parts.add("曝光参数: ${exposureInfo.joinToString(", ")}")
        }
        
        whiteBalance?.let { parts.add("白平衡: $it") }
        flash?.let { parts.add("闪光灯: $it") }
        
        width?.let { w ->
            height?.let { h ->
                parts.add("分辨率: ${w}x${h}")
            }
        }
        
        return if (parts.isNotEmpty()) {
            parts.joinToString("\n")
        } else {
            "无 EXIF 信息"
        }
    }
}

data class HistogramInfo(
    val redHistogram: IntArray,
    val greenHistogram: IntArray,
    val blueHistogram: IntArray,
    val luminanceHistogram: IntArray
) {
    fun analyze(): String {
        val parts = mutableListOf<String>()
        val lumStats = analyzeChannel(luminanceHistogram, "亮度")
        parts.add(lumStats)
        
        val redPeak = findPeak(redHistogram)
        val greenPeak = findPeak(greenHistogram)
        val bluePeak = findPeak(blueHistogram)
        
        parts.add("色彩峰值: R=$redPeak, G=$greenPeak, B=$bluePeak")
        
        val colorBias = when {
            redPeak > greenPeak + 20 && redPeak > bluePeak + 20 -> "偏暖色调"
            bluePeak > redPeak + 20 && bluePeak > greenPeak + 20 -> "偏冷色调"
            greenPeak > redPeak + 20 && greenPeak > bluePeak + 20 -> "偏绿色调"
            else -> "色调平衡"
        }
        parts.add("色调: $colorBias")
        
        return parts.joinToString("\n")
    }
    
    private fun analyzeChannel(histogram: IntArray, name: String): String {
        val total = histogram.sum()
        if (total == 0) return "$name: 无数据"
        
        val shadows = histogram.sliceArray(0..84).sum() * 100 / total
        val midtones = histogram.sliceArray(85..170).sum() * 100 / total
        val highlights = histogram.sliceArray(171..255).sum() * 100 / total
        
        val exposure = when {
            highlights > 15 -> "可能过曝"
            shadows > 40 -> "可能欠曝"
            midtones > 50 -> "曝光正常"
            else -> "对比度较高"
        }
        
        return "$name 分布: 暗部${shadows}%, 中间调${midtones}%, 高光${highlights}% ($exposure)"
    }
    
    private fun findPeak(histogram: IntArray): Int {
        var maxIndex = 0
        var maxValue = 0
        histogram.forEachIndexed { index, value ->
            if (value > maxValue) {
                maxValue = value
                maxIndex = index
            }
        }
        return maxIndex
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HistogramInfo
        return redHistogram.contentEquals(other.redHistogram) &&
                greenHistogram.contentEquals(other.greenHistogram) &&
                blueHistogram.contentEquals(other.blueHistogram) &&
                luminanceHistogram.contentEquals(other.luminanceHistogram)
    }
    
    override fun hashCode(): Int {
        var result = redHistogram.contentHashCode()
        result = 31 * result + greenHistogram.contentHashCode()
        result = 31 * result + blueHistogram.contentHashCode()
        result = 31 * result + luminanceHistogram.contentHashCode()
        return result
    }
}

object ExifHelper {
    fun extractExifInfo(context: Context, uri: Uri): ExifInfo? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                
                ExifInfo(
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE),
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL),
                    lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL),
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY),
                    fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
                    exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                        val parts = it.split("/")
                        if (parts.size == 2) {
                            val numerator = parts[0].toDoubleOrNull() ?: 0.0
                            val denominator = parts[1].toDoubleOrNull() ?: 1.0
                            String.format("%.1f", numerator / denominator)
                        } else {
                            it
                        }
                    },
                    dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME),
                    whiteBalance = when (exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)) {
                        ExifInterface.WHITE_BALANCE_AUTO.toInt() -> "自动"
                        ExifInterface.WHITE_BALANCE_MANUAL.toInt() -> "手动"
                        else -> null
                    },
                    flash = when (exif.getAttributeInt(ExifInterface.TAG_FLASH, -1)) {
                        0 -> "未闪光"
                        1 -> "已闪光"
                        else -> null
                    },
                    width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1).takeIf { it > 0 },
                    height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1).takeIf { it > 0 }
                )
            }
        } catch (e: IOException) {
            null
        }
    }
    
    fun calculateHistogram(bitmap: Bitmap): HistogramInfo {
        val redHistogram = IntArray(256)
        val greenHistogram = IntArray(256)
        val blueHistogram = IntArray(256)
        val luminanceHistogram = IntArray(256)
        
        val width = bitmap.width
        val height = bitmap.height
        val step = 4
        
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val pixel = bitmap.getPixel(x, y)
                
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                
                val luminance = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
                
                redHistogram[red]++
                greenHistogram[green]++
                blueHistogram[blue]++
                luminanceHistogram[luminance]++
            }
        }
        
        return HistogramInfo(
            redHistogram = redHistogram,
            greenHistogram = greenHistogram,
            blueHistogram = blueHistogram,
            luminanceHistogram = luminanceHistogram
        )
    }
}
