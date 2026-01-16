#include "include/parallel_processor.h"
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
    int x = 0;
    
    // 准备 NEON 向量
    float32x4_t exposure_vec = vdupq_n_f32(std::pow(2.0f, params.globalExposure));
    float32x4_t contrast_vec = vdupq_n_f32(params.contrast);
    float32x4_t half_vec = vdupq_n_f32(0.5f);
    float32x4_t zero_vec = vdupq_n_f32(0.0f);
    
    // NEON 向量化处理（一次处理 4 个像素）
    for (; x + 3 < width; x += 4) {
        int idx = y * width + x;
        
        // 加载 4 个像素的 RGB 数据（分离通道）
        float32x4_t r = vld1q_f32(&input.r[idx]);
        float32x4_t g = vld1q_f32(&input.g[idx]);
        float32x4_t b = vld1q_f32(&input.b[idx]);
        
        // 应用曝光：color *= pow(2, exposure)
        r = vmulq_f32(r, exposure_vec);
        g = vmulq_f32(g, exposure_vec);
        b = vmulq_f32(b, exposure_vec);
        
        // 应用对比度：color = (color - 0.5) * contrast + 0.5
        r = vsubq_f32(r, half_vec);
        r = vmulq_f32(r, contrast_vec);
        r = vaddq_f32(r, half_vec);
        
        g = vsubq_f32(g, half_vec);
        g = vmulq_f32(g, contrast_vec);
        g = vaddq_f32(g, half_vec);
        
        b = vsubq_f32(b, half_vec);
        b = vmulq_f32(b, contrast_vec);
        b = vaddq_f32(b, half_vec);
        
        // 应用饱和度（如果需要）
        if (std::abs(params.saturation - 1.0f) > 0.01f) {
            float32x4_t saturation_vec = vdupq_n_f32(params.saturation);
            
            // 计算亮度：0.2126*R + 0.7152*G + 0.0722*B
            float32x4_t lum_r = vmulq_n_f32(r, 0.2126f);
            float32x4_t lum_g = vmulq_n_f32(g, 0.7152f);
            float32x4_t lum_b = vmulq_n_f32(b, 0.0722f);
            float32x4_t luminance = vaddq_f32(vaddq_f32(lum_r, lum_g), lum_b);
            
            // color = luminance + (color - luminance) * saturation
            r = vaddq_f32(luminance, vmulq_f32(vsubq_f32(r, luminance), saturation_vec));
            g = vaddq_f32(luminance, vmulq_f32(vsubq_f32(g, luminance), saturation_vec));
            b = vaddq_f32(luminance, vmulq_f32(vsubq_f32(b, luminance), saturation_vec));
        }
        
        // 应用色温和色调（简化版本）
        if (std::abs(params.temperature) > 0.01f || std::abs(params.tint) > 0.01f) {
            float tempFactor = 1.0f + (params.temperature / 100.0f) * 0.3f;
            float tempFactorB = 1.0f - (params.temperature / 100.0f) * 0.3f;
            float tintFactor = 1.0f + (params.tint / 100.0f) * 0.2f;
            
            r = vmulq_n_f32(r, tempFactor);
            g = vmulq_n_f32(g, tintFactor);
            b = vmulq_n_f32(b, tempFactorB);
        }
        
        // Clamp 到 [0, ∞)（保留动态范围，只限制下界）
        r = vmaxq_f32(r, zero_vec);
        g = vmaxq_f32(g, zero_vec);
        b = vmaxq_f32(b, zero_vec);
        
        // 存储结果（分离通道）
        vst1q_f32(&output.r[idx], r);
        vst1q_f32(&output.g[idx], g);
        vst1q_f32(&output.b[idx], b);
    }
    
    // 处理剩余像素（标量处理）
    for (; x < width; x++) {
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
    
    // 1. 应用曝光
    float exposureFactor = std::pow(2.0f, params.globalExposure);
    r *= exposureFactor;
    g *= exposureFactor;
    b *= exposureFactor;
    
    // 2. 应用对比度
    r = (r - 0.5f) * params.contrast + 0.5f;
    g = (g - 0.5f) * params.contrast + 0.5f;
    b = (b - 0.5f) * params.contrast + 0.5f;
    
    // 3. 应用饱和度
    if (std::abs(params.saturation - 1.0f) > 0.01f) {
        float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
        r = luminance + (r - luminance) * params.saturation;
        g = luminance + (g - luminance) * params.saturation;
        b = luminance + (b - luminance) * params.saturation;
    }
    
    // 4. 应用色温和色调（简化版本）
    if (std::abs(params.temperature) > 0.01f || std::abs(params.tint) > 0.01f) {
        // 色温调整：影响 R 和 B 通道
        float tempFactor = params.temperature / 100.0f;
        r *= (1.0f + tempFactor * 0.3f);
        b *= (1.0f - tempFactor * 0.3f);
        
        // 色调调整：影响 G 通道
        float tintFactor = params.tint / 100.0f;
        g *= (1.0f + tintFactor * 0.2f);
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
