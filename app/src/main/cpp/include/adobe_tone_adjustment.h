#ifndef FILMTRACKER_ADOBE_TONE_ADJUSTMENT_H
#define FILMTRACKER_ADOBE_TONE_ADJUSTMENT_H

namespace filmtracker {

/**
 * Adobe 标准色调调整模块
 * 
 * 实现符合 Adobe Camera RAW / Lightroom 标准的色调调整算法。
 * 使用感知亮度（CIE L*）进行区域划分，确保调整符合人眼特性。
 * 使用三次样条实现平滑的区域过渡。
 */
class AdobeToneAdjustment {
public:
    /**
     * 计算感知亮度（CIE L*）
     * 
     * CIE L* 是一个感知均匀的亮度空间，更符合人眼对亮度的感知。
     * 
     * 算法：
     * - 如果 Y <= 0.008856：L* = Y * 903.3
     * - 否则：L* = 116 * Y^(1/3) - 16
     * 
     * @param Y 线性亮度值（0.0 到 1.0）
     * @return CIE L* 值（0 到 100）
     */
    static float rgbToLuminance(float r, float g, float b);
    
    /**
     * 计算感知亮度（CIE L*）
     * 
     * @param Y 线性亮度值（0.0 到 1.0）
     * @return CIE L* 值（0 到 100）
     */
    static float luminanceToLstar(float Y);
    
    /**
     * CIE L* 逆转换
     * 
     * 将 CIE L* 转换回线性亮度。
     * 
     * @param Lstar CIE L* 值（0 到 100）
     * @return 线性亮度值（0.0 到 1.0）
     */
    static float lstarToLuminance(float Lstar);
    
    /**
     * 高光权重函数
     * 
     * 使用三次样条实现平滑过渡。
     * L* > 70 时影响最大。
     * 
     * @param Lstar CIE L* 值（0 到 100）
     * @return 权重值（0.0 到 1.0）
     */
    static float highlightWeight(float Lstar);
    
    /**
     * 阴影权重函数
     * 
     * 使用三次样条实现平滑过渡。
     * L* < 30 时影响最大。
     * 
     * @param Lstar CIE L* 值（0 到 100）
     * @return 权重值（0.0 到 1.0）
     */
    static float shadowWeight(float Lstar);
    
    /**
     * 白场权重函数
     * 
     * 使用三次样条实现平滑过渡。
     * L* > 80 时影响最大。
     * 
     * @param Lstar CIE L* 值（0 到 100）
     * @return 权重值（0.0 到 1.0）
     */
    static float whiteWeight(float Lstar);
    
    /**
     * 黑场权重函数
     * 
     * 使用三次样条实现平滑过渡。
     * L* < 20 时影响最大。
     * 
     * @param Lstar CIE L* 值（0 到 100）
     * @return 权重值（0.0 到 1.0）
     */
    static float blackWeight(float Lstar);
    
    /**
     * 应用色调调整
     * 
     * 根据感知亮度和权重函数计算调整量。
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     * @param highlights 高光调整（-100 到 +100）
     * @param shadows 阴影调整（-100 到 +100）
     * @param whites 白场调整（-100 到 +100）
     * @param blacks 黑场调整（-100 到 +100）
     */
    static void applyToneAdjustments(float& r, float& g, float& b,
                                    float highlights, float shadows,
                                    float whites, float blacks);

private:
    /**
     * 三次样条权重函数
     * 
     * 实现平滑的 S 曲线过渡。
     * 
     * @param x 输入值
     * @param center 中心点
     * @param width 过渡宽度
     * @return 权重值（0.0 到 1.0）
     */
    static float cubicSplineWeight(float x, float center, float width);
};

} // namespace filmtracker

#endif // FILMTRACKER_ADOBE_TONE_ADJUSTMENT_H
