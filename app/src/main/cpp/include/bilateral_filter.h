#ifndef FILMTRACKER_BILATERAL_FILTER_H
#define FILMTRACKER_BILATERAL_FILTER_H

#include "raw_types.h"

namespace filmtracker {

/**
 * 双边滤波器模块
 * 
 * 实现边缘保持的平滑滤波，用于清晰度调整和降噪
 * 双边滤波器同时考虑空间距离和强度差异，能够在平滑图像的同时保留边缘
 * 
 * 参考：
 * - Tomasi & Manduchi (1998) "Bilateral Filtering for Gray and Color Images"
 * - Paris & Durand (2006) "A Fast Approximation of the Bilateral Filter"
 */
class BilateralFilter {
public:
    /**
     * 应用双边滤波器
     * 
     * @param input 输入图像
     * @param output 输出图像（必须预先分配）
     * @param spatialSigma 空间域标准差（像素单位，控制滤波器大小）
     * @param rangeSigma 强度域标准差（0.0-1.0，控制边缘保持程度）
     */
    static void apply(const LinearImage& input, 
                     LinearImage& output,
                     float spatialSigma,
                     float rangeSigma);
    
    /**
     * 应用快速双边滤波器（使用可分离近似）
     * 
     * 使用可分离滤波器近似双边滤波，速度更快但精度略低
     * 适用于实时处理或大图像
     * 
     * @param input 输入图像
     * @param output 输出图像（必须预先分配）
     * @param spatialSigma 空间域标准差
     * @param rangeSigma 强度域标准差
     */
    static void applyFast(const LinearImage& input,
                         LinearImage& output,
                         float spatialSigma,
                         float rangeSigma);
    
    /**
     * 提取细节层（用于清晰度调整）
     * 
     * 使用双边滤波器分离基础层和细节层
     * 细节层 = 原图 - 基础层（双边滤波结果）
     * 
     * @param input 输入图像
     * @param detail 输出细节层（必须预先分配）
     * @param spatialSigma 空间域标准差
     * @param rangeSigma 强度域标准差
     */
    static void extractDetail(const LinearImage& input,
                             LinearImage& detail,
                             float spatialSigma,
                             float rangeSigma);
    
private:
    /**
     * 计算高斯权重
     * 
     * @param distance 距离（空间或强度）
     * @param sigma 标准差
     * @return 高斯权重
     */
    static float gaussianWeight(float distance, float sigma);
    
    /**
     * 计算滤波器半径
     * 
     * @param sigma 标准差
     * @return 滤波器半径（像素）
     */
    static int calculateRadius(float sigma);
};

} // namespace filmtracker

#endif // FILMTRACKER_BILATERAL_FILTER_H
