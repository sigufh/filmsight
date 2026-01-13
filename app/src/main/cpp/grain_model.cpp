#include "film_engine.h"
#include <random>
#include <cmath>

namespace filmtracker {

// 线程安全的随机数生成器（每个线程独立）
thread_local std::mt19937 rng(std::random_device{}());

/**
 * 生成泊松分布的颗粒值
 * 
 * 颗粒密度遵循泊松统计，模拟银盐颗粒的随机分布
 */
float FilmEngine::generatePoissonGrain(float mean, float variation) {
    std::poisson_distribution<int> poisson(mean);
    int sample = poisson(rng);
    return static_cast<float>(sample) * variation;
}

/**
 * 应用颗粒模型
 * 
 * 颗粒参与曝光过程，而非后期叠加
 * 密度与 ISO、亮度、颜色耦合
 */
void FilmEngine::applyGrain(LinearImage& image, 
                            const GrainParams& grainParams,
                            const RawMetadata& metadata) {
    if (!grainParams.enableGrain) {
        return;
    }
    
    const uint32_t pixelCount = image.width * image.height;
    
    // 计算基础颗粒密度（与 ISO 耦合）
    float isoDensity = grainParams.baseDensity * 
                      (metadata.iso / 100.0f) * 
                      grainParams.isoMultiplier;
    
    #pragma omp parallel for
    for (uint32_t i = 0; i < pixelCount; ++i) {
        // 计算像素亮度
        float luminance = 0.299f * image.r[i] + 
                         0.587f * image.g[i] + 
                         0.114f * image.b[i];
        
        // 颗粒密度与亮度相关（暗部颗粒更明显）
        float brightnessFactor = 1.0f - luminance * 0.5f;
        float pixelDensity = isoDensity * brightnessFactor;
        
        // 为每个通道生成独立的颗粒（颜色耦合）
        float grainR = generatePoissonGrain(pixelDensity, grainParams.sizeVariation);
        float grainG = generatePoissonGrain(pixelDensity * grainParams.colorCoupling, 
                                           grainParams.sizeVariation);
        float grainB = generatePoissonGrain(pixelDensity * grainParams.colorCoupling, 
                                           grainParams.sizeVariation);
        
        // 颗粒参与曝光（加法，在线性域）
        image.r[i] += grainR;
        image.g[i] += grainG;
        image.b[i] += grainB;
        
        // 限制范围
        image.r[i] = std::max(0.0f, std::min(1.0f, image.r[i]));
        image.g[i] = std::max(0.0f, std::min(1.0f, image.g[i]));
        image.b[i] = std::max(0.0f, std::min(1.0f, image.b[i]));
    }
}

} // namespace filmtracker
