package com.filmtracker.app.native

/**
 * 双边滤波器配置和统计信息 Native 接口
 * 提供配置管理、性能监控和缓存控制功能
 */
object BilateralFilterNative {
    
    /**
     * 双边滤波器配置
     * @param enableCache 启用结果缓存
     * @param enableFastApproximation 启用快速近似算法
     * @param enableGPU 启用GPU加速
     * @param maxCacheSize 最大缓存条目数
     * @param maxCacheMemoryMB 最大缓存内存(MB)
     * @param fastApproxThreshold 快速近似触发阈值
     * @param gpuThresholdPixels GPU加速触发阈值(像素)
     */
    data class Config @JvmOverloads constructor(
        val enableCache: Boolean = true,
        val enableFastApproximation: Boolean = true,
        val enableGPU: Boolean = true,
        val maxCacheSize: Int = 100,
        val maxCacheMemoryMB: Int = 512,
        val fastApproxThreshold: Float = 4.5f,
        val gpuThresholdPixels: Int = 1_500_000
    )
    
    /**
     * 双边滤波器统计信息
     * @param totalCalls 总调用次数
     * @param standardCalls 标准CPU实现调用次数
     * @param fastApproxCalls 快速近似调用次数
     * @param gpuCalls GPU加速调用次数
     * @param cacheHits 缓存命中次数
     * @param cacheMisses 缓存未命中次数
     * @param avgProcessingTimeMs 平均处理时间(毫秒)
     */
    data class Stats(
        val totalCalls: Long,
        val standardCalls: Long,
        val fastApproxCalls: Long,
        val gpuCalls: Long,
        val cacheHits: Long,
        val cacheMisses: Long,
        val avgProcessingTimeMs: Double
    )
    
    /**
     * 设置配置（便捷方法）
     * @param config 配置对象
     */
    fun setConfig(config: Config) {
        nativeSetConfig(
            config.enableCache,
            config.enableFastApproximation,
            config.enableGPU,
            config.maxCacheSize,
            config.maxCacheMemoryMB,
            config.fastApproxThreshold,
            config.gpuThresholdPixels
        )
    }
    
    /**
     * 初始化默认配置（便捷方法）
     * 启用所有优化选项
     */
    fun initializeDefaults() {
        nativeInitializeDefaultConfig()
    }
    
    // Native 方法声明
    
    /**
     * 设置双边滤波器配置
     */
    external fun nativeSetConfig(
        enableCache: Boolean,
        enableFastApproximation: Boolean,
        enableGPU: Boolean,
        maxCacheSize: Int,
        maxCacheMemoryMB: Int,
        fastApproxThreshold: Float,
        gpuThresholdPixels: Int
    )
    
    /**
     * 获取当前配置
     */
    external fun nativeGetConfig(): Config
    
    /**
     * 获取统计信息
     */
    external fun nativeGetStats(): Stats
    
    /**
     * 重置统计信息
     */
    external fun nativeResetStats()
    
    /**
     * 清除缓存
     */
    external fun nativeClearCache()
    
    /**
     * 初始化默认配置
     */
    external fun nativeInitializeDefaultConfig()
    
    init {
        System.loadLibrary("filmtracker")
    }
}
