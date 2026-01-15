package com.filmtracker.app.data.source.native

import android.graphics.Bitmap
import com.filmtracker.app.native.RawProcessorNative

/**
 * Native RAW 处理器
 * 封装 RAW 文件处理相关的 Native 调用
 */
class NativeRawProcessor {
    
    private val rawProcessor = RawProcessorNative()
    
    /**
     * 提取 RAW 预览
     */
    fun extractPreview(filePath: String): Bitmap? {
        return try {
            rawProcessor.extractPreview(filePath)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取 RAW 图像尺寸
     */
    fun getImageSize(filePath: String): Pair<Int, Int>? {
        return try {
            rawProcessor.getRawImageSize(filePath)
        } catch (e: Exception) {
            null
        }
    }
}
