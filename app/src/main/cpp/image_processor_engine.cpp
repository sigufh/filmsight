#include "image_processor_engine.h"
#include "adobe_tone_adjustment.h"
#include "contrast_adjustment.h"
#include "color_temperature.h"
#include "color_grading.h"
#include "bilateral_filter.h"
#include <algorithm>
#include <cmath>
#include <thread>
#include <vector>
#include <android/log.h>

#define LOG_TAG "ImageProcessorEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

ImageProcessorEngine::ImageProcessorEngine() {
    LOGI("ImageProcessorEngine created");
}

ImageProcessorEngine::~ImageProcessorEngine() {
    LOGI("ImageProcessorEngine destroyed");
}

// ========== 基础调整模块 ==========

void ImageProcessorEngine::applyBasicAdjustments(LinearImage& image,
                                                 float exposure,
                                                 float contrast,
                                                 float saturation) {
    LOGI("applyBasicAdjustments: exposure=%.2f, contrast=%.2f, saturation=%.2f", 
         exposure, contrast, saturation);
    
    const uint32_t pixelCount = image.width * image.height;
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t pixelsPerThread = pixelCount / numThreads;
    
    // 计算曝光因子
    const float exposureFactor = std::pow(2.0f, exposure);
    
    std::vector<std::thread> threads;
    for (uint32_t t = 0; t < numThreads; ++t) {
        uint32_t start = t * pixelsPerThread;
        uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
        
        threads.emplace_back([&image, start, end, exposureFactor, contrast, saturation]() {
            for (uint32_t i = start; i < end; ++i) {
                float r = image.r[i];
                float g = image.g[i];
                float b = image.b[i];
                
                // 1. 曝光调整（在线性空间）
                r *= exposureFactor;
                g *= exposureFactor;
                b *= exposureFactor;
                
                // 2. 对比度调整（使用 S 曲线）
                if (std::abs(contrast - 1.0f) > 0.01f) {
                    ContrastAdjustment::applyContrast(r, g, b, contrast);
                }
                
                // 3. 饱和度调整
                if (std::abs(saturation - 1.0f) > 0.01f) {
                    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
                    r = luminance + (r - luminance) * saturation;
                    g = luminance + (g - luminance) * saturation;
                    b = luminance + (b - luminance) * saturation;
                }
                
                // 限制范围（允许超出 [0,1]，保留动态范围）
                image.r[i] = std::max(0.0f, r);
                image.g[i] = std::max(0.0f, g);
                image.b[i] = std::max(0.0f, b);
            }
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
    
    LOGI("applyBasicAdjustments completed");
}

void ImageProcessorEngine::applyToneAdjustments(LinearImage& image,
                                               float highlights,
                                               float shadows,
                                               float whites,
                                               float blacks) {
    LOGI("applyToneAdjustments: highlights=%.2f, shadows=%.2f, whites=%.2f, blacks=%.2f",
         highlights, shadows, whites, blacks);
    
    // 如果所有参数都是 0，直接返回
    if (std::abs(highlights) < 0.01f && std::abs(shadows) < 0.01f &&
        std::abs(whites) < 0.01f && std::abs(blacks) < 0.01f) {
        return;
    }
    
    const uint32_t pixelCount = image.width * image.height;
    const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
    const uint32_t pixelsPerThread = pixelCount / numThreads;
    
    std::vector<std::thread> threads;
    for (uint32_t t = 0; t < numThreads; ++t) {
        uint32_t start = t * pixelsPerThread;
        uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
        
        threads.emplace_back([&image, start, end, highlights, shadows, whites, blacks]() {
            for (uint32_t i = start; i < end; ++i) {
                float r = image.r[i];
                float g = image.g[i];
                float b = image.b[i];
                
                // 使用 Adobe 标准算法应用色调调整
                AdobeToneAdjustment::applyToneAdjustments(r, g, b, highlights, shadows, whites, blacks);
                
                // 保留动态范围，只限制下界
                image.r[i] = std::max(0.0f, r);
                image.g[i] = std::max(0.0f, g);
                image.b[i] = std::max(0.0f, b);
            }
        });
    }
    
    for (auto& thread : threads) {
        thread.join();
    }
    
    LOGI("applyToneAdjustments completed");
}

void ImageProcessorEngine::applyPresence(LinearImage& image,
                                        float clarity,
                                        float vibrance) {
    LOGI("applyPresence: clarity=%.2f, vibrance=%.2f", clarity, vibrance);
    
    // 如果所有参数都是 0，直接返回
    if (std::abs(clarity) < 0.01f && std::abs(vibrance) < 0.01f) {
        return;
    }
    
    const uint32_t pixelCount = image.width * image.height;
    
    // 1. 清晰度调整（使用双边滤波器）
    if (std::abs(clarity) > 0.01f) {
        LOGI("applyPresence: Applying clarity adjustment");
        
        // 归一化清晰度参数（-100 到 +100 -> -1.0 到 +1.0）
        float clarityAmount = clarity / 100.0f;
        
        // 双边滤波器参数
        // spatialSigma: 控制滤波器大小（像素）
        // rangeSigma: 控制边缘保持程度（0.0-1.0）
        float spatialSigma = 5.0f;  // 中等尺度
        float rangeSigma = 0.2f;    // 较强的边缘保持
        
        // 提取细节层
        LinearImage detail(image.width, image.height);
        BilateralFilter::extractDetail(image, detail, spatialSigma, rangeSigma);
        
        // 应用清晰度调整
        // clarity > 0: 增强细节
        // clarity < 0: 柔化图像
        const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
        const uint32_t pixelsPerThread = pixelCount / numThreads;
        
        std::vector<std::thread> threads;
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t start = t * pixelsPerThread;
            uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
            
            threads.emplace_back([&image, &detail, clarityAmount, start, end]() {
                for (uint32_t i = start; i < end; ++i) {
                    // 计算亮度（用于保护高光和阴影）
                    float luminance = 0.2126f * image.r[i] + 0.7152f * image.g[i] + 0.0722f * image.b[i];
                    
                    // 保护高光和阴影区域
                    // 在高光（> 0.8）和阴影（< 0.2）区域减少清晰度效果
                    float protection = 1.0f;
                    if (luminance > 0.8f) {
                        protection = 1.0f - (luminance - 0.8f) / 0.2f;  // 0.8-1.0 -> 1.0-0.0
                    } else if (luminance < 0.2f) {
                        protection = luminance / 0.2f;  // 0.0-0.2 -> 0.0-1.0
                    }
                    protection = std::max(0.2f, protection);  // 至少保留 20% 效果
                    
                    // 应用清晰度调整
                    float amount = clarityAmount * protection;
                    image.r[i] = image.r[i] + detail.r[i] * amount;
                    image.g[i] = image.g[i] + detail.g[i] * amount;
                    image.b[i] = image.b[i] + detail.b[i] * amount;
                    
                    // 限制范围（允许超出 [0,1]，保留动态范围）
                    image.r[i] = std::max(0.0f, image.r[i]);
                    image.g[i] = std::max(0.0f, image.g[i]);
                    image.b[i] = std::max(0.0f, image.b[i]);
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
        
        LOGI("applyPresence: Clarity adjustment completed");
    }
    
    // 2. 自然饱和度调整
    if (std::abs(vibrance) > 0.01f) {
        LOGI("applyPresence: Applying vibrance adjustment");
        
        // 归一化参数
        vibrance /= 100.0f;
        
        const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
        const uint32_t pixelsPerThread = pixelCount / numThreads;
        
        std::vector<std::thread> threads;
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t start = t * pixelsPerThread;
            uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
            
            threads.emplace_back([&image, vibrance, start, end]() {
                for (uint32_t i = start; i < end; ++i) {
                    float r = image.r[i];
                    float g = image.g[i];
                    float b = image.b[i];
                    
                    // 计算当前饱和度
                    float maxC = std::max(r, std::max(g, b));
                    float minC = std::min(r, std::min(g, b));
                    float currentSat = (maxC > 0.0f) ? (maxC - minC) / maxC : 0.0f;
                    
                    // 自然饱和度：低饱和区域提升更多
                    float factor = 1.0f + vibrance * (1.0f - currentSat);
                    
                    float avg = (r + g + b) / 3.0f;
                    image.r[i] = std::max(0.0f, avg + (r - avg) * factor);
                    image.g[i] = std::max(0.0f, avg + (g - avg) * factor);
                    image.b[i] = std::max(0.0f, avg + (b - avg) * factor);
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
        
        LOGI("applyPresence: Vibrance adjustment completed");
    }
    
    LOGI("applyPresence completed");
}

// ========== 色调曲线模块 ==========

void ImageProcessorEngine::applyToneCurves(LinearImage& image, const ToneCurveParams& curveParams) {
    LOGI("applyToneCurves: RGB=%d, R=%d, G=%d, B=%d",
         curveParams.rgbCurve.enabled, curveParams.redCurve.enabled,
         curveParams.greenCurve.enabled, curveParams.blueCurve.enabled);
    
    // 如果所有曲线都未启用，直接返回
    if (!curveParams.rgbCurve.enabled && !curveParams.redCurve.enabled &&
        !curveParams.greenCurve.enabled && !curveParams.blueCurve.enabled) {
        return;
    }
    
    // 生成高精度 LUT（256 点）
    const int LUT_SIZE = 256;
    float rgbLUT[LUT_SIZE];
    float redLUT[LUT_SIZE];
    float greenLUT[LUT_SIZE];
    float blueLUT[LUT_SIZE];
    
    // 初始化为线性
    for (int i = 0; i < LUT_SIZE; ++i) {
        float t = i / (LUT_SIZE - 1.0f);
        rgbLUT[i] = t;
        redLUT[i] = t;
        greenLUT[i] = t;
        blueLUT[i] = t;
    }
    
    // 从控制点生成 LUT
    if (curveParams.rgbCurve.enabled && curveParams.rgbCurve.pointCount >= 2) {
        buildLUTFromControlPoints(curveParams.rgbCurve, rgbLUT, LUT_SIZE);
    }
    if (curveParams.redCurve.enabled && curveParams.redCurve.pointCount >= 2) {
        buildLUTFromControlPoints(curveParams.redCurve, redLUT, LUT_SIZE);
    }
    if (curveParams.greenCurve.enabled && curveParams.greenCurve.pointCount >= 2) {
        buildLUTFromControlPoints(curveParams.greenCurve, greenLUT, LUT_SIZE);
    }
    if (curveParams.blueCurve.enabled && curveParams.blueCurve.pointCount >= 2) {
        buildLUTFromControlPoints(curveParams.blueCurve, blueLUT, LUT_SIZE);
    }
    
    // 应用 LUT 到图像
    const uint32_t pixelCount = image.width * image.height;
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];
        
        // 应用 RGB 总曲线
        if (curveParams.rgbCurve.enabled) {
            r = applyLUT(rgbLUT, LUT_SIZE, r);
            g = applyLUT(rgbLUT, LUT_SIZE, g);
            b = applyLUT(rgbLUT, LUT_SIZE, b);
        }
        
        // 应用单通道曲线
        if (curveParams.redCurve.enabled) {
            r = applyLUT(redLUT, LUT_SIZE, r);
        }
        if (curveParams.greenCurve.enabled) {
            g = applyLUT(greenLUT, LUT_SIZE, g);
        }
        if (curveParams.blueCurve.enabled) {
            b = applyLUT(blueLUT, LUT_SIZE, b);
        }
        
        image.r[i] = std::max(0.0f, std::min(1.0f, r));
        image.g[i] = std::max(0.0f, std::min(1.0f, g));
        image.b[i] = std::max(0.0f, std::min(1.0f, b));
    }
    
    LOGI("applyToneCurves completed");
}

/**
 * 从控制点生成 LUT（使用 Hermite 样条插值）
 */
void ImageProcessorEngine::buildLUTFromControlPoints(
    const ToneCurveParams::CurveData& curveData,
    float* lut,
    int lutSize) const {
    
    if (curveData.pointCount < 2 || !curveData.xCoords || !curveData.yCoords) {
        // 如果控制点不足，使用线性
        for (int i = 0; i < lutSize; ++i) {
            lut[i] = i / (lutSize - 1.0f);
        }
        return;
    }
    
    // 为每个 LUT 条目计算插值值
    for (int i = 0; i < lutSize; ++i) {
        float x = i / (lutSize - 1.0f);
        lut[i] = interpolateHermiteSpline(
            curveData.xCoords,
            curveData.yCoords,
            curveData.pointCount,
            x
        );
    }
}

/**
 * Hermite 样条插值（与 UI 层一致）
 */
float ImageProcessorEngine::interpolateHermiteSpline(
    const float* xCoords,
    const float* yCoords,
    int pointCount,
    float x) const {
    
    if (pointCount == 0) return x;
    if (pointCount == 1) return yCoords[0];
    
    // 边界情况
    if (x <= xCoords[0]) return yCoords[0];
    if (x >= xCoords[pointCount - 1]) return yCoords[pointCount - 1];
    
    // 找到 x 所在的区间
    int i1 = 0;
    int i2 = 1;
    
    for (int i = 0; i < pointCount - 1; ++i) {
        if (x >= xCoords[i] && x <= xCoords[i + 1]) {
            i1 = i;
            i2 = i + 1;
            break;
        }
    }
    
    float x1 = xCoords[i1];
    float y1 = yCoords[i1];
    float x2 = xCoords[i2];
    float y2 = yCoords[i2];
    
    // 防止除零
    float dx = x2 - x1;
    if (dx < 0.0001f) return y1;
    
    // 归一化 t
    float t = (x - x1) / dx;
    t = std::max(0.0f, std::min(1.0f, t));
    float t2 = t * t;
    float t3 = t2 * t;
    
    // Hermite 基函数
    float h00 = 2 * t3 - 3 * t2 + 1;
    float h10 = t3 - 2 * t2 + t;
    float h01 = -2 * t3 + 3 * t2;
    float h11 = t3 - t2;
    
    // 计算切线（使用相邻点）
    float m0;
    if (i1 > 0) {
        float prevDx = x2 - xCoords[i1 - 1];
        if (prevDx > 0.0001f) {
            m0 = (y2 - yCoords[i1 - 1]) / prevDx;
        } else {
            m0 = (y2 - y1) / dx;
        }
    } else {
        m0 = (y2 - y1) / dx;
    }
    
    float m1;
    if (i2 < pointCount - 1) {
        float nextDx = xCoords[i2 + 1] - x1;
        if (nextDx > 0.0001f) {
            m1 = (yCoords[i2 + 1] - y1) / nextDx;
        } else {
            m1 = (y2 - y1) / dx;
        }
    } else {
        m1 = (y2 - y1) / dx;
    }
    
    float result = h00 * y1 + h10 * dx * m0 + h01 * y2 + h11 * dx * m1;
    
    return std::max(0.0f, std::min(1.0f, result));
}

/**
 * 应用 LUT 到单个值
 */
float ImageProcessorEngine::applyLUT(const float* lut, int lutSize, float x) const {
    x = std::max(0.0f, std::min(1.0f, x));
    float index = x * (lutSize - 1);
    int i0 = static_cast<int>(index);
    int i1 = std::min(i0 + 1, lutSize - 1);
    float t = index - i0;
    return lut[i0] * (1.0f - t) + lut[i1] * t;
}

// ========== HSL 调整模块 ==========

void ImageProcessorEngine::applyHSL(LinearImage& image, const HSLParams& hslParams) {
    LOGI("applyHSL: enabled=%d", hslParams.enableHSL);
    
    if (!hslParams.enableHSL) {
        return;
    }
    
    const uint32_t pixelCount = image.width * image.height;
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];
        
        float h, s, l;
        rgbToHSL(r, g, b, h, s, l);
        
        int segment = getHueSegment(h);
        
        // 应用色相偏移
        h += hslParams.hueShift[segment];
        if (h < 0.0f) h += 360.0f;
        if (h >= 360.0f) h -= 360.0f;
        
        // 应用饱和度调整
        s *= (1.0f + hslParams.saturation[segment] / 100.0f);
        s = std::max(0.0f, std::min(1.0f, s));
        
        // 应用亮度调整
        l *= (1.0f + hslParams.luminance[segment] / 100.0f);
        l = std::max(0.0f, std::min(1.0f, l));
        
        hslToRGB(h, s, l, r, g, b);
        
        image.r[i] = r;
        image.g[i] = g;
        image.b[i] = b;
    }
    
    LOGI("applyHSL completed");
}

void ImageProcessorEngine::rgbToHSL(float r, float g, float b, float& h, float& s, float& l) const {
    float maxC = std::max(r, std::max(g, b));
    float minC = std::min(r, std::min(g, b));
    float delta = maxC - minC;
    
    l = (maxC + minC) / 2.0f;
    
    if (delta < 1e-5f) {
        h = 0.0f;
        s = 0.0f;
    } else {
        s = (l > 0.5f) ? delta / (2.0f - maxC - minC) : delta / (maxC + minC);
        
        if (maxC == r) {
            h = 60.0f * fmod((g - b) / delta, 6.0f);
        } else if (maxC == g) {
            h = 60.0f * ((b - r) / delta + 2.0f);
        } else {
            h = 60.0f * ((r - g) / delta + 4.0f);
        }
        
        if (h < 0.0f) h += 360.0f;
    }
}

void ImageProcessorEngine::hslToRGB(float h, float s, float l, float& r, float& g, float& b) const {
    auto hueToRGB = [](float p, float q, float t) {
        if (t < 0.0f) t += 1.0f;
        if (t > 1.0f) t -= 1.0f;
        if (t < 1.0f/6.0f) return p + (q - p) * 6.0f * t;
        if (t < 1.0f/2.0f) return q;
        if (t < 2.0f/3.0f) return p + (q - p) * (2.0f/3.0f - t) * 6.0f;
        return p;
    };
    
    if (s < 1e-5f) {
        r = g = b = l;
    } else {
        float q = (l < 0.5f) ? l * (1.0f + s) : l + s - l * s;
        float p = 2.0f * l - q;
        float hNorm = h / 360.0f;
        r = hueToRGB(p, q, hNorm + 1.0f/3.0f);
        g = hueToRGB(p, q, hNorm);
        b = hueToRGB(p, q, hNorm - 1.0f/3.0f);
    }
}

int ImageProcessorEngine::getHueSegment(float hue) const {
    // 8 个色相段：红(0)、橙(1)、黄(2)、绿(3)、青(4)、蓝(5)、紫(6)、品红(7)
    int segment = static_cast<int>(hue / 45.0f);
    return std::max(0, std::min(7, segment));
}

// ========== 颜色调整模块 ==========

void ImageProcessorEngine::applyColorAdjustments(LinearImage& image, const BasicAdjustmentParams& params) {
    // 检查是否有任何颜色调整
    if (params.temperature == 0.0f && params.tint == 0.0f &&
        params.gradingHighlightsTemp == 0.0f && params.gradingHighlightsTint == 0.0f &&
        params.gradingMidtonesTemp == 0.0f && params.gradingMidtonesTint == 0.0f &&
        params.gradingShadowsTemp == 0.0f && params.gradingShadowsTint == 0.0f) {
        return; // 没有调整，直接返回
    }
    
    LOGI("applyColorAdjustments: temp=%.2f, tint=%.2f, grading enabled", params.temperature, params.tint);
    
    // 1. 首先应用全局色温和色调调整
    if (std::abs(params.temperature) > 0.01f || std::abs(params.tint) > 0.01f) {
        const uint32_t pixelCount = image.width * image.height;
        const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
        const uint32_t pixelsPerThread = pixelCount / numThreads;
        
        std::vector<std::thread> threads;
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t start = t * pixelsPerThread;
            uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
            
            threads.emplace_back([&image, &params, start, end]() {
                for (uint32_t i = start; i < end; ++i) {
                    float r = image.r[i];
                    float g = image.g[i];
                    float b = image.b[i];
                    
                    // 使用 Planckian Locus 算法应用色温和色调调整
                    ColorTemperature::applyColorTemperature(r, g, b, params.temperature, params.tint);
                    
                    image.r[i] = std::max(0.0f, r);
                    image.g[i] = std::max(0.0f, g);
                    image.b[i] = std::max(0.0f, b);
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
    }
    
    // 2. 应用色彩分级（使用高斯权重函数）
    bool hasGrading = std::abs(params.gradingHighlightsTemp) > 0.01f || 
                     std::abs(params.gradingHighlightsTint) > 0.01f ||
                     std::abs(params.gradingMidtonesTemp) > 0.01f || 
                     std::abs(params.gradingMidtonesTint) > 0.01f ||
                     std::abs(params.gradingShadowsTemp) > 0.01f || 
                     std::abs(params.gradingShadowsTint) > 0.01f;
    
    if (hasGrading) {
        // 准备色彩分级参数
        ColorGrading::GradingParams gradingParams;
        
        // 将色温和色调转换为 RGB 偏移
        // 这里使用简化的映射：
        // - 色温主要影响 R 和 B 通道
        // - 色调主要影响 G 通道
        float tempScale = 0.01f;  // 色温缩放因子
        float tintScale = 0.01f;  // 色调缩放因子
        
        // 高光分级
        gradingParams.highlightR = params.gradingHighlightsTemp * tempScale;
        gradingParams.highlightG = params.gradingHighlightsTint * tintScale;
        gradingParams.highlightB = -params.gradingHighlightsTemp * tempScale * 0.5f;
        
        // 中间调分级
        gradingParams.midtoneR = params.gradingMidtonesTemp * tempScale;
        gradingParams.midtoneG = params.gradingMidtonesTint * tintScale;
        gradingParams.midtoneB = -params.gradingMidtonesTemp * tempScale * 0.5f;
        
        // 阴影分级
        gradingParams.shadowR = params.gradingShadowsTemp * tempScale;
        gradingParams.shadowG = params.gradingShadowsTint * tintScale;
        gradingParams.shadowB = -params.gradingShadowsTemp * tempScale * 0.5f;
        
        // 整体强度和区域平衡
        gradingParams.blending = params.gradingBlending / 100.0f;  // 0-1
        gradingParams.balance = params.gradingBalance / 100.0f;    // -1 到 +1
        
        // 应用色彩分级
        ColorGrading::applyGrading(image, gradingParams);
    }
    
    LOGI("applyColorAdjustments completed");
}

// ========== 效果模块 ==========

void ImageProcessorEngine::applyEffects(LinearImage& image, const BasicAdjustmentParams& params) {
    if (params.texture == 0.0f && params.dehaze == 0.0f && 
        params.vignette == 0.0f && params.grain == 0.0f) {
        return; // 没有调整，直接返回
    }
    
    LOGI("applyEffects: texture=%.2f, dehaze=%.2f", params.texture, params.dehaze);
    
    const uint32_t pixelCount = image.width * image.height;
    
    // 纹理效果：使用双边滤波器提取细节并增强
    if (std::abs(params.texture) > 0.01f) {
        LOGI("applyEffects: Applying texture adjustment");
        
        // 归一化纹理参数（-100 到 +100 -> -1.0 到 +1.0）
        float textureAmount = params.texture / 100.0f;
        
        // 使用较小的 spatialSigma 来提取高频细节
        float spatialSigma = 2.0f;  // 小尺度，提取细节
        float rangeSigma = 0.1f;    // 强边缘保持
        
        // 提取细节层
        LinearImage detail(image.width, image.height);
        BilateralFilter::extractDetail(image, detail, spatialSigma, rangeSigma);
        
        // 应用纹理调整
        const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
        const uint32_t pixelsPerThread = pixelCount / numThreads;
        
        std::vector<std::thread> threads;
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t start = t * pixelsPerThread;
            uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
            
            threads.emplace_back([&image, &detail, textureAmount, start, end]() {
                for (uint32_t i = start; i < end; ++i) {
                    // 应用纹理调整（增强或减弱细节）
                    image.r[i] = image.r[i] + detail.r[i] * textureAmount;
                    image.g[i] = image.g[i] + detail.g[i] * textureAmount;
                    image.b[i] = image.b[i] + detail.b[i] * textureAmount;
                    
                    // 限制范围
                    image.r[i] = std::max(0.0f, image.r[i]);
                    image.g[i] = std::max(0.0f, image.g[i]);
                    image.b[i] = std::max(0.0f, image.b[i]);
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
        
        LOGI("applyEffects: Texture adjustment completed");
    }
    
    // 去雾效果（增强对比度和饱和度）
    if (std::abs(params.dehaze) > 0.01f) {
        LOGI("applyEffects: Applying dehaze");
        
        float dehazeFactor = params.dehaze / 100.0f;
        
        const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
        const uint32_t pixelsPerThread = pixelCount / numThreads;
        
        std::vector<std::thread> threads;
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t start = t * pixelsPerThread;
            uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
            
            threads.emplace_back([&image, dehazeFactor, start, end]() {
                for (uint32_t i = start; i < end; ++i) {
                    float r = image.r[i];
                    float g = image.g[i];
                    float b = image.b[i];
                    
                    // 增强对比度
                    r = r + (r - 0.5f) * dehazeFactor * 0.5f;
                    g = g + (g - 0.5f) * dehazeFactor * 0.5f;
                    b = b + (b - 0.5f) * dehazeFactor * 0.5f;
                    
                    // 增强饱和度
                    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
                    r = luminance + (r - luminance) * (1.0f + dehazeFactor * 0.3f);
                    g = luminance + (g - luminance) * (1.0f + dehazeFactor * 0.3f);
                    b = luminance + (b - luminance) * (1.0f + dehazeFactor * 0.3f);
                    
                    image.r[i] = std::max(0.0f, r);
                    image.g[i] = std::max(0.0f, g);
                    image.b[i] = std::max(0.0f, b);
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
        
        LOGI("applyEffects: Dehaze completed");
    }
    
    LOGI("applyEffects completed");
}

// ========== 细节模块 ==========

void ImageProcessorEngine::applyDetails(LinearImage& image, const BasicAdjustmentParams& params) {
    if (params.sharpening == 0.0f && params.noiseReduction == 0.0f) {
        return; // 没有调整，直接返回
    }
    
    LOGI("applyDetails: sharpening=%.2f, noiseReduction=%.2f", params.sharpening, params.noiseReduction);
    
    const uint32_t width = image.width;
    const uint32_t height = image.height;
    const uint32_t pixelCount = width * height;
    
    // 降噪效果：使用双边滤波器
    if (params.noiseReduction > 0.0f) {
        LOGI("applyDetails: Applying noise reduction");
        
        // 归一化降噪参数（0 到 100 -> 0.0 到 1.0）
        float nrAmount = params.noiseReduction / 100.0f;
        
        // 双边滤波器参数
        float spatialSigma = 3.0f + nrAmount * 5.0f;  // 3-8 像素
        float rangeSigma = 0.1f + nrAmount * 0.2f;    // 0.1-0.3
        
        // 应用双边滤波器
        LinearImage filtered(width, height);
        BilateralFilter::apply(image, filtered, spatialSigma, rangeSigma);
        
        // 混合原图和滤波结果
        const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
        const uint32_t pixelsPerThread = pixelCount / numThreads;
        
        std::vector<std::thread> threads;
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t start = t * pixelsPerThread;
            uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * pixelsPerThread;
            
            threads.emplace_back([&image, &filtered, nrAmount, start, end]() {
                for (uint32_t i = start; i < end; ++i) {
                    // 线性混合
                    image.r[i] = image.r[i] * (1.0f - nrAmount) + filtered.r[i] * nrAmount;
                    image.g[i] = image.g[i] * (1.0f - nrAmount) + filtered.g[i] * nrAmount;
                    image.b[i] = image.b[i] * (1.0f - nrAmount) + filtered.b[i] * nrAmount;
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
        
        LOGI("applyDetails: Noise reduction completed");
    }
    
    // 锐化效果：使用 Unsharp Mask
    if (params.sharpening > 0.0f) {
        LOGI("applyDetails: Applying sharpening");
        
        // 归一化锐化参数（0 到 100 -> 0.0 到 1.0）
        float sharpenAmount = params.sharpening / 100.0f;
        
        // 创建模糊版本（使用简单的高斯模糊）
        std::vector<float> blurR(pixelCount);
        std::vector<float> blurG(pixelCount);
        std::vector<float> blurB(pixelCount);
        
        // 简单的 3x3 高斯模糊核
        // 1  2  1
        // 2  4  2
        // 1  2  1
        // 总和 = 16
        const uint32_t numThreads = std::min(4u, std::thread::hardware_concurrency());
        const uint32_t rowsPerThread = height / numThreads;
        
        std::vector<std::thread> threads;
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t startRow = t * rowsPerThread;
            uint32_t endRow = (t == numThreads - 1) ? height : (t + 1) * rowsPerThread;
            
            threads.emplace_back([&image, &blurR, &blurG, &blurB, width, height, startRow, endRow]() {
                for (uint32_t y = startRow; y < endRow; ++y) {
                    for (uint32_t x = 0; x < width; ++x) {
                        uint32_t idx = y * width + x;
                        
                        float sumR = 0.0f, sumG = 0.0f, sumB = 0.0f;
                        float sumWeight = 0.0f;
                        
                        // 3x3 高斯核
                        for (int dy = -1; dy <= 1; ++dy) {
                            int ny = static_cast<int>(y) + dy;
                            if (ny < 0 || ny >= static_cast<int>(height)) continue;
                            
                            for (int dx = -1; dx <= 1; ++dx) {
                                int nx = static_cast<int>(x) + dx;
                                if (nx < 0 || nx >= static_cast<int>(width)) continue;
                                
                                uint32_t nidx = ny * width + nx;
                                
                                // 高斯权重
                                float weight = 1.0f;
                                if (dx == 0 || dy == 0) {
                                    weight = (dx == 0 && dy == 0) ? 4.0f : 2.0f;
                                }
                                
                                sumR += image.r[nidx] * weight;
                                sumG += image.g[nidx] * weight;
                                sumB += image.b[nidx] * weight;
                                sumWeight += weight;
                            }
                        }
                        
                        blurR[idx] = sumR / sumWeight;
                        blurG[idx] = sumG / sumWeight;
                        blurB[idx] = sumB / sumWeight;
                    }
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
        
        // 应用 Unsharp Mask：原图 + (原图 - 模糊) * 强度
        threads.clear();
        for (uint32_t t = 0; t < numThreads; ++t) {
            uint32_t start = t * (pixelCount / numThreads);
            uint32_t end = (t == numThreads - 1) ? pixelCount : (t + 1) * (pixelCount / numThreads);
            
            threads.emplace_back([&image, &blurR, &blurG, &blurB, sharpenAmount, start, end]() {
                for (uint32_t i = start; i < end; ++i) {
                    // Unsharp Mask
                    float r = image.r[i] + (image.r[i] - blurR[i]) * sharpenAmount;
                    float g = image.g[i] + (image.g[i] - blurG[i]) * sharpenAmount;
                    float b = image.b[i] + (image.b[i] - blurB[i]) * sharpenAmount;
                    
                    image.r[i] = std::max(0.0f, r);
                    image.g[i] = std::max(0.0f, g);
                    image.b[i] = std::max(0.0f, b);
                }
            });
        }
        
        for (auto& thread : threads) {
            thread.join();
        }
        
        LOGI("applyDetails: Sharpening completed");
    }
    
    LOGI("applyDetails completed");
}

} // namespace filmtracker
