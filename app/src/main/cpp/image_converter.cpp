#include "image_converter.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

/**
 * sRGB Gamma 函数
 * 仅在输出阶段应用，核心算法始终在线性域
 */
float ImageConverter::sRGBGamma(float linear) {
    if (linear <= 0.0031308f) {
        return 12.92f * linear;
    } else {
        return 1.055f * std::pow(linear, 1.0f / 2.4f) - 0.055f;
    }
}

/**
 * 线性到 sRGB 转换
 */
float ImageConverter::linearToSRGB(float linear) {
    float clamped = std::max(0.0f, std::min(1.0f, linear));
    return sRGBGamma(clamped);
}

/**
 * 将线性 RGB 转换为 sRGB 输出图像
 */
OutputImage ImageConverter::linearToSRGB(const LinearImage& linear) {
    OutputImage output(linear.width, linear.height);
    
    const uint32_t pixelCount = linear.width * linear.height;
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        uint32_t idx = i * 4;
        output.data[idx + 0] = static_cast<uint8_t>(linearToSRGB(linear.r[i]) * 255.0f);
        output.data[idx + 1] = static_cast<uint8_t>(linearToSRGB(linear.g[i]) * 255.0f);
        output.data[idx + 2] = static_cast<uint8_t>(linearToSRGB(linear.b[i]) * 255.0f);
        output.data[idx + 3] = 255; // Alpha
    }
    
    return output;
}

/**
 * 色调映射（用于高动态范围场景）
 */
void ImageConverter::applyToneMapping(LinearImage& image, float exposure) {
    const uint32_t pixelCount = image.width * image.height;
    float exposureMultiplier = std::pow(2.0f, exposure);
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        image.r[i] *= exposureMultiplier;
        image.g[i] *= exposureMultiplier;
        image.b[i] *= exposureMultiplier;
        
        // Reinhard 色调映射
        image.r[i] = image.r[i] / (1.0f + image.r[i]);
        image.g[i] = image.g[i] / (1.0f + image.g[i]);
        image.b[i] = image.b[i] / (1.0f + image.b[i]);
    }
}

} // namespace filmtracker
