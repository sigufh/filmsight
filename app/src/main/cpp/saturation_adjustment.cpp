#include "include/saturation_adjustment.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

float SaturationAdjustment::getCurrentSaturation(float r, float g, float b, float luminance) {
    // 计算当前饱和度（色度与亮度的比值）
    if (luminance < 0.001f) {
        return 0.0f;
    }
    
    float maxChannel = std::max({r, g, b});
    float minChannel = std::min({r, g, b});
    float chroma = maxChannel - minChannel;
    
    // 归一化饱和度
    return chroma / (luminance + 0.001f);
}

bool SaturationAdjustment::isSkinTone(float r, float g, float b) {
    // 简单的肤色检测
    // 肤色特征：R > G > B，且比例在特定范围内
    
    if (r < g || g < b) {
        return false;
    }
    
    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
    
    // 肤色亮度范围：0.2 到 0.8
    if (luminance < 0.2f || luminance > 0.8f) {
        return false;
    }
    
    // 肤色比例检测
    float rg_ratio = r / (g + 0.001f);
    float gb_ratio = g / (b + 0.001f);
    
    // 典型肤色比例范围
    return (rg_ratio > 1.1f && rg_ratio < 1.6f) && 
           (gb_ratio > 1.1f && gb_ratio < 1.5f);
}

float SaturationAdjustment::protectOversaturation(float satDelta, float currentSat) {
    // 防止过饱和：当饱和度已经很高时，减少增强效果
    
    if (satDelta <= 0.0f) {
        // 降低饱和度时不需要保护
        return satDelta;
    }
    
    // 当前饱和度越高，增强效果越弱
    if (currentSat > 0.6f) {
        // 使用对数曲线压缩
        float protection = 1.0f - std::log(1.0f + (currentSat - 0.6f) * 5.0f) / std::log(3.0f);
        protection = std::max(0.2f, protection);  // 至少保留20%的效果
        return satDelta * protection;
    }
    
    return satDelta;
}

void SaturationAdjustment::applySaturation(float& r, float& g, float& b, float saturation) {
    // 如果饱和度接近0，不做调整
    if (std::abs(saturation) < 0.01f) {
        return;
    }
    
    // 计算亮度
    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
    
    // 将饱和度从 [-100, 100] 转换为乘数
    // saturation = 0 时，factor = 1.0（不变）
    // saturation = 100 时，factor = 2.0（翻倍）
    // saturation = -100 时，factor = 0.0（完全去饱和）
    float saturationFactor = 1.0f + saturation / 100.0f;
    saturationFactor = std::max(0.0f, saturationFactor);
    
    // 计算当前饱和度
    float currentSat = getCurrentSaturation(r, g, b, luminance);
    
    // 肤色保护
    bool isSkin = isSkinTone(r, g, b);
    if (isSkin && saturation > 0.0f) {
        // 肤色区域减少饱和度增强
        saturationFactor = 1.0f + (saturationFactor - 1.0f) * 0.5f;
    }
    
    // 过饱和保护
    if (saturation > 0.0f) {
        float satDelta = saturationFactor - 1.0f;
        satDelta = protectOversaturation(satDelta, currentSat);
        saturationFactor = 1.0f + satDelta;
    }
    
    // 应用饱和度调整
    r = luminance + (r - luminance) * saturationFactor;
    g = luminance + (g - luminance) * saturationFactor;
    b = luminance + (b - luminance) * saturationFactor;
    
    // 确保非负
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
}

void SaturationAdjustment::applyVibrance(float& r, float& g, float& b, float vibrance) {
    // 如果自然饱和度接近0，不做调整
    if (std::abs(vibrance) < 0.01f) {
        return;
    }
    
    // 计算亮度
    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
    
    // 计算当前饱和度
    float currentSat = getCurrentSaturation(r, g, b, luminance);
    
    // 自然饱和度的核心：根据当前饱和度调整增强强度
    // 低饱和度区域增强更多，高饱和度区域增强更少
    float vibranceAmount = vibrance / 100.0f;
    
    // 自适应强度：当前饱和度越低，增强越多
    float adaptiveFactor;
    if (vibranceAmount > 0.0f) {
        // 增加自然饱和度：低饱和度区域增强更多
        adaptiveFactor = 1.0f + vibranceAmount * (1.0f - currentSat);
    } else {
        // 减少自然饱和度：均匀减少
        adaptiveFactor = 1.0f + vibranceAmount;
    }
    
    adaptiveFactor = std::max(0.0f, adaptiveFactor);
    
    // 肤色保护（比普通饱和度更强）
    bool isSkin = isSkinTone(r, g, b);
    if (isSkin && vibranceAmount > 0.0f) {
        // 肤色区域几乎不增强
        adaptiveFactor = 1.0f + (adaptiveFactor - 1.0f) * 0.2f;
    }
    
    // 应用调整
    r = luminance + (r - luminance) * adaptiveFactor;
    g = luminance + (g - luminance) * adaptiveFactor;
    b = luminance + (b - luminance) * adaptiveFactor;
    
    // 确保非负
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
}

} // namespace filmtracker
