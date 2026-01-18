package com.filmtracker.app.processing

import android.graphics.Bitmap
import com.filmtracker.app.data.BasicAdjustmentParams

/**
 * 阶段处理器接口
 * 
 * 定义每个处理阶段的标准接口，用于：
 * 1. 统一的处理流程
 * 2. 阶段级缓存管理
 * 3. 增量计算支持
 * 
 * 基于设计文档中的 5 阶段固定管线：
 * - TONE_BASE: 基础影调（曝光、高光、阴影、白场、黑场、对比度）
 * - CURVES: 曲线（RGB 曲线、单通道曲线）
 * - COLOR: 色彩（色温、色调、饱和度、HSL、色彩分级）
 * - EFFECTS: 效果（清晰度、纹理、去雾）
 * - DETAILS: 细节（锐化、降噪）
 */
interface StageProcessor {
    
    /**
     * 处理阶段标识
     */
    val stage: ProcessingStage
    
    /**
     * 是否需要缓存此阶段的输出
     * 
     * 根据设计文档：
     * - TONE_BASE, CURVES, COLOR: 不缓存（实时计算足够快）
     * - EFFECTS, DETAILS: 需要缓存（计算密集型）
     */
    val shouldCache: Boolean
        get() = stage.shouldCache
    
    /**
     * 获取此阶段受影响的参数列表
     * 
     * 用于：
     * 1. 参数变化检测
     * 2. 确定是否需要重新计算此阶段
     */
    fun getAffectedParams(): Set<StageParameterMapping.ParameterName>
    
    /**
     * 处理图像
     * 
     * @param input 输入图像（上一阶段的输出或原始图像）
     * @param params 调整参数
     * @return 处理后的图像，如果处理失败返回 null
     */
    fun process(input: Bitmap, params: BasicAdjustmentParams): Bitmap?
    
    /**
     * 检查是否需要执行此阶段
     * 
     * 当所有相关参数都是默认值时，可以跳过此阶段
     * 这是一个重要的优化：跳过不必要的处理
     * 
     * @param params 调整参数
     * @return true 如果需要执行，false 如果可以跳过
     */
    fun shouldExecute(params: BasicAdjustmentParams): Boolean
    
    /**
     * 获取此阶段的缓存键
     * 
     * 用于缓存管理，确保缓存的唯一性
     * 
     * @param params 调整参数
     * @param inputHash 输入图像的哈希值
     * @return 缓存键
     */
    fun getCacheKey(params: BasicAdjustmentParams, inputHash: String): StageCache.CacheKey {
        val paramHash = ParameterHasher.hashParameters(stage, params)
        return StageCache.CacheKey(stage, paramHash, inputHash)
    }
    
    /**
     * 获取阶段描述
     */
    fun getDescription(): String = stage.description
    
    /**
     * 获取预估执行时间（毫秒）
     */
    fun getEstimatedTimeMs(): Long = StageConfig.getConfig(stage).estimatedTimeMs
}

/**
 * 阶段处理器基类
 * 
 * 提供通用的实现，子类只需要实现具体的处理逻辑
 */
abstract class BaseStageProcessor(
    override val stage: ProcessingStage
) : StageProcessor {
    
    override fun getAffectedParams(): Set<StageParameterMapping.ParameterName> {
        return StageParameterMapping.getParametersForStage(stage)
    }
    
    /**
     * 检查参数是否为默认值
     * 子类可以重写此方法提供更精确的检查
     */
    protected abstract fun areParamsDefault(params: BasicAdjustmentParams): Boolean
    
    override fun shouldExecute(params: BasicAdjustmentParams): Boolean {
        return !areParamsDefault(params)
    }
}

/**
 * 阶段处理结果
 */
sealed class StageProcessResult {
    /**
     * 处理成功
     */
    data class Success(
        val output: Bitmap,
        val executionTimeMs: Long,
        val fromCache: Boolean = false
    ) : StageProcessResult()
    
    /**
     * 跳过处理（参数为默认值）
     */
    data class Skipped(
        val output: Bitmap,
        val reason: String = "Parameters are default values"
    ) : StageProcessResult()
    
    /**
     * 处理失败
     */
    data class Failure(
        val error: Throwable,
        val message: String
    ) : StageProcessResult()
}

/**
 * 阶段处理器工厂
 * 
 * 用于创建各阶段的处理器实例
 */
object StageProcessorFactory {
    
    private val processors = mutableMapOf<ProcessingStage, StageProcessor>()
    
    /**
     * 获取指定阶段的处理器
     */
    fun getProcessor(stage: ProcessingStage): StageProcessor {
        return processors.getOrPut(stage) {
            createProcessor(stage)
        }
    }
    
    /**
     * 获取所有阶段的处理器（按顺序）
     */
    fun getAllProcessors(): List<StageProcessor> {
        return ProcessingStage.getOrderedStages().map { getProcessor(it) }
    }
    
    /**
     * 创建处理器实例
     */
    private fun createProcessor(stage: ProcessingStage): StageProcessor {
        return when (stage) {
            ProcessingStage.GEOMETRY -> GeometryProcessor()
            ProcessingStage.TONE_BASE -> ToneBaseProcessor()
            ProcessingStage.CURVES -> CurvesProcessor()
            ProcessingStage.COLOR -> ColorProcessor()
            ProcessingStage.EFFECTS -> EffectsProcessor()
            ProcessingStage.DETAILS -> DetailsProcessor()
        }
    }
    
    /**
     * 清理所有处理器
     */
    fun clear() {
        processors.clear()
    }
}
