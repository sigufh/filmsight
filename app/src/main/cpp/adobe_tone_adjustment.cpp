#include "adobe_tone_adjustment.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

float AdobeToneAdjustment::rgbToLuminance(float r, float g, float b) {
    // 使用 Rec.709 系数计算相对亮度
    return 0.2126f * r + 0.7152f * g + 0.0722f * b;
}

float AdobeToneAdjustment::luminanceToLstar(float Y) {
    // CIE L* 转换
    // 参考：CIE 1976 L*a*b* 色彩空间
    
    const float epsilon = 0.008856f;  // (6/29)^3
    const float kappa = 903.3f;       // (29/3)^3
    
    if (Y <= epsilon) {
        return kappa * Y;
    } else {
        return 116.0f * std::pow(Y, 1.0f / 3.0f) - 16.0f;
    }
}

float AdobeToneAdjustment::lstarToLuminance(float Lstar) {
    // CIE L* 逆转换
    
    const float epsilon = 0.008856f;
    const float kappa = 903.3f;
    
    if (Lstar <= kappa * epsilon) {
        return Lstar / kappa;
    } else {
        float temp = (Lstar + 16.0f) / 116.0f;
        return temp * temp * temp;
    }
}

float AdobeToneAdjustment::cubicSplineWeight(float x, float center, float width) {
    // 三次样条权重函数
    // 在 [center - width, center + width] 范围内从 0 平滑过渡到 1
    
    float t = (x - center) / width;
    t = std::clamp(t, -1.0f, 1.0f);
    
    // 使用 smoothstep 函数（三次 Hermite 插值）
    // 将 [-1, 1] 映射到 [0, 1]
    float normalized = (t + 1.0f) * 0.5f;
    return normalized * normalized * (3.0f - 2.0f * normalized);
}

float AdobeToneAdjustment::highlightWeight(float Lstar) {
    // 高光权重：L* > 70 时影响最大
    // 使用三次样条在 50-90 范围内平滑过渡
    
    if (Lstar < 50.0f) {
        return 0.0f;
    } else if (Lstar > 90.0f) {
        return 1.0f;
    } else {
        // 在 50-90 之间平滑过渡
        float t = (Lstar - 50.0f) / 40.0f;
        return t * t * (3.0f - 2.0f * t);  // smoothstep
    }
}

float AdobeToneAdjustment::shadowWeight(float Lstar) {
    // 阴影权重：L* < 30 时影响最大
    // 使用三次样条在 10-50 范围内平滑过渡
    
    if (Lstar > 50.0f) {
        return 0.0f;
    } else if (Lstar < 10.0f) {
        return 1.0f;
    } else {
        // 在 10-50 之间平滑过渡（反向）
        float t = (Lstar - 10.0f) / 40.0f;
        return 1.0f - t * t * (3.0f - 2.0f * t);  // 1 - smoothstep
    }
}

float AdobeToneAdjustment::whiteWeight(float Lstar) {
    // 白场权重：L* > 80 时影响最大
    // 使用三次样条在 60-95 范围内平滑过渡
    
    if (Lstar < 60.0f) {
        return 0.0f;
    } else if (Lstar > 95.0f) {
        return 1.0f;
    } else {
        // 在 60-95 之间平滑过渡
        float t = (Lstar - 60.0f) / 35.0f;
        return t * t * (3.0f - 2.0f * t);  // smoothstep
    }
}

float AdobeToneAdjustment::blackWeight(float Lstar) {
    // 黑场权重：L* < 20 时影响最大
    // 使用三次样条在 5-40 范围内平滑过渡
    
    if (Lstar > 40.0f) {
        return 0.0f;
    } else if (Lstar < 5.0f) {
        return 1.0f;
    } else {
        // 在 5-40 之间平滑过渡（反向）
        float t = (Lstar - 5.0f) / 35.0f;
        return 1.0f - t * t * (3.0f - 2.0f * t);  // 1 - smoothstep
    }
}

void AdobeToneAdjustment::applyToneAdjustments(float& r, float& g, float& b,
                                               float highlights, float shadows,
                                               float whites, float blacks) {
    // 计算当前像素的感知亮度
    float Y = rgbToLuminance(r, g, b);
    float Lstar = luminanceToLstar(Y);
    
    // 计算各区域的权重
    float wHighlights = highlightWeight(Lstar);
    float wShadows = shadowWeight(Lstar);
    float wWhites = whiteWeight(Lstar);
    float wBlacks = blackWeight(Lstar);
    
    // 将调整参数从 [-100, 100] 归一化到合适的范围
    // 高光和阴影：[-1, 1] 范围，影响较大
    // 白场和黑场：[-0.5, 0.5] 范围，影响较小
    float highlightAdj = highlights / 100.0f;
    float shadowAdj = shadows / 100.0f;
    float whiteAdj = whites / 200.0f;  // 减半以获得更细腻的控制
    float blackAdj = blacks / 200.0f;
    
    // 计算总的亮度调整量（在 L* 空间中）
    float LstarAdjustment = 0.0f;
    
    // 高光调整：负值压暗高光，正值提亮高光
    LstarAdjustment += wHighlights * highlightAdj * 30.0f;
    
    // 阴影调整：正值提亮阴影，负值压暗阴影
    LstarAdjustment += wShadows * shadowAdj * 30.0f;
    
    // 白场调整：影响最亮区域
    LstarAdjustment += wWhites * whiteAdj * 20.0f;
    
    // 黑场调整：影响最暗区域
    LstarAdjustment += wBlacks * blackAdj * 20.0f;
    
    // 应用调整
    if (std::abs(LstarAdjustment) > 0.001f) {
        // 调整 L*
        float newLstar = Lstar + LstarAdjustment;
        newLstar = std::clamp(newLstar, 0.0f, 100.0f);
        
        // 转换回线性亮度
        float newY = lstarToLuminance(newLstar);
        
        // 计算缩放因子
        float scale = (Y > 0.0001f) ? (newY / Y) : 1.0f;
        
        // 应用到 RGB 通道（保持色相和饱和度）
        r *= scale;
        g *= scale;
        b *= scale;
    }
}

} // namespace filmtracker
