package com.filmtracker.app.native

import android.util.Log

/**
 * RAW 处理器 Native 封装
 * 桥接 Kotlin 和 C++ 层
 */
class RawProcessorNative {
    
    private external fun nativeInit(): Long
    private external fun nativeLoadRaw(nativePtr: Long, filePath: String): Long
    
    private var nativePtr: Long = 0
    
    init {
        System.loadLibrary("filmtracker")
        nativePtr = nativeInit()
    }
    
    /**
     * 加载 RAW 图像文件
     */
    fun loadRaw(filePath: String): LinearImageNative? {
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
    
    companion object {
        private const val TAG = "RawProcessorNative"
    }
}
