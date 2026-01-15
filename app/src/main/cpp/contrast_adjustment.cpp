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

float ContrastAdjustment::applySCurveContrast(float value, float contrast) {
    // 18% 中灰（线性空间）
    const float midGray = 0.18f;
    
    // 将 contrast 参数从 [0.5, 2.0] 映射到合适的强度范围
    // contrast = 1.0 时，strength = 0（无调整）
    // contrast > 1.0 时，strength > 0（增加对比度）
    // contrast < 1.0 时，strength < 0（减少对比度）
    float strength = (contrast - 1.0f);
    
    // 将值归一化到以中灰为中心的范围
    // 先转换到对数空间以获得更好的感知均匀性
    float logValue = (value > 0.0001f) ? std::log2(value / midGray) : -10.0f;
    
    // 限制范围以避免极端值
    logValue = std::clamp(logValue, -10.0f, 10.0f);
    
    // 归一化到 [0, 1] 范围（假设 ±5 stops 的范围）
    float normalized = (logValue + 5.0f) / 10.0f;
    normalized = std::clamp(normalized, 0.0f, 1.0f);
    
    // 应用 S 曲线
    float adjusted = sCurve(normalized, strength);
    
    // 转换回对数空间
    float newLogValue = adjusted * 10.0f - 5.0f;
    
    // 转换回线性空间
    float result = midGray * std::pow(2.0f, newLogValue);
    
    // 渐进式压缩（当对比度 > 1.5 时）
    if (contrast > 1.5f) {
        // 对高光应用压缩
        if (result > 0.8f) {
            result = progressiveCompression(result, 0.8f);
        }
        // 对阴影应用保护
        if (result < 0.05f) {
            result = std::max(result, 0.001f);
        }
    }
    
    return std::max(0.0f, result);
}

void ContrastAdjustment::applyContrast(float& r, float& g, float& b, float contrast) {
    // 如果对比度接近 1.0，不做调整
    if (std::abs(contrast - 1.0f) < 0.01f) {
        return;
    }
    
    // 计算原始亮度（用于保持饱和度）
    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
    
    // 应用 S 曲线对比度到每个通道
    float newR = applySCurveContrast(r, contrast);
    float newG = applySCurveContrast(g, contrast);
    float newB = applySCurveContrast(b, contrast);
    
    // 计算新的亮度
    float newLuminance = 0.2126f * newR + 0.7152f * newG + 0.0722f * newB;
    
    // 保持色彩饱和度：混合原始饱和度和新的对比度
    // 这样可以避免对比度调整导致的饱和度变化
    if (newLuminance > 0.0001f && luminance > 0.0001f) {
        // 计算饱和度保持因子
        float saturationFactor = 0.8f;  // 保持 80% 的原始饱和度关系
        
        // 混合：保持一定的色彩关系
        float scale = newLuminance / luminance;
        
        r = newR * (1.0f - saturationFactor) + r * scale * saturationFactor;
        g = newG * (1.0f - saturationFactor) + g * scale * saturationFactor;
        b = newB * (1.0f - saturationFactor) + b * scale * saturationFactor;
    } else {
        r = newR;
        g = newG;
        b = newB;
    }
    
    // 确保非负
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
}

} // namespace filmtracker
