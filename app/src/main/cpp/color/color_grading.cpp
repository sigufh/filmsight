#include "color_grading.h"
#include <cmath>
#include <algorithm>
#include <thread>
#include <vector>
#include <android/log.h>

#define LOG_TAG "ColorGrading"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

/**
 * 高斯函数
 * 
 * 计算标准高斯分布的值
 */
float ColorGrading::gaussian(float x, float center, float width) {
    float diff = x - center;
    return std::exp(-(diff * diff) / (2.0f * width * width));
}

/**
 * 计算高斯权重
 * 
 * 使用三个高斯函数分别计算阴影、中间调和高光的权重
 * 确保三个权重之和为 1.0，实现平滑过渡
 */
void ColorGrading::calculateGaussianWeights(float luminance,
                                           float balance,
                                           float& shadowWeight,
                                           float& midtoneWeight,
                                           float& highlightWeight) {
    // 根据 balance 参数调整区域中心
    // balance = 0: 标准分布（阴影 0.2, 中间调 0.5, 高光 0.8）
    // balance < 0: 向阴影偏移
    // balance > 0: 向高光偏移
    float shadowCenter = 0.2f + balance * 0.15f;
    float midtoneCenter = 0.5f + balance * 0.1f;
    float highlightCenter = 0.8f + balance * 0.15f;
    
    // 高斯宽度（标准差）
    // 较大的宽度意味着更平滑的过渡
    const float shadowWidth = 0.25f;
    const float midtoneWidth = 0.3f;
    const float highlightWidth = 0.25f;
    
    // 计算原始高斯权重
    float sw = gaussian(luminance, shadowCenter, shadowWidth);
    float mw = gaussian(luminance, midtoneCenter, midtoneWidth);
    float hw = gaussian(luminance, highlightCenter, highlightWidth);
    
    // 归一化权重，确保总和为 1.0
    float totalWeight = sw + mw + hw;
    if (totalWeight > 0.0f) {
        shadowWeight = sw / totalWeight;
        midtoneWeight = mw / totalWeight;
        highlightWeight = hw / totalWeight;
    } else {
        // 极端情况：默认为中间调
        shadowWeight = 0.0f;
        midtoneWeight = 1.0f;
        highlightWeight = 0.0f;
    }
}

/**
 * RGB 到 LMS 色彩空间转换
 * 
 * 使用 Hunt-Pointer-Estevez 矩阵（D65 白点）
 * 这是基于人眼锥细胞响应的色彩空间
 */
void ColorGrading::rgbToLMS(float r, float g, float b, float& l, float& m, float& s) {
    // Hunt-Pointer-Estevez 矩阵
    l = 0.4002f * r + 0.7075f * g - 0.0807f * b;
    m = -0.2280f * r + 1.1500f * g + 0.0612f * b;
    s = 0.0000f * r + 0.0000f * g + 0.9184f * b;
}

/**
 * LMS 到 RGB 色彩空间转换
 * 
 * 使用 Hunt-Pointer-Estevez 逆矩阵
 */
void ColorGrading::lmsToRGB(float l, float m, float s, float& r, float& g, float& b) {
    // Hunt-Pointer-Estevez 逆矩阵
    r = 1.8599f * l - 1.1294f * m + 0.2198f * s;
    g = 0.3611f * l + 0.6388f * m - 0.0000f * s;
    b = 0.0000f * l + 0.0000f * m + 1.0890f * s;
}

/**
 * 应用色彩分级
 * 
 * 实现流程：
 * 1. 计算每个像素的亮度
 * 2. 根据亮度计算三个区域的高斯权重
 * 3. 转换到 LMS 色彩空间
 * 4. 应用加权的色彩调整
 * 5. 转换回 RGB 色彩空间
 * 6. 应用 blending 参数控制整体强度
 */
void ColorGrading::applyGrading(LinearImage& image, const GradingParams& params) {
    LOGI("applyGrading: blending=%.2f, balance=%.2f", params.blending, params.balance);
    
    // 如果所有调整都是 0，直接返回
    if (std::abs(params.highlightR) < 0.001f && std::abs(params.highlightG) < 0.001f && 
        std::abs(params.highlightB) < 0.001f &&
        std::abs(params.midtoneR) < 0.001f && std::abs(params.midtoneG) < 0.001f && 
        std::abs(params.midtoneB) < 0.001f &&
        std::abs(params.shadowR) < 0.001f && std::abs(params.shadowG) < 0.001f && 
        std::abs(params.shadowB) < 0.001f) {
        LOGI("applyGrading: All adjustments are zero, skipping");
        return;
    }
    
    const uint32_t pixelCount = image.width * image.height;
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t pixelsPerThread = pixelCount / numThreads;
    
    LOGI("applyGrading: Processing %u pixels with %u threads", pixelCount, numThreads);
    
    std::vector<std::thread> threads;
    for (uint32_t t = 0; t < numThreads; ++t) {
        uint32_t start = t * pixelsPerThread;
        uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
        
        threads.emplace_back([&image, &params, start, end]() {
            for (uint32_t i = start; i < end; ++i) {
                float r = image.r[i];
                float g = image.g[i];
                float b = image.b[i];
                
                // 1. 计算亮度（使用 Rec. 709 系数）
                float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
                luminance = std::max(0.0f, std::min(1.0f, luminance));
                
                // 2. 计算高斯权重
                float shadowWeight, midtoneWeight, highlightWeight;
                calculateGaussianWeights(luminance, params.balance,
                                        shadowWeight, midtoneWeight, highlightWeight);
                
                // 3. 转换到 LMS 色彩空间
                float l, m, s;
                rgbToLMS(r, g, b, l, m, s);
                
                // 4. 计算加权的色彩调整
                // 在 LMS 空间中，色彩调整更加自然
                float adjustL = shadowWeight * params.shadowR +
                               midtoneWeight * params.midtoneR +
                               highlightWeight * params.highlightR;
                
                float adjustM = shadowWeight * params.shadowG +
                               midtoneWeight * params.midtoneG +
                               highlightWeight * params.highlightG;
                
                float adjustS = shadowWeight * params.shadowB +
                               midtoneWeight * params.midtoneB +
                               highlightWeight * params.highlightB;
                
                // 应用调整（加法模式）
                l += adjustL * params.blending;
                m += adjustM * params.blending;
                s += adjustS * params.blending;
                
                // 5. 转换回 RGB 色彩空间
                lmsToRGB(l, m, s, r, g, b);
                
                // 6. 保存结果（允许超出 [0,1] 范围，保留动态范围）
                image.r[i] = std::max(0.0f, r);
                image.g[i] = std::max(0.0f, g);
                image.b[i] = std::max(0.0f, b);
            }
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
    
    LOGI("applyGrading: Completed successfully");
}

} // namespace filmtracker
