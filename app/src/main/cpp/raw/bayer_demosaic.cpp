/**
 * Bayer 去马赛克算法的实现文件
 * 
 * 注意：主要的去马赛克实现已经在 raw_processor.cpp 中完成
 * 本文件提供辅助函数和高级去马赛克算法的扩展实现
 * 
 * 当前实现：
 * - raw_processor.cpp 中的 demosaicBayer() 提供基础双线性插值
 * - raw_processor.cpp 中的 demosaicBayerNormalized() 提供归一化数据的去马赛克
 * - raw_processor.cpp 中的 demosaicAHD() 提供改进的边缘感知插值
 * 
 * 未来扩展：
 * - 完整的 AHD (Adaptive Homogeneity-Directed) 算法
 * - VNG (Variable Number of Gradients) 算法
 * - PPG (Patterned Pixel Grouping) 算法
 * - LMMSE (Linear Minimum Mean Square Error) 算法
 */

#include "raw_processor.h"
#include <cmath>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "BayerDemosaic"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

/**
 * 计算像素的梯度（用于边缘检测）
 * 
 * @param data RAW 数据
 * @param x, y 像素坐标
 * @param width, height 图像尺寸
 * @return 水平和垂直梯度的元组
 */
std::pair<float, float> calculateGradients(const std::vector<uint16_t>& data,
                                           uint32_t x, uint32_t y,
                                           uint32_t width, uint32_t height) {
    float gradH = 0.0f;
    float gradV = 0.0f;
    
    if (x > 0 && x < width - 1) {
        uint32_t idx = y * width + x;
        gradH = std::abs(static_cast<float>(data[idx - 1]) - static_cast<float>(data[idx + 1]));
    }
    
    if (y > 0 && y < height - 1) {
        uint32_t idx = y * width + x;
        gradV = std::abs(static_cast<float>(data[idx - width]) - static_cast<float>(data[idx + width]));
    }
    
    return {gradH, gradV};
}

/**
 * 边缘感知插值（用于改进去马赛克质量）
 * 
 * 根据局部梯度选择插值方向，减少伪影
 */
float edgeAwareInterpolation(const std::vector<uint16_t>& data,
                             uint32_t x, uint32_t y,
                             uint32_t width, uint32_t height,
                             bool horizontal) {
    uint32_t idx = y * width + x;
    
    if (horizontal) {
        if (x > 0 && x < width - 1) {
            return (static_cast<float>(data[idx - 1]) + static_cast<float>(data[idx + 1])) * 0.5f;
        }
    } else {
        if (y > 0 && y < height - 1) {
            return (static_cast<float>(data[idx - width]) + static_cast<float>(data[idx + width])) * 0.5f;
        }
    }
    
    return static_cast<float>(data[idx]);
}

/**
 * 绿色通道插值（Bayer 模式中绿色像素最多，质量最关键）
 * 
 * 使用更高质量的插值算法处理绿色通道
 */
float interpolateGreen(const std::vector<uint16_t>& data,
                      uint32_t x, uint32_t y,
                      uint32_t width, uint32_t height) {
    uint32_t idx = y * width + x;
    
    // 检查是否已经是绿色像素
    bool isRedRow = (y % 2 == 0);
    bool isRedCol = (x % 2 == 0);
    bool isGreen = (isRedRow && !isRedCol) || (!isRedRow && isRedCol);
    
    if (isGreen) {
        return static_cast<float>(data[idx]);
    }
    
    // 使用四个相邻绿色像素的加权平均
    float sum = 0.0f;
    int count = 0;
    
    if (x > 0) {
        sum += static_cast<float>(data[idx - 1]);
        count++;
    }
    if (x < width - 1) {
        sum += static_cast<float>(data[idx + 1]);
        count++;
    }
    if (y > 0) {
        sum += static_cast<float>(data[idx - width]);
        count++;
    }
    if (y < height - 1) {
        sum += static_cast<float>(data[idx + width]);
        count++;
    }
    
    return (count > 0) ? (sum / count) : static_cast<float>(data[idx]);
}

/**
 * 色度差值插值（利用色度平滑性）
 * 
 * 基于观察：色度（R-G, B-G）通常比绝对值更平滑
 */
void chromaDifferenceInterpolation(LinearImage& image,
                                   const std::vector<uint16_t>& rawData,
                                   uint32_t width, uint32_t height) {
    // 这是一个高级技术，用于减少色彩伪影
    // 当前实现为简化版本，完整实现需要更复杂的算法
    
    for (uint32_t y = 1; y < height - 1; ++y) {
        for (uint32_t x = 1; x < width - 1; ++x) {
            uint32_t idx = y * width + x;
            
            // 计算色度差值
            float rg = image.r[idx] - image.g[idx];
            float bg = image.b[idx] - image.g[idx];
            
            // 平滑色度差值（使用相邻像素）
            float rgSum = rg;
            float bgSum = bg;
            int count = 1;
            
            if (x > 0) {
                rgSum += (image.r[idx - 1] - image.g[idx - 1]);
                bgSum += (image.b[idx - 1] - image.g[idx - 1]);
                count++;
            }
            if (x < width - 1) {
                rgSum += (image.r[idx + 1] - image.g[idx + 1]);
                bgSum += (image.b[idx + 1] - image.g[idx + 1]);
                count++;
            }
            
            float avgRG = rgSum / count;
            float avgBG = bgSum / count;
            
            // 应用平滑后的色度差值
            image.r[idx] = image.g[idx] + avgRG;
            image.b[idx] = image.g[idx] + avgBG;
            
            // 限制范围
            image.r[idx] = std::max(0.0f, image.r[idx]);
            image.b[idx] = std::max(0.0f, image.b[idx]);
        }
    }
}

/**
 * 后处理：减少拉链效应（zipper artifacts）
 * 
 * 拉链效应是去马赛克中常见的伪影，表现为边缘处的锯齿状图案
 */
void reduceZipperArtifacts(LinearImage& image) {
    const uint32_t width = image.width;
    const uint32_t height = image.height;
    
    // 使用中值滤波减少拉链效应
    std::vector<float> tempR = image.r;
    std::vector<float> tempG = image.g;
    std::vector<float> tempB = image.b;
    
    for (uint32_t y = 1; y < height - 1; ++y) {
        for (uint32_t x = 1; x < width - 1; ++x) {
            uint32_t idx = y * width + x;
            
            // 收集 3x3 邻域
            std::vector<float> rVals, gVals, bVals;
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dx = -1; dx <= 1; ++dx) {
                    uint32_t nIdx = (y + dy) * width + (x + dx);
                    rVals.push_back(tempR[nIdx]);
                    gVals.push_back(tempG[nIdx]);
                    bVals.push_back(tempB[nIdx]);
                }
            }
            
            // 计算中值
            std::sort(rVals.begin(), rVals.end());
            std::sort(gVals.begin(), gVals.end());
            std::sort(bVals.begin(), bVals.end());
            
            // 使用加权平均（中值权重更高）
            float medianR = rVals[4];
            float medianG = gVals[4];
            float medianB = bVals[4];
            
            image.r[idx] = 0.7f * tempR[idx] + 0.3f * medianR;
            image.g[idx] = 0.7f * tempG[idx] + 0.3f * medianG;
            image.b[idx] = 0.7f * tempB[idx] + 0.3f * medianB;
        }
    }
}

/**
 * 辅助函数：检测 CFA 模式
 * 
 * @param rawData RAW 数据
 * @param width, height 图像尺寸
 * @return CFA 模式 (0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR)
 */
uint32_t detectCFAPattern(const std::vector<uint16_t>& rawData,
                         uint32_t width, uint32_t height) {
    // 简化实现：假设 RGGB（最常见）
    // 完整实现需要分析图像内容来检测实际的 CFA 模式
    LOGI("detectCFAPattern: Assuming RGGB pattern (most common)");
    return 0; // RGGB
}

} // namespace filmtracker
