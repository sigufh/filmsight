#include "error_diffusion_dithering.h"
#include <algorithm>
#include <cmath>
#include <vector>
#include <android/log.h>

#define LOG_TAG "ErrorDiffusionDithering"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

ErrorDiffusionDithering::ErrorDiffusionDithering() {
    LOGI("ErrorDiffusionDithering created");
}

ErrorDiffusionDithering::~ErrorDiffusionDithering() {
    LOGI("ErrorDiffusionDithering destroyed");
}

void ErrorDiffusionDithering::applyFloydSteinberg(const LinearImage& image,
                                                  uint8_t* output,
                                                  bool applyGamma) {
    if (!output) {
        LOGE("Output buffer is null");
        return;
    }
    
    const uint32_t width = image.width;
    const uint32_t height = image.height;
    
    LOGI("Applying Floyd-Steinberg dithering: %dx%d, gamma=%d", width, height, applyGamma);
    
    // 为每个颜色通道创建误差缓冲区（当前行 + 下一行）
    // 我们需要两行来存储误差：当前行和下一行
    std::vector<float> errorBufferR(width * 2, 0.0f);
    std::vector<float> errorBufferG(width * 2, 0.0f);
    std::vector<float> errorBufferB(width * 2, 0.0f);
    
    // 当前行和下一行的指针
    float* currentRowR = errorBufferR.data();
    float* nextRowR = errorBufferR.data() + width;
    float* currentRowG = errorBufferG.data();
    float* nextRowG = errorBufferG.data() + width;
    float* currentRowB = errorBufferB.data();
    float* nextRowB = errorBufferB.data() + width;
    
    // 逐行处理
    for (uint32_t y = 0; y < height; ++y) {
        // 清空下一行的误差缓冲区
        std::fill(nextRowR, nextRowR + width, 0.0f);
        std::fill(nextRowG, nextRowG + width, 0.0f);
        std::fill(nextRowB, nextRowB + width, 0.0f);
        
        // 逐像素处理
        for (uint32_t x = 0; x < width; ++x) {
            const uint32_t pixelIdx = y * width + x;
            
            // 获取原始像素值（线性空间，0.0-1.0）
            float r = image.r[pixelIdx];
            float g = image.g[pixelIdx];
            float b = image.b[pixelIdx];
            
            // 裁剪到 [0, 1] 范围
            r = std::max(0.0f, std::min(1.0f, r));
            g = std::max(0.0f, std::min(1.0f, g));
            b = std::max(0.0f, std::min(1.0f, b));
            
            // 应用 gamma 编码（如果需要）
            if (applyGamma) {
                r = applyGammaEncoding(r);
                g = applyGammaEncoding(g);
                b = applyGammaEncoding(b);
            }
            
            // 加上累积的误差
            r += currentRowR[x];
            g += currentRowG[x];
            b += currentRowB[x];
            
            // 再次裁剪（加上误差后可能超出范围）
            r = std::max(0.0f, std::min(1.0f, r));
            g = std::max(0.0f, std::min(1.0f, g));
            b = std::max(0.0f, std::min(1.0f, b));
            
            // 量化到 8-bit
            int quantizedR = quantize(r, 255);
            int quantizedG = quantize(g, 255);
            int quantizedB = quantize(b, 255);
            
            // 计算量化误差
            float errorR = calculateError(r, quantizedR, 255);
            float errorG = calculateError(g, quantizedG, 255);
            float errorB = calculateError(b, quantizedB, 255);
            
            // 写入输出（RGB 交错格式）
            const uint32_t outputIdx = pixelIdx * 3;
            output[outputIdx + 0] = static_cast<uint8_t>(quantizedR);
            output[outputIdx + 1] = static_cast<uint8_t>(quantizedG);
            output[outputIdx + 2] = static_cast<uint8_t>(quantizedB);
            
            // 分配误差到相邻像素
            // Floyd-Steinberg 误差分配模式：
            //        X   7/16
            //    3/16 5/16 1/16
            
            if (x + 1 < width) {
                // 右边像素（当前行）：7/16
                currentRowR[x + 1] += errorR * (7.0f / 16.0f);
                currentRowG[x + 1] += errorG * (7.0f / 16.0f);
                currentRowB[x + 1] += errorB * (7.0f / 16.0f);
            }
            
            if (y + 1 < height) {
                // 下一行的像素
                if (x > 0) {
                    // 左下像素：3/16
                    nextRowR[x - 1] += errorR * (3.0f / 16.0f);
                    nextRowG[x - 1] += errorG * (3.0f / 16.0f);
                    nextRowB[x - 1] += errorB * (3.0f / 16.0f);
                }
                
                // 正下像素：5/16
                nextRowR[x] += errorR * (5.0f / 16.0f);
                nextRowG[x] += errorG * (5.0f / 16.0f);
                nextRowB[x] += errorB * (5.0f / 16.0f);
                
                if (x + 1 < width) {
                    // 右下像素：1/16
                    nextRowR[x + 1] += errorR * (1.0f / 16.0f);
                    nextRowG[x + 1] += errorG * (1.0f / 16.0f);
                    nextRowB[x + 1] += errorB * (1.0f / 16.0f);
                }
            }
        }
        
        // 交换当前行和下一行的指针
        std::swap(currentRowR, nextRowR);
        std::swap(currentRowG, nextRowG);
        std::swap(currentRowB, nextRowB);
    }
    
    LOGI("Floyd-Steinberg dithering completed");
}

void ErrorDiffusionDithering::applyFloydSteinbergInPlace(LinearImage& image, int bitDepth) {
    const uint32_t width = image.width;
    const uint32_t height = image.height;
    const int maxValue = (1 << bitDepth) - 1;  // 例如 8-bit: 255
    
    LOGI("Applying Floyd-Steinberg in-place: %dx%d, bitDepth=%d", width, height, bitDepth);
    
    // 为每个颜色通道创建误差缓冲区
    std::vector<float> errorBufferR(width * 2, 0.0f);
    std::vector<float> errorBufferG(width * 2, 0.0f);
    std::vector<float> errorBufferB(width * 2, 0.0f);
    
    float* currentRowR = errorBufferR.data();
    float* nextRowR = errorBufferR.data() + width;
    float* currentRowG = errorBufferG.data();
    float* nextRowG = errorBufferG.data() + width;
    float* currentRowB = errorBufferB.data();
    float* nextRowB = errorBufferB.data() + width;
    
    for (uint32_t y = 0; y < height; ++y) {
        std::fill(nextRowR, nextRowR + width, 0.0f);
        std::fill(nextRowG, nextRowG + width, 0.0f);
        std::fill(nextRowB, nextRowB + width, 0.0f);
        
        for (uint32_t x = 0; x < width; ++x) {
            const uint32_t pixelIdx = y * width + x;
            
            float r = image.r[pixelIdx];
            float g = image.g[pixelIdx];
            float b = image.b[pixelIdx];
            
            // 裁剪到 [0, 1]
            r = std::max(0.0f, std::min(1.0f, r));
            g = std::max(0.0f, std::min(1.0f, g));
            b = std::max(0.0f, std::min(1.0f, b));
            
            // 加上累积误差
            r += currentRowR[x];
            g += currentRowG[x];
            b += currentRowB[x];
            
            // 再次裁剪
            r = std::max(0.0f, std::min(1.0f, r));
            g = std::max(0.0f, std::min(1.0f, g));
            b = std::max(0.0f, std::min(1.0f, b));
            
            // 量化
            int quantizedR = quantize(r, maxValue);
            int quantizedG = quantize(g, maxValue);
            int quantizedB = quantize(b, maxValue);
            
            // 反量化回浮点
            float dequantizedR = quantizedR / static_cast<float>(maxValue);
            float dequantizedG = quantizedG / static_cast<float>(maxValue);
            float dequantizedB = quantizedB / static_cast<float>(maxValue);
            
            // 计算误差
            float errorR = r - dequantizedR;
            float errorG = g - dequantizedG;
            float errorB = b - dequantizedB;
            
            // 写回图像（反量化后的值）
            image.r[pixelIdx] = dequantizedR;
            image.g[pixelIdx] = dequantizedG;
            image.b[pixelIdx] = dequantizedB;
            
            // 分配误差
            if (x + 1 < width) {
                currentRowR[x + 1] += errorR * (7.0f / 16.0f);
                currentRowG[x + 1] += errorG * (7.0f / 16.0f);
                currentRowB[x + 1] += errorB * (7.0f / 16.0f);
            }
            
            if (y + 1 < height) {
                if (x > 0) {
                    nextRowR[x - 1] += errorR * (3.0f / 16.0f);
                    nextRowG[x - 1] += errorG * (3.0f / 16.0f);
                    nextRowB[x - 1] += errorB * (3.0f / 16.0f);
                }
                
                nextRowR[x] += errorR * (5.0f / 16.0f);
                nextRowG[x] += errorG * (5.0f / 16.0f);
                nextRowB[x] += errorB * (5.0f / 16.0f);
                
                if (x + 1 < width) {
                    nextRowR[x + 1] += errorR * (1.0f / 16.0f);
                    nextRowG[x + 1] += errorG * (1.0f / 16.0f);
                    nextRowB[x + 1] += errorB * (1.0f / 16.0f);
                }
            }
        }
        
        std::swap(currentRowR, nextRowR);
        std::swap(currentRowG, nextRowG);
        std::swap(currentRowB, nextRowB);
    }
    
    LOGI("Floyd-Steinberg in-place dithering completed");
}

float ErrorDiffusionDithering::applyGammaEncoding(float linear) const {
    // sRGB gamma 编码
    // 参考：https://en.wikipedia.org/wiki/SRGB
    if (linear <= 0.0031308f) {
        return 12.92f * linear;
    } else {
        return 1.055f * std::pow(linear, 1.0f / 2.4f) - 0.055f;
    }
}

int ErrorDiffusionDithering::quantize(float value, int maxValue) const {
    // 四舍五入到最近的整数
    int quantized = static_cast<int>(value * maxValue + 0.5f);
    return std::max(0, std::min(maxValue, quantized));
}

float ErrorDiffusionDithering::calculateError(float original, int quantized, int maxValue) const {
    // 计算量化误差（浮点空间）
    float dequantized = quantized / static_cast<float>(maxValue);
    return original - dequantized;
}

void ErrorDiffusionDithering::distributeError(float* errorBuffer,
                                             uint32_t width,
                                             uint32_t x,
                                             float error) const {
    // Floyd-Steinberg 误差分配
    // 这个函数目前没有被使用，因为误差分配已经内联到主循环中
    // 保留它以便将来可能的优化或其他抖动算法
    
    if (x + 1 < width) {
        errorBuffer[x + 1] += error * (7.0f / 16.0f);
    }
}

} // namespace filmtracker
