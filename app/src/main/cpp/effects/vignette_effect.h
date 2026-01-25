#ifndef FILMTRACKER_VIGNETTE_EFFECT_H
#define FILMTRACKER_VIGNETTE_EFFECT_H

namespace filmtracker {

/**
 * 暗角效果模块
 * 
 * 提供高质量的暗角效果，模拟镜头边缘光线衰减。
 */
class VignetteEffect {
public:
    /**
     * 应用暗角效果
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     * @param amount 暗角强度（-1.0 到 1.0，负值为亮角）
     * @param x 像素 X 坐标
     * @param y 像素 Y 坐标
     * @param width 图像宽度
     * @param height 图像高度
     */
    static void applyVignette(float& r, float& g, float& b, float amount,
                             int x, int y, int width, int height);

private:
    /**
     * 计算暗角权重
     * 基于像素到中心的距离
     */
    static float calculateVignetteWeight(float normalizedDistance, float amount);
};

} // namespace filmtracker

#endif // FILMTRACKER_VIGNETTE_EFFECT_H
