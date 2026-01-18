package com.filmtracker.app.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

/**
 * 存储空间工具类
 * 
 * 提供存储空间检查和管理功能
 * Requirements: 1.5
 */
object StorageUtil {
    
    /**
     * 检查是否有足够的存储空间
     * 
     * @param requiredBytes 需要的字节数
     * @param path 目标路径（可选，默认使用内部存储）
     * @return 是否有足够空间
     */
    fun hasEnoughSpace(requiredBytes: Long, path: String? = null): Boolean {
        val availableBytes = getAvailableSpace(path)
        // 添加 10% 的安全边际
        val requiredWithMargin = (requiredBytes * 1.1).toLong()
        return availableBytes >= requiredWithMargin
    }
    
    /**
     * 获取可用存储空间（字节）
     * 
     * @param path 目标路径（可选，默认使用内部存储）
     * @return 可用字节数
     */
    fun getAvailableSpace(path: String? = null): Long {
        return try {
            val statFs = if (path != null) {
                StatFs(File(path).parent ?: path)
            } else {
                StatFs(Environment.getDataDirectory().path)
            }
            statFs.availableBlocksLong * statFs.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 获取总存储空间（字节）
     * 
     * @param path 目标路径（可选，默认使用内部存储）
     * @return 总字节数
     */
    fun getTotalSpace(path: String? = null): Long {
        return try {
            val statFs = if (path != null) {
                StatFs(File(path).parent ?: path)
            } else {
                StatFs(Environment.getDataDirectory().path)
            }
            statFs.blockCountLong * statFs.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 估算图像导出所需的存储空间
     * 
     * @param width 图像宽度
     * @param height 图像高度
     * @param format 导出格式
     * @param quality 质量（仅对 JPEG 有效）
     * @return 估算的字节数
     */
    fun estimateExportSize(
        width: Int,
        height: Int,
        format: String,
        quality: Int = 95
    ): Long {
        val pixels = width.toLong() * height
        
        return when (format.uppercase()) {
            "JPEG", "JPG" -> {
                // JPEG: 根据质量估算
                // 高质量 (90-100): ~3 bytes/pixel
                // 中质量 (70-89): ~2 bytes/pixel
                // 低质量 (<70): ~1 byte/pixel
                when {
                    quality >= 90 -> pixels * 3
                    quality >= 70 -> pixels * 2
                    else -> pixels
                }
            }
            "PNG" -> {
                // PNG: 无损压缩，通常 ~4 bytes/pixel
                pixels * 4
            }
            "TIFF" -> {
                // TIFF: 未压缩，4 bytes/pixel (RGBA)
                pixels * 4
            }
            else -> pixels * 3 // 默认估算
        }
    }
    
    /**
     * 格式化字节数为人类可读的字符串
     * 
     * @param bytes 字节数
     * @return 格式化的字符串（如 "1.5 MB"）
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
    
    /**
     * 检查文件是否存在且可读
     * 
     * @param path 文件路径
     * @return 是否存在且可读
     */
    fun isFileAccessible(path: String): Boolean {
        return try {
            val file = File(path)
            file.exists() && file.canRead()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查目录是否可写
     * 
     * @param path 目录路径
     * @return 是否可写
     */
    fun isDirectoryWritable(path: String): Boolean {
        return try {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.canWrite()
        } catch (e: Exception) {
            false
        }
    }
}
