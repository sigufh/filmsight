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
    
    const uint32_t pixelCount = image.width * image.height;
    
    // 归一化参数
    float globalTemp = params.temperature / 100.0f;
    float globalTint = params.tint / 100.0f;
    
    // 分级参数
    float highlightsTemp = params.gradingHighlightsTemp / 100.0f;
    float highlightsTint = params.gradingHighlightsTint / 100.0f;
    float midtonesTemp = params.gradingMidtonesTemp / 100.0f;
    float midtonesTint = params.gradingMidtonesTint / 100.0f;
    float shadowsTemp = params.gradingShadowsTemp / 100.0f;
    float shadowsTint = params.gradingShadowsTint / 100.0f;
    float blending = params.gradingBlending / 100.0f; // 0-1
    float balance = params.gradingBalance / 100.0f;   // -1 到 +1
    
    // 平滑过渡函数
    auto smoothstep = [](float edge0, float edge1, float v) {
        float t = std::max(0.0f, std::min(1.0f, (v - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    };
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];
        
        // 计算亮度（用于分级权重）
        float luminance = 0.299f * r + 0.587f * g + 0.114f * b;
        
        // 根据 balance 调整分级区域的边界
        float shadowEdge = 0.33f + balance * 0.2f;
        float highlightEdge = 0.67f + balance * 0.2f;
        
        // 计算三个区域的权重（使用平滑过渡）
        float shadowWeight = 1.0f - smoothstep(0.0f, shadowEdge, luminance);
        float highlightWeight = smoothstep(highlightEdge, 1.0f, luminance);
        float midtoneWeight = 1.0f - shadowWeight - highlightWeight;
        
        // 应用 blending 参数（控制分级效果的强度）
        shadowWeight *= blending;
        highlightWeight *= blending;
        midtoneWeight *= blending;
        
        // 计算总的色温和色调调整
        float totalTemp = globalTemp + 
                         shadowWeight * shadowsTemp +
                         midtoneWeight * midtonesTemp +
                         highlightWeight * highlightsTemp;
        
        float totalTint = globalTint +
                         shadowWeight * shadowsTint +
                         midtoneWeight * midtonesTint +
                         highlightWeight * highlightsTint;
        
        // 应用色温调整（Lightroom 风格）
        // 色温：正值增加暖色（红/黄），负值增加冷色（蓝）
        if (totalTemp != 0.0f) {
            float tempScale = totalTemp * 0.4f;
            r *= (1.0f + tempScale);
            b *= (1.0f - tempScale);
        }
        
        // 应用色调调整（绿-品红轴）
        // 色调：正值增加品红，负值增加绿色
        if (totalTint != 0.0f) {
            float tintScale = totalTint * 0.3f;
            r *= (1.0f + tintScale * 0.5f);
            g *= (1.0f - tintScale);
            b *= (1.0f + tintScale * 0.5f);
        }
        
        image.r[i] = std::max(0.0f, r);
        image.g[i] = std::max(0.0f, g);
        image.b[i] = std::max(0.0f, b);
    }
    
    LOGI("applyColorAdjustments completed");
}

// ========== 效果模块 ==========

void ImageProcessorEngine::applyEffects(LinearImage& image, const BasicAdjustmentParams& params) {
    // 简单实现
    // TODO: 实现完整的纹理、去雾、晕影、颗粒效果
    
    if (params.texture == 0.0f && params.dehaze == 0.0f && 
        params.vignette == 0.0f && params.grain == 0.0f) {
        return; // 没有调整，直接返回
    }
    
    const uint32_t pixelCount = image.width * image.height;
    
    // 简单的对比度调整作为纹理效果的占位符
    if (params.texture != 0.0f) {
        float textureFactor = 1.0f + params.texture / 200.0f;
        for (uint32_t i = 0; i < pixelCount; ++i) {
            float r = image.r[i];
            float g = image.g[i];
            float b = image.b[i];
            
            // 增强中频细节
            r = 0.5f + (r - 0.5f) * textureFactor;
            g = 0.5f + (g - 0.5f) * textureFactor;
            b = 0.5f + (b - 0.5f) * textureFactor;
            
            image.r[i] = std::max(0.0f, std::min(1.0f, r));
            image.g[i] = std::max(0.0f, std::min(1.0f, g));
            image.b[i] = std::max(0.0f, std::min(1.0f, b));
        }
    }
    
    // 去雾效果（简化版：增加对比度和饱和度）
    if (params.dehaze != 0.0f) {
        float dehazeFactor = params.dehaze / 100.0f;
        for (uint32_t i = 0; i < pixelCount; ++i) {
            float r = image.r[i];
            float g = image.g[i];
            float b = image.b[i];
            
            // 增强对比度
            r = r + (r - 0.5f) * dehazeFactor * 0.5f;
            g = g + (g - 0.5f) * dehazeFactor * 0.5f;
            b = b + (b - 0.5f) * dehazeFactor * 0.5f;
            
            image.r[i] = std::max(0.0f, std::min(1.0f, r));
            image.g[i] = std::max(0.0f, std::min(1.0f, g));
            image.b[i] = std::max(0.0f, std::min(1.0f, b));
        }
    }
}

// ========== 细节模块 ==========

void ImageProcessorEngine::applyDetails(LinearImage& image, const BasicAdjustmentParams& params) {
    // 简单实现
    // TODO: 实现完整的锐化和降噪效果
    
    if (params.sharpening == 0.0f && params.noiseReduction == 0.0f) {
        return; // 没有调整，直接返回
    }
    
    // 锐化效果（简化版：增强边缘）
    if (params.sharpening > 0.0f) {
        float sharpenAmount = params.sharpening / 100.0f;
        
        // 简单的 unsharp mask
        const uint32_t width = image.width;
        const uint32_t height = image.height;
        
        std::vector<float> blurR(width * height);
        std::vector<float> blurG(width * height);
        std::vector<float> blurB(width * height);
        
        // 简单的 3x3 box blur
        for (uint32_t y = 1; y < height - 1; ++y) {
            for (uint32_t x = 1; x < width - 1; ++x) {
                uint32_t idx = y * width + x;
                
                float sumR = 0.0f, sumG = 0.0f, sumB = 0.0f;
                for (int dy = -1; dy <= 1; ++dy) {
                    for (int dx = -1; dx <= 1; ++dx) {
                        uint32_t nidx = (y + dy) * width + (x + dx);
                        sumR += image.r[nidx];
                        sumG += image.g[nidx];
                        sumB += image.b[nidx];
                    }
                }
                
                blurR[idx] = sumR / 9.0f;
                blurG[idx] = sumG / 9.0f;
                blurB[idx] = sumB / 9.0f;
            }
        }
        
        // 应用锐化
        for (uint32_t y = 1; y < height - 1; ++y) {
            for (uint32_t x = 1; x < width - 1; ++x) {
                uint32_t idx = y * width + x;
                
                float r = image.r[idx] + (image.r[idx] - blurR[idx]) * sharpenAmount;
                float g = image.g[idx] + (image.g[idx] - blurG[idx]) * sharpenAmount;
                float b = image.b[idx] + (image.b[idx] - blurB[idx]) * sharpenAmount;
                
                image.r[idx] = std::max(0.0f, std::min(1.0f, r));
                image.g[idx] = std::max(0.0f, std::min(1.0f, g));
                image.b[idx] = std::max(0.0f, std::min(1.0f, b));
            }
        }
    }
}

} // namespace filmtracker
