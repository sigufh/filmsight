#include "vignette_effect.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

float VignetteEffect::calculateVignetteWeight(float normalizedDistance, float amount) {
    // 使用平滑的衰减曲线
    // normalizedDistance: 0（中心）到 1（角落）
    
    // 暗角范围：从中心的 60% 开始衰减
    float falloffStart = 0.6f;
    
    if (normalizedDistance < falloffStart) {
        // 中心区域不受影响
        return 1.0f;
    }
    
    // 计算衰减区域的归一化距离
    float falloffDistance = (normalizedDistance - falloffStart) / (1.0f - falloffStart);
    
    // 使用平滑的三次曲线
    float falloff = 1.0f - falloffDistance * falloffDistance * falloffDistance;
    
    // 应用强度
    // amount > 0: 暗角（变暗）
    // amount < 0: 亮角（变亮）
    float vignetteStrength = 1.0f - (1.0f - falloff) * std::abs(amount);
    
    if (amount < 0.0f) {
        // 亮角：增亮边缘
        vignetteStrength = 1.0f + (1.0f - falloff) * std::abs(amount);
    }
    
    return vignetteStrength;
}

void VignetteEffect::applyVignette(float& r, float& g, float& b, float amount,
                                   int x, int y, int width, int height) {
    // 如果强度接近0，不做调整
    if (std::abs(amount) < 0.001f) {
        return;
    }
    
    // 计算像素到图像中心的归一化距离
    float centerX = width * 0.5f;
    float centerY = height * 0.5f;
    
    float dx = (x - centerX) / centerX;
    float dy = (y - centerY) / centerY;
    
    // 计算到中心的距离（归一化到 0-1）
    float distance = std::sqrt(dx * dx + dy * dy);
    
    // 对角线距离作为最大距离
    float maxDistance = std::sqrt(2.0f);
    float normalizedDistance = std::min(distance / maxDistance, 1.0f);
    
    // 计算暗角权重
    float vignetteWeight = calculateVignetteWeight(normalizedDistance, amount);
    
    // 应用暗角
    r *= vignetteWeight;
    g *= vignetteWeight;
    b *= vignetteWeight;
    
    // 确保非负
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
}

} // namespace filmtracker
