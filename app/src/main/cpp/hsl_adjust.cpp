#include "film_engine.h"
#include <algorithm>
#include <cmath>

namespace filmtracker {

/**
 * RGB 转 HSL
 */
void FilmEngine::rgbToHSL(float r, float g, float b, float& h, float& s, float& l) const {
    float max = std::max(r, std::max(g, b));
    float min = std::min(r, std::min(g, b));
    float delta = max - min;
    
    l = (max + min) / 2.0f;
    
    if (delta < 1e-5f) {
        h = 0.0f;
        s = 0.0f;
        return;
    }
    
    s = (l > 0.5f) ? (delta / (2.0f - max - min)) : (delta / (max + min));
    
    if (max == r) {
        h = ((g - b) / delta) + (g < b ? 6.0f : 0.0f);
    } else if (max == g) {
        h = ((b - r) / delta) + 2.0f;
    } else {
        h = ((r - g) / delta) + 4.0f;
    }
    
    h /= 6.0f; // 归一化到 [0, 1]
}

/**
 * HSL 转 RGB
 */
void FilmEngine::hslToRGB(float h, float s, float l, float& r, float& g, float& b) const {
    h = std::fmod(h, 1.0f);
    if (h < 0.0f) h += 1.0f;
    
    if (s < 1e-5f) {
        r = g = b = l;
        return;
    }
    
    float q = (l < 0.5f) ? (l * (1.0f + s)) : (l + s - l * s);
    float p = 2.0f * l - q;
    
    float t[3];
    t[0] = h + 1.0f / 3.0f;
    t[1] = h;
    t[2] = h - 1.0f / 3.0f;
    
    for (int i = 0; i < 3; ++i) {
        if (t[i] < 0.0f) t[i] += 1.0f;
        if (t[i] > 1.0f) t[i] -= 1.0f;
        
        float val;
        if (t[i] < 1.0f / 6.0f) {
            val = p + (q - p) * 6.0f * t[i];
        } else if (t[i] < 0.5f) {
            val = q;
        } else if (t[i] < 2.0f / 3.0f) {
            val = p + (q - p) * (2.0f / 3.0f - t[i]) * 6.0f;
        } else {
            val = p;
        }
        
        if (i == 0) r = val;
        else if (i == 1) g = val;
        else b = val;
    }
}

/**
 * 获取色相段索引（0-7）
 * 0: 红, 1: 橙, 2: 黄, 3: 绿, 4: 青, 5: 蓝, 6: 紫, 7: 品红
 */
int FilmEngine::getHueSegment(float hue) const {
    // 归一化到 [0, 1]
    hue = std::fmod(hue, 1.0f);
    if (hue < 0.0f) hue += 1.0f;
    
    // 8 个色相段，每个 45 度
    int segment = static_cast<int>(hue * 8.0f);
    return std::min(segment, 7);
}

/**
 * 应用 HSL 调整
 */
void FilmEngine::applyHSL(LinearImage& image, const HSLParams& hslParams) {
    if (!hslParams.enableHSL) {
        return;
    }
    
    const uint32_t pixelCount = image.width * image.height;
    
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];
        
        // 转换到 HSL
        float h, s, l;
        rgbToHSL(r, g, b, h, s, l);
        
        // 获取色相段
        int segment = getHueSegment(h);
        
        // 应用调整
        // hue_shift: [-180, 180] 度 -> [-0.5, 0.5] 归一化
        h += hslParams.hueShift[segment] / 360.0f;
        h = std::fmod(h, 1.0f);
        if (h < 0.0f) h += 1.0f;
        
        // saturation: [-100, 100] % -> [-1, 1]
        s = std::max(0.0f, std::min(1.0f, s + hslParams.saturation[segment] / 100.0f));
        
        // luminance: [-100, 100] % -> [-1, 1]
        l = std::max(0.0f, std::min(1.0f, l + hslParams.luminance[segment] / 100.0f));
        
        // 转换回 RGB
        hslToRGB(h, s, l, r, g, b);
        
        image.r[i] = std::max(0.0f, std::min(1.0f, r));
        image.g[i] = std::max(0.0f, std::min(1.0f, g));
        image.b[i] = std::max(0.0f, std::min(1.0f, b));
    }
}

} // namespace filmtracker
