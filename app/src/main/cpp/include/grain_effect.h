#ifndef FILMTRACKER_GRAIN_EFFECT_H
#define FILMTRACKER_GRAIN_EFFECT_H

#include <cstdint>

namespace filmtracker {

/**
 * 胶片颗粒效果模块
 * 
 * 模拟真实胶片的颗粒质感，提供高质量的噪点效果。
 */
class GrainEffect {
public:
    /**
     * 应用胶片颗粒效果
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     * @param amount 颗粒强度（0.0 到 1.0）
     * @param x 像素 X 坐标
     * @param y 像素 Y 坐标
     * @param seed 随机种子
     */
    static void applyGrain(float& r, float& g, float& b, float amount, 
                          int x, int y, uint32_t seed = 12345);

private:
    /**
     * 生成伪随机噪声
     * 使用快速哈希函数生成确定性噪声
     */
    static float generateNoise(int x, int y, uint32_t seed);
    
    /**
     * 计算亮度相关的颗粒强度
     * 暗部和高光颗粒更明显
     */
    static float getLuminanceWeight(float luminance);
};

} // namespace filmtracker

#endif // FILMTRACKER_GRAIN_EFFECT_H
