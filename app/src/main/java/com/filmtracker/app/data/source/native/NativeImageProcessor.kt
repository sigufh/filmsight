package com.filmtracker.app.data.source.native

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.native.BasicAdjustmentParamsNative
import com.filmtracker.app.native.ImageConverterNative
import com.filmtracker.app.native.ImageProcessorEngineNative
import com.filmtracker.app.native.LinearImageNative
import com.filmtracker.app.native.ParallelProcessorNative
import com.filmtracker.app.processing.IncrementalRenderingEngine

/**
 * 处理模块枚举
 */
enum class ProcessingModule {
    BASIC,      // 基础调整（曝光、对比度、饱和度、色调调整、清晰度、自然饱和度）
    CURVES,     // 曲线
    HSL,        // HSL 调整
    COLOR,      // 颜色调整（色温、色调、分级）
    EFFECTS,    // 效果（纹理、去雾、晕影、颗粒）
    DETAILS     // 细节（锐化、降噪）
}

/**
 * Native 图像处理器
 * 封装所有 Native 调用，提供简洁的接口
 * 
 * 增量处理优化：
 * - 支持新的 IncrementalRenderingEngine（阶段级缓存和增量计算）
 * - 保留旧的处理逻辑作为回退
 * - 缓存线性空间图像，避免重复 Bitmap→Linear 转换
 * - 缓存基础调整后的图像
 * - 当只有高级调整变化时，跳过基础调整步骤
 * 
 * 分辨率分层优化：
 * - 预览模式使用低分辨率图像（大幅提升响应速度）
 * - 导出模式使用原始分辨率（保证最终质量）
 * 
 * 注意：高级调整（曲线、HSL、颜色、效果、细节）是顺序应用的，
 * 必须全部重新应用以保持正确性。
 * 
 * Requirements: 10.1, 10.4
 */
open class NativeImageProcessor {
    
    private val processorEngine = ImageProcessorEngineNative()
    private val imageConverter = ImageConverterNative()
    private val parallelProcessor = ParallelProcessorNative()
    
    // 增量渲染引擎（新系统）
    private var incrementalEngine: IncrementalRenderingEngine? = null
    
    // 并行处理开关
    private var useParallelProcessing: Boolean = true
    
    // 增量计算开关（Requirements: 10.1）
    private var useIncrementalRendering: Boolean = false
    
    // 预览模式配置
    private var isPreviewMode: Boolean = true
    private var previewMaxWidth: Int = 1920
    private var previewMaxHeight: Int = 1080
    
    // 缓存：原始 Bitmap
    private var cachedOriginalBitmap: Bitmap? = null
    
    // 缓存：预览 Bitmap
    private var cachedPreviewBitmap: Bitmap? = null
    
    // 缓存：线性空间图像
    private var cachedLinearImage: LinearImageNative? = null
    
    // 缓存：上一次的参数
    private var cachedParams: BasicAdjustmentParams? = null
    
    // 缓存：基础调整后的图像
    private var cachedAfterBasic: LinearImageNative? = null
    
    /**
     * 设置预览模式
     * 
     * @param enabled 是否启用预览模式
     * @param maxWidth 预览最大宽度（默认 1920）
     * @param maxHeight 预览最大高度（默认 1080）
     */
    fun setPreviewMode(enabled: Boolean, maxWidth: Int = 1920, maxHeight: Int = 1080) {
        if (isPreviewMode != enabled || previewMaxWidth != maxWidth || previewMaxHeight != maxHeight) {
            isPreviewMode = enabled
            previewMaxWidth = maxWidth
            previewMaxHeight = maxHeight
            
            // 清理预览缓存
            cachedPreviewBitmap?.recycle()
            cachedPreviewBitmap = null
            
            // 清理处理缓存（因为分辨率变化了）
            clearCache()
            
            Log.d(TAG, "Preview mode: $enabled, max size: ${maxWidth}x${maxHeight}")
        }
    }
    
    /**
     * 设置是否使用并行处理
     * 
     * @param enabled 是否启用并行处理（多线程 + SIMD）
     */
    fun setParallelProcessing(enabled: Boolean) {
        if (useParallelProcessing != enabled) {
            useParallelProcessing = enabled
            Log.d(TAG, "Parallel processing: $enabled (threads: ${parallelProcessor.getNumThreads()})")
        }
    }
    
    /**
     * 设置是否使用增量渲染
     * 
     * @param enabled 是否启用增量渲染（阶段级缓存和增量计算）
     * 
     * Requirements: 10.1
     */
    open fun setIncrementalRendering(enabled: Boolean) {
        if (useIncrementalRendering != enabled) {
            useIncrementalRendering = enabled
            
            if (enabled && incrementalEngine == null) {
                // 延迟初始化增量渲染引擎
                incrementalEngine = IncrementalRenderingEngine.getInstance()
                Log.d(TAG, "IncrementalRenderingEngine initialized")
            }
            
            Log.d(TAG, "Incremental rendering: $enabled")
        }
    }
    
    /**
     * 检查是否启用增量渲染
     */
    open fun isIncrementalRenderingEnabled(): Boolean = useIncrementalRendering
    
    /**
     * 获取并行处理器线程数
     */
    fun getParallelThreadCount(): Int {
        return parallelProcessor.getNumThreads()
    }
    
    /**
     * 获取或创建预览图像
     */
    private fun getOrCreatePreviewBitmap(original: Bitmap): Bitmap {
        // 如果原图已经小于等于预览尺寸，直接返回
        if (original.width <= previewMaxWidth && original.height <= previewMaxHeight) {
            return original
        }
        
        // 检查缓存
        if (cachedPreviewBitmap != null && cachedOriginalBitmap === original) {
            return cachedPreviewBitmap!!
        }
        
        // 计算预览尺寸（保持宽高比）
        val ratio = minOf(
            previewMaxWidth.toFloat() / original.width,
            previewMaxHeight.toFloat() / original.height,
            1.0f  // 不放大
        )
        
        val newWidth = (original.width * ratio).toInt()
        val newHeight = (original.height * ratio).toInt()
        
        Log.d(TAG, "Creating preview: ${original.width}x${original.height} → ${newWidth}x${newHeight} (ratio: $ratio)")
        
        // 创建预览图像（使用高质量缩放）
        cachedPreviewBitmap?.recycle()
        cachedPreviewBitmap = Bitmap.createScaledBitmap(
            original,
            newWidth,
            newHeight,
            true  // 使用双线性插值
        )
        
        return cachedPreviewBitmap!!
    }
    
    /**
     * 处理图像（增量处理优化版本）
     * 
     * 根据预览模式自动选择处理分辨率：
     * - 预览模式：使用低分辨率（快速响应）
     * - 导出模式：使用原始分辨率（保证质量）
     * 
     * 根据增量渲染开关选择处理引擎：
     * - 启用增量渲染：使用 IncrementalRenderingEngine（阶段级缓存）
     * - 禁用增量渲染：使用旧的处理逻辑（模块级缓存）
     * 
     * Requirements: 10.1, 10.4
     */
    open fun process(
        originalBitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        // 根据预览模式选择输入图像
        val inputBitmap = if (isPreviewMode) {
            getOrCreatePreviewBitmap(originalBitmap)
        } else {
            originalBitmap
        }
        
        // 根据增量渲染开关选择处理引擎
        return if (useIncrementalRendering && incrementalEngine != null) {
            processWithIncrementalEngine(inputBitmap, params)
        } else {
            processInternal(inputBitmap, params)
        }
    }
    
    /**
     * 使用增量渲染引擎处理图像
     * 
     * Requirements: 10.1
     */
    private fun processWithIncrementalEngine(
        bitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        return try {
            val result = incrementalEngine!!.process(bitmap, params)
            
            if (result.success) {
                Log.d(TAG, "Incremental rendering: ${result.totalTimeMs}ms, " +
                        "executed: ${result.stagesExecuted}, skipped: ${result.stagesSkipped}, " +
                        "cacheHitRate: ${String.format("%.1f%%", result.cacheHitRate * 100)}")
                result.output
            } else {
                Log.e(TAG, "Incremental rendering failed: ${result.errorMessage}")
                // 回退到旧系统（Requirements: 10.5）
                Log.w(TAG, "Falling back to legacy processing")
                processInternal(bitmap, params)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in incremental rendering, falling back to legacy", e)
            // 回退到旧系统（Requirements: 10.5）
            processInternal(bitmap, params)
        }
    }
    
    /**
     * 内部处理方法（原有的 process 逻辑）
     */
    private fun processInternal(
        bitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        var nativeParams: BasicAdjustmentParamsNative? = null
        
        try {
            // 先应用几何（旋转、裁剪），确保与增量管线一致
            val processingBitmap = applyGeometryIfNeeded(bitmap, params) ?: return null
            
            // 检查是否需要重新转换
            val needsConversion = cachedOriginalBitmap !== processingBitmap || cachedLinearImage == null
            
            if (needsConversion) {
                // 清理旧缓存
                clearCache()
                
                // 转换到线性空间
                cachedLinearImage = imageConverter.bitmapToLinear(processingBitmap) 
                    ?: return null
                cachedOriginalBitmap = processingBitmap
                cachedParams = null // 重置参数缓存
            }
            
            val linearImage = cachedLinearImage!!
            
            // 检查哪些模块需要重新处理
            val changedModules = detectChangedModules(cachedParams, params)
            
            // 如果基础调整变化，需要从头开始
            if (changedModules.contains(ProcessingModule.BASIC) || cachedAfterBasic == null) {
                // 从原始线性图像开始
                val workingImage = cloneLinearImage(linearImage)
                
                // 应用基础调整
                applyBasicAdjustments(workingImage, params)
                
                // 缓存基础调整后的结果
                cachedAfterBasic?.let { imageConverter.release(it) }
                cachedAfterBasic = cloneLinearImage(workingImage)
                
                // 应用所有高级调整
                if (needsAdvancedAdjustments(params)) {
                    nativeParams = convertToNativeParams(params)
                    applyAllAdvancedAdjustments(workingImage, params, nativeParams)
                }
                
                // 转换回 sRGB
                val result = imageConverter.linearToBitmap(workingImage)
                imageConverter.release(workingImage)
                
                cachedParams = params.copy()
                return result
                
            } else {
                // 只有高级调整变化，从缓存的基础调整结果开始
                val workingImage = cloneLinearImage(cachedAfterBasic!!)
                
                // 应用所有高级调整（必须全部应用以保持正确性）
                if (needsAdvancedAdjustments(params)) {
                    nativeParams = convertToNativeParams(params)
                    applyAllAdvancedAdjustments(workingImage, params, nativeParams)
                }
                
                // 转换回 sRGB
                val result = imageConverter.linearToBitmap(workingImage)
                imageConverter.release(workingImage)
                
                cachedParams = params.copy()
                return result
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            clearCache()
            return null
        } finally {
            nativeParams?.release()
        }
    }
    
    /**
     * 克隆线性图像
     */
    private fun cloneLinearImage(source: LinearImageNative): LinearImageNative {
        // 创建新的线性图像并复制数据
        return imageConverter.cloneLinearImage(source)
    }
    
    /**
     * 检测变化的模块
     */
    private fun detectChangedModules(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams
    ): Set<ProcessingModule> {
        if (oldParams == null) {
            return ProcessingModule.entries.toSet()
        }
        
        val changed = mutableSetOf<ProcessingModule>()
        
        // 基础调整
        if (oldParams.globalExposure != newParams.globalExposure ||
            oldParams.contrast != newParams.contrast ||
            oldParams.saturation != newParams.saturation ||
            oldParams.highlights != newParams.highlights ||
            oldParams.shadows != newParams.shadows ||
            oldParams.whites != newParams.whites ||
            oldParams.blacks != newParams.blacks ||
            oldParams.clarity != newParams.clarity ||
            oldParams.vibrance != newParams.vibrance) {
            changed.add(ProcessingModule.BASIC)
        }
        
        // 曲线
        if (oldParams.enableRgbCurve != newParams.enableRgbCurve ||
            oldParams.rgbCurvePoints != newParams.rgbCurvePoints ||
            oldParams.enableRedCurve != newParams.enableRedCurve ||
            oldParams.redCurvePoints != newParams.redCurvePoints ||
            oldParams.enableGreenCurve != newParams.enableGreenCurve ||
            oldParams.greenCurvePoints != newParams.greenCurvePoints ||
            oldParams.enableBlueCurve != newParams.enableBlueCurve ||
            oldParams.blueCurvePoints != newParams.blueCurvePoints) {
            changed.add(ProcessingModule.CURVES)
        }
        
        // HSL
        if (oldParams.enableHSL != newParams.enableHSL ||
            !oldParams.hslHueShift.contentEquals(newParams.hslHueShift) ||
            !oldParams.hslSaturation.contentEquals(newParams.hslSaturation) ||
            !oldParams.hslLuminance.contentEquals(newParams.hslLuminance)) {
            changed.add(ProcessingModule.HSL)
        }
        
        // 颜色调整
        if (oldParams.temperature != newParams.temperature ||
            oldParams.tint != newParams.tint ||
            oldParams.gradingHighlightsTemp != newParams.gradingHighlightsTemp ||
            oldParams.gradingHighlightsTint != newParams.gradingHighlightsTint ||
            oldParams.gradingMidtonesTemp != newParams.gradingMidtonesTemp ||
            oldParams.gradingMidtonesTint != newParams.gradingMidtonesTint ||
            oldParams.gradingShadowsTemp != newParams.gradingShadowsTemp ||
            oldParams.gradingShadowsTint != newParams.gradingShadowsTint ||
            oldParams.gradingBlending != newParams.gradingBlending ||
            oldParams.gradingBalance != newParams.gradingBalance) {
            changed.add(ProcessingModule.COLOR)
        }
        
        // 效果
        if (oldParams.texture != newParams.texture ||
            oldParams.dehaze != newParams.dehaze ||
            oldParams.vignette != newParams.vignette ||
            oldParams.grain != newParams.grain) {
            changed.add(ProcessingModule.EFFECTS)
        }
        
        // 细节
        if (oldParams.sharpening != newParams.sharpening ||
            oldParams.noiseReduction != newParams.noiseReduction) {
            changed.add(ProcessingModule.DETAILS)
        }
        
        return changed
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        cachedLinearImage?.let { imageConverter.release(it) }
        cachedLinearImage = null
        
        cachedAfterBasic?.let { imageConverter.release(it) }
        cachedAfterBasic = null
        
        cachedPreviewBitmap?.recycle()
        cachedPreviewBitmap = null
        
        cachedOriginalBitmap = null
        cachedParams = null
        
        // 清理增量渲染引擎缓存
        incrementalEngine?.invalidateAllCaches()
    }
    
    /**
     * 获取增量渲染引擎的缓存统计
     * 
     * Requirements: 10.1
     */
    open fun getIncrementalCacheStats(): String? {
        return incrementalEngine?.let { engine ->
            val stats = engine.getCacheStats()
            "Cache: ${stats.totalEntries} entries, " +
                    "${stats.memoryUsageBytes / 1024 / 1024}MB, " +
                    "hitRate: ${String.format("%.1f%%", stats.hitRate * 100)}"
        }
    }
    
    /**
     * 获取增量渲染引擎的性能统计
     * 
     * Requirements: 10.1
     */
    open fun getIncrementalPerformanceStats(): String? {
        return incrementalEngine?.let { engine ->
            val stats = engine.getPerformanceStats()
            "Performance: ${stats.totalProcessingCount} processes, " +
                    "avg: ${stats.averageProcessingTimeMs}ms, " +
                    "cacheHitRate: ${String.format("%.1f%%", stats.cacheHitRate * 100)}"
        }
    }
    
    /**
     * 释放所有资源
     */
    fun release() {
        clearCache()
        parallelProcessor.release()
        incrementalEngine?.release()
        incrementalEngine = null
    }
    
    private fun applyBasicAdjustments(
        linearImage: LinearImageNative,
        params: BasicAdjustmentParams
    ) {
        if (useParallelProcessing) {
            // 使用并行处理器（多线程 + SIMD）
            // 注意：并行处理器目前只支持基础调整（曝光、对比度、饱和度、色温、色调）
            // 其他调整（色调调整、清晰度、自然饱和度）仍使用原有引擎
            
            // 创建临时参数对象
            val nativeParams = BasicAdjustmentParamsNative.create()
            try {
                nativeParams.setParams(
                    params.globalExposure, params.contrast, params.saturation,
                    0f, 0f, 0f, 0f,  // 色调调整由原引擎处理
                    0f, 0f,  // 清晰度和自然饱和度由原引擎处理
                    params.temperature, params.tint,
                    0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,  // 分级由原引擎处理
                    0f, 0f, 0f, 0f, 0f, 0f  // 效果和细节由原引擎处理
                )
                
                // 使用并行处理器处理基础调整
                parallelProcessor.process(
                    linearImage.nativePtr,
                    linearImage.nativePtr,  // 原地处理
                    nativeParams.handle
                )
            } finally {
                nativeParams.release()
            }
            
            // 使用原引擎处理色调调整
            processorEngine.applyToneAdjustments(
                linearImage,
                params.highlights,
                params.shadows,
                params.whites,
                params.blacks
            )
            
            // 使用原引擎处理清晰度和自然饱和度
            processorEngine.applyPresence(
                linearImage,
                params.clarity,
                params.vibrance
            )
        } else {
            // 使用原有的单线程处理器
            processorEngine.applyBasicAdjustments(
                linearImage,
                params.globalExposure,
                params.contrast,
                params.saturation
            )
            
            processorEngine.applyToneAdjustments(
                linearImage,
                params.highlights,
                params.shadows,
                params.whites,
                params.blacks
            )
            
            processorEngine.applyPresence(
                linearImage,
                params.clarity,
                params.vibrance
            )
        }
    }
    
    /**
     * 应用所有高级调整
     * 注意：高级调整是顺序应用的，必须全部应用以保持正确性
     */
    private fun applyAllAdvancedAdjustments(
        linearImage: LinearImageNative,
        params: BasicAdjustmentParams,
        nativeParams: BasicAdjustmentParamsNative
    ) {
        // 曲线
        if (params.enableRgbCurve || params.enableRedCurve || 
            params.enableGreenCurve || params.enableBlueCurve) {
            processorEngine.applyToneCurves(linearImage, nativeParams)
        }
        
        // HSL
        if (params.enableHSL) {
            processorEngine.applyHSL(linearImage, nativeParams)
        }
        
        // 颜色调整（色温、色调、分级）
        processorEngine.applyColorAdjustments(linearImage, nativeParams)
        
        // 效果（纹理、去雾、晕影、颗粒）
        processorEngine.applyEffects(linearImage, nativeParams)
        
        // 细节（锐化、降噪）
        processorEngine.applyDetails(linearImage, nativeParams)
    }
    
    private fun needsAdvancedAdjustments(params: BasicAdjustmentParams): Boolean {
        return params.enableRgbCurve || params.enableRedCurve || 
               params.enableGreenCurve || params.enableBlueCurve ||
               params.enableHSL ||
               params.temperature != 0f || params.tint != 0f ||
               params.gradingHighlightsTemp != 0f || params.gradingHighlightsTint != 0f ||
               params.gradingMidtonesTemp != 0f || params.gradingMidtonesTint != 0f ||
               params.gradingShadowsTemp != 0f || params.gradingShadowsTint != 0f ||
               params.texture != 0f || params.dehaze != 0f || 
               params.vignette != 0f || params.grain != 0f ||
               params.sharpening != 0f || params.noiseReduction != 0f
    }
    
    private fun convertToNativeParams(params: BasicAdjustmentParams): BasicAdjustmentParamsNative {
        val nativeParams = BasicAdjustmentParamsNative.create()
        
        nativeParams.setParams(
            params.globalExposure, params.contrast, params.saturation,
            params.highlights, params.shadows, params.whites, params.blacks,
            params.clarity, params.vibrance,
            params.temperature, params.tint,
            params.gradingHighlightsTemp, params.gradingHighlightsTint,
            params.gradingMidtonesTemp, params.gradingMidtonesTint,
            params.gradingShadowsTemp, params.gradingShadowsTint,
            params.gradingBlending, params.gradingBalance,
            params.texture, params.dehaze, params.vignette, params.grain,
            params.sharpening, params.noiseReduction
        )
        
        nativeParams.setAllToneCurves(
            params.enableRgbCurve, params.rgbCurvePoints,
            params.enableRedCurve, params.redCurvePoints,
            params.enableGreenCurve, params.greenCurvePoints,
            params.enableBlueCurve, params.blueCurvePoints
        )
        
        // 安全设置 HSL
        try {
            val hueShift = if (params.hslHueShift.size == 8) params.hslHueShift 
                          else FloatArray(8) { 0f }
            val saturation = if (params.hslSaturation.size == 8) params.hslSaturation 
                            else FloatArray(8) { 0f }
            val luminance = if (params.hslLuminance.size == 8) params.hslLuminance 
                           else FloatArray(8) { 0f }
            
            nativeParams.setHSL(params.enableHSL, hueShift, saturation, luminance)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting HSL parameters", e)
            nativeParams.setHSL(false, FloatArray(8) { 0f }, FloatArray(8) { 0f }, FloatArray(8) { 0f })
        }
        
        return nativeParams
    }
    
    companion object {
        private const val TAG = "NativeImageProcessor"
    }

    /**
     * 如果需要，应用几何变换（旋转、裁剪）。
     */
    private fun applyGeometryIfNeeded(bitmap: Bitmap?, params: BasicAdjustmentParams): Bitmap? {
        bitmap ?: return null
        var working = bitmap
        var owns = false
        // 旋转
        val r = normalizeRotation(params.rotation)
        if (kotlin.math.abs(r) > 0.001f) {
            val m = Matrix()
            m.postRotate(r)
            val rotated = Bitmap.createBitmap(working, 0, 0, working.width, working.height, m, true)
            if (owns) working.recycle()
            working = rotated
            owns = true
        }
        // 裁剪
        if (params.cropEnabled) {
            val l = params.cropLeft.coerceIn(0f, 1f)
            val t = params.cropTop.coerceIn(0f, 1f)
            val rgt = params.cropRight.coerceIn(0f, 1f)
            val btm = params.cropBottom.coerceIn(0f, 1f)
            val leftPx = (l * working.width).toInt().coerceIn(0, working.width - 1)
            val topPx = (t * working.height).toInt().coerceIn(0, working.height - 1)
            val rightPx = (rgt * working.width).toInt().coerceIn(leftPx + 1, working.width)
            val bottomPx = (btm * working.height).toInt().coerceIn(topPx + 1, working.height)
            val w = (rightPx - leftPx).coerceAtLeast(1)
            val h = (bottomPx - topPx).coerceAtLeast(1)
            val cropped = Bitmap.createBitmap(working, leftPx, topPx, w, h)
            if (owns) working.recycle()
            working = cropped
            owns = true
        }
        return working
    }

    private fun normalizeRotation(deg: Float): Float {
        var r = deg % 360f
        if (r > 180f) r -= 360f
        if (r < -180f) r += 360f
        return r
    }
}
