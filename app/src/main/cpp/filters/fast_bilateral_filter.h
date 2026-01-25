#ifndef FILMTRACKER_FAST_BILATERAL_FILTER_H
#define FILMTRACKER_FAST_BILATERAL_FILTER_H

#include "raw_types.h"

namespace filmtracker {

/**
 * 快速近似双边滤波器
 * 
 * 基于 Paris & Durand (2006) 的降采样方法
 * 通过在降采样图像上应用标准双边滤波，然后上采样回原始分辨率
 * 在保持视觉质量的前提下实现 2-4 倍加速
 * 
 * 参考：
 * - Paris & Durand (2006) "A Fast Approximation of the Bilateral Filter"
 */
class FastBilateralFilter {
public:
    /**
     * 应用快速近似双边滤波
     * 
     * @param input 输入图像
     * @param output 输出图像
     * @param spatialSigma 空间域标准差
     * @param rangeSigma 强度域标准差
     */
    static void apply(
        const LinearImage& input,
        LinearImage& output,
        float spatialSigma,
        float rangeSigma
    );
    
private:
    /**
     * 计算降采样因子
     * 
     * 基于 spatialSigma 计算合适的降采样因子
     * spatialSigma 越大，可以使用更大的降采样因子
     * 
     * @param spatialSigma 空间域标准差
     * @return 降采样因子（1, 2, 4, 8 等）
     */
    static int calculateDownsampleFactor(float spatialSigma);
    
    /**
     * 降采样图像（使用区域平均）
     * 
     * @param input 输入图像
     * @param output 输出图像（尺寸为 input.width/factor x input.height/factor）
     * @param factor 降采样因子
     */
    static void downsample(
        const LinearImage& input,
        LinearImage& output,
        int factor
    );
    
    /**
     * 上采样图像（使用双线性插值）
     * 
     * @param input 输入图像
     * @param output 输出图像
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     */
    static void upsample(
        const LinearImage& input,
        LinearImage& output,
        uint32_t targetWidth,
        uint32_t targetHeight
    );
    
    /**
     * 在降采样图像上应用标准双边滤波
     * 
     * @param input 输入图像
     * @param output 输出图像
     * @param spatialSigma 空间域标准差（需要根据降采样因子调整）
     * @param rangeSigma 强度域标准差
     */
    static void applyStandard(
        const LinearImage& input,
        LinearImage& output,
        float spatialSigma,
        float rangeSigma
    );
};

} // namespace filmtracker

#endif // FILMTRACKER_FAST_BILATERAL_FILTER_H
