package com.filmtracker.app.native

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

/**
 * RAW 处理器 Native 封装
 * 桥接 Kotlin 和 C++ 层
 */
class RawProcessorNative {
    
    private external fun nativeInit(): Long
    private external fun nativeLoadRaw(nativePtr: Long, filePath: String): Long
    private external fun nativeExtractPreview(nativePtr: Long, filePath: String): ByteArray?
    
    private var nativePtr: Long = 0
    
    init {
        System.loadLibrary("filmtracker")
        nativePtr = nativeInit()
    }
    
    /**
     * 提取 RAW 文件的嵌入式 JPEG 预览图
     * 这比完整解码快得多，适合用于快速预览
     */
    fun extractPreview(filePath: String): Bitmap? {
        return try {
            val jpegData = nativeExtractPreview(nativePtr, filePath)
            if (jpegData != null && jpegData.isNotEmpty()) {
                BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            } else {
                Log.w(TAG, "No preview data extracted")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting preview", e)
            null
        }
    }
    
    /**
     * 加载 RAW 图像文件
     * @return Pair<LinearImageNative, RawMetadataNative> 或 null
     */
    fun loadRaw(filePath: String): Pair<LinearImageNative, RawMetadataNative>? {
        return try {
            val result = nativeLoadRawWithMetadata(nativePtr, filePath)
            if (result != null && result.size >= 2 && result[0] != 0L && result[1] != 0L) {
                Pair(LinearImageNative(result[0]), RawMetadataNative(result[1]))
            } else {
                Log.e(TAG, "Failed to load RAW image")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading RAW", e)
            null
        }
    }
    
    /**
     * 加载 RAW 图像文件（兼容旧接口）
     */
    fun loadRawLegacy(filePath: String): LinearImageNative? {
        return try {
            val imagePtr = nativeLoadRaw(nativePtr, filePath)
            if (imagePtr != 0L) {
                LinearImageNative(imagePtr)
            } else {
                Log.e(TAG, "Failed to load RAW image")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading RAW", e)
            null
        }
    }
    
    private external fun nativeLoadRawWithMetadata(nativePtr: Long, filePath: String): LongArray?
    
    companion object {
        private const val TAG = "RawProcessorNative"
    }
}
