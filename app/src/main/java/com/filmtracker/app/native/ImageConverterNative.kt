package com.filmtracker.app.native

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

/**
 * 图像转换器 Native 封装
 */
class ImageConverterNative {
    
    private external fun nativeGetImageSize(imagePtr: Long): IntArray?
    private external fun nativeLinearToSRGB(imagePtr: Long): ByteArray?
    private external fun nativeLinearToSRGBWithDithering(imagePtr: Long): ByteArray?
    private external fun nativeBitmapToLinear(bitmap: Bitmap): Long
    private external fun nativeReleaseImage(imagePtr: Long)
    
    /**
     * 获取图像尺寸
     */
    fun getImageSize(image: LinearImageNative): Pair<Int, Int>? {
        val size = nativeGetImageSize(image.nativePtr) ?: return null
        return Pair(size[0], size[1])
    }
    
    /**
     * 将线性图像转换为 sRGB Bitmap（用于显示）
     */
    fun linearToBitmap(image: LinearImageNative): Bitmap? {
        return try {
            val (width, height) = getImageSize(image) ?: return null
            val rgbaData = nativeLinearToSRGB(image.nativePtr) ?: return null
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val buffer = java.nio.ByteBuffer.wrap(rgbaData)
            bitmap.copyPixelsFromBuffer(buffer)
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting to bitmap", e)
            null
        }
    }
    
    /**
     * 将线性图像转换为 sRGB Bitmap，使用误差扩散抖动
     * 
     * 这个版本使用 Floyd-Steinberg 误差扩散算法来减少色彩断层。
     * 推荐用于最终输出，特别是在渐变区域较多的图像。
     */
    fun linearToBitmapWithDithering(image: LinearImageNative): Bitmap? {
        return try {
            val (width, height) = getImageSize(image) ?: return null
            val rgbaData = nativeLinearToSRGBWithDithering(image.nativePtr) ?: return null
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val buffer = java.nio.ByteBuffer.wrap(rgbaData)
            bitmap.copyPixelsFromBuffer(buffer)
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting to bitmap with dithering", e)
            null
        }
    }
    
    /**
     * 将Bitmap转换为LinearImage（sRGB到线性域）
     */
    fun bitmapToLinear(bitmap: Bitmap): LinearImageNative? {
        return try {
            val imagePtr = nativeBitmapToLinear(bitmap)
            if (imagePtr != 0L) {
                LinearImageNative(imagePtr)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to linear", e)
            null
        }
    }
    
    /**
     * 释放图像资源
     */
    fun release(image: LinearImageNative) {
        nativeReleaseImage(image.nativePtr)
    }
    
    companion object {
        private const val TAG = "ImageConverterNative"
    }
}

/**
 * 线性图像 Native 包装
 */
class LinearImageNative(val nativePtr: Long)
