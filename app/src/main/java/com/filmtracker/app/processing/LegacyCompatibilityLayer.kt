package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.data.source.native.NativeImageProcessor

/**
 * 向后兼容层
 * 
 * 提供统一接口支持新旧处理系统的平滑切换：
 * - 新系统：IncrementalRenderingEngine（阶段级缓存和增量计算）
 * - 旧系统：NativeImageProcessor 的传统处理逻辑（模块级缓存）
 * 
 * 核心功能：
 * 1. 支持现有的处理接口
 * 2. 自动转换为阶段级处理
 * 3. 错误时自动回退到旧系统
 * 4. 提供开关在新旧系统之间切换
 * 
 * Requirements: 10.2, 10.3
 */
class LegacyCompatibilityLayer(
    private val nativeProcessor: NativeImageProcessor
) {
    companion object {
        private const val TAG = "LegacyCompatibilityLayer"
    }
    
    // 使用模块化系统的开关（默认禁用，逐步迁移）
    private var useModularSystem: Boolean = false
    
    // 错误计数器（用于自动回退）
    private var consecutiveErrors: Int = 0
    private val maxConsecutiveErrors: Int = 3
    
    // 自动回退开关
    private var autoFallbackEnabled: Boolean = true
    
    /**
     * 设置是否使用模块化系统
     * 
     * @param enabled 是否启用模块化系统（IncrementalRenderingEngine）
     * 
     * Requirements: 10.2
     */
    fun setModularSystemEnabled(enabled: Boolean) {
        if (useModularSystem != enabled) {
            useModularSystem = enabled
            nativeProcessor.setIncrementalRendering(enabled)
            
            // 重置错误计数器
            consecutiveErrors = 0
            
            Log.d(TAG, "Modular system ${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    /**
     * 检查是否启用模块化系统
     */
    fun isModularSystemEnabled(): Boolean = useModularSystem
    
    /**
     * 设置是否启用自动回退
     * 
     * @param enabled 是否启用自动回退（当新系统连续失败时自动切换到旧系统）
     * 
     * Requirements: 10.3
     */
    fun setAutoFallbackEnabled(enabled: Boolean) {
        autoFallbackEnabled = enabled
        Log.d(TAG, "Auto fallback ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 处理图像（兼容接口）
     * 
     * 根据配置自动选择处理系统：
     * - 启用模块化系统：使用 IncrementalRenderingEngine
     * - 禁用模块化系统：使用传统处理逻辑
     * - 新系统失败：自动回退到旧系统
     * 
     * @param bitmap 输入图像
     * @param params 调整参数
     * @return 处理后的图像，失败返回 null
     * 
     * Requirements: 10.2, 10.3
     */
    fun process(
        bitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        return if (useModularSystem) {
            processWithFallback(bitmap, params)
        } else {
            // 直接使用旧系统
            processLegacy(bitmap, params)
        }
    }
    
    /**
     * 使用新系统处理，失败时自动回退
     * 
     * Requirements: 10.3
     */
    private fun processWithFallback(
        bitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        return try {
            // 尝试使用新系统
            val result = nativeProcessor.process(bitmap, params)
            
            if (result != null) {
                // 成功，重置错误计数器
                consecutiveErrors = 0
                result
            } else {
                // 失败，增加错误计数器
                consecutiveErrors++
                Log.w(TAG, "Modular system returned null (consecutive errors: $consecutiveErrors)")
                
                // 检查是否需要自动回退
                if (autoFallbackEnabled && consecutiveErrors >= maxConsecutiveErrors) {
                    Log.e(TAG, "Too many consecutive errors, disabling modular system")
                    setModularSystemEnabled(false)
                }
                
                // 回退到旧系统
                Log.w(TAG, "Falling back to legacy processing")
                processLegacy(bitmap, params)
            }
        } catch (e: Exception) {
            // 异常，增加错误计数器
            consecutiveErrors++
            Log.e(TAG, "Error in modular system (consecutive errors: $consecutiveErrors)", e)
            
            // 检查是否需要自动回退
            if (autoFallbackEnabled && consecutiveErrors >= maxConsecutiveErrors) {
                Log.e(TAG, "Too many consecutive errors, disabling modular system")
                setModularSystemEnabled(false)
            }
            
            // 回退到旧系统
            Log.w(TAG, "Falling back to legacy processing")
            processLegacy(bitmap, params)
        }
    }
    
    /**
     * 使用旧系统处理
     * 
     * Requirements: 10.2
     */
    private fun processLegacy(
        bitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        // 确保使用旧系统
        val wasIncremental = nativeProcessor.isIncrementalRenderingEnabled()
        if (wasIncremental) {
            nativeProcessor.setIncrementalRendering(false)
        }
        
        return try {
            nativeProcessor.process(bitmap, params)
        } finally {
            // 恢复设置
            if (wasIncremental) {
                nativeProcessor.setIncrementalRendering(true)
            }
        }
    }
    
    /**
     * 强制使用旧系统处理（用于测试和对比）
     * 
     * @param bitmap 输入图像
     * @param params 调整参数
     * @return 处理后的图像
     * 
     * Requirements: 10.2
     */
    fun processLegacyOnly(
        bitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        return processLegacy(bitmap, params)
    }
    
    /**
     * 强制使用新系统处理（用于测试和对比）
     * 
     * @param bitmap 输入图像
     * @param params 调整参数
     * @return 处理后的图像
     * 
     * Requirements: 10.2
     */
    fun processModularOnly(
        bitmap: Bitmap,
        params: BasicAdjustmentParams
    ): Bitmap? {
        val wasIncremental = nativeProcessor.isIncrementalRenderingEnabled()
        if (!wasIncremental) {
            nativeProcessor.setIncrementalRendering(true)
        }
        
        return try {
            nativeProcessor.process(bitmap, params)
        } finally {
            if (!wasIncremental) {
                nativeProcessor.setIncrementalRendering(false)
            }
        }
    }
    
    /**
     * 获取连续错误次数
     */
    fun getConsecutiveErrors(): Int = consecutiveErrors
    
    /**
     * 重置错误计数器
     */
    fun resetErrorCounter() {
        consecutiveErrors = 0
        Log.d(TAG, "Error counter reset")
    }
    
    /**
     * 获取处理统计信息
     */
    fun getProcessingStats(): ProcessingStats {
        return ProcessingStats(
            useModularSystem = useModularSystem,
            consecutiveErrors = consecutiveErrors,
            autoFallbackEnabled = autoFallbackEnabled,
            incrementalCacheStats = nativeProcessor.getIncrementalCacheStats(),
            incrementalPerformanceStats = nativeProcessor.getIncrementalPerformanceStats()
        )
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        nativeProcessor.clearCache()
    }
    
    /**
     * 释放资源
     */
    fun release() {
        nativeProcessor.release()
    }
    
    /**
     * 处理统计信息
     */
    data class ProcessingStats(
        val useModularSystem: Boolean,
        val consecutiveErrors: Int,
        val autoFallbackEnabled: Boolean,
        val incrementalCacheStats: String?,
        val incrementalPerformanceStats: String?
    ) {
        override fun toString(): String {
            return buildString {
                append("System: ${if (useModularSystem) "Modular" else "Legacy"}\n")
                append("Consecutive errors: $consecutiveErrors\n")
                append("Auto fallback: ${if (autoFallbackEnabled) "enabled" else "disabled"}\n")
                if (incrementalCacheStats != null) {
                    append("$incrementalCacheStats\n")
                }
                if (incrementalPerformanceStats != null) {
                    append(incrementalPerformanceStats)
                }
            }
        }
    }
}
