#include "dynamic_range_protection.h"
#include <cmath>
#include <algorithm>

namespace filmtracker {

float DynamicRangeProtection::hermiteSpline(float t, float p0, float p1, float m0, float m1) {
    // Hermite 样条基函数
    float t2 = t * t;
    float t3 = t2 * t;
    
    float h00 = 2.0f * t3 - 3.0f * t2 + 1.0f;  // 起始点权重
    float h10 = t3 - 2.0f * t2 + t;             // 起始切线权重
    float h01 = -2.0f * t3 + 3.0f * t2;         // 结束点权重
    float h11 = t3 - t2;                        // 结束切线权重
    
    return h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
}

float DynamicRangeProtection::softClip(float x, float threshold, float knee, float limit) {
    // 线性区域：x < threshold
    if (x < threshold) {
        return x;
    }
    
    // 过渡区域：threshold <= x < threshold + knee
    // 使用 Hermite 样条实现平滑过渡
    if (x < threshold + knee) {
        float t = (x - threshold) / knee;  // 归一化到 [0, 1]
        
        // 起始点：(threshold, threshold)，斜率为 1（线性延续）
        float p0 = threshold;
        float m0 = knee;  // 切线 = 斜率 * 区间长度
        
        // 结束点：进入 tanh 区域的起始点
        // 计算 tanh 在该点的值和斜率
        float tanhInput = 0.0f;  // tanh 区域从 0 开始
        float p1 = threshold + knee * 0.8f;  // 过渡到约 80% 的位置
        float m1 = knee * 0.2f;  // 斜率逐渐减小
        
        return hermiteSpline(t, p0, p1, m0, m1);
    }
    
    // 渐近线区域：x >= threshold + knee
    // 使用 tanh 函数实现渐近线裁剪，永不完全裁剪
    float excess = x - (threshold + knee);
    float scale = (limit - threshold - knee * 0.8f) * 0.5f;  // tanh 的缩放因子
    float tanhValue = std::tanh(excess / scale);
    
    return threshold + knee * 0.8f + scale * tanhValue;
}

float DynamicRangeProtection::highlightRolloff(float value, float amount) {
    if (amount <= 0.0f) {
        return value;
    }
    
    // 高光压缩：对高亮区域应用软裁剪
    // amount 控制压缩的强度和起始点
    
    // 根据 amount 调整阈值和 knee
    float threshold = 0.8f - amount * 0.3f;  // amount 越大，越早开始压缩
    float knee = 0.15f + amount * 0.1f;      // amount 越大，过渡区域越宽
    float limit = 1.0f;
    
    // 应用软裁剪
    float compressed = softClip(value, threshold, knee, limit);
    
    // 根据 amount 混合原始值和压缩值
    return value + amount * (compressed - value);
}

float DynamicRangeProtection::shadowLift(float value, float amount) {
    if (amount <= 0.0f) {
        return value;
    }
    
    // 阴影提升：提升暗部区域的亮度
    // 使用平滑的权重函数，避免噪声放大
    
    // 计算阴影权重（暗部权重更高）
    // 使用平滑的三次函数
    float shadowThreshold = 0.3f;
    float weight;
    
    if (value < shadowThreshold) {
        // 暗部区域：使用平滑的权重曲线
        float t = value / shadowThreshold;
        weight = 1.0f - t * t * (3.0f - 2.0f * t);  // smoothstep
    } else {
        // 亮部区域：权重为 0
        weight = 0.0f;
    }
    
    // 计算提升量
    // 使用对数曲线提升，避免线性提升导致的不自然效果
    float lift = amount * weight * 0.3f;  // 最大提升 30%
    
    // 应用提升，使用加法而非乘法（保持细节）
    float lifted = value + lift * (1.0f - value);  // 避免超过 1.0
    
    return lifted;
}

} // namespace filmtracker
