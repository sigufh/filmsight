#include "image_converter.h"
#include "error_diffusion_dithering.h"
#include "dynamic_range_protection.h"
#include <cmath>
#include <algorithm>
#include <thread>
#include <vector>
#include <android/log.h>

#define LOG_TAG "ImageConverter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

/**
 * sRGB Gamma 函数
 * 仅在输出阶段应用，核心算法始终在线性域
 */
float ImageConverter::sRGBGamma(float linear) {
    if (linear <= 0.0031308f) {
        return 12.92f * linear;
    } else {
        return 1.055f * std::pow(linear, 1.0f / 2.4f) - 0.055f;
    }
}

/**
 * 线性到 sRGB 转换
 */
float ImageConverter::linearToSRGB(float linear) {
    float clamped = std::max(0.0f, std::min(1.0f, linear));
    return sRGBGamma(clamped);
}

/**
 * 将线性 RGB 转换为 sRGB 输出图像
 * 使用多线程优化大图像转换
 */
OutputImage ImageConverter::linearToSRGB(const LinearImage& linear) {
    LOGI("linearToSRGB: Starting, image size=%dx%d", linear.width, linear.height);
    
    OutputImage output(linear.width, linear.height);
    LOGI("linearToSRGB: Output image created, data size=%zu bytes", output.data.size());
    
    const uint32_t pixelCount = linear.width * linear.height;
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t pixelsPerThread = pixelCount / numThreads;
    
    LOGI("linearToSRGB: Using %u threads, %u pixels per thread", numThreads, pixelsPerThread);
    
    std::vector<std::thread> threads;
    for (uint32_t t = 0; t < numThreads; ++t) {
        uint32_t start = t * pixelsPerThread;
        uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
        
        threads.emplace_back([&linear, &output, start, end, t]() {
            LOGI("linearToSRGB: Thread %u processing pixels %u to %u", t, start, end);
            for (uint32_t i = start; i < end; ++i) {
        uint32_t idx = i * 4;
        output.data[idx + 0] = static_cast<uint8_t>(linearToSRGB(linear.r[i]) * 255.0f);
        output.data[idx + 1] = static_cast<uint8_t>(linearToSRGB(linear.g[i]) * 255.0f);
        output.data[idx + 2] = static_cast<uint8_t>(linearToSRGB(linear.b[i]) * 255.0f);
        output.data[idx + 3] = 255; // Alpha
            }
            LOGI("linearToSRGB: Thread %u completed", t);
        });
    }
    
    LOGI("linearToSRGB: Waiting for threads to complete");
    for (auto& thread : threads) {
        thread.join();
    }
    
    LOGI("linearToSRGB: All threads completed successfully");
    return output;
}

/**
 * 将线性 RGB 转换为 sRGB 输出图像，使用误差扩散抖动
 * 
 * 这个版本使用 Floyd-Steinberg 误差扩散算法来减少色彩断层。
 * 推荐用于最终输出，特别是在渐变区域较多的图像。
 */
OutputImage ImageConverter::linearToSRGBWithDithering(const LinearImage& linear) {
    LOGI("linearToSRGBWithDithering: Starting, image size=%dx%d", linear.width, linear.height);
    
    OutputImage output(linear.width, linear.height);
    
    // 创建误差扩散抖动器
    ErrorDiffusionDithering dithering;
    
    // 创建临时缓冲区用于 RGB 数据（不包含 alpha）
    std::vector<uint8_t> rgbBuffer(linear.width * linear.height * 3);
    
    // 应用 Floyd-Steinberg 抖动（包含 gamma 编码）
    dithering.applyFloydSteinberg(linear, rgbBuffer.data(), true);
    
    // 将 RGB 数据复制到 RGBA 输出
    const uint32_t pixelCount = linear.width * linear.height;
    for (uint32_t i = 0; i < pixelCount; ++i) {
        uint32_t rgbIdx = i * 3;
        uint32_t rgbaIdx = i * 4;
        
        output.data[rgbaIdx + 0] = rgbBuffer[rgbIdx + 0];  // R
        output.data[rgbaIdx + 1] = rgbBuffer[rgbIdx + 1];  // G
        output.data[rgbaIdx + 2] = rgbBuffer[rgbIdx + 2];  // B
        output.data[rgbaIdx + 3] = 255;                     // A
    }
    
    LOGI("linearToSRGBWithDithering: Completed successfully");
    return output;
}

/**
 * 将线性 RGB 转换为 sRGB 输出图像，使用软裁剪和抖动
 * 
 * 这是推荐的最终输出方法，包含：
 * 1. 软裁剪：保护高光和阴影细节
 * 2. Gamma 编码：转换到 sRGB 空间
 * 3. 误差扩散抖动：消除色彩断层
 * 
 * @param linear 线性空间的图像数据
 * @param applySoftClip 是否应用软裁剪（默认 true）
 * @return sRGB 空间的 8-bit 输出图像
 */
OutputImage ImageConverter::linearToSRGBWithSoftClipAndDithering(
    const LinearImage& linear, 
    bool applySoftClip) {
    
    LOGI("linearToSRGBWithSoftClipAndDithering: Starting, image size=%dx%d, softClip=%d", 
         linear.width, linear.height, applySoftClip);
    
    // 创建临时图像用于软裁剪
    LinearImage processed = linear;
    
    // 应用软裁剪（如果启用）
    if (applySoftClip) {
        const uint32_t pixelCount = linear.width * linear.height;
        const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
        const uint32_t pixelsPerThread = pixelCount / numThreads;
        
        std::vector<std::thread> threads;
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t start = t * pixelsPerThread;
            uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
            
            threads.emplace_back([&processed, start, end]() {
                for (uint32_t i = start; i < end; ++i) {
                    // 应用软裁剪到每个通道
                    // 使用默认参数：threshold=0.8, knee=0.15, limit=1.0
                    processed.r[i] = DynamicRangeProtection::softClip(processed.r[i]);
                    processed.g[i] = DynamicRangeProtection::softClip(processed.g[i]);
                    processed.b[i] = DynamicRangeProtection::softClip(processed.b[i]);
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
        
        LOGI("linearToSRGBWithSoftClipAndDithering: Soft clipping completed");
    }
    
    // 应用 Gamma 编码和抖动
    OutputImage output(processed.width, processed.height);
    
    // 创建误差扩散抖动器
    ErrorDiffusionDithering dithering;
    
    // 创建临时缓冲区用于 RGB 数据
    std::vector<uint8_t> rgbBuffer(processed.width * processed.height * 3);
    
    // 应用 Floyd-Steinberg 抖动（包含 gamma 编码）
    dithering.applyFloydSteinberg(processed, rgbBuffer.data(), true);
    
    // 将 RGB 数据复制到 RGBA 输出
    const uint32_t pixelCount = processed.width * processed.height;
    for (uint32_t i = 0; i < pixelCount; ++i) {
        uint32_t rgbIdx = i * 3;
        uint32_t rgbaIdx = i * 4;
        
        output.data[rgbaIdx + 0] = rgbBuffer[rgbIdx + 0];  // R
        output.data[rgbaIdx + 1] = rgbBuffer[rgbIdx + 1];  // G
        output.data[rgbaIdx + 2] = rgbBuffer[rgbIdx + 2];  // B
        output.data[rgbaIdx + 3] = 255;                     // A
    }
    
    LOGI("linearToSRGBWithSoftClipAndDithering: Completed successfully");
    return output;
}

/**
 * 色调映射（用于高动态范围场景）
 */
void ImageConverter::applyToneMapping(LinearImage& image, float exposure) {
    const uint32_t pixelCount = image.width * image.height;
    float exposureMultiplier = std::pow(2.0f, exposure);
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        image.r[i] *= exposureMultiplier;
        image.g[i] *= exposureMultiplier;
        image.b[i] *= exposureMultiplier;
        
        // Reinhard 色调映射
        image.r[i] = image.r[i] / (1.0f + image.r[i]);
        image.g[i] = image.g[i] / (1.0f + image.g[i]);
        image.b[i] = image.b[i] / (1.0f + image.b[i]);
    }
}

/**
 * sRGB 到线性域的反 Gamma 函数
 */
float ImageConverter::sRGBToLinear(float srgb) {
    if (srgb <= 0.04045f) {
        return srgb / 12.92f;
    } else {
        return std::pow((srgb + 0.055f) / 1.055f, 2.4f);
    }
}

/**
 * 将 sRGB Bitmap 转换为线性域图像
 */
LinearImage ImageConverter::sRGBToLinear(const uint8_t* rgbaData, uint32_t width, uint32_t height) {
    LinearImage linear(width, height);
    const uint32_t pixelCount = width * height;
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        uint32_t idx = i * 4;
        float r = rgbaData[idx + 0] / 255.0f;
        float g = rgbaData[idx + 1] / 255.0f;
        float b = rgbaData[idx + 2] / 255.0f;
        
        linear.r[i] = sRGBToLinear(r);
        linear.g[i] = sRGBToLinear(g);
        linear.b[i] = sRGBToLinear(b);
    }
    
    return linear;
}

} // namespace filmtracker
