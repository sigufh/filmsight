package com.filmtracker.app.processing

import com.filmtracker.app.data.BasicAdjustmentParams

/**
 * 处理阶段枚举
 * 
 * 基于 DaVinci Resolve 的智能缓存策略和 Capture One 的固定管线思路：
 * - 简单调整（TONE_BASE、CURVES、COLOR）实时计算，不缓存
 * - 计算密集型调整（EFFECTS、DETAILS）需要缓存
 */
enum class ProcessingStage(
    val order: Int,
    val shouldCache: Boolean,
    val description: String
) {
    /**
     * 阶段 1：基础影调
     * 包含：曝光、高光、阴影、白场、黑场、对比度
     * 不缓存：SIMD 优化后足够快
     */
    TONE_BASE(
        order = 1,
        shouldCache = false,
        description = "基础影调调整（曝光、高光、阴影、白场、黑场、对比度）"
    ),
    
    /**
     * 阶段 2：曲线
     * 包含：RGB 曲线、R/G/B 单通道曲线
     * 不缓存：LUT 查找非常快
     */
    CURVES(
        order = 2,
        shouldCache = false,
        description = "曲线调整（RGB 曲线、单通道曲线）"
    ),
    
    /**
     * 阶段 3：色彩
     * 包含：色温、色调、饱和度、自然饱和度、HSL、色彩分级
     * 不缓存：简单的颜色变换
     */
    COLOR(
        order = 3,
        shouldCache = false,
        description = "色彩调整（色温、色调、饱和度、HSL、色彩分级）"
    ),
    
    /**
     * 阶段 4：效果
     * 包含：清晰度、纹理、去雾
     * 需要缓存：清晰度和纹理需要卷积运算
     */
    EFFECTS(
        order = 4,
        shouldCache = true,
        description = "效果调整（清晰度、纹理、去雾）"
    ),
    
    /**
     * 阶段 5：细节
     * 包含：锐化、降噪
     * 需要缓存：计算最密集
     */
    DETAILS(
        order = 5,
        shouldCache = true,
        description = "细节调整（锐化、降噪）"
    );
    
    companion object {
        /**
         * 获取所有需要缓存的阶段
         */
        fun getCacheableStages(): List<ProcessingStage> {
            return entries.filter { it.shouldCache }
        }
        
        /**
         * 获取按顺序排列的所有阶段
         */
        fun getOrderedStages(): List<ProcessingStage> {
            return entries.sortedBy { it.order }
        }
        
        /**
         * 根据顺序获取阶段
         */
        fun fromOrder(order: Int): ProcessingStage? {
            return entries.find { it.order == order }
        }
    }
}
