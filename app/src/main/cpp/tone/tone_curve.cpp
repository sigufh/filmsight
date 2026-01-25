#include "film_engine.h"
#include <algorithm>
#include <cmath>

namespace filmtracker {

/**
 * Catmull-Rom 样条插值
 * 用于平滑的曲线插值
 */
float FilmEngine::interpolateCurve(const float* curve, int numPoints, float x) const {
    x = std::max(0.0f, std::min(1.0f, x));
    
    if (numPoints < 2) {
        return x;
    }
    
    // 找到 x 所在的区间
    float segmentSize = 1.0f / (numPoints - 1);
    int segment = static_cast<int>(x / segmentSize);
    segment = std::min(segment, numPoints - 2);
    
    float t = (x - segment * segmentSize) / segmentSize;
    
    // Catmull-Rom 插值
    float p0 = (segment > 0) ? curve[segment - 1] : curve[0];
    float p1 = curve[segment];
    float p2 = curve[segment + 1];
    float p3 = (segment < numPoints - 2) ? curve[segment + 2] : curve[numPoints - 1];
    
    float t2 = t * t;
    float t3 = t2 * t;
    
    return 0.5f * (
        (2.0f * p1) +
        (-p0 + p2) * t +
        (2.0f * p0 - 5.0f * p1 + 4.0f * p2 - p3) * t2 +
        (-p0 + 3.0f * p1 - 3.0f * p2 + p3) * t3
    );
}

/**
 * 应用色调曲线
 */
void FilmEngine::applyToneCurves(LinearImage& image, const ToneCurveParams& curveParams) {
    if (!curveParams.enableRgbCurve && 
        !curveParams.enableRedCurve && 
        !curveParams.enableGreenCurve && 
        !curveParams.enableBlueCurve) {
        return;
    }
    
    const uint32_t pixelCount = image.width * image.height;
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];
        
        // RGB 总曲线（应用到亮度）
        if (curveParams.enableRgbCurve) {
            float luminance = 0.299f * r + 0.587f * g + 0.114f * b;
            float newLuminance = interpolateCurve(curveParams.rgbCurve, 16, luminance);
            
            // 按比例缩放 RGB
            if (luminance > 1e-5f) {
                float scale = newLuminance / luminance;
                r *= scale;
                g *= scale;
                b *= scale;
            }
        }
        
        // 单通道曲线
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
}

} // namespace filmtracker
