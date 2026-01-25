#include "exposure_adjustment.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

float ExposureAdjustment::compressHighlights(float value, float amount) {
    // 高光压缩：使用软裁剪保护高光细节
    // amount 越大，压缩越强
    
    if (value < 0.5f || amount < 0.001f) {
        return value;
    }
    
    // 计算高光权重（0.5以上逐渐增强）
    float highlightWeight = std::pow((value - 0.5f) / 0.5f, 0.7f);
    
    // 使用 tanh 进行软压缩
    float compressed = 0.5f + std::tanh((value - 0.5f) * (1.0f - amount * 0.5f)) / std::tanh(0.5f * (1.0f - amount * 0.5f)) * 0.5f;
    
    // 混合原始值和压缩值
    return value * (1.0f - highlightWeight * amount) + compressed * (highlightWeight * amount);
}

float ExposureAdjustment::liftShadows(float value, float amount) {
    // 暗部提升：当增加曝光时，适度提升暗部
    
    if (value > 0.3f || amount < 0.001f) {
        return value;
    }
    
    // 计算暗部权重（0.3以下逐渐增强）
    float shadowWeight = std::pow((0.3f - value) / 0.3f, 0.8f);
    
    // 提升量：暗部提升更多
    float lift = shadowWeight * amount * 0.15f;
    
    return value + lift;
}

float ExposureAdjustment::applyExposureToValue(float value, float exposureEV) {
    // 计算基础曝光因子
    float exposureFactor = std::pow(2.0f, exposureEV);
    
    // 应用曝光
    float result = value * exposureFactor;
    
    // 如果是增加曝光，应用高光保护
    if (exposureEV > 0.0f && result > 0.5f) {
        // 压缩强度随曝光量增加
        float compressionAmount = std::min(exposureEV / 5.0f, 1.0f);
        result = compressHighlights(result, compressionAmount * 0.6f);
    }
    
    // 如果是增加曝光，适度提升暗部
    if (exposureEV > 0.0f && result < 0.3f) {
        float liftAmount = std::min(exposureEV / 5.0f, 1.0f);
        result = liftShadows(result, liftAmount);
    }
    
    // 确保非负
    return std::max(0.0f, result);
}

void ExposureAdjustment::applyExposure(float& r, float& g, float& b, float exposureEV) {
    // 如果曝光接近0，不做调整
    if (std::abs(exposureEV) < 0.01f) {
        return;
    }
    
    // 对每个通道应用曝光
    r = applyExposureToValue(r, exposureEV);
    g = applyExposureToValue(g, exposureEV);
    b = applyExposureToValue(b, exposureEV);
}

} // namespace filmtracker
