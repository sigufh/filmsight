#include "bilateral_filter.h"
#include "fast_bilateral_filter.h"
#include "vulkan_bilateral_filter.h"
#include "image_hash_cache.h"
#include <cmath>
#include <algorithm>
#include <thread>
#include <vector>
#include <chrono>
#include <sstream>
#include <android/log.h>

#define LOG_TAG "BilateralFilter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

// 静态成员初始化
BilateralFilter::Config BilateralFilter::s_config;
BilateralFilter::Stats BilateralFilter::s_stats;

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
 * 应用双边滤波器（内部实现，不使用缓存）
 * 
 * 根据配置和参数选择使用标准实现、快速近似算法或 GPU 加速
 */
static void applyInternal(const LinearImage& input,
                         LinearImage& output,
                         float spatialSigma,
                         float rangeSigma,
                         bool& usedFastApprox,
                         bool& usedGPU,
                         const BilateralFilter::Config& config) {
    usedFastApprox = false;
    usedGPU = false;
    
    // 计算图像像素数
    const uint32_t pixelCount = input.width * input.height;
    
    // 记录当前配置状态
    LOGI("========== BilateralFilter Decision Process ==========");
    LOGI("applyInternal: Current Configuration:");
    LOGI("  - enableGPU: %d", config.enableGPU);
    LOGI("  - enableFastApproximation: %d", config.enableFastApproximation);
    LOGI("  - enableCache: %d", config.enableCache);
    LOGI("  - fastApproxThreshold: %.2f", config.fastApproxThreshold);
    LOGI("  - gpuThresholdPixels: %u", config.gpuThresholdPixels);
    LOGI("applyInternal: Input Parameters:");
    LOGI("  - spatialSigma: %.2f", spatialSigma);
    LOGI("  - rangeSigma: %.2f", rangeSigma);
    LOGI("  - image size: %ux%u", input.width, input.height);
    LOGI("  - pixelCount: %u", pixelCount);
    
    // 优先级 1: 检查是否应该使用 GPU 加速
    // 条件：GPU 启用 && 图像足够大 && GPU 可用
    // 修改：使用 >= 而不是 >
    LOGI("applyInternal: [Decision 1] Checking GPU acceleration eligibility...");
    
    if (config.enableGPU && pixelCount >= config.gpuThresholdPixels) {
        LOGI("  ✓ GPU threshold met: pixels=%u >= threshold=%u", 
             pixelCount, config.gpuThresholdPixels);
        
        // 尝试初始化 GPU（如果尚未初始化）
        if (!VulkanBilateralFilter::isAvailable()) {
            LOGI("  → GPU not initialized, attempting initialization...");
            VulkanBilateralFilter::initialize();
        }
        
        // 如果 GPU 可用，使用 GPU 加速
        if (VulkanBilateralFilter::isAvailable()) {
            LOGI("  ✓ GPU is available");
            LOGI("  → DECISION: Using GPU acceleration");
            
            bool success = VulkanBilateralFilter::apply(input, output, spatialSigma, rangeSigma);
            if (success) {
                usedGPU = true;
                LOGI("  ✓ GPU execution successful");
                LOGI("=======================================================");
                return;
            } else {
                LOGW("  ✗ GPU execution failed, falling back to CPU");
            }
        } else {
            LOGW("  ✗ GPU not available after initialization attempt");
            LOGW("  → Reason: GPU initialization failed or device not supported");
        }
    } else {
        if (!config.enableGPU) {
            LOGI("  ✗ GPU disabled in configuration");
        } else {
            LOGI("  ✗ GPU threshold not met: pixels=%u < threshold=%u", 
                 pixelCount, config.gpuThresholdPixels);
        }
        LOGI("  → Skipping GPU acceleration");
    }
    
    // 优先级 2: 检查是否应该使用快速近似算法
    // 条件：快速近似启用 && spatialSigma 足够大
    // 修改：使用 >= 而不是 >
    LOGI("applyInternal: [Decision 2] Checking fast approximation eligibility...");
    
    bool shouldUseFastApprox = config.enableFastApproximation && 
                               (spatialSigma >= config.fastApproxThreshold);
    
    if (shouldUseFastApprox) {
        LOGI("  ✓ Fast approximation threshold met: spatialSigma=%.2f >= threshold=%.2f", 
             spatialSigma, config.fastApproxThreshold);
        LOGI("  → DECISION: Using fast approximation algorithm");
        FastBilateralFilter::apply(input, output, spatialSigma, rangeSigma);
        usedFastApprox = true;
        LOGI("  ✓ Fast approximation execution successful");
        LOGI("=======================================================");
        return;
    } else {
        if (!config.enableFastApproximation) {
            LOGI("  ✗ Fast approximation disabled in configuration");
        } else {
            LOGI("  ✗ Fast approximation threshold not met: spatialSigma=%.2f < threshold=%.2f", 
                 spatialSigma, config.fastApproxThreshold);
        }
        LOGI("  → Skipping fast approximation");
    }
    
    // 优先级 3: 使用标准 CPU 实现
    LOGI("applyInternal: [Decision 3] Using standard CPU implementation");
    LOGI("  → Reason: No optimizations met their thresholds or were enabled");
    
    const uint32_t width = input.width;
    const uint32_t height = input.height;
    const int radius = static_cast<int>(std::ceil(3.0f * spatialSigma));
    
    LOGI("  - Filter radius: %d pixels", radius);
    LOGI("  - Processing with multi-threading...");
    
    // 确保输出图像大小正确
    if (output.width != width || output.height != height) {
        output = LinearImage(width, height);
    }
    
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t rowsPerThread = height / numThreads;
    
    LOGI("  - Using %u threads", numThreads);
    
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
    
    LOGI("  ✓ Standard CPU execution successful");
    LOGI("=======================================================");
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
    // 如果启用缓存，使用带缓存的版本
    if (s_config.enableCache) {
        applyWithCache(input, output, spatialSigma, rangeSigma, true);
        return;
    }
    
    auto startTime = std::chrono::high_resolution_clock::now();
    
    LOGI("apply: spatialSigma=%.2f, rangeSigma=%.2f", spatialSigma, rangeSigma);
    
    bool usedFastApprox = false;
    bool usedGPU = false;
    applyInternal(input, output, spatialSigma, rangeSigma, usedFastApprox, usedGPU, s_config);
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
    
    // 更新统计信息
    s_stats.totalCalls++;
    if (usedGPU) {
        s_stats.gpuCalls++;
    } else if (usedFastApprox) {
        s_stats.fastApproxCalls++;
    } else {
        s_stats.standardCalls++;
    }
    s_stats.avgProcessingTimeMs = (s_stats.avgProcessingTimeMs * (s_stats.totalCalls - 1) + duration.count()) / s_stats.totalCalls;
    
    LOGI("apply: Completed successfully in %lld ms (GPU=%d, fastApprox=%d)", 
         static_cast<long long>(duration.count()), usedGPU, usedFastApprox);
}

/**
 * 应用带缓存的双边滤波器
 */
void BilateralFilter::applyWithCache(const LinearImage& input,
                                    LinearImage& output,
                                    float spatialSigma,
                                    float rangeSigma,
                                    bool enableCache) {
    auto startTime = std::chrono::high_resolution_clock::now();
    
    s_stats.totalCalls++;
    
    if (enableCache && s_config.enableCache) {
        // 计算图像哈希
        uint64_t imageHash = ImageHashCache::computeImageHash(input);
        
        // 创建缓存键
        ImageHashCache::HashKey key{imageHash, spatialSigma, rangeSigma};
        
        // 查找缓存
        ImageHashCache& cache = ImageHashCache::getInstance();
        if (cache.find(key, output)) {
            // 缓存命中
            s_stats.cacheHits++;
            
            auto endTime = std::chrono::high_resolution_clock::now();
            auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
            
            LOGI("applyWithCache: Cache hit, completed in %lld ms", static_cast<long long>(duration.count()));
            return;
        }
        
        // 缓存未命中
        s_stats.cacheMisses++;
        
        // 执行双边滤波（使用内部实现）
        bool usedFastApprox = false;
        bool usedGPU = false;
        applyInternal(input, output, spatialSigma, rangeSigma, usedFastApprox, usedGPU, s_config);
        if (usedGPU) {
            s_stats.gpuCalls++;
        } else if (usedFastApprox) {
            s_stats.fastApproxCalls++;
        } else {
            s_stats.standardCalls++;
        }
        
        // 插入缓存
        cache.insert(key, output);
    } else {
        // 缓存禁用，直接执行
        bool usedFastApprox = false;
        bool usedGPU = false;
        applyInternal(input, output, spatialSigma, rangeSigma, usedFastApprox, usedGPU, s_config);
        if (usedGPU) {
            s_stats.gpuCalls++;
        } else if (usedFastApprox) {
            s_stats.fastApproxCalls++;
        } else {
            s_stats.standardCalls++;
        }
    }
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
    
    s_stats.avgProcessingTimeMs = (s_stats.avgProcessingTimeMs * (s_stats.totalCalls - 1) + duration.count()) / s_stats.totalCalls;
    
    LOGI("applyWithCache: Completed in %lld ms", static_cast<long long>(duration.count()));
}

/**
 * 应用快速双边滤波器
 * 
 * 使用快速近似算法（降采样 + 标准滤波 + 上采样）
 */
void BilateralFilter::applyFast(const LinearImage& input,
                               LinearImage& output,
                               float spatialSigma,
                               float rangeSigma) {
    LOGI("applyFast: spatialSigma=%.2f, rangeSigma=%.2f", spatialSigma, rangeSigma);
    
    auto startTime = std::chrono::high_resolution_clock::now();
    
    FastBilateralFilter::apply(input, output, spatialSigma, rangeSigma);
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime);
    
    // 更新统计信息
    s_stats.totalCalls++;
    s_stats.fastApproxCalls++;
    s_stats.avgProcessingTimeMs = (s_stats.avgProcessingTimeMs * (s_stats.totalCalls - 1) + duration.count()) / s_stats.totalCalls;
    
    LOGI("applyFast: Completed in %lld ms", static_cast<long long>(duration.count()));
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
    applyWithCache(input, base, spatialSigma, rangeSigma, s_config.enableCache);
    
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

/**
 * 配置管理
 */
void BilateralFilter::setConfig(const Config& config) {
    LOGI("========== BilateralFilter Configuration Update ==========");
    LOGI("setConfig: Updating configuration...");
    
    // 记录旧配置
    LOGI("setConfig: Previous configuration:");
    LOGI("  - enableCache: %d", s_config.enableCache);
    LOGI("  - enableFastApproximation: %d", s_config.enableFastApproximation);
    LOGI("  - enableGPU: %d", s_config.enableGPU);
    LOGI("  - maxCacheSize: %zu", s_config.maxCacheSize);
    LOGI("  - maxCacheMemoryMB: %zu", s_config.maxCacheMemoryMB);
    LOGI("  - fastApproxThreshold: %.2f", s_config.fastApproxThreshold);
    LOGI("  - gpuThresholdPixels: %u", s_config.gpuThresholdPixels);
    
    // 验证新配置
    Config validatedConfig = config;
    bool configModified = false;
    
    // 验证 fastApproxThreshold
    if (validatedConfig.fastApproxThreshold < 0.0f || validatedConfig.fastApproxThreshold > 100.0f) {
        LOGW("setConfig: Invalid fastApproxThreshold=%.2f, using default 4.5", 
             validatedConfig.fastApproxThreshold);
        validatedConfig.fastApproxThreshold = 4.5f;
        configModified = true;
    }
    
    // 验证 gpuThresholdPixels
    if (validatedConfig.gpuThresholdPixels < 100000 || validatedConfig.gpuThresholdPixels > 100000000) {
        LOGW("setConfig: Invalid gpuThresholdPixels=%u, using default 1500000", 
             validatedConfig.gpuThresholdPixels);
        validatedConfig.gpuThresholdPixels = 1500000;
        configModified = true;
    }
    
    // 验证缓存大小
    if (validatedConfig.maxCacheSize == 0) {
        LOGW("setConfig: maxCacheSize is 0, cache will be effectively disabled");
    }
    
    if (validatedConfig.maxCacheMemoryMB == 0) {
        LOGW("setConfig: maxCacheMemoryMB is 0, cache will be effectively disabled");
    }
    
    if (configModified) {
        LOGI("setConfig: Configuration was modified during validation");
    } else {
        LOGI("setConfig: Configuration validation passed");
    }
    
    // 应用新配置
    s_config = validatedConfig;
    
    // 更新缓存配置
    ImageHashCache& cache = ImageHashCache::getInstance();
    cache.setMaxSize(validatedConfig.maxCacheSize);
    cache.setMaxMemoryMB(validatedConfig.maxCacheMemoryMB);
    
    // 记录新配置
    LOGI("setConfig: New configuration applied:");
    LOGI("  - enableCache: %d", s_config.enableCache);
    LOGI("  - enableFastApproximation: %d", s_config.enableFastApproximation);
    LOGI("  - enableGPU: %d", s_config.enableGPU);
    LOGI("  - maxCacheSize: %zu", s_config.maxCacheSize);
    LOGI("  - maxCacheMemoryMB: %zu", s_config.maxCacheMemoryMB);
    LOGI("  - fastApproxThreshold: %.2f", s_config.fastApproxThreshold);
    LOGI("  - gpuThresholdPixels: %u", s_config.gpuThresholdPixels);
    
    // 记录配置变更摘要
    LOGI("setConfig: Configuration summary:");
    if (s_config.enableCache) {
        LOGI("  ✓ Caching ENABLED (max %zu entries, %zu MB)", 
             s_config.maxCacheSize, s_config.maxCacheMemoryMB);
    } else {
        LOGI("  ✗ Caching DISABLED");
    }
    
    if (s_config.enableFastApproximation) {
        LOGI("  ✓ Fast approximation ENABLED (threshold: spatialSigma >= %.2f)", 
             s_config.fastApproxThreshold);
    } else {
        LOGI("  ✗ Fast approximation DISABLED");
    }
    
    if (s_config.enableGPU) {
        LOGI("  ✓ GPU acceleration ENABLED (threshold: pixels >= %u)", 
             s_config.gpuThresholdPixels);
    } else {
        LOGI("  ✗ GPU acceleration DISABLED");
    }
    
    LOGI("===========================================================");
}

BilateralFilter::Config BilateralFilter::getConfig() {
    return s_config;
}

void BilateralFilter::initializeDefaultConfig() {
    Config defaultConfig;
    defaultConfig.enableCache = true;
    defaultConfig.enableFastApproximation = true;
    defaultConfig.enableGPU = true;
    defaultConfig.maxCacheSize = 100;
    defaultConfig.maxCacheMemoryMB = 512;
    defaultConfig.fastApproxThreshold = 4.5f;
    defaultConfig.gpuThresholdPixels = 1500000;
    
    setConfig(defaultConfig);
    
    LOGI("initializeDefaultConfig: Default configuration initialized");
    LOGI("  - enableCache: %d", defaultConfig.enableCache);
    LOGI("  - enableFastApproximation: %d", defaultConfig.enableFastApproximation);
    LOGI("  - enableGPU: %d", defaultConfig.enableGPU);
    LOGI("  - maxCacheSize: %zu", defaultConfig.maxCacheSize);
    LOGI("  - maxCacheMemoryMB: %zu", defaultConfig.maxCacheMemoryMB);
    LOGI("  - fastApproxThreshold: %.2f", defaultConfig.fastApproxThreshold);
    LOGI("  - gpuThresholdPixels: %u", defaultConfig.gpuThresholdPixels);
}

std::string BilateralFilter::getConfigString() {
    std::ostringstream oss;
    oss << "BilateralFilter Configuration:\n";
    oss << "  enableCache: " << (s_config.enableCache ? "true" : "false") << "\n";
    oss << "  enableFastApproximation: " << (s_config.enableFastApproximation ? "true" : "false") << "\n";
    oss << "  enableGPU: " << (s_config.enableGPU ? "true" : "false") << "\n";
    oss << "  maxCacheSize: " << s_config.maxCacheSize << "\n";
    oss << "  maxCacheMemoryMB: " << s_config.maxCacheMemoryMB << "\n";
    oss << "  fastApproxThreshold: " << s_config.fastApproxThreshold << "\n";
    oss << "  gpuThresholdPixels: " << s_config.gpuThresholdPixels << "\n";
    
    // Add statistics
    oss << "\nStatistics:\n";
    oss << "  totalCalls: " << s_stats.totalCalls << "\n";
    oss << "  standardCalls: " << s_stats.standardCalls << "\n";
    oss << "  fastApproxCalls: " << s_stats.fastApproxCalls << "\n";
    oss << "  gpuCalls: " << s_stats.gpuCalls << "\n";
    oss << "  cacheHits: " << s_stats.cacheHits << "\n";
    oss << "  cacheMisses: " << s_stats.cacheMisses << "\n";
    oss << "  avgProcessingTimeMs: " << s_stats.avgProcessingTimeMs << "\n";
    
    return oss.str();
}

/**
 * 性能统计
 */
BilateralFilter::Stats BilateralFilter::getStats() {
    return s_stats;
}

void BilateralFilter::resetStats() {
    s_stats = Stats();
    LOGI("resetStats: Statistics reset");
}

/**
 * 缓存管理
 */
void BilateralFilter::clearCache() {
    ImageHashCache& cache = ImageHashCache::getInstance();
    cache.clear();
    LOGI("clearCache: Cache cleared");
}

size_t BilateralFilter::getCacheSize() {
    ImageHashCache& cache = ImageHashCache::getInstance();
    return cache.size();
}

} // namespace filmtracker
