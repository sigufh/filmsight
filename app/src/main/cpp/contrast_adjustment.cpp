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
    // 使用 S 曲线对比度算法，保护暗部和高光细节
    // 设计目标：对比度80以上才开始丢失暗部细节
    
    // 计算调整强度 - 使用极度压缩的映射
    float strength;
    if (contrast >= 1.0f) {
        // 增加对比度：使用双重对数映射，极大压缩高值
        float delta = contrast - 1.0f;
        
        // 第一层：对数压缩
        float compressed1 = std::log(1.0f + delta * 0.15f);
        // 第二层：再次对数压缩，使曲线更平缓
        strength = std::log(1.0f + compressed1) * 0.15f;
        
        // 极高的上限，允许更大的对比度范围
        strength = std::min(strength, 0.6f);
    } else {
        // 减少对比度：线性映射
        strength = (contrast - 1.0f) * 0.5f;
        strength = std::max(strength, -0.5f);
    }
    
    // 如果强度接近 0，直接返回
    if (std::abs(strength) < 0.001f) {
        return value;
    }
    
    // 使用改进的 S 曲线，强力保护暗部和高光
    float x = std::clamp(value, 0.0f, 1.0f);
    
    float result;
    
    if (strength > 0.0f) {
        // 增加对比度：使用温和的 S 曲线
        
        // 使用非常温和的曲线陡峭度
        float k = strength * 1.5f;  // 进一步降低陡峭度
        
        // 将 [0, 1] 映射到 [-1, 1]
        float centered = (x - 0.5f) * 2.0f;
        
        // 应用 tanh S 曲线
        float adjusted = std::tanh(centered * (1.0f + k)) / std::tanh(1.0f + k);
        
        // 映射回 [0, 1]
        result = adjusted * 0.5f + 0.5f;
        
        // 强力暗部保护：大幅扩大保护范围
        if (x < 0.25f) {
            // 暗部保护：使用更平滑的过渡曲线
            float protection = std::pow(x / 0.25f, 0.5f);  // 更平滑的过渡
            
            // 极暗区域（0-0.15）几乎完全保护
            float darkPreserve = std::max(0.0f, 1.0f - x * 6.67f);  // 0-0.15 范围
            darkPreserve = std::pow(darkPreserve, 0.7f);  // 平滑衰减
            
            // 混合：极暗区域保留更多原始值
            float preserveAmount = darkPreserve * 0.7f;
            result = x * (1.0f - protection + preserveAmount) + result * (protection - preserveAmount);
        }
        
        // 强力高光保护：扩大保护范围
        if (x > 0.8f) {
            // 高光保护：使用平滑的过渡
            float protection = std::pow((1.0f - x) / 0.2f, 0.5f);
            result = x * (1.0f - protection) + result * protection;
        }
        
        // 全局暗部补偿：防止任何暗部过度压缩
        if (result < x && x < 0.35f) {
            // 如果对比度调整使暗部变得更暗，强力补偿
            float compensation = (0.35f - x) / 0.35f * 0.4f;  // 增强补偿
            result = result * (1.0f - compensation) + x * compensation;
        }
        
        // 额外保护：确保暗部不会变得比原始值暗太多
        if (x < 0.2f && result < x * 0.85f) {
            // 限制暗部最多只能变暗15%
            result = x * 0.85f;
        }
        
    } else {
        // 减少对比度：向中灰混合
        float midGray = 0.5f;
        result = x * (1.0f + strength) + midGray * (-strength);
    }
    
    // 确保在有效范围内
    return std::clamp(result, 0.0f, 1.0f);
}

void ContrastAdjustment::applyContrast(float& r, float& g, float& b, float contrast) {
    // 如果对比度接近 1.0，不做调整
    if (std::abs(contrast - 1.0f) < 0.01f) {
        return;
    }
    
    // 简化版本：直接应用线性对比度调整
    // 不需要复杂的饱和度保持，因为我们使用了更温和的算法
    
    r = applySCurveContrast(r, contrast);
    g = applySCurveContrast(g, contrast);
    b = applySCurveContrast(b, contrast);
    
    // 确保非负
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
}

} // namespace filmtracker
