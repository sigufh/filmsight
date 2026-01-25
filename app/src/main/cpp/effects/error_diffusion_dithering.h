#ifndef FILMTRACKER_ERROR_DIFFUSION_DITHERING_H
#define FILMTRACKER_ERROR_DIFFUSION_DITHERING_H

#include "raw_types.h"
#include <cstdint>

namespace filmtracker {

/**
 * 误差扩散抖动模块
 * 
 * 实现 Floyd-Steinberg 误差扩散算法，用于在转换到 8-bit 输出时
 * 减少色彩断层（color banding）。
 * 
 * Floyd-Steinberg 误差分配模式：
 *        X   7/16
 *    3/16 5/16 1/16
 * 
 * 其中 X 是当前像素，误差按照上述权重分配到相邻像素。
 */
class ErrorDiffusionDithering {
public:
    ErrorDiffusionDithering();
    ~ErrorDiffusionDithering();
    
    /**
     * 应用 Floyd-Steinberg 误差扩散抖动
     * 
     * 将 32-bit 浮点图像（0.0-1.0）转换为 8-bit 整数（0-255），
     * 同时使用误差扩散减少量化误差导致的色彩断层。
     * 
     * @param image 输入图像（32-bit 浮点，线性空间）
     * @param output 输出缓冲区（8-bit 整数，需预分配 width*height*3 字节）
     * @param applyGamma 是否在量化前应用 sRGB gamma 编码（默认 true）
     * 
     * 注意：
     * - 输入图像应该已经过软裁剪，值域在 [0, 1] 范围内
     * - 如果 applyGamma=true，会先应用 sRGB gamma 编码再量化
     * - 误差扩散是逐行处理的，因此输出是确定性的
     */
    void applyFloydSteinberg(const LinearImage& image, 
                            uint8_t* output, 
                            bool applyGamma = true);
    
    /**
     * 应用 Floyd-Steinberg 抖动（就地修改浮点图像）
     * 
     * 这个版本不进行量化，只是将误差扩散应用到浮点图像上，
     * 可以用于中间处理步骤。
     * 
     * @param image 输入/输出图像（就地修改）
     * @param bitDepth 目标位深度（用于计算量化步长）
     */
    void applyFloydSteinbergInPlace(LinearImage& image, int bitDepth = 8);
    
private:
    /**
     * 应用 sRGB gamma 编码
     * 
     * @param linear 线性值（0.0-1.0）
     * @return gamma 编码后的值（0.0-1.0）
     */
    float applyGammaEncoding(float linear) const;
    
    /**
     * 量化浮点值到整数
     * 
     * @param value 浮点值（0.0-1.0）
     * @param maxValue 最大整数值（例如 255 对应 8-bit）
     * @return 量化后的整数值
     */
    int quantize(float value, int maxValue) const;
    
    /**
     * 计算量化误差
     * 
     * @param original 原始浮点值（0.0-1.0）
     * @param quantized 量化后的整数值
     * @param maxValue 最大整数值
     * @return 量化误差（浮点）
     */
    float calculateError(float original, int quantized, int maxValue) const;
    
    /**
     * 分配误差到相邻像素
     * 
     * Floyd-Steinberg 误差分配：
     *        X   7/16
     *    3/16 5/16 1/16
     * 
     * @param errorBuffer 误差缓冲区（当前行和下一行）
     * @param width 图像宽度
     * @param x 当前像素 x 坐标
     * @param error 要分配的误差
     */
    void distributeError(float* errorBuffer, 
                        uint32_t width, 
                        uint32_t x, 
                        float error) const;
};

} // namespace filmtracker

#endif // FILMTRACKER_ERROR_DIFFUSION_DITHERING_H
