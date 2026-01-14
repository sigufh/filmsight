package com.filmtracker.app.native

/**
 * 图像处理引擎 Native 接口（模块化设计）
 * 
 * 遵循 Adobe Camera RAW / Lightroom 的工作流程：
 * - 每个调整模块独立
 * - 用户主动控制每个模块的应用
 * - 非破坏性处理
 */
class ImageProcessorEngineNative {
    
    private var nativePtr: Long = 0
    
    init {
        nativePtr = nativeInit()
    }
    
    /**
     * 应用基础调整（曝光、对比度、饱和度）
     * 这些是最基础的调整，直接作用于线性 RGB 数据
     */
    fun applyBasicAdjustments(
        image: LinearImageNative,
        exposure: Float,
        contrast: Float,
        saturation: Float
    ) {
        nativeApplyBasicAdjustments(nativePtr, image.nativePtr, exposure, contrast, saturation)
    }
    
    /**
     * 应用色调调整（高光、阴影、白场、黑场）
     */
    fun applyToneAdjustments(
        image: LinearImageNative,
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float
    ) {
        nativeApplyToneAdjustments(nativePtr, image.nativePtr, highlights, shadows, whites, blacks)
    }
    
    /**
     * 应用清晰度和自然饱和度
     */
    fun applyPresence(
        image: LinearImageNative,
        clarity: Float,
        vibrance: Float
    ) {
        nativeApplyPresence(nativePtr, image.nativePtr, clarity, vibrance)
    }
    
    /**
     * 应用色调曲线
     */
    fun applyToneCurves(
        image: LinearImageNative,
        params: BasicAdjustmentParamsNative
    ) {
        nativeApplyToneCurves(nativePtr, image.nativePtr, params.nativePtr)
    }
    
    /**
     * 应用 HSL 调整
     */
    fun applyHSL(
        image: LinearImageNative,
        params: BasicAdjustmentParamsNative
    ) {
        nativeApplyHSL(nativePtr, image.nativePtr, params.nativePtr)
    }
    
    /**
     * 应用胶片模拟（已移除，保留空实现以兼容）
     */
    @Deprecated("Film simulation has been removed")
    fun applyFilmSimulation(
        image: LinearImageNative,
        params: BasicAdjustmentParamsNative,
        metadata: RawMetadataNative? = null
    ) {
        // 空实现，不做任何事
    }
    
    /**
     * 释放资源
     */
    fun release() {
        if (nativePtr != 0L) {
            nativeRelease(nativePtr)
            nativePtr = 0
        }
    }
    
    protected fun finalize() {
        release()
    }
    
    // Native 方法声明
    private external fun nativeInit(): Long
    
    private external fun nativeApplyBasicAdjustments(
        enginePtr: Long,
        imagePtr: Long,
        exposure: Float,
        contrast: Float,
        saturation: Float
    )
    
    private external fun nativeApplyToneAdjustments(
        enginePtr: Long,
        imagePtr: Long,
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float
    )
    
    private external fun nativeApplyPresence(
        enginePtr: Long,
        imagePtr: Long,
        clarity: Float,
        vibrance: Float
    )
    
    private external fun nativeApplyToneCurves(
        enginePtr: Long,
        imagePtr: Long,
        paramsPtr: Long
    )
    
    private external fun nativeApplyHSL(
        enginePtr: Long,
        imagePtr: Long,
        paramsPtr: Long
    )
    
    private external fun nativeRelease(enginePtr: Long)
    
    companion object {
        init {
            System.loadLibrary("filmtracker")
        }
    }
}
