package com.filmtracker.app.processing

/**
 * 阶段配置数据类
 * 
 * 包含每个处理阶段的详细配置信息
 */
data class StageConfig(
    val stage: ProcessingStage,
    val parameters: Set<StageParameterMapping.ParameterName>,
    val shouldCache: Boolean,
    val estimatedTimeMs: Long,
    val description: String
) {
    companion object {
        /**
         * 获取所有阶段的配置
         */
        fun getAllConfigs(): List<StageConfig> {
            return ProcessingStage.getOrderedStages().map { stage ->
                StageConfig(
                    stage = stage,
                    parameters = StageParameterMapping.getParametersForStage(stage),
                    shouldCache = stage.shouldCache,
                    estimatedTimeMs = getEstimatedTime(stage),
                    description = stage.description
                )
            }
        }
        
        /**
         * 获取指定阶段的配置
         */
        fun getConfig(stage: ProcessingStage): StageConfig {
            return StageConfig(
                stage = stage,
                parameters = StageParameterMapping.getParametersForStage(stage),
                shouldCache = stage.shouldCache,
                estimatedTimeMs = getEstimatedTime(stage),
                description = stage.description
            )
        }
        
        /**
         * 获取阶段的预估执行时间（毫秒）
         * 基于设计文档中的性能分析
         */
        private fun getEstimatedTime(stage: ProcessingStage): Long {
            return when (stage) {
                ProcessingStage.TONE_BASE -> 5L   // SIMD 优化，非常快
                ProcessingStage.CURVES -> 3L      // LUT 查找，非常快
                ProcessingStage.COLOR -> 8L       // 颜色变换，较快
                ProcessingStage.EFFECTS -> 25L    // 卷积运算，较慢
                ProcessingStage.DETAILS -> 30L    // 计算最密集
            }
        }
    }
    
    /**
     * 检查参数是否属于此阶段
     */
    fun containsParameter(parameter: StageParameterMapping.ParameterName): Boolean {
        return parameters.contains(parameter)
    }
    
    /**
     * 获取参数数量
     */
    fun getParameterCount(): Int {
        return parameters.size
    }
}

/**
 * 阶段执行结果
 */
data class StageExecutionResult(
    val stage: ProcessingStage,
    val executionTimeMs: Long,
    val fromCache: Boolean,
    val success: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success(stage: ProcessingStage, executionTimeMs: Long, fromCache: Boolean): StageExecutionResult {
            return StageExecutionResult(
                stage = stage,
                executionTimeMs = executionTimeMs,
                fromCache = fromCache,
                success = true
            )
        }
        
        fun failure(stage: ProcessingStage, executionTimeMs: Long, errorMessage: String): StageExecutionResult {
            return StageExecutionResult(
                stage = stage,
                executionTimeMs = executionTimeMs,
                fromCache = false,
                success = false,
                errorMessage = errorMessage
            )
        }
    }
}

/**
 * 处理计划
 * 描述需要执行哪些阶段以及从哪里开始
 */
data class ProcessingPlan(
    val startStage: ProcessingStage,
    val stagesToExecute: List<ProcessingStage>,
    val stagesToCache: List<ProcessingStage>,
    val estimatedTotalTimeMs: Long,
    val changedParameters: Set<StageParameterMapping.ParameterName>
) {
    companion object {
        /**
         * 创建完整处理计划（从头开始）
         */
        fun fullProcessing(): ProcessingPlan {
            val allStages = ProcessingStage.getOrderedStages()
            val cacheableStages = ProcessingStage.getCacheableStages()
            val totalTime = allStages.sumOf { StageConfig.getConfig(it).estimatedTimeMs }
            
            return ProcessingPlan(
                startStage = ProcessingStage.TONE_BASE,
                stagesToExecute = allStages,
                stagesToCache = cacheableStages,
                estimatedTotalTimeMs = totalTime,
                changedParameters = StageParameterMapping.getAllParameters()
            )
        }
        
        /**
         * 创建增量处理计划
         */
        fun incrementalProcessing(
            changedParams: Set<StageParameterMapping.ParameterName>
        ): ProcessingPlan {
            val startStage = StageParameterMapping.getEarliestAffectedStage(changedParams)
                ?: return ProcessingPlan(
                    startStage = ProcessingStage.TONE_BASE,
                    stagesToExecute = emptyList(),
                    stagesToCache = emptyList(),
                    estimatedTotalTimeMs = 0,
                    changedParameters = emptySet()
                )
            
            val stagesToExecute = StageParameterMapping.getStagesFromStage(startStage)
            val stagesToCache = stagesToExecute.filter { it.shouldCache }
            val totalTime = stagesToExecute.sumOf { StageConfig.getConfig(it).estimatedTimeMs }
            
            return ProcessingPlan(
                startStage = startStage,
                stagesToExecute = stagesToExecute,
                stagesToCache = stagesToCache,
                estimatedTotalTimeMs = totalTime,
                changedParameters = changedParams
            )
        }
    }
    
    /**
     * 是否需要执行任何阶段
     */
    fun hasWork(): Boolean {
        return stagesToExecute.isNotEmpty()
    }
    
    /**
     * 获取跳过的阶段（使用缓存的阶段）
     */
    fun getSkippedStages(): List<ProcessingStage> {
        return ProcessingStage.getOrderedStages()
            .filter { it.order < startStage.order }
    }
}
