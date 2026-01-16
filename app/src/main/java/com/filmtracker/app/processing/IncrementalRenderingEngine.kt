package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams

/**
 * 增量渲染引擎
 * 
 * 核心功能：
 * 1. 集成所有阶段处理器（TONE_BASE, CURVES, COLOR, EFFECTS, DETAILS）
 * 2. 实现阶段级缓存管理
 * 3. 实现增量计算逻辑 - 只重新计算受参数变化影响的阶段
 * 
 * 基于 DaVinci Resolve 的智能缓存策略：
 * - 简单调整（TONE_BASE, CURVES, COLOR）实时计算，不缓存
 * - 计算密集型调整（EFFECTS, DETAILS）需要缓存
 * 
 * Requirements: 4.1, 4.4
 */
class IncrementalRenderingEngine(
    private val cacheMaxMemoryBytes: Long = StageCache.DEFAULT_MAX_MEMORY_BYTES
) {
    companion object {
        private const val TAG = "IncrementalRenderingEngine"
        
        @Volatile
        private var instance: IncrementalRenderingEngine? = null
        
        fun getInstance(cacheMaxMemoryBytes: Long = StageCache.DEFAULT_MAX_MEMORY_BYTES): IncrementalRenderingEngine {
            return instance ?: synchronized(this) {
                instance ?: IncrementalRenderingEngine(cacheMaxMemoryBytes).also { instance = it }
            }
        }
        
        fun clearInstance() {
            instance?.release()
            instance = null
        }
    }
    
    // 阶段缓存
    private val stageCache = StageCache(cacheMaxMemoryBytes)
    
    // 参数变化检测器
    private val changeDetector = ParameterChangeDetector.getInstance()
    
    // 性能监控
    private val performanceMonitor = PerformanceMonitor()
    
    // 上一次处理的参数（用于增量计算）
    private var lastParams: BasicAdjustmentParams? = null
    
    // 上一次处理的输入图像哈希（用于检测图像变化）
    private var lastInputHash: String? = null
    
    // 是否启用增量计算
    private var incrementalEnabled: Boolean = true
    
    // 是否启用缓存
    private var cacheEnabled: Boolean = true
    
    /**
     * 处理结果
     */
    data class ProcessingResult(
        val output: Bitmap?,
        val success: Boolean,
        val totalTimeMs: Long,
        val stageResults: List<StageExecutionResult>,
        val cacheHits: Int,
        val cacheMisses: Int,
        val stagesExecuted: Int,
        val stagesSkipped: Int,
        val errorMessage: String? = null
    ) {
        val cacheHitRate: Float
            get() = if (cacheHits + cacheMisses > 0) {
                cacheHits.toFloat() / (cacheHits + cacheMisses)
            } else 0f
    }
    
    /**
     * 处理图像（增量计算）
     * 
     * @param input 输入图像
     * @param params 调整参数
     * @return 处理结果
     */
    fun process(input: Bitmap, params: BasicAdjustmentParams): ProcessingResult {
        val startTime = System.currentTimeMillis()
        val stageResults = mutableListOf<StageExecutionResult>()
        var cacheHits = 0
        var cacheMisses = 0
        var stagesExecuted = 0
        var stagesSkipped = 0
        
        try {
            // 计算输入图像哈希
            val inputHash = ParameterHasher.hashBitmap(input)
            
            // 检测是否需要完整重新计算
            val needsFullRecompute = shouldFullRecompute(inputHash)
            
            // 检测参数变化
            val changeResult = if (needsFullRecompute || !incrementalEnabled) {
                ParameterChangeDetector.ChangeDetectionResult.fullRecompute()
            } else {
                changeDetector.detectChanges(lastParams, params)
            }
            
            Log.d(TAG, "Change detection: hasChanges=${changeResult.hasChanges}, " +
                    "startStage=${changeResult.startStage}, " +
                    "stagesToRecompute=${changeResult.stagesToRecompute.map { it.name }}")
            
            // 如果没有变化，尝试返回缓存的最终结果
            if (!changeResult.hasChanges && lastParams != null) {
                val cachedFinal = getCachedFinalResult(params, inputHash)
                if (cachedFinal != null) {
                    Log.d(TAG, "Returning cached final result")
                    return ProcessingResult(
                        output = cachedFinal,
                        success = true,
                        totalTimeMs = System.currentTimeMillis() - startTime,
                        stageResults = emptyList(),
                        cacheHits = 1,
                        cacheMisses = 0,
                        stagesExecuted = 0,
                        stagesSkipped = ProcessingStage.entries.size
                    )
                }
            }
            
            // 执行增量处理
            var currentBitmap: Bitmap = input
            val orderedStages = ProcessingStage.getOrderedStages()
            
            for (stage in orderedStages) {
                val stageStartTime = System.currentTimeMillis()
                val processor = StageProcessorFactory.getProcessor(stage)
                
                // 检查是否可以使用缓存
                if (cacheEnabled && changeResult.canUseCache(stage) && stage.shouldCache) {
                    val paramHash = ParameterHasher.hashParameters(stage, params)
                    val cachedResult = stageCache.getForStage(stage, paramHash, inputHash)
                    
                    if (cachedResult != null) {
                        currentBitmap = cachedResult
                        cacheHits++
                        stagesSkipped++
                        stageResults.add(StageExecutionResult.success(
                            stage = stage,
                            executionTimeMs = System.currentTimeMillis() - stageStartTime,
                            fromCache = true
                        ))
                        Log.d(TAG, "Stage ${stage.name}: cache hit")
                        continue
                    }
                    cacheMisses++
                }
                
                // 检查是否需要执行此阶段
                if (!processor.shouldExecute(params)) {
                    stagesSkipped++
                    stageResults.add(StageExecutionResult.success(
                        stage = stage,
                        executionTimeMs = 0,
                        fromCache = false
                    ))
                    Log.d(TAG, "Stage ${stage.name}: skipped (default params)")
                    continue
                }
                
                // 执行阶段处理
                val result = processor.process(currentBitmap, params)
                val stageTime = System.currentTimeMillis() - stageStartTime
                
                if (result == null) {
                    Log.e(TAG, "Stage ${stage.name} failed")
                    stageResults.add(StageExecutionResult.failure(
                        stage = stage,
                        executionTimeMs = stageTime,
                        errorMessage = "Processing failed"
                    ))
                    return ProcessingResult(
                        output = null,
                        success = false,
                        totalTimeMs = System.currentTimeMillis() - startTime,
                        stageResults = stageResults,
                        cacheHits = cacheHits,
                        cacheMisses = cacheMisses,
                        stagesExecuted = stagesExecuted,
                        stagesSkipped = stagesSkipped,
                        errorMessage = "Stage ${stage.name} processing failed"
                    )
                }
                
                // 更新当前位图
                if (currentBitmap != input && currentBitmap != result) {
                    currentBitmap.recycle()
                }
                currentBitmap = result
                stagesExecuted++
                
                // 缓存结果（如果需要）
                if (cacheEnabled && stage.shouldCache) {
                    val paramHash = ParameterHasher.hashParameters(stage, params)
                    stageCache.putForStage(stage, paramHash, inputHash, result)
                }
                
                stageResults.add(StageExecutionResult.success(
                    stage = stage,
                    executionTimeMs = stageTime,
                    fromCache = false
                ))
                
                // 记录性能
                performanceMonitor.recordStageExecution(stage, stageTime, false)
                
                Log.d(TAG, "Stage ${stage.name}: executed in ${stageTime}ms")
            }
            
            // 更新状态
            lastParams = params.deepCopy()
            lastInputHash = inputHash
            
            val totalTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordTotalProcessing(totalTime)
            
            Log.d(TAG, "Processing completed in ${totalTime}ms, " +
                    "executed: $stagesExecuted, skipped: $stagesSkipped, " +
                    "cacheHits: $cacheHits, cacheMisses: $cacheMisses")
            
            return ProcessingResult(
                output = currentBitmap,
                success = true,
                totalTimeMs = totalTime,
                stageResults = stageResults,
                cacheHits = cacheHits,
                cacheMisses = cacheMisses,
                stagesExecuted = stagesExecuted,
                stagesSkipped = stagesSkipped
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Processing error", e)
            return ProcessingResult(
                output = null,
                success = false,
                totalTimeMs = System.currentTimeMillis() - startTime,
                stageResults = stageResults,
                cacheHits = cacheHits,
                cacheMisses = cacheMisses,
                stagesExecuted = stagesExecuted,
                stagesSkipped = stagesSkipped,
                errorMessage = e.message
            )
        }
    }

    
    /**
     * 完整处理图像（不使用增量计算）
     * 
     * 用于：
     * 1. 首次处理
     * 2. 图像变化后的处理
     * 3. 强制完整重新计算
     * 
     * @param input 输入图像
     * @param params 调整参数
     * @return 处理结果
     */
    fun processFullCompute(input: Bitmap, params: BasicAdjustmentParams): ProcessingResult {
        val wasIncrementalEnabled = incrementalEnabled
        incrementalEnabled = false
        
        // 清除缓存以确保完整重新计算
        invalidateAllCaches()
        lastParams = null
        lastInputHash = null
        
        try {
            return process(input, params)
        } finally {
            incrementalEnabled = wasIncrementalEnabled
        }
    }
    
    /**
     * 检查是否需要完整重新计算
     */
    private fun shouldFullRecompute(currentInputHash: String): Boolean {
        // 如果输入图像变化，需要完整重新计算
        if (lastInputHash != currentInputHash) {
            Log.d(TAG, "Input image changed, need full recompute")
            invalidateAllCaches()
            return true
        }
        
        // 如果没有上一次的参数，需要完整重新计算
        if (lastParams == null) {
            Log.d(TAG, "No previous params, need full recompute")
            return true
        }
        
        return false
    }
    
    /**
     * 获取缓存的最终结果
     */
    private fun getCachedFinalResult(params: BasicAdjustmentParams, inputHash: String): Bitmap? {
        // 尝试获取最后一个阶段的缓存
        val lastStage = ProcessingStage.DETAILS
        if (lastStage.shouldCache) {
            val paramHash = ParameterHasher.hashParameters(lastStage, params)
            return stageCache.getForStage(lastStage, paramHash, inputHash)
        }
        return null
    }
    
    /**
     * 使所有缓存失效
     */
    fun invalidateAllCaches() {
        stageCache.clear()
        Log.d(TAG, "All caches invalidated")
    }
    
    /**
     * 使指定阶段及其后续阶段的缓存失效
     */
    fun invalidateFromStage(stage: ProcessingStage) {
        stageCache.invalidateFromStage(stage)
        Log.d(TAG, "Caches invalidated from stage ${stage.name}")
    }
    
    /**
     * 设置是否启用增量计算
     */
    fun setIncrementalEnabled(enabled: Boolean) {
        incrementalEnabled = enabled
        Log.d(TAG, "Incremental computing ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 检查是否启用增量计算
     */
    fun isIncrementalEnabled(): Boolean = incrementalEnabled
    
    /**
     * 设置是否启用缓存
     */
    fun setCacheEnabled(enabled: Boolean) {
        cacheEnabled = enabled
        if (!enabled) {
            invalidateAllCaches()
        }
        Log.d(TAG, "Cache ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 检查是否启用缓存
     */
    fun isCacheEnabled(): Boolean = cacheEnabled
    
    /**
     * 获取缓存统计
     */
    fun getCacheStats(): StageCache.CacheStats = stageCache.getStats()
    
    /**
     * 获取性能统计
     */
    fun getPerformanceStats(): PerformanceMonitor.PerformanceStats = performanceMonitor.getStats()
    
    /**
     * 重置性能统计
     */
    fun resetPerformanceStats() {
        performanceMonitor.reset()
        stageCache.resetStats()
    }
    
    /**
     * 获取上一次处理的参数
     */
    fun getLastParams(): BasicAdjustmentParams? = lastParams
    
    /**
     * 预估处理时间
     * 
     * @param oldParams 旧参数（可为 null）
     * @param newParams 新参数
     * @return 预估执行时间（毫秒）
     */
    fun estimateProcessingTime(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams
    ): Long {
        return changeDetector.estimateRecomputeTime(oldParams, newParams)
    }
    
    /**
     * 获取处理计划
     * 
     * @param oldParams 旧参数（可为 null）
     * @param newParams 新参数
     * @return 处理计划
     */
    fun getProcessingPlan(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams
    ): ProcessingPlan {
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        return if (changedParams.isEmpty()) {
            ProcessingPlan(
                startStage = ProcessingStage.TONE_BASE,
                stagesToExecute = emptyList(),
                stagesToCache = emptyList(),
                estimatedTotalTimeMs = 0,
                changedParameters = emptySet()
            )
        } else {
            ProcessingPlan.incrementalProcessing(changedParams)
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        invalidateAllCaches()
        StageProcessorFactory.clear()
        lastParams = null
        lastInputHash = null
        Log.d(TAG, "IncrementalRenderingEngine released")
    }
}

/**
 * 性能监控器
 * 
 * 记录处理性能数据，用于分析和优化
 */
class PerformanceMonitor {
    
    data class PerformanceStats(
        val totalProcessingCount: Long,
        val averageProcessingTimeMs: Long,
        val stageStats: Map<ProcessingStage, StageStats>,
        val cacheHitRate: Float
    )
    
    data class StageStats(
        val stage: ProcessingStage,
        val executionCount: Long,
        val totalTimeMs: Long,
        val averageTimeMs: Long,
        val cacheHitCount: Long,
        val cacheMissCount: Long
    ) {
        val cacheHitRate: Float
            get() = if (cacheHitCount + cacheMissCount > 0) {
                cacheHitCount.toFloat() / (cacheHitCount + cacheMissCount)
            } else 0f
    }
    
    private var totalProcessingCount = 0L
    private var totalProcessingTimeMs = 0L
    private val stageExecutionCounts = mutableMapOf<ProcessingStage, Long>()
    private val stageTotalTimes = mutableMapOf<ProcessingStage, Long>()
    private val stageCacheHits = mutableMapOf<ProcessingStage, Long>()
    private val stageCacheMisses = mutableMapOf<ProcessingStage, Long>()
    
    /**
     * 记录阶段执行
     */
    fun recordStageExecution(stage: ProcessingStage, timeMs: Long, fromCache: Boolean) {
        stageExecutionCounts[stage] = (stageExecutionCounts[stage] ?: 0) + 1
        stageTotalTimes[stage] = (stageTotalTimes[stage] ?: 0) + timeMs
        
        if (fromCache) {
            stageCacheHits[stage] = (stageCacheHits[stage] ?: 0) + 1
        } else {
            stageCacheMisses[stage] = (stageCacheMisses[stage] ?: 0) + 1
        }
    }
    
    /**
     * 记录总处理时间
     */
    fun recordTotalProcessing(timeMs: Long) {
        totalProcessingCount++
        totalProcessingTimeMs += timeMs
    }
    
    /**
     * 获取性能统计
     */
    fun getStats(): PerformanceStats {
        val stageStats = ProcessingStage.entries.associate { stage ->
            val count = stageExecutionCounts[stage] ?: 0
            val totalTime = stageTotalTimes[stage] ?: 0
            val hits = stageCacheHits[stage] ?: 0
            val misses = stageCacheMisses[stage] ?: 0
            
            stage to StageStats(
                stage = stage,
                executionCount = count,
                totalTimeMs = totalTime,
                averageTimeMs = if (count > 0) totalTime / count else 0,
                cacheHitCount = hits,
                cacheMissCount = misses
            )
        }
        
        val totalHits = stageCacheHits.values.sum()
        val totalMisses = stageCacheMisses.values.sum()
        val cacheHitRate = if (totalHits + totalMisses > 0) {
            totalHits.toFloat() / (totalHits + totalMisses)
        } else 0f
        
        return PerformanceStats(
            totalProcessingCount = totalProcessingCount,
            averageProcessingTimeMs = if (totalProcessingCount > 0) {
                totalProcessingTimeMs / totalProcessingCount
            } else 0,
            stageStats = stageStats,
            cacheHitRate = cacheHitRate
        )
    }
    
    /**
     * 重置统计
     */
    fun reset() {
        totalProcessingCount = 0
        totalProcessingTimeMs = 0
        stageExecutionCounts.clear()
        stageTotalTimes.clear()
        stageCacheHits.clear()
        stageCacheMisses.clear()
    }
}
