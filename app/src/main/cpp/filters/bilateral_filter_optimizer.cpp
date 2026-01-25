#include "bilateral_filter_optimizer.h"
#include "fast_bilateral_filter.h"
#include "vulkan_bilateral_filter.h"
#include <cmath>
#include <algorithm>
#include <thread>
#include <vector>
#include <android/log.h>

#define LOG_TAG "BilateralFilterOptimizer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

/**
 * 选择最优实现方式
 */
BilateralFilterOptimizer::Implementation BilateralFilterOptimizer::selectImplementation(
    uint32_t width,
    uint32_t height,
    float spatialSigma,
    float rangeSigma,
    bool enableFastApproximation,
    bool enableGPU
) {
    // 计算图像像素数
    const uint32_t pixelCount = width * height;
    
    LOGI("selectImplementation: width=%u, height=%u, pixels=%u, spatialSigma=%.2f, rangeSigma=%.2f",
         width, height, pixelCount, spatialSigma, rangeSigma);
    LOGI("selectImplementation: enableFastApproximation=%d, enableGPU=%d",
         enableFastApproximation, enableGPU);
    
    // 优先级 1: 检查是否应该使用 GPU 加速
    // 条件：GPU 启用 && 图像足够大 (> 2MP) && GPU 可用
    if (enableGPU && pixelCount > LARGE_IMAGE_PIXELS) {
        if (isGPUAvailable()) {
            LOGI("selectImplementation: Selected GPU_VULKAN (pixels=%u > %u, GPU available)",
                 pixelCount, LARGE_IMAGE_PIXELS);
            return Implementation::GPU_VULKAN;
        } else {
            LOGI("selectImplementation: GPU requested but not available, checking alternatives");
        }
    }
    
    // 优先级 2: 检查是否应该使用快速近似算法
    // 条件：快速近似启用 && spatialSigma 足够大 (> 5.0)
    if (enableFastApproximation && spatialSigma > LARGE_SPATIAL_SIGMA) {
        LOGI("selectImplementation: Selected FAST_APPROXIMATION (spatialSigma=%.2f > %.2f)",
             spatialSigma, LARGE_SPATIAL_SIGMA);
        return Implementation::FAST_APPROXIMATION;
    }
    
    // 优先级 3: 使用标准 CPU 实现
    LOGI("selectImplementation: Selected STANDARD_CPU (default)");
    return Implementation::STANDARD_CPU;
}

/**
 * 执行双边滤波（自动选择实现）
 */
BilateralFilterOptimizer::Implementation BilateralFilterOptimizer::execute(
    const LinearImage& input,
    LinearImage& output,
    float spatialSigma,
    float rangeSigma,
    Implementation hint,
    bool enableFastApproximation,
    bool enableGPU
) {
    // 如果提供了 hint 且不是 STANDARD_CPU，尝试使用 hint
    Implementation selectedImpl = hint;
    
    // 如果 hint 是 STANDARD_CPU，则自动选择
    if (hint == Implementation::STANDARD_CPU) {
        selectedImpl = selectImplementation(
            input.width,
            input.height,
            spatialSigma,
            rangeSigma,
            enableFastApproximation,
            enableGPU
        );
    }
    
    LOGI("execute: Using implementation: %s", getImplementationName(selectedImpl));
    
    // 根据选择的实现执行
    bool success = false;
    
    switch (selectedImpl) {
        case Implementation::GPU_VULKAN:
            success = executeGPU(input, output, spatialSigma, rangeSigma);
            if (success) {
                return Implementation::GPU_VULKAN;
            }
            // GPU 失败，回退到快速近似或标准 CPU
            LOGW("execute: GPU execution failed, falling back");
            if (enableFastApproximation && spatialSigma > LARGE_SPATIAL_SIGMA) {
                selectedImpl = Implementation::FAST_APPROXIMATION;
            } else {
                selectedImpl = Implementation::STANDARD_CPU;
            }
            break;
            
        case Implementation::FAST_APPROXIMATION:
            executeFastApproximation(input, output, spatialSigma, rangeSigma);
            return Implementation::FAST_APPROXIMATION;
            
        case Implementation::STANDARD_CPU:
            executeStandardCPU(input, output, spatialSigma, rangeSigma);
            return Implementation::STANDARD_CPU;
    }
    
    // 如果 GPU 失败，执行回退的实现
    if (selectedImpl == Implementation::FAST_APPROXIMATION) {
        LOGI("execute: Fallback to FAST_APPROXIMATION");
        executeFastApproximation(input, output, spatialSigma, rangeSigma);
        return Implementation::FAST_APPROXIMATION;
    } else {
        LOGI("execute: Fallback to STANDARD_CPU");
        executeStandardCPU(input, output, spatialSigma, rangeSigma);
        return Implementation::STANDARD_CPU;
    }
}

/**
 * 获取实现方式的名称
 */
const char* BilateralFilterOptimizer::getImplementationName(Implementation impl) {
    switch (impl) {
        case Implementation::STANDARD_CPU:
            return "STANDARD_CPU";
        case Implementation::FAST_APPROXIMATION:
            return "FAST_APPROXIMATION";
        case Implementation::GPU_VULKAN:
            return "GPU_VULKAN";
        default:
            return "UNKNOWN";
    }
}

/**
 * 检查 GPU 是否可用
 */
bool BilateralFilterOptimizer::isGPUAvailable() {
    // 尝试初始化 GPU（如果尚未初始化）
    if (!VulkanBilateralFilter::isAvailable()) {
        LOGI("isGPUAvailable: GPU not initialized, attempting initialization");
        VulkanBilateralFilter::initialize();
    }
    
    bool available = VulkanBilateralFilter::isAvailable();
    LOGI("isGPUAvailable: %s", available ? "true" : "false");
    return available;
}

/**
 * 执行标准 CPU 实现
 */
void BilateralFilterOptimizer::executeStandardCPU(
    const LinearImage& input,
    LinearImage& output,
    float spatialSigma,
    float rangeSigma
) {
    LOGI("executeStandardCPU: Starting standard CPU bilateral filter");
    
    const uint32_t width = input.width;
    const uint32_t height = input.height;
    const int radius = static_cast<int>(std::ceil(3.0f * spatialSigma));
    
    // 确保输出图像大小正确
    if (output.width != width || output.height != height) {
        output = LinearImage(width, height);
    }
    
    // 多线程处理
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t rowsPerThread = height / numThreads;
    
    LOGI("executeStandardCPU: radius=%d, threads=%u", radius, numThreads);
    
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
                            float spatialWeight = std::exp(-(spatialDist * spatialDist) / (2.0f * spatialSigma * spatialSigma));
                            
                            // 计算强度权重（基于亮度差异）
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
    
    LOGI("executeStandardCPU: Completed successfully");
}

/**
 * 执行快速近似算法
 */
void BilateralFilterOptimizer::executeFastApproximation(
    const LinearImage& input,
    LinearImage& output,
    float spatialSigma,
    float rangeSigma
) {
    LOGI("executeFastApproximation: Starting fast approximation bilateral filter");
    
    FastBilateralFilter::apply(input, output, spatialSigma, rangeSigma);
    
    LOGI("executeFastApproximation: Completed successfully");
}

/**
 * 执行 GPU 加速
 */
bool BilateralFilterOptimizer::executeGPU(
    const LinearImage& input,
    LinearImage& output,
    float spatialSigma,
    float rangeSigma
) {
    LOGI("executeGPU: Starting GPU-accelerated bilateral filter");
    
    bool success = VulkanBilateralFilter::apply(input, output, spatialSigma, rangeSigma);
    
    if (success) {
        LOGI("executeGPU: Completed successfully");
    } else {
        LOGE("executeGPU: Failed to execute on GPU");
    }
    
    return success;
}

} // namespace filmtracker
