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

/**
 * 胶片参数 Native 包装
 */
class FilmParamsNative(val nativePtr: Long) {
    
    companion object {
        external fun nativeCreate(): Long
        
        fun create(): FilmParamsNative {
            val ptr = nativeCreate()
            return FilmParamsNative(ptr)
        }
    }
    
    external fun nativeSetParams(
        paramsPtr: Long,
        globalExposure: Float,
        contrast: Float,
        saturation: Float,
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float,
        clarity: Float,
        vibrance: Float
    )
    
    fun setParams(
        globalExposure: Float,
        contrast: Float,
        saturation: Float,
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float,
        clarity: Float,
        vibrance: Float
    ) {
        nativeSetParams(
            nativePtr,
            globalExposure,
            contrast,
            saturation,
            highlights,
            shadows,
            whites,
            blacks,
            clarity,
            vibrance
        )
    }
    
    external fun nativeSetToneCurves(
        paramsPtr: Long,
        enableRgb: Boolean,
        rgbCurve: FloatArray?,
        enableRed: Boolean,
        redCurve: FloatArray?,
        enableGreen: Boolean,
        greenCurve: FloatArray?,
        enableBlue: Boolean,
        blueCurve: FloatArray?
    )
    
    fun setToneCurves(
        enableRgb: Boolean, rgbCurve: FloatArray?,
        enableRed: Boolean, redCurve: FloatArray?,
        enableGreen: Boolean, greenCurve: FloatArray?,
        enableBlue: Boolean, blueCurve: FloatArray?
    ) {
        nativeSetToneCurves(nativePtr, enableRgb, rgbCurve,
                           enableRed, redCurve,
                           enableGreen, greenCurve,
                           enableBlue, blueCurve)
    }
    
    external fun nativeSetHSL(
        paramsPtr: Long,
        enableHSL: Boolean,
        hueShift: FloatArray?,
        saturation: FloatArray?,
        luminance: FloatArray?
    )
    
    fun setHSL(
        enableHSL: Boolean,
        hueShift: FloatArray?,
        saturation: FloatArray?,
        luminance: FloatArray?
    ) {
        nativeSetHSL(nativePtr, enableHSL, hueShift, saturation, luminance)
    }
}
