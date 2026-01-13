#include "film_engine.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

/**
 * 计算单个通道的非线性响应曲线
 * 
 * 实现三段曲线：Toe（暗部抬升）-> Linear（中间调）-> Shoulder（高光压缩）
 * 
 * 这是胶片银盐成像的核心：模拟银盐颗粒在不同曝光强度下的非线性响应
 */
float FilmEngine::computeResponse(float linearValue, const ChannelResponseParams& params) {
    // 应用曝光偏移
    float exposed = linearValue * std::pow(2.0f, params.exposureOffset);
    
    // 限制输入范围
    exposed = std::max(0.0f, std::min(1.0f, exposed));
    
    float result;
    
    if (exposed < params.toePoint) {
        // Toe 区域：暗部抬升（模拟胶片对低光的非线性响应）
        float toeRatio = exposed / params.toePoint;
        result = params.toeSlope * toeRatio * toeRatio + 
                 params.toeStrength * toeRatio;
    } else if (exposed < params.shoulderPoint) {
        // Linear 区域：中间调线性响应
        float linearRatio = (exposed - params.toePoint) / 
                           (params.shoulderPoint - params.toePoint);
        float toeEnd = params.toeSlope + params.toeStrength;
        result = toeEnd + linearRatio * (params.linearSlope * 
                (params.shoulderPoint - params.toePoint) + params.linearOffset);
    } else {
        // Shoulder 区域：高光压缩（模拟胶片对高光的饱和响应）
        float shoulderRatio = (exposed - params.shoulderPoint) / 
                             (1.0f - params.shoulderPoint);
        float shoulderStart = params.toeSlope + params.toeStrength + 
                            params.linearSlope * (params.shoulderPoint - params.toePoint);
        result = shoulderStart + 
                params.shoulderSlope * (1.0f - std::exp(-shoulderRatio * params.shoulderStrength));
    }
    
    // 归一化到 [0, 1]
    return std::max(0.0f, std::min(1.0f, result));
}

/**
 * 应用通道独立的非线性响应曲线
 * 
 * 每个 RGB 通道使用独立的参数，模拟真实胶片各层银盐的不同响应特性
 */
void FilmEngine::applyResponseCurve(LinearImage& image, const FilmParams& params) {
    const uint32_t pixelCount = image.width * image.height;
    
    // 应用全局曝光
    float globalExposureMultiplier = std::pow(2.0f, params.globalExposure);
    
    #pragma omp parallel for
    for (uint32_t i = 0; i < pixelCount; ++i) {
        // 应用全局曝光（在线性域）
        float r = image.r[i] * globalExposureMultiplier;
        float g = image.g[i] * globalExposureMultiplier;
        float b = image.b[i] * globalExposureMultiplier;
        
        // 应用通道独立的响应曲线
        image.r[i] = computeResponse(r, params.redChannel);
        image.g[i] = computeResponse(g, params.greenChannel);
        image.b[i] = computeResponse(b, params.blueChannel);
        
        // 应用对比度（在响应曲线之后）
        float avg = (image.r[i] + image.g[i] + image.b[i]) / 3.0f;
        image.r[i] = avg + (image.r[i] - avg) * params.contrast;
        image.g[i] = avg + (image.g[i] - avg) * params.contrast;
        image.b[i] = avg + (image.b[i] - avg) * params.contrast;
    }
}

} // namespace filmtracker
