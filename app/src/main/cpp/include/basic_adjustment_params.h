#ifndef FILMTRACKER_BASIC_ADJUSTMENT_PARAMS_H
#define FILMTRACKER_BASIC_ADJUSTMENT_PARAMS_H

namespace filmtracker {

/**
 * 基础调整参数（独立于胶片模拟）
 * 对应 Adobe Camera RAW / Lightroom 的基础面板
 */
struct BasicAdjustmentParams {
    // 全局调整
    float globalExposure;   // 曝光（EV，-5 到 +5）
    float contrast;         // 对比度（0.5 到 2.0，1.0 为不变）
    float saturation;       // 饱和度（0.0 到 2.0，1.0 为不变）
    
    // 色调调整
    float highlights;       // 高光（-100 到 +100）
    float shadows;          // 阴影（-100 到 +100）
    float whites;           // 白场（-100 到 +100）
    float blacks;           // 黑场（-100 到 +100）
    
    // 存在感调整
    float clarity;          // 清晰度（-100 到 +100）
    float vibrance;         // 自然饱和度（-100 到 +100）
    
    BasicAdjustmentParams()
        : globalExposure(0.0f),
          contrast(1.0f),
          saturation(1.0f),
          highlights(0.0f),
          shadows(0.0f),
          whites(0.0f),
          blacks(0.0f),
          clarity(0.0f),
          vibrance(0.0f) {}
};

/**
 * 曲线参数
 */
struct ToneCurveParams {
    float rgbCurve[16];
    float redCurve[16];
    float greenCurve[16];
    float blueCurve[16];
    
    bool enableRgbCurve;
    bool enableRedCurve;
    bool enableGreenCurve;
    bool enableBlueCurve;
    
    ToneCurveParams() {
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

/**
 * HSL 调整参数
 */
struct HSLParams {
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

} // namespace filmtracker

#endif // FILMTRACKER_BASIC_ADJUSTMENT_PARAMS_H
