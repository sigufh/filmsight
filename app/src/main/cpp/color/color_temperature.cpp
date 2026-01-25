#include "color_temperature.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

void ColorTemperature::temperatureToCIExy(float temperature, float& x, float& y) {
    // Planckian Locus 近似公式
    // 基于 Hernández-Andrés et al. (1999) 改进版，使用平滑过渡
    // 适用于 1000K 到 100000K（专业调色软件标准）
    
    temperature = std::clamp(temperature, 1000.0f, 100000.0f);
    
    // 计算倒数温度（用于多项式）
    float T = temperature;
    float T2 = T * T;
    float T3 = T2 * T;
    
    // 计算 CIE x 色度坐标
    // 使用平滑的分段多项式，在分段点使用线性混合避免突变
    if (T <= 4000.0f) {
        // 极低色温（1000K-4000K）：烛光到白炽灯
        x = -0.2661239e9f / T3 - 0.2343589e6f / T2 + 0.8776956e3f / T + 0.179910f;
    } else if (T < 7000.0f) {
        // 低色温（4000K-7000K）：暖白到日光
        // 在 4000K 附近使用平滑过渡
        float x1 = -0.2661239e9f / T3 - 0.2343589e6f / T2 + 0.8776956e3f / T + 0.179910f;
        float x2 = -4.6070e9f / T3 + 2.9678e6f / T2 + 0.09911e3f / T + 0.244063f;
        
        if (T < 4500.0f) {
            // 4000K-4500K：平滑过渡
            float blend = (T - 4000.0f) / 500.0f;
            blend = blend * blend * (3.0f - 2.0f * blend);  // Smoothstep
            x = x1 * (1.0f - blend) + x2 * blend;
        } else {
            x = x2;
        }
    } else if (T < 25000.0f) {
        // 高色温（7000K-25000K）：冷白到天空光
        // 在 7000K 附近使用平滑过渡
        float x1 = -4.6070e9f / T3 + 2.9678e6f / T2 + 0.09911e3f / T + 0.244063f;
        float x2 = -2.0064e9f / T3 + 1.9018e6f / T2 + 0.24748e3f / T + 0.237040f;
        
        if (T < 8000.0f) {
            // 7000K-8000K：平滑过渡
            float blend = (T - 7000.0f) / 1000.0f;
            blend = blend * blend * (3.0f - 2.0f * blend);  // Smoothstep
            x = x1 * (1.0f - blend) + x2 * blend;
        } else {
            x = x2;
        }
    } else {
        // 极高色温（25000K+）：保持稳定
        x = -2.0064e9f / T3 + 1.9018e6f / T2 + 0.24748e3f / T + 0.237040f;
    }
    
    // 计算 CIE y 色度坐标（基于 x）
    float x2 = x * x;
    float x3 = x2 * x;
    
    if (T <= 2222.0f) {
        // 极低色温：使用修正公式避免过度偏移
        y = -1.1063814f * x3 - 1.34811020f * x2 + 2.18555832f * x - 0.20219683f;
    } else if (T < 4000.0f) {
        // 低色温过渡区：平滑混合
        float y1 = -1.1063814f * x3 - 1.34811020f * x2 + 2.18555832f * x - 0.20219683f;
        float y2 = -0.9549476f * x3 - 1.37418593f * x2 + 2.09137015f * x - 0.16748867f;
        
        if (T < 3000.0f) {
            // 2222K-3000K：平滑过渡
            float blend = (T - 2222.0f) / (3000.0f - 2222.0f);
            blend = blend * blend * (3.0f - 2.0f * blend);  // Smoothstep
            y = y1 * (1.0f - blend) + y2 * blend;
        } else {
            y = y2;
        }
    } else if (T < 7000.0f) {
        // 中等色温：平滑混合
        float y1 = -0.9549476f * x3 - 1.37418593f * x2 + 2.09137015f * x - 0.16748867f;
        float y2 = -3.000f * x2 + 2.870f * x - 0.275f;
        
        if (T < 5000.0f) {
            // 4000K-5000K：平滑过渡
            float blend = (T - 4000.0f) / 1000.0f;
            blend = blend * blend * (3.0f - 2.0f * blend);  // Smoothstep
            y = y1 * (1.0f - blend) + y2 * blend;
        } else {
            y = y2;
        }
    } else {
        // 高色温：使用单一公式避免紫色偏移
        // 使用更温和的系数确保平滑过渡
        y = -2.400f * x2 + 2.600f * x - 0.239f;
    }
    
    // 确保在有效范围内
    x = std::clamp(x, 0.0f, 1.0f);
    y = std::clamp(y, 0.0f, 1.0f);
}

void ColorTemperature::xyToRGB(float x, float y, float& r, float& g, float& b) {
    // CIE xy 到 XYZ 的转换（假设 Y = 1）
    float Y = 1.0f;
    float X = (y > 0.0001f) ? (Y * x / y) : 0.0f;
    float Z = (y > 0.0001f) ? (Y * (1.0f - x - y) / y) : 0.0f;
    
    // XYZ 到 RGB 的转换（使用 sRGB/Rec.709 矩阵）
    r =  3.2406f * X - 1.5372f * Y - 0.4986f * Z;
    g = -0.9689f * X + 1.8758f * Y + 0.0415f * Z;
    b =  0.0557f * X - 0.2040f * Y + 1.0570f * Z;
}

void ColorTemperature::normalizeLuminance(float& r, float& g, float& b) {
    // 计算当前亮度
    float luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
    
    // 归一化以保持亮度为 1.0
    if (luminance > 0.0001f) {
        float scale = 1.0f / luminance;
        r *= scale;
        g *= scale;
        b *= scale;
    }
}

void ColorTemperature::calculateTemperatureScale(float temperatureShift,
                                                 float& rScale,
                                                 float& gScale,
                                                 float& bScale) {
    // 将 temperatureShift 从 [-100, 100] 映射到色温变化
    // 参考专业调色软件标准（DaVinci Resolve, Lightroom）
    // temperatureShift = 0 对应 D65 (6500K)
    // temperatureShift = -100 对应约 2000K（暖色，黄橙色）
    // temperatureShift = +100 对应约 10000K（冷色，蓝色）
    
    // 基准色温（D65 标准光源）
    const float baseTemp = 6500.0f;
    
    // 计算目标色温
    // 使用分段线性映射，参考 Adobe Lightroom 的色温曲线
    float targetTemp;
    if (temperatureShift < 0.0f) {
        // 负值：降低色温（更暖）
        // 从 6500K 线性映射到 2000K
        // 使用对数空间映射以获得更自然的感知过渡
        float t = -temperatureShift / 100.0f;  // 0 到 1
        
        // 在对数空间中线性插值
        float logBase = std::log(baseTemp);
        float logMin = std::log(2000.0f);
        float logTarget = logBase + t * (logMin - logBase);
        targetTemp = std::exp(logTarget);
    } else {
        // 正值：提高色温（更冷）
        // 从 6500K 线性映射到 10000K
        // 使用线性映射，因为高色温区域感知较为线性
        float t = temperatureShift / 100.0f;  // 0 到 1
        targetTemp = baseTemp + t * (10000.0f - baseTemp);
    }
    
    // 确保在有效范围内
    targetTemp = std::clamp(targetTemp, 1000.0f, 100000.0f);
    
    // 计算基准和目标的 CIE xy 坐标
    float baseX, baseY, targetX, targetY;
    temperatureToCIExy(baseTemp, baseX, baseY);
    temperatureToCIExy(targetTemp, targetX, targetY);
    
    // 转换到 RGB
    float baseR, baseG, baseB;
    float targetR, targetG, targetB;
    xyToRGB(baseX, baseY, baseR, baseG, baseB);
    xyToRGB(targetX, targetY, targetR, targetG, targetB);
    
    // 归一化亮度
    normalizeLuminance(baseR, baseG, baseB);
    normalizeLuminance(targetR, targetG, targetB);
    
    // 计算缩放因子
    rScale = (baseR > 0.0001f) ? (targetR / baseR) : 1.0f;
    gScale = (baseG > 0.0001f) ? (targetG / baseG) : 1.0f;
    bScale = (baseB > 0.0001f) ? (targetB / baseB) : 1.0f;
    
    // 限制缩放范围以避免极端值
    // 参考 Adobe Camera Raw 的限制范围
    rScale = std::clamp(rScale, 0.3f, 3.0f);
    gScale = std::clamp(gScale, 0.3f, 3.0f);
    bScale = std::clamp(bScale, 0.3f, 3.0f);
}

void ColorTemperature::calculateTintScale(float tintShift,
                                          float& rScale,
                                          float& gScale,
                                          float& bScale) {
    // 色调调整沿着绿-品红轴
    // tintShift = 0：无调整
    // tintShift < 0：增加绿色
    // tintShift > 0：增加品红（红+蓝）
    
    // 归一化参数
    float tint = tintShift / 100.0f;  // -1 到 +1
    
    // 计算缩放因子
    if (tint < 0.0f) {
        // 增加绿色
        rScale = 1.0f + tint * 0.3f;  // 减少红色
        gScale = 1.0f - tint * 0.5f;  // 增加绿色
        bScale = 1.0f + tint * 0.3f;  // 减少蓝色
    } else {
        // 增加品红
        rScale = 1.0f + tint * 0.4f;  // 增加红色
        gScale = 1.0f - tint * 0.5f;  // 减少绿色
        bScale = 1.0f + tint * 0.4f;  // 增加蓝色
    }
    
    // 确保在合理范围内
    rScale = std::clamp(rScale, 0.7f, 1.5f);
    gScale = std::clamp(gScale, 0.5f, 1.5f);
    bScale = std::clamp(bScale, 0.7f, 1.5f);
}

void ColorTemperature::applyColorTemperature(float& r, float& g, float& b,
                                             float temperatureShift,
                                             float tintShift) {
    // 如果没有调整，直接返回
    if (std::abs(temperatureShift) < 0.01f && std::abs(tintShift) < 0.01f) {
        return;
    }
    
    // 保存原始亮度
    float originalLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
    
    // 计算色温缩放因子
    float tempR = 1.0f, tempG = 1.0f, tempB = 1.0f;
    if (std::abs(temperatureShift) > 0.01f) {
        calculateTemperatureScale(temperatureShift, tempR, tempG, tempB);
    }
    
    // 计算色调缩放因子
    float tintR = 1.0f, tintG = 1.0f, tintB = 1.0f;
    if (std::abs(tintShift) > 0.01f) {
        calculateTintScale(tintShift, tintR, tintG, tintB);
    }
    
    // 组合缩放因子
    float totalR = tempR * tintR;
    float totalG = tempG * tintG;
    float totalB = tempB * tintB;
    
    // 应用缩放
    r *= totalR;
    g *= totalG;
    b *= totalB;
    
    // 恢复原始亮度（保持亮度不变）
    float newLuminance = 0.2126f * r + 0.7152f * g + 0.0722f * b;
    if (newLuminance > 0.0001f && originalLuminance > 0.0001f) {
        float luminanceScale = originalLuminance / newLuminance;
        r *= luminanceScale;
        g *= luminanceScale;
        b *= luminanceScale;
    }
    
    // 确保非负
    r = std::max(0.0f, r);
    g = std::max(0.0f, g);
    b = std::max(0.0f, b);
}

} // namespace filmtracker
