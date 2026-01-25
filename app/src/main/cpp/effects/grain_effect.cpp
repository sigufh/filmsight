#include "grain_effect.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

float GrainEffect::generateNoise(int x, int y, uint32_t seed) {
    // 使用快速哈希函数生成伪随机噪声
    // 基于 xxHash 的简化版本
    
    uint32_t h = seed;
    h ^= x * 374761393U;
    h ^= y * 668265263U;
    h = (h ^ (h >> 13)) * 1274126177U;
    h = h ^ (h >> 16);
    
    // 转换为 [-1, 1] 范围的浮点数
    return (float)(h & 0xFFFFFF) / (float)0xFFFFFF * 2.0f - 1.0f;
}

float GrainEffect::getLuminanceWeight(float luminance) {
    // 胶片颗粒特性：中间调颗粒较少，暗部和高光颗粒更明显
    // 使用抛物线函数
    
    // 计算到中间调（0.5）的距离
    float distanceFromMid = std::abs(luminance - 0.5f) * 2.0f;  // 0 到 1
    
    // 使用平方函数增强边缘
    float weight = 0.5f + distanceFromMid * distanceFromMid * 0.5f;
    
    return weight;
}

void GrainEffect::applyGrain(float& r, float& g, float& b, float amount, 
                             int x, int y, uint32_t seed) {
    // 如果强度接近0，不做调整
    if (amount < 0.001f) {
        return;
    }
    
    // 计算当前像素的亮度
    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
    
    // 生成三个通道的独立噪声（模拟彩色胶片的颗粒）
    float noiseR = generateNoise(x, y, seed);
    float noiseG = generateNoise(x, y, seed + 1);
    float noiseB = generateNoise(x, y, seed + 2);
    
    // 计算亮度相关的权重
    float lumWeight = getLuminanceWeight(luminance);
    
    // 调整噪声强度
    // amount 范围：0.0 到 1.0（对应 UI 的 0 到 100）
    float grainStrength = amount * 0.05f * lumWeight;  // 最大5%的变化
    
    // 应用颗粒（加性噪声）
    r = r + noiseR * grainStrength;
    g = g + noiseG * grainStrength;
    b = b + noiseB * grainStrength;
    
    // 确保非负
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
}

} // namespace filmtracker
