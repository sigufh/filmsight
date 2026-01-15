#ifndef FILMTRACKER_DYNAMIC_RANGE_PROTECTION_H
#define FILMTRACKER_DYNAMIC_RANGE_PROTECTION_H

namespace filmtracker {

/**
 * 动态范围保护模块
 * 
 * 提供软裁剪（soft clipping）功能，避免硬裁剪导致的细节丢失。
 * 使用 Hermite 样条和 tanh 函数实现平滑的过渡和渐近线裁剪。
 * 
 * 参考 Adobe Camera RAW 的动态范围保护策略。
 */
class DynamicRangeProtection {
public:
    /**
     * 软裁剪函数
     * 
     * 算法：
     * - 如果 x < threshold：返回 x（线性区域）
     * - 如果 x < threshold + knee：使用 Hermite 样条平滑过渡
     * - 否则：使用 tanh 渐近线，永不完全裁剪
     * 
     * @param x 输入值
     * @param threshold 开始软裁剪的阈值（默认 0.8）
     * @param knee 过渡区域宽度（默认 0.15）
     * @param limit 渐近线限制（默认 1.0）
     * @return 软裁剪后的值
     */
    static float softClip(float x, float threshold = 0.8f, float knee = 0.15f, float limit = 1.0f);
    
    /**
     * 高光压缩（Highlight Rolloff）
     * 
     * 模拟胶片的高光压缩特性，保留高光细节而不产生硬裁剪。
     * 使用软裁剪算法压缩高光区域。
     * 
     * @param value 输入亮度值
     * @param amount 压缩强度（0.0 到 1.0）
     * @return 压缩后的值
     */
    static float highlightRolloff(float value, float amount);
    
    /**
     * 阴影提升（Shadow Lift）
     * 
     * 提升阴影区域的亮度，同时保护细节不被噪声放大。
     * 使用平滑的权重函数避免突变。
     * 
     * @param value 输入亮度值
     * @param amount 提升强度（0.0 到 1.0）
     * @return 提升后的值
     */
    static float shadowLift(float value, float amount);

private:
    /**
     * Hermite 样条插值
     * 
     * 用于在 threshold 和 threshold+knee 之间创建平滑过渡。
     * 
     * @param t 归一化参数（0 到 1）
     * @param p0 起始值
     * @param p1 结束值
     * @param m0 起始斜率
     * @param m1 结束斜率
     * @return 插值结果
     */
    static float hermiteSpline(float t, float p0, float p1, float m0, float m1);
};

} // namespace filmtracker

#endif // FILMTRACKER_DYNAMIC_RANGE_PROTECTION_H
