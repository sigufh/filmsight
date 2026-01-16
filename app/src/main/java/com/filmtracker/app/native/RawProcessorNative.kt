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
    private external fun nativeGetRawImageSize(nativePtr: Long, filePath: String): IntArray?
    
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
     * 获取 RAW 文件的原图尺寸（不解码完整图像）
     * 这是一个快速操作，只读取文件元数据
     * 
     * @return Pair<width, height> 或 null
     */
    fun getRawImageSize(filePath: String): Pair<Int, Int>? {
        return try {
            val size = nativeGetRawImageSize(nativePtr, filePath)
            if (size != null && size.size >= 2 && size[0] > 0 && size[1] > 0) {
                Pair(size[0], size[1])
            } else {
                Log.w(TAG, "Failed to get RAW image size")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting RAW image size", e)
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
    
    private external fun nativeLoadRawWithMetadata(nativePtr: Long, filePath: String): LongArray?
    
    companion object {
        private const val TAG = "RawProcessorNative"
    }
}
