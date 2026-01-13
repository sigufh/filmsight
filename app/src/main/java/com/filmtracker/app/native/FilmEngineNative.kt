package com.filmtracker.app.native

import android.util.Log

/**
 * 胶片引擎 Native 封装
 */
class FilmEngineNative {
    
    private external fun nativeInit(): Long
    private external fun nativeProcess(
        enginePtr: Long,
        imagePtr: Long,
        paramsPtr: Long
    ): Long
    
    private var nativePtr: Long = 0
    
    init {
        nativePtr = nativeInit()
    }
    
    /**
     * 处理图像，应用胶片模拟
     */
    fun process(image: LinearImageNative, params: FilmParamsNative): LinearImageNative? {
        return try {
            val outputPtr = nativeProcess(nativePtr, image.nativePtr, params.nativePtr)
            if (outputPtr != 0L) {
                LinearImageNative(outputPtr)
            } else {
                Log.e(TAG, "Failed to process image")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            null
        }
    }
    
    companion object {
        private const val TAG = "FilmEngineNative"
    }
}
