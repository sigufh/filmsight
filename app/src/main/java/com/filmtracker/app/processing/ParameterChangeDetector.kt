package com.filmtracker.app.processing

import com.filmtracker.app.data.BasicAdjustmentParams

/**
 * 参数变化检测器
 * 
 * 负责：
 * 1. 比较两组参数，检测变化的参数
 * 2. 确定变化的参数属于哪个处理阶段
 * 3. 返回需要重新计算的起始阶段
 * 
 * 这是增量计算的核心组件，用于确定从哪个阶段开始重新处理图像。
 * 
 * Requirements: 3.2, 4.1
 */
class ParameterChangeDetector {
    
    /**
     * 参数变化检测结果
     */
    data class ChangeDetectionResult(
        /** 变化的参数集合 */
        val changedParameters: Set<StageParameterMapping.ParameterName>,
        
        /** 受影响的阶段集合 */
        val affectedStages: Set<ProcessingStage>,
        
        /** 需要重新计算的起始阶段（最早受影响的阶段） */
        val startStage: ProcessingStage?,
        
        /** 需要重新计算的所有阶段（从起始阶段到最后） */
        val stagesToRecompute: List<ProcessingStage>,
        
        /** 是否有任何变化 */
        val hasChanges: Boolean
    ) {
        companion object {
            /**
             * 创建无变化的结果
             */
            fun noChanges(): ChangeDetectionResult {
                return ChangeDetectionResult(
                    changedParameters = emptySet(),
                    affectedStages = emptySet(),
                    startStage = null,
                    stagesToRecompute = emptyList(),
                    hasChanges = false
                )
            }
            
            /**
             * 创建完整重新计算的结果（用于首次处理或 null 旧参数）
             */
            fun fullRecompute(): ChangeDetectionResult {
                return ChangeDetectionResult(
                    changedParameters = StageParameterMapping.getAllParameters(),
                    affectedStages = ProcessingStage.entries.toSet(),
                    startStage = ProcessingStage.TONE_BASE,
                    stagesToRecompute = ProcessingStage.getOrderedStages(),
                    hasChanges = true
                )
            }
        }
        
        /**
         * 获取可以使用缓存的阶段（不需要重新计算的阶段）
         */
        fun getCacheableStages(): List<ProcessingStage> {
            if (startStage == null) {
                return ProcessingStage.getOrderedStages()
            }
            return ProcessingStage.getOrderedStages()
                .filter { it.order < startStage.order }
        }
        
        /**
         * 检查指定阶段是否需要重新计算
         */
        fun needsRecompute(stage: ProcessingStage): Boolean {
            return stagesToRecompute.contains(stage)
        }
        
        /**
         * 检查指定阶段是否可以使用缓存
         */
        fun canUseCache(stage: ProcessingStage): Boolean {
            return !needsRecompute(stage)
        }
    }
    
    /**
     * 检测参数变化
     * 
     * @param oldParams 旧参数（可为 null，表示首次处理）
     * @param newParams 新参数
     * @return 变化检测结果
     */
    fun detectChanges(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams
    ): ChangeDetectionResult {
        // 如果旧参数为 null，需要完整重新计算
        if (oldParams == null) {
            return ChangeDetectionResult.fullRecompute()
        }
        
        // 检测变化的参数
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        
        // 如果没有变化，返回无变化结果
        if (changedParams.isEmpty()) {
            return ChangeDetectionResult.noChanges()
        }
        
        // 确定受影响的阶段
        val affectedStages = changedParams
            .map { StageParameterMapping.getStageForParameter(it) }
            .toSet()
        
        // 确定起始阶段（最早受影响的阶段）
        val startStage = StageParameterMapping.getEarliestAffectedStage(changedParams)
        
        // 确定需要重新计算的所有阶段
        val stagesToRecompute = if (startStage != null) {
            StageParameterMapping.getStagesFromStage(startStage)
        } else {
            emptyList()
        }
        
        return ChangeDetectionResult(
            changedParameters = changedParams,
            affectedStages = affectedStages,
            startStage = startStage,
            stagesToRecompute = stagesToRecompute,
            hasChanges = true
        )
    }
    
    /**
     * 快速检查是否有任何参数变化
     * 
     * @param oldParams 旧参数
     * @param newParams 新参数
     * @return 是否有变化
     */
    fun hasAnyChange(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams
    ): Boolean {
        if (oldParams == null) return true
        return oldParams != newParams
    }
    
    /**
     * 检查指定阶段的参数是否有变化
     * 
     * @param oldParams 旧参数
     * @param newParams 新参数
     * @param stage 要检查的阶段
     * @return 该阶段的参数是否有变化
     */
    fun hasStageChange(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams,
        stage: ProcessingStage
    ): Boolean {
        if (oldParams == null) return true
        
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        val stageParams = StageParameterMapping.getParametersForStage(stage)
        
        return changedParams.any { it in stageParams }
    }
    
    /**
     * 获取指定阶段中变化的参数
     * 
     * @param oldParams 旧参数
     * @param newParams 新参数
     * @param stage 要检查的阶段
     * @return 该阶段中变化的参数集合
     */
    fun getChangedParametersInStage(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams,
        stage: ProcessingStage
    ): Set<StageParameterMapping.ParameterName> {
        if (oldParams == null) {
            return StageParameterMapping.getParametersForStage(stage)
        }
        
        val changedParams = StageParameterMapping.detectChangedParameters(oldParams, newParams)
        val stageParams = StageParameterMapping.getParametersForStage(stage)
        
        return changedParams.filter { it in stageParams }.toSet()
    }
    
    /**
     * 计算需要重新计算的阶段数量
     * 用于性能估算
     * 
     * @param oldParams 旧参数
     * @param newParams 新参数
     * @return 需要重新计算的阶段数量
     */
    fun countStagesToRecompute(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams
    ): Int {
        return detectChanges(oldParams, newParams).stagesToRecompute.size
    }
    
    /**
     * 估算重新计算所需时间（毫秒）
     * 基于 StageConfig 中的预估时间
     * 
     * @param oldParams 旧参数
     * @param newParams 新参数
     * @return 预估执行时间（毫秒）
     */
    fun estimateRecomputeTime(
        oldParams: BasicAdjustmentParams?,
        newParams: BasicAdjustmentParams
    ): Long {
        val result = detectChanges(oldParams, newParams)
        return result.stagesToRecompute.sumOf { stage ->
            StageConfig.getConfig(stage).estimatedTimeMs
        }
    }
    
    companion object {
        /**
         * 单例实例
         */
        @Volatile
        private var instance: ParameterChangeDetector? = null
        
        fun getInstance(): ParameterChangeDetector {
            return instance ?: synchronized(this) {
                instance ?: ParameterChangeDetector().also { instance = it }
            }
        }
    }
}
