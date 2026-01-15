#include "bilateral_filter.h"
#include <cmath>
#include <algorithm>
#include <thread>
#include <vector>
#include <android/log.h>

#define LOG_TAG "BilateralFilter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

/**
 * 计算高斯权重
 */
float BilateralFilter::gaussianWeight(float distance, float sigma) {
    return std::exp(-(distance * distance) / (2.0f * sigma * sigma));
}

/**
 * 计算滤波器半径
 * 
 * 使用 3-sigma 规则：99.7% 的权重在 3*sigma 范围内
 */
int BilateralFilter::calculateRadius(float sigma) {
    return static_cast<int>(std::ceil(3.0f * sigma));
}

/**
 * 应用双边滤波器
 * 
 * 标准实现：对每个像素，遍历其邻域，计算空间和强度权重
 */
void BilateralFilter::apply(const LinearImage& input,
                           LinearImage& output,
                           float spatialSigma,
                           float rangeSigma) {
    LOGI("apply: spatialSigma=%.2f, rangeSigma=%.2f", spatialSigma, rangeSigma);
    
    const uint32_t width = input.width;
    const uint32_t height = input.height;
    const int radius = calculateRadius(spatialSigma);
    
    LOGI("apply: radius=%d", radius);
    
    // 确保输出图像大小正确
    if (output.width != width || output.height != height) {
        output = LinearImage(width, height);
    }
    
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t rowsPerThread = height / numThreads;
    
    std::vector<std::thread> threads;
    for (uint32_t t = 0; t < numThreads; ++t) {
        uint32_t startRow = t * rowsPerThread;
        uint32_t endRow = (t == numThreads - 1) ? height : (t + 1) * rowsPerThread;
        
        threads.emplace_back([&input, &output, width, height, radius, spatialSigma, rangeSigma, startRow, endRow]() {
            for (uint32_t y = startRow; y < endRow; ++y) {
                for (uint32_t x = 0; x < width; ++x) {
                    uint32_t centerIdx = y * width + x;
                    
                    float centerR = input.r[centerIdx];
                    float centerG = input.g[centerIdx];
                    float centerB = input.b[centerIdx];
                    
                    // 计算中心像素的亮度（用于强度权重）
                    float centerLuminance = 0.2126f * centerR + 0.7152f * centerG + 0.0722f * centerB;
                    
                    float sumR = 0.0f, sumG = 0.0f, sumB = 0.0f;
                    float sumWeight = 0.0f;
                    
                    // 遍历邻域
                    for (int dy = -radius; dy <= radius; ++dy) {
                        int ny = static_cast<int>(y) + dy;
                        if (ny < 0 || ny >= static_cast<int>(height)) continue;
                        
                        for (int dx = -radius; dx <= radius; ++dx) {
                            int nx = static_cast<int>(x) + dx;
                            if (nx < 0 || nx >= static_cast<int>(width)) continue;
                            
                            uint32_t neighborIdx = ny * width + nx;
                            
                            float neighborR = input.r[neighborIdx];
                            float neighborG = input.g[neighborIdx];
                            float neighborB = input.b[neighborIdx];
                            
                            // 计算邻域像素的亮度
                            float neighborLuminance = 0.2126f * neighborR + 0.7152f * neighborG + 0.0722f * neighborB;
                            
                            // 计算空间权重（基于欧氏距离）
                            float spatialDist = std::sqrt(static_cast<float>(dx * dx + dy * dy));
                            float spatialWeight = gaussianWeight(spatialDist, spatialSigma);
                            
                            // 计算强度权重（基于亮度差异）
                            float rangeDist = std::abs(neighborLuminance - centerLuminance);
                            float rangeWeight = gaussianWeight(rangeDist, rangeSigma);
                            
                            // 组合权重
                            float weight = spatialWeight * rangeWeight;
                            
                            sumR += neighborR * weight;
                            sumG += neighborG * weight;
                            sumB += neighborB * weight;
                            sumWeight += weight;
                        }
                    }
                    
                    // 归一化
                    if (sumWeight > 0.0f) {
                        output.r[centerIdx] = sumR / sumWeight;
                        output.g[centerIdx] = sumG / sumWeight;
                        output.b[centerIdx] = sumB / sumWeight;
                    } else {
                        output.r[centerIdx] = centerR;
                        output.g[centerIdx] = centerG;
                        output.b[centerIdx] = centerB;
                    }
                }
            }
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
    
    LOGI("apply: Completed successfully");
}

/**
 * 应用快速双边滤波器
 * 
 * 使用可分离近似：先水平后垂直
 * 注意：这是一个简化实现，真正的快速双边滤波需要更复杂的算法
 */
void BilateralFilter::applyFast(const LinearImage& input,
                               LinearImage& output,
                               float spatialSigma,
                               float rangeSigma) {
    LOGI("applyFast: Using standard implementation (fast version not yet optimized)");
    // 目前使用标准实现
    // TODO: 实现真正的快速版本（如 Paris & Durand 的方法）
    apply(input, output, spatialSigma, rangeSigma);
}

/**
 * 提取细节层
 * 
 * 细节层 = 原图 - 基础层（双边滤波结果）
 * 这个细节层可以用于清晰度调整
 */
void BilateralFilter::extractDetail(const LinearImage& input,
                                   LinearImage& detail,
                                   float spatialSigma,
                                   float rangeSigma) {
    LOGI("extractDetail: spatialSigma=%.2f, rangeSigma=%.2f", spatialSigma, rangeSigma);
    
    // 确保细节图像大小正确
    if (detail.width != input.width || detail.height != input.height) {
        detail = LinearImage(input.width, input.height);
    }
    
    // 创建基础层（双边滤波结果）
    LinearImage base(input.width, input.height);
    apply(input, base, spatialSigma, rangeSigma);
    
    // 计算细节层 = 原图 - 基础层
    const uint32_t pixelCount = input.width * input.height;
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t pixelsPerThread = pixelCount / numThreads;
    
    std::vector<std::thread> threads;
    for (uint32_t t = 0; t < numThreads; ++t) {
        uint32_t start = t * pixelsPerThread;
        uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
        
        threads.emplace_back([&input, &base, &detail, start, end]() {
            for (uint32_t i = start; i < end; ++i) {
                detail.r[i] = input.r[i] - base.r[i];
                detail.g[i] = input.g[i] - base.g[i];
                detail.b[i] = input.b[i] - base.b[i];
            }
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
    
    LOGI("extractDetail: Completed successfully");
}

} // namespace filmtracker
