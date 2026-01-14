#include "image_processor_engine.h"
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
                
                // 2. 对比度调整（围绕中灰）
                const float mid = 0.18f;  // 中灰值（线性空间）
                r = mid + (r - mid) * contrast;
                g = mid + (g - mid) * contrast;
                b = mid + (b - mid) * contrast;
                
                // 3. 饱和度调整
                float luminance = 0.299f * r + 0.587f * g + 0.114f * b;
                r = luminance + (r - luminance) * saturation;
                g = luminance + (g - luminance) * saturation;
                b = luminance + (b - luminance) * saturation;
                
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
    
    // 归一化参数到 [-1, 1]
    highlights /= 100.0f;
    shadows /= 100.0f;
    whites /= 100.0f;
    blacks /= 100.0f;
    
    auto smoothstep = [](float edge0, float edge1, float v) {
        float t = std::max(0.0f, std::min(1.0f, (v - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    };
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];
        
        // 计算亮度
        float luminance = 0.299f * r + 0.587f * g + 0.114f * b;
        float newLuminance = luminance;
        
        // 高光调整（影响亮部）
        if (std::abs(highlights) > 0.01f) {
            float weight = smoothstep(0.5f, 1.0f, luminance);
            float adjustment = highlights * weight * (luminance - 0.5f);
            newLuminance -= adjustment;
        }
        
        // 阴影调整（影响暗部）
        if (std::abs(shadows) > 0.01f) {
            float weight = 1.0f - smoothstep(0.2f, 0.6f, luminance);
            float adjustment = shadows * weight * (0.5f - luminance);
            newLuminance += adjustment;
        }
        
        // 白场调整
        if (std::abs(whites) > 0.01f) {
            float weight = smoothstep(0.5f, 1.0f, luminance);
            newLuminance += whites * weight * 0.2f;
        }
        
        // 黑场调整
        if (std::abs(blacks) > 0.01f) {
            float weight = 1.0f - smoothstep(0.0f, 0.4f, luminance);
            newLuminance += blacks * weight * 0.2f;
        }
        
        // 按比例调整 RGB
        float eps = 1e-5f;
        float scale = (luminance > eps) ? (newLuminance / luminance) : 1.0f;
        
        image.r[i] = std::max(0.0f, r * scale);
        image.g[i] = std::max(0.0f, g * scale);
        image.b[i] = std::max(0.0f, b * scale);
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
    
    // 归一化参数
    vibrance /= 100.0f;
    
    // 自然饱和度调整
    if (std::abs(vibrance) > 0.01f) {
        for (uint32_t i = 0; i < pixelCount; ++i) {
            float r = image.r[i];
            float g = image.g[i];
            float b = image.b[i];
            
            float maxC = std::max(r, std::max(g, b));
            float minC = std::min(r, std::min(g, b));
            float currentSat = (maxC > 0.0f) ? (maxC - minC) / maxC : 0.0f;
            
            // 低饱和区域提升更多
            float factor = 1.0f + vibrance * (1.0f - currentSat);
            
            float avg = (r + g + b) / 3.0f;
            image.r[i] = std::max(0.0f, avg + (r - avg) * factor);
            image.g[i] = std::max(0.0f, avg + (g - avg) * factor);
            image.b[i] = std::max(0.0f, avg + (b - avg) * factor);
        }
    }
    
    // 清晰度调整（简化实现，实际应该用局部对比度）
    // 这里暂时跳过，因为需要空间域滤波
    
    LOGI("applyPresence completed");
}

// ========== 色调曲线模块 ==========

void ImageProcessorEngine::applyToneCurves(LinearImage& image, const ToneCurveParams& curveParams) {
    LOGI("applyToneCurves: RGB=%d, R=%d, G=%d, B=%d",
         curveParams.enableRgbCurve, curveParams.enableRedCurve,
         curveParams.enableGreenCurve, curveParams.enableBlueCurve);
    
    // 如果所有曲线都未启用，直接返回
    if (!curveParams.enableRgbCurve && !curveParams.enableRedCurve &&
        !curveParams.enableGreenCurve && !curveParams.enableBlueCurve) {
        return;
    }
    
    const uint32_t pixelCount = image.width * image.height;
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];
        
        // 应用 RGB 总曲线
        if (curveParams.enableRgbCurve) {
            r = interpolateCurve(curveParams.rgbCurve, 16, r);
            g = interpolateCurve(curveParams.rgbCurve, 16, g);
            b = interpolateCurve(curveParams.rgbCurve, 16, b);
        }
        
        // 应用单通道曲线
        if (curveParams.enableRedCurve) {
            r = interpolateCurve(curveParams.redCurve, 16, r);
        }
        if (curveParams.enableGreenCurve) {
            g = interpolateCurve(curveParams.greenCurve, 16, g);
        }
        if (curveParams.enableBlueCurve) {
            b = interpolateCurve(curveParams.blueCurve, 16, b);
        }
        
        image.r[i] = std::max(0.0f, std::min(1.0f, r));
        image.g[i] = std::max(0.0f, std::min(1.0f, g));
        image.b[i] = std::max(0.0f, std::min(1.0f, b));
    }
    
    LOGI("applyToneCurves completed");
}

float ImageProcessorEngine::interpolateCurve(const float* curve, int numPoints, float x) const {
    x = std::max(0.0f, std::min(1.0f, x));
    float index = x * (numPoints - 1);
    int i0 = static_cast<int>(index);
    int i1 = std::min(i0 + 1, numPoints - 1);
    float t = index - i0;
    return curve[i0] * (1.0f - t) + curve[i1] * t;
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

} // namespace filmtracker
