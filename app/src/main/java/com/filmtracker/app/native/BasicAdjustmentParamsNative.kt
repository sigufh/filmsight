package com.filmtracker.app.native

/**
 * 基础调整参数的 Native 接口
 * 对应 C++ 的 BasicAdjustmentParams 结构
 */
class BasicAdjustmentParamsNative private constructor(private val nativeHandle: Long) {
    
    companion object {
        init {
            System.loadLibrary("filmtracker")
        }
        
        /**
         * 创建新的基础调整参数对象
         */
        fun create(): BasicAdjustmentParamsNative {
            val handle = nativeCreate()
            return BasicAdjustmentParamsNative(handle)
        }
        
        @JvmStatic
        private external fun nativeCreate(): Long
    }
    
    /**
     * 设置基础调整参数
     */
    external fun setBasicParams(
        exposure: Float,
        contrast: Float,
        saturation: Float
    )
    
    /**
     * 设置色调调整参数
     */
    external fun setToneParams(
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float
    )
    
    /**
     * 设置存在感参数
     */
    external fun setPresenceParams(
        clarity: Float,
        vibrance: Float
    )
    
    /**
     * 便捷方法：一次性设置所有基础参数
     */
    fun setParams(
        exposure: Float,
        contrast: Float,
        saturation: Float,
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float,
        clarity: Float,
        vibrance: Float
    ) {
        setBasicParams(exposure, contrast, saturation)
        setToneParams(highlights, shadows, whites, blacks)
        setPresenceParams(clarity, vibrance)
    }
    
    /**
     * 设置色调曲线
     */
    external fun setToneCurves(
        enableRgbCurve: Boolean, rgbCurve: FloatArray,
        enableRedCurve: Boolean, redCurve: FloatArray,
        enableGreenCurve: Boolean, greenCurve: FloatArray,
        enableBlueCurve: Boolean, blueCurve: FloatArray
    )
    
    /**
     * 设置 HSL 调整
     */
    external fun setHSL(
        enableHSL: Boolean,
        hueShift: FloatArray,
        saturation: FloatArray,
        luminance: FloatArray
    )
    
    /**
     * 获取 native 句柄（用于传递给其他 native 方法）
     */
    fun getNativeHandle(): Long = nativeHandle
    
    /**
     * 兼容性属性：nativePtr（指向 nativeHandle）
     */
    val nativePtr: Long
        get() = nativeHandle
    
    /**
     * 释放资源
     */
    external fun release()
    
    protected fun finalize() {
        try {
            release()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
