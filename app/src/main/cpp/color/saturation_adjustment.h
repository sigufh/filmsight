#ifndef FILMTRACKER_SATURATION_ADJUSTMENT_H
#define FILMTRACKER_SATURATION_ADJUSTMENT_H

namespace filmtracker {

/**
 * 饱和度调整模块
 * 
 * 提供高质量的饱和度调整，保护肤色和防止过饱和。
 * 使用自适应策略和色彩保护。
 */
class SaturationAdjustment {
public:
    /**
     * 应用饱和度调整（带保护）
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     * @param saturationMultiplier 饱和度乘数（0.0 到 2.0，1.0 为不变）
     *                             由 Kotlin 层的 AdobeParameterConverter.saturationToMultiplier() 转换而来
     */
    static void applySaturation(float& r, float& g, float& b, float saturationMultiplier);
    
    /**
     * 应用自然饱和度（Vibrance）
     * 
     * 自然饱和度优先增强低饱和度区域，保护肤色和高饱和度区域
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     * @param vibrance 自然饱和度值（-100 到 +100）
     */
    static void applyVibrance(float& r, float& g, float& b, float vibrance);

private:
    /**
     * 计算当前饱和度
     */
    static float getCurrentSaturation(float r, float g, float b, float luminance);
    
    /**
     * 检测是否为肤色
     */
    static bool isSkinTone(float r, float g, float b);
    
    /**
     * 过饱和保护
     */
    static float protectOversaturation(float satDelta, float currentSat);
};

} // namespace filmtracker

#endif // FILMTRACKER_SATURATION_ADJUSTMENT_H
