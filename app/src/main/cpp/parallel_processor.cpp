#include "include/parallel_processor.h"
#include "contrast_adjustment.h"
#include "include/exposure_adjustment.h"
#include "include/saturation_adjustment.h"
#include "include/grain_effect.h"
#include "include/vignette_effect.h"
#include <thread>
#include <vector>
#include <cmath>
#include <algorithm>

#ifdef __ARM_NEON
#include <arm_neon.h>
#endif

#include <android/log.h>
#define LOG_TAG "ParallelProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace filmtracker;

ParallelProcessor::ParallelProcessor() {
    // 获取 CPU 核心数
    numThreads = std::thread::hardware_concurrency();
    
    // 限制线程数在合理范围内
    if (numThreads < 2) numThreads = 2;
    if (numThreads > 8) numThreads = 8;
    
    LOGI("ParallelProcessor initialized with %d threads", numThreads);
}

void ParallelProcessor::process(
    const LinearImage& input,
    LinearImage& output,
    const BasicAdjustmentParams& params
) {
    int height = input.height;
    int rowsPerThread = height / numThreads;
    
    std::vector<std::thread> threads;
    threads.reserve(numThreads);
    
    // 创建工作线程
    for (int i = 0; i < numThreads; i++) {
        int startRow = i * rowsPerThread;
        int endRow = (i == numThreads - 1) ? height : (i + 1) * rowsPerThread;
        
        threads.emplace_back([this, &input, &output, &params, startRow, endRow]() {
            processBlock(input, output, params, startRow, endRow);
        });
    }
    
    // 等待所有线程完成
    for (auto& thread : threads) {
        thread.join();
    }
}

void ParallelProcessor::processBlock(
    const LinearImage& input,
    LinearImage& output,
    const BasicAdjustmentParams& params,
    int startRow,
    int endRow
) {
    for (int y = startRow; y < endRow; y++) {
#ifdef __ARM_NEON
        processRowWithSIMD(input, output, params, y);
#else
        // 如果不支持 NEON，使用标量处理
        for (int x = 0; x < input.width; x++) {
            processPixelScalar(input, output, params, x, y);
        }
#endif
    }
}

#ifdef __ARM_NEON
void ParallelProcessor::processRowWithSIMD(
    const LinearImage& input,
    LinearImage& output,
    const BasicAdjustmentParams& params,
    int y
) {
    int width = input.width;
    
    // 对于复杂的调整算法，使用标量处理以保证质量
    // SIMD 优化会在后续版本中针对新算法进行优化
    for (int x = 0; x < width; x++) {
        processPixelScalar(input, output, params, x, y);
    }
}
#else
void ParallelProcessor::processRowWithSIMD(
    const LinearImage& input,
    LinearImage& output,
    const BasicAdjustmentParams& params,
    int y
) {
    // 如果不支持 NEON，这个函数不会被调用
    // 但为了编译通过，提供一个空实现
}
#endif

void ParallelProcessor::processPixelScalar(
    const LinearImage& input,
    LinearImage& output,
    const BasicAdjustmentParams& params,
    int x,
    int y
) {
    int idx = y * input.width + x;
    
    // 读取像素
    float r = input.r[idx];
    float g = input.g[idx];
    float b = input.b[idx];
    
    // 1. 应用曝光（使用改进的算法，带高光保护）
    if (std::abs(params.globalExposure) > 0.01f) {
        ExposureAdjustment::applyExposure(r, g, b, params.globalExposure);
    }
    
    // 2. 应用对比度（已转换为乘数：0.5 到 2.0）
    if (std::abs(params.contrast - 1.0f) > 0.001f) {
        ContrastAdjustment::applyContrast(r, g, b, params.contrast);
    }
    
    // 3. 应用饱和度（已转换为乘数：0.0 到 2.0）
    if (std::abs(params.saturation - 1.0f) > 0.001f) {
        SaturationAdjustment::applySaturation(r, g, b, params.saturation);
    }
    
    // 4. 应用色温和色调（Adobe 标准：-100 到 +100）
    if (std::abs(params.temperature) > 0.01f || std::abs(params.tint) > 0.01f) {
        // 色温调整：影响 R 和 B 通道
        float tempFactor = params.temperature / 100.0f;
        r *= (1.0f + tempFactor * 0.3f);
        b *= (1.0f - tempFactor * 0.3f);
        
        // 色调调整：影响 G 通道
        float tintFactor = params.tint / 100.0f;
        g *= (1.0f + tintFactor * 0.2f);
    }
    
    // 5. 应用暗角效果
    if (std::abs(params.vignette) > 0.01f) {
        // vignette 范围：-100 到 +100，转换为 -1.0 到 1.0
        float vignetteAmount = params.vignette / 100.0f;
        VignetteEffect::applyVignette(r, g, b, vignetteAmount, x, y, 
                                     input.width, input.height);
    }
    
    // 6. 应用颗粒效果
    if (params.grain > 0.01f) {
        // grain 范围：0 到 100，转换为 0.0 到 1.0
        float grainAmount = params.grain / 100.0f;
        GrainEffect::applyGrain(r, g, b, grainAmount, x, y);
    }
    
    // Clamp 到 [0, ∞)（保留动态范围，只限制下界）
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
    
    // 写入输出
    output.r[idx] = r;
    output.g[idx] = g;
    output.b[idx] = b;
}
