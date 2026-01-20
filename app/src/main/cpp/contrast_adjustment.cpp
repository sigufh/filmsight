#include "contrast_adjustment.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

float ContrastAdjustment::sCurve(float x, float strength) {
    // S 曲线函数：使用修改的 sigmoid 函数
    // 确保在 x=0.5 时输出也是 0.5（中点不变）
    
    if (strength < 0.001f) {
        return x;  // 无调整
    }
    
    // 将 x 从 [0, 1] 映射到 [-1, 1]
    float t = (x - 0.5f) * 2.0f;
    
    // 应用 S 曲线（使用 tanh 的变体）
    // strength 控制曲线的陡峭程度
    float result;
    if (strength > 0.0f) {
        // 增加对比度：使用更陡的曲线
        float k = strength * 2.0f;
        result = std::tanh(t * k) / std::tanh(k);
    } else {
        // 减少对比度：使用更平缓的曲线
        float k = (1.0f + strength) * 2.0f;
        if (k > 0.001f) {
            result = std::tanh(t * k) / std::tanh(k);
        } else {
            result = 0.0f;  // 完全平坦，所有值趋向中灰
        }
    }
    
    // 映射回 [0, 1]
    return result * 0.5f + 0.5f;
}

float ContrastAdjustment::progressiveCompression(float value, float threshold) {
    // 渐进式压缩：避免硬裁剪
    
    if (value <= threshold) {
        return value;
    }
    
    // 使用软裁剪函数
    float excess = value - threshold;
    float range = 1.0f - threshold;
    
    if (range < 0.001f) {
        return threshold;
    }
    
    // 使用 tanh 实现渐近压缩
    float compressed = std::tanh(excess / range * 2.0f) * range * 0.5f;
    return threshold + compressed;
}

float ContrastAdjustment::applySCurveContrast(float value, float contrastMultiplier) {
    // 简化的对比度算法
    // contrastMultiplier 参数已经是乘数格式（0.5 到 2.0）
    // 由 Kotlin 层的 AdobeParameterConverter.contrastToMultiplier() 转换而来
    // 使用简单但有效的公式，保证数值稳定性
    
    // 如果乘数接近1.0，不做调整
    if (std::abs(contrastMultiplier - 1.0f) < 0.001f) {
        return value;
    }
    
    // 标准对比度公式：(value - 0.5) * multiplier + 0.5
    // 围绕中灰点（0.5）进行缩放
    float result = (value - 0.5f) * contrastMultiplier + 0.5f;
    
    // 确保在有效范围内
    return std::clamp(result, 0.0f, 1.0f);
}

void ContrastAdjustment::applyContrast(float& r, float& g, float& b, float contrastMultiplier) {
    // contrastMultiplier 参数已经是乘数格式（0.5 到 2.0）
    // 由 Kotlin 层的 AdobeParameterConverter.contrastToMultiplier() 转换而来
    
    // 如果乘数接近 1.0，不做调整
    if (std::abs(contrastMultiplier - 1.0f) < 0.001f) {
        return;
    }
    
    // 应用对比度调整
    r = applySCurveContrast(r, contrastMultiplier);
    g = applySCurveContrast(g, contrastMultiplier);
    b = applySCurveContrast(b, contrastMultiplier);
    
    // 确保非负
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
}

} // namespace filmtracker
