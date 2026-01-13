#ifndef FILMTRACKER_FILM_PARAMS_H
#define FILMTRACKER_FILM_PARAMS_H

namespace filmtracker {

// 胶片响应曲线参数（每个通道独立）
struct ChannelResponseParams {
    // Toe 区域（暗部抬升）
    float toeSlope;      // Toe 斜率
    float toeStrength;   // Toe 强度
    float toePoint;      // Toe 起始点
    
    // Linear 区域（中间调）
    float linearSlope;   // 线性斜率
    float linearOffset;  // 线性偏移
    
    // Shoulder 区域（高光压缩）
    float shoulderSlope;    // Shoulder 斜率
    float shoulderStrength; // Shoulder 强度
    float shoulderPoint;    // Shoulder 起始点
    
    // 整体曝光调整
    float exposureOffset;   // 曝光偏移（EV）
};

// 全局基础色调参数（在线性域的亮度通道上工作）
// 对应类似 LR 的 Basic 面板：高光 / 阴影 / 白场 / 黑场 / 清晰度 / 自然饱和度
struct BasicToneParams {
    // 取值建议范围 [-1, 1]，0 为不调整
    float highlights;   // 高光压缩（>0 压高光，<0 提高光）
    float shadows;      // 阴影提升（>0 提暗部，<0 压暗部）
    float whites;       // 白场（影响亮度上端的整体偏移）
    float blacks;       // 黑场（影响亮度下端的整体偏移）
    float clarity;      // 清晰度（局部对比度，在线性域近似实现）
    float vibrance;     // 自然饱和度（偏向低饱和区域）

    BasicToneParams()
        : highlights(0.0f),
          shadows(0.0f),
          whites(0.0f),
          blacks(0.0f),
          clarity(0.0f),
          vibrance(0.0f) {}
};

// 曲线参数（RGB 曲线 + 单通道曲线）
// 使用控制点插值，而非 LUT
struct ToneCurveParams {
    // RGB 总曲线控制点（16个点，均匀分布）
    float rgbCurve[16];  // 0.0-1.0 映射到 0.0-1.0
    
    // 单通道曲线控制点
    float redCurve[16];
    float greenCurve[16];
    float blueCurve[16];
    
    bool enableRgbCurve;
    bool enableRedCurve;
    bool enableGreenCurve;
    bool enableBlueCurve;
    
    ToneCurveParams() {
        // 默认线性曲线（y=x）
        for (int i = 0; i < 16; ++i) {
            float t = i / 15.0f;
            rgbCurve[i] = t;
            redCurve[i] = t;
            greenCurve[i] = t;
            blueCurve[i] = t;
        }
        enableRgbCurve = false;
        enableRedCurve = false;
        enableGreenCurve = false;
        enableBlueCurve = false;
    }
};

// HSL 调整参数（按色相分段）
// 8 个色相段：红、橙、黄、绿、青、蓝、紫、品红
struct HSLParams {
    // 每个色相段的调整 [hue_shift, saturation, luminance]
    // hue_shift: [-180, 180] 度
    // saturation: [-100, 100] %
    // luminance: [-100, 100] %
    float hueShift[8];
    float saturation[8];
    float luminance[8];
    
    bool enableHSL;
    
    HSLParams() {
        for (int i = 0; i < 8; ++i) {
            hueShift[i] = 0.0f;
            saturation[i] = 0.0f;
            luminance[i] = 0.0f;
        }
        enableHSL = false;
    }
};

// 颜色猜色/串扰矩阵（3x3，非对角）
// 模拟胶片银盐对光谱的误判
struct ColorCrosstalkMatrix {
    float matrix[9];  // [R->R, G->R, B->R, R->G, G->G, B->G, R->B, G->B, B->B]
    
    ColorCrosstalkMatrix() {
        // 默认单位矩阵（无串扰）
        matrix[0] = 1.0f; matrix[1] = 0.0f; matrix[2] = 0.0f;
        matrix[3] = 0.0f; matrix[4] = 1.0f; matrix[5] = 0.0f;
        matrix[6] = 0.0f; matrix[7] = 0.0f; matrix[8] = 1.0f;
    }
};

// 颗粒参数（基于泊松统计）
struct GrainParams {
    float baseDensity;      // 基础颗粒密度
    float isoMultiplier;    // ISO 倍增系数
    float sizeVariation;    // 颗粒大小变化
    float colorCoupling;    // 颜色耦合强度
    bool enableGrain;       // 是否启用颗粒
};

// 完整胶片参数集
struct FilmParams {
    ChannelResponseParams redChannel;
    ChannelResponseParams greenChannel;
    ChannelResponseParams blueChannel;
    
    ColorCrosstalkMatrix crosstalk;
    GrainParams grain;

    // 全局基础色调参数（在胶片响应之后、导出之前应用）
    BasicToneParams basicTone;
    
    // 曲线调整
    ToneCurveParams toneCurve;
    
    // HSL 调整
    HSLParams hsl;
    
    // 全局调整
    float globalExposure;   // 全局曝光（EV）
    float contrast;         // 对比度
    float saturation;       // 饱和度（在猜色后应用）
    
    FilmParams() {
        // 默认参数（模拟 Portra 400 风格）
        globalExposure = 0.0f;
        contrast = 1.0f;
        saturation = 1.0f;
        
        // 默认响应曲线（软肩部，轻微 Toe）
        setupDefaultResponse();
    }
    
private:
    void setupDefaultResponse() {
        // Red channel
        redChannel.toeSlope = 0.3f;
        redChannel.toeStrength = 0.15f;
        redChannel.toePoint = 0.05f;
        redChannel.linearSlope = 1.0f;
        redChannel.linearOffset = 0.0f;
        redChannel.shoulderSlope = 0.4f;
        redChannel.shoulderStrength = 0.8f;
        redChannel.shoulderPoint = 0.7f;
        redChannel.exposureOffset = 0.0f;
        
        // Green channel (similar)
        greenChannel = redChannel;
        greenChannel.toeStrength = 0.12f;
        
        // Blue channel (slightly different)
        blueChannel = redChannel;
        blueChannel.shoulderStrength = 0.75f;
        
        // 轻微颜色串扰（模拟真实胶片）
        crosstalk.matrix[1] = 0.05f;  // G -> R
        crosstalk.matrix[3] = 0.03f;  // R -> G
        crosstalk.matrix[7] = 0.04f;  // G -> B
        
        // 颗粒参数
        grain.baseDensity = 0.02f;
        grain.isoMultiplier = 1.0f;
        grain.sizeVariation = 0.3f;
        grain.colorCoupling = 0.5f;
        grain.enableGrain = true;

        // 基础色调默认不做额外调整
        basicTone = BasicToneParams();
    }
};

} // namespace filmtracker

#endif // FILMTRACKER_FILM_PARAMS_H
