#ifndef FILMTRACKER_CONTRAST_ADJUSTMENT_H
#define FILMTRACKER_CONTRAST_ADJUSTMENT_H

namespace filmtracker {

/**
 * 对比度调整模块
 * 
 * 实现基于 S 曲线的对比度调整算法，围绕 18% 中灰进行调整。
 * 相比线性缩放，S 曲线能够更好地保护高光和阴影，产生更自然的效果。
 */
class ContrastAdjustment {
public:
    /**
     * S 曲线对比度调整
     * 
     * 围绕中点（0.5）进行调整，使用标准对比度公式。
     * 
     * 算法特点：
     * - 中点保持不变
     * - 增加对比度时，亮部更亮，暗部更暗
     * - 减少对比度时，整体趋向中灰
     * - 保护高光和阴影不被过度裁剪
     * 
     * @param value 输入值（线性空间，0.0 到 1.0）
     * @param contrastMultiplier 对比度乘数（0.5 到 2.0，1.0 为不变）
     *                           由 Kotlin 层的 AdobeParameterConverter.contrastToMultiplier() 转换而来
     * @return 调整后的值
     */
    static float applySCurveContrast(float value, float contrastMultiplier);
    
    /**
     * 应用对比度调整到 RGB 像素
     * 
     * 标准对比度算法，围绕中点（0.5）进行缩放。
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     * @param contrastMultiplier 对比度乘数（0.5 到 2.0，1.0 为不变）
     *                           由 Kotlin 层的 AdobeParameterConverter.contrastToMultiplier() 转换而来
     */
    static void applyContrast(float& r, float& g, float& b, float contrastMultiplier);

private:
    /**
     * S 曲线函数
     * 
     * 实现平滑的 S 形曲线，用于对比度调整。
     * 
     * @param x 输入值（0 到 1）
     * @param strength 曲线强度
     * @return S 曲线变换后的值
     */
    static float sCurve(float x, float strength);
    
    /**
     * 渐进式压缩函数
     * 
     * 当对比度 > 1.5 时，应用渐进式压缩避免硬裁剪。
     * 
     * @param value 输入值
     * @param threshold 压缩阈值
     * @return 压缩后的值
     */
    static float progressiveCompression(float value, float threshold);
};

} // namespace filmtracker

#endif // FILMTRACKER_CONTRAST_ADJUSTMENT_H
