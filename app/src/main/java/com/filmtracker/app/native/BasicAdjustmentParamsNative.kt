package com.filmtracker.app.native

/**
 * 基础调整参数的 Native 接口
 * 对应 C++ 的 BasicAdjustmentParams 结构
 */
class BasicAdjustmentParamsNative private constructor(private var nativeHandle: Long) {
    
    /**
     * 获取 Native 句柄（用于传递给其他 Native 方法）
     */
    internal val handle: Long
        get() = nativeHandle
    
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
     * 设置基础调整参数（内部方法）
     */
    private external fun nativeSetBasicParams(
        nativeHandle: Long,
        exposure: Float,
        contrast: Float,
        saturation: Float
    )
    
    /**
     * 设置色调调整参数（内部方法）
     */
    private external fun nativeSetToneParams(
        nativeHandle: Long,
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float
    )
    
    /**
     * 设置存在感参数（内部方法）
     */
    private external fun nativeSetPresenceParams(
        nativeHandle: Long,
        clarity: Float,
        vibrance: Float
    )
    
    /**
     * 设置颜色参数（内部方法）
     */
    private external fun nativeSetColorParams(
        nativeHandle: Long,
        temperature: Float,
        tint: Float
    )
    
    /**
     * 设置分级参数（内部方法）
     */
    private external fun nativeSetGradingParams(
        nativeHandle: Long,
        highlightsTemp: Float,
        highlightsTint: Float,
        midtonesTemp: Float,
        midtonesTint: Float,
        shadowsTemp: Float,
        shadowsTint: Float,
        blending: Float,
        balance: Float
    )
    
    /**
     * 设置效果参数（内部方法）
     */
    private external fun nativeSetEffectsParams(
        nativeHandle: Long,
        texture: Float,
        dehaze: Float,
        vignette: Float,
        grain: Float
    )
    
    /**
     * 设置细节参数（内部方法）
     */
    private external fun nativeSetDetailParams(
        nativeHandle: Long,
        sharpening: Float,
        noiseReduction: Float
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
        vibrance: Float,
        temperature: Float,
        tint: Float,
        gradingHighlightsTemp: Float,
        gradingHighlightsTint: Float,
        gradingMidtonesTemp: Float,
        gradingMidtonesTint: Float,
        gradingShadowsTemp: Float,
        gradingShadowsTint: Float,
        gradingBlending: Float,
        gradingBalance: Float,
        texture: Float,
        dehaze: Float,
        vignette: Float,
        grain: Float,
        sharpening: Float,
        noiseReduction: Float
    ) {
        nativeSetBasicParams(nativeHandle, exposure, contrast, saturation)
        nativeSetToneParams(nativeHandle, highlights, shadows, whites, blacks)
        nativeSetPresenceParams(nativeHandle, clarity, vibrance)
        nativeSetColorParams(nativeHandle, temperature, tint)
        nativeSetGradingParams(nativeHandle, gradingHighlightsTemp, gradingHighlightsTint,
            gradingMidtonesTemp, gradingMidtonesTint, gradingShadowsTemp, gradingShadowsTint,
            gradingBlending, gradingBalance)
        nativeSetEffectsParams(nativeHandle, texture, dehaze, vignette, grain)
        nativeSetDetailParams(nativeHandle, sharpening, noiseReduction)
    }
    
    /**
     * 设置色调曲线（动态控制点）- 内部方法
     * @param nativeHandle Native 对象句柄
     * @param channel 通道：0=RGB, 1=Red, 2=Green, 3=Blue
     * @param enable 是否启用该通道的曲线
     * @param xCoords X 坐标数组（0.0-1.0）
     * @param yCoords Y 坐标数组（0.0-1.0）
     */
    private external fun nativeSetToneCurve(
        nativeHandle: Long,
        channel: Int,
        enable: Boolean,
        xCoords: FloatArray,
        yCoords: FloatArray
    )
    
    /**
     * 便捷方法：设置所有曲线通道
     */
    fun setAllToneCurves(
        enableRgb: Boolean, rgbPoints: List<Pair<Float, Float>>,
        enableRed: Boolean, redPoints: List<Pair<Float, Float>>,
        enableGreen: Boolean, greenPoints: List<Pair<Float, Float>>,
        enableBlue: Boolean, bluePoints: List<Pair<Float, Float>>
    ) {
        // RGB 通道
        if (enableRgb && rgbPoints.isNotEmpty()) {
            val xCoords = FloatArray(rgbPoints.size) { rgbPoints[it].first }
            val yCoords = FloatArray(rgbPoints.size) { rgbPoints[it].second }
            nativeSetToneCurve(nativeHandle, 0, true, xCoords, yCoords)
        } else {
            nativeSetToneCurve(nativeHandle, 0, false, floatArrayOf(), floatArrayOf())
        }
        
        // Red 通道
        if (enableRed && redPoints.isNotEmpty()) {
            val xCoords = FloatArray(redPoints.size) { redPoints[it].first }
            val yCoords = FloatArray(redPoints.size) { redPoints[it].second }
            nativeSetToneCurve(nativeHandle, 1, true, xCoords, yCoords)
        } else {
            nativeSetToneCurve(nativeHandle, 1, false, floatArrayOf(), floatArrayOf())
        }
        
        // Green 通道
        if (enableGreen && greenPoints.isNotEmpty()) {
            val xCoords = FloatArray(greenPoints.size) { greenPoints[it].first }
            val yCoords = FloatArray(greenPoints.size) { greenPoints[it].second }
            nativeSetToneCurve(nativeHandle, 2, true, xCoords, yCoords)
        } else {
            nativeSetToneCurve(nativeHandle, 2, false, floatArrayOf(), floatArrayOf())
        }
        
        // Blue 通道
        if (enableBlue && bluePoints.isNotEmpty()) {
            val xCoords = FloatArray(bluePoints.size) { bluePoints[it].first }
            val yCoords = FloatArray(bluePoints.size) { bluePoints[it].second }
            nativeSetToneCurve(nativeHandle, 3, true, xCoords, yCoords)
        } else {
            nativeSetToneCurve(nativeHandle, 3, false, floatArrayOf(), floatArrayOf())
        }
    }
    
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
    fun release() {
        if (nativeHandle != 0L) {
            nativeRelease(nativeHandle)
            nativeHandle = 0L  // 防止双重释放
        }
    }
    
    private external fun nativeRelease(nativeHandle: Long)
    
    protected fun finalize() {
        try {
            release()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
