#include "fast_bilateral_filter.h"
#include <cmath>
#include <algorithm>
#include <thread>
#include <vector>
#include <android/log.h>

#define LOG_TAG "FastBilateralFilter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

/**
 * 计算降采样因子
 * 
 * 基于 spatialSigma 计算合适的降采样因子
 * 规则：spatialSigma / factor 应该在 2-4 的范围内，以保持滤波质量
 */
int FastBilateralFilter::calculateDownsampleFactor(float spatialSigma) {
    // 目标：降采样后的 spatialSigma 应该在 2-4 之间
    // 这样可以保持滤波质量，同时减少计算量
    
    if (spatialSigma <= 4.0f) {
        return 1;  // 不降采样
    } else if (spatialSigma <= 8.0f) {
        return 2;
    } else if (spatialSigma <= 16.0f) {
        return 4;
    } else if (spatialSigma <= 32.0f) {
        return 8;
    } else {
        return 16;  // 最大降采样因子
    }
}

/**
 * 降采样图像（使用区域平均）
 * 
 * 对每个 factor x factor 的区域计算平均值
 */
void FastBilateralFilter::downsample(
    const LinearImage& input,
    LinearImage& output,
    int factor
) {
    const uint32_t inputWidth = input.width;
    const uint32_t inputHeight = input.height;
    const uint32_t outputWidth = (inputWidth + factor - 1) / factor;
    const uint32_t outputHeight = (inputHeight + factor - 1) / factor;
    
    LOGI("downsample: %ux%u -> %ux%u (factor=%d)", 
         inputWidth, inputHeight, outputWidth, outputHeight, factor);
    
    // 确保输出图像大小正确
    if (output.width != outputWidth || output.height != outputHeight) {
        output = LinearImage(outputWidth, outputHeight);
    }
    
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t rowsPerThread = outputHeight / numThreads;
    
    std::vector<std::thread> threads;
    for (uint32_t t = 0; t < numThreads; ++t) {
        uint32_t startRow = t * rowsPerThread;
        uint32_t endRow = (t == numThreads - 1) ? outputHeight : (t + 1) * rowsPerThread;
        
        threads.emplace_back([&input, &output, inputWidth, inputHeight, outputWidth, factor, startRow, endRow]() {
            for (uint32_t outY = startRow; outY < endRow; ++outY) {
                for (uint32_t outX = 0; outX < outputWidth; ++outX) {
                    // 计算输入图像中对应的区域
                    uint32_t inStartX = outX * factor;
                    uint32_t inStartY = outY * factor;
                    uint32_t inEndX = std::min(inStartX + factor, inputWidth);
                    uint32_t inEndY = std::min(inStartY + factor, inputHeight);
                    
                    // 计算区域平均值
                    float sumR = 0.0f, sumG = 0.0f, sumB = 0.0f;
                    uint32_t count = 0;
                    
                    for (uint32_t inY = inStartY; inY < inEndY; ++inY) {
                        for (uint32_t inX = inStartX; inX < inEndX; ++inX) {
                            uint32_t inIdx = inY * inputWidth + inX;
                            sumR += input.r[inIdx];
                            sumG += input.g[inIdx];
                            sumB += input.b[inIdx];
                            count++;
                        }
                    }
                    
                    // 写入输出
                    uint32_t outIdx = outY * outputWidth + outX;
                    if (count > 0) {
                        output.r[outIdx] = sumR / count;
                        output.g[outIdx] = sumG / count;
                        output.b[outIdx] = sumB / count;
                    } else {
                        output.r[outIdx] = 0.0f;
                        output.g[outIdx] = 0.0f;
                        output.b[outIdx] = 0.0f;
                    }
                }
            }
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
}

/**
 * 上采样图像（使用双线性插值）
 */
void FastBilateralFilter::upsample(
    const LinearImage& input,
    LinearImage& output,
    uint32_t targetWidth,
    uint32_t targetHeight
) {
    const uint32_t inputWidth = input.width;
    const uint32_t inputHeight = input.height;
    
    LOGI("upsample: %ux%u -> %ux%u", 
         inputWidth, inputHeight, targetWidth, targetHeight);
    
    // 确保输出图像大小正确
    if (output.width != targetWidth || output.height != targetHeight) {
        output = LinearImage(targetWidth, targetHeight);
    }
    
    // 计算缩放比例
    const float scaleX = static_cast<float>(inputWidth) / targetWidth;
    const float scaleY = static_cast<float>(inputHeight) / targetHeight;
    
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t rowsPerThread = targetHeight / numThreads;
    
    std::vector<std::thread> threads;
    for (uint32_t t = 0; t < numThreads; ++t) {
        uint32_t startRow = t * rowsPerThread;
        uint32_t endRow = (t == numThreads - 1) ? targetHeight : (t + 1) * rowsPerThread;
        
        threads.emplace_back([&input, &output, inputWidth, inputHeight, targetWidth, scaleX, scaleY, startRow, endRow]() {
            for (uint32_t outY = startRow; outY < endRow; ++outY) {
                for (uint32_t outX = 0; outX < targetWidth; ++outX) {
                    // 计算输入图像中的浮点坐标
                    float srcX = (outX + 0.5f) * scaleX - 0.5f;
                    float srcY = (outY + 0.5f) * scaleY - 0.5f;
                    
                    // 限制在有效范围内
                    srcX = std::max(0.0f, std::min(srcX, static_cast<float>(inputWidth - 1)));
                    srcY = std::max(0.0f, std::min(srcY, static_cast<float>(inputHeight - 1)));
                    
                    // 计算整数坐标和小数部分
                    uint32_t x0 = static_cast<uint32_t>(srcX);
                    uint32_t y0 = static_cast<uint32_t>(srcY);
                    uint32_t x1 = std::min(x0 + 1, inputWidth - 1);
                    uint32_t y1 = std::min(y0 + 1, inputHeight - 1);
                    
                    float fx = srcX - x0;
                    float fy = srcY - y0;
                    
                    // 双线性插值
                    uint32_t idx00 = y0 * inputWidth + x0;
                    uint32_t idx01 = y0 * inputWidth + x1;
                    uint32_t idx10 = y1 * inputWidth + x0;
                    uint32_t idx11 = y1 * inputWidth + x1;
                    
                    float r00 = input.r[idx00];
                    float r01 = input.r[idx01];
                    float r10 = input.r[idx10];
                    float r11 = input.r[idx11];
                    
                    float g00 = input.g[idx00];
                    float g01 = input.g[idx01];
                    float g10 = input.g[idx10];
                    float g11 = input.g[idx11];
                    
                    float b00 = input.b[idx00];
                    float b01 = input.b[idx01];
                    float b10 = input.b[idx10];
                    float b11 = input.b[idx11];
                    
                    // 插值计算
                    float r0 = r00 * (1.0f - fx) + r01 * fx;
                    float r1 = r10 * (1.0f - fx) + r11 * fx;
                    float r = r0 * (1.0f - fy) + r1 * fy;
                    
                    float g0 = g00 * (1.0f - fx) + g01 * fx;
                    float g1 = g10 * (1.0f - fx) + g11 * fx;
                    float g = g0 * (1.0f - fy) + g1 * fy;
                    
                    float b0 = b00 * (1.0f - fx) + b01 * fx;
                    float b1 = b10 * (1.0f - fx) + b11 * fx;
                    float b = b0 * (1.0f - fy) + b1 * fy;
                    
                    // 写入输出
                    uint32_t outIdx = outY * targetWidth + outX;
                    output.r[outIdx] = r;
                    output.g[outIdx] = g;
                    output.b[outIdx] = b;
                }
            }
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
}

/**
 * 在降采样图像上应用标准双边滤波
 */
void FastBilateralFilter::applyStandard(
    const LinearImage& input,
    LinearImage& output,
    float spatialSigma,
    float rangeSigma
) {
    const uint32_t width = input.width;
    const uint32_t height = input.height;
    const int radius = static_cast<int>(std::ceil(3.0f * spatialSigma));
    
    LOGI("applyStandard: width=%u, height=%u, radius=%d, spatialSigma=%.2f", 
         width, height, radius, spatialSigma);
    
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
                    
                    // 计算中心像素的亮度
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
                            
                            // 计算空间权重
                            float spatialDist = std::sqrt(static_cast<float>(dx * dx + dy * dy));
                            float spatialWeight = std::exp(-(spatialDist * spatialDist) / (2.0f * spatialSigma * spatialSigma));
                            
                            // 计算强度权重
                            float rangeDist = std::abs(neighborLuminance - centerLuminance);
                            float rangeWeight = std::exp(-(rangeDist * rangeDist) / (2.0f * rangeSigma * rangeSigma));
                            
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
}

/**
 * 应用快速近似双边滤波
 * 
 * 算法流程：
 * 1. 根据 spatialSigma 计算降采样因子
 * 2. 降采样输入图像
 * 3. 在降采样图像上应用标准双边滤波（使用调整后的 spatialSigma）
 * 4. 上采样结果到原始分辨率
 */
void FastBilateralFilter::apply(
    const LinearImage& input,
    LinearImage& output,
    float spatialSigma,
    float rangeSigma
) {
    LOGI("apply: input=%ux%u, spatialSigma=%.2f, rangeSigma=%.2f", 
         input.width, input.height, spatialSigma, rangeSigma);
    
    // 计算降采样因子
    int downsampleFactor = calculateDownsampleFactor(spatialSigma);
    
    LOGI("apply: downsampleFactor=%d", downsampleFactor);
    
    // 如果不需要降采样，直接使用标准算法
    if (downsampleFactor == 1) {
        applyStandard(input, output, spatialSigma, rangeSigma);
        return;
    }
    
    // 降采样
    uint32_t downsampledWidth = (input.width + downsampleFactor - 1) / downsampleFactor;
    uint32_t downsampledHeight = (input.height + downsampleFactor - 1) / downsampleFactor;
    LinearImage downsampled(downsampledWidth, downsampledHeight);
    downsample(input, downsampled, downsampleFactor);
    
    // 在降采样图像上应用标准双边滤波
    // 注意：spatialSigma 需要根据降采样因子调整
    float adjustedSpatialSigma = spatialSigma / downsampleFactor;
    LinearImage filtered(downsampledWidth, downsampledHeight);
    applyStandard(downsampled, filtered, adjustedSpatialSigma, rangeSigma);
    
    // 上采样到原始分辨率
    upsample(filtered, output, input.width, input.height);
    
    LOGI("apply: Completed successfully");
}

} // namespace filmtracker
