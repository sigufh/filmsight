#ifndef FILMTRACKER_IMAGE_CONVERTER_H
#define FILMTRACKER_IMAGE_CONVERTER_H

#include "raw_types.h"

namespace filmtracker {

/**
 * 图像转换器
 * 
 * 将线性域图像转换为输出格式（sRGB，8位）
 * 注意：这是唯一允许应用 Gamma 的地方（输出阶段）
 */
class ImageConverter {
public:
    /**
     * 将线性 RGB 转换为 sRGB（8位 RGBA）
     * 
     * @param linear 线性域图像
     * @return sRGB 输出图像
     */
    static OutputImage linearToSRGB(const LinearImage& linear);
    
    /**
     * 应用 Gamma 校正（线性 -> sRGB）
     */
    static float linearToSRGB(float linear);
    
    /**
     * 色调映射（可选，用于高动态范围）
     */
    static void applyToneMapping(LinearImage& image, float exposure);
    
    /**
     * 将 sRGB Bitmap 转换为线性域图像
     * @param rgbaData RGBA 数据（8位）
     * @param width 图像宽度
     * @param height 图像高度
     * @return 线性域图像
     */
    static LinearImage sRGBToLinear(const uint8_t* rgbaData, uint32_t width, uint32_t height);
    
private:
    // sRGB Gamma 函数
    static float sRGBGamma(float linear);
    
    // sRGB 到线性域的反 Gamma 函数
    static float sRGBToLinear(float srgb);
};

} // namespace filmtracker

#endif // FILMTRACKER_IMAGE_CONVERTER_H
