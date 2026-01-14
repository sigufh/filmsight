#ifndef FILMTRACKER_BASIC_ADJUSTMENT_PARAMS_H
#define FILMTRACKER_BASIC_ADJUSTMENT_PARAMS_H

#include <cstring>

namespace filmtracker {

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

/**
 * 曲线参数（动态控制点）
 */
struct ToneCurveParams {
    // 动态控制点数据
    struct CurveData {
        int pointCount;
        float* xCoords;  // X 坐标数组
        float* yCoords;  // Y 坐标数组
        bool enabled;
        
        CurveData() : pointCount(0), xCoords(nullptr), yCoords(nullptr), enabled(false) {}
        
        ~CurveData() {
            if (xCoords) delete[] xCoords;
            if (yCoords) delete[] yCoords;
        }
        
        // 拷贝构造函数
        CurveData(const CurveData& other) : pointCount(other.pointCount), enabled(other.enabled) {
            if (other.pointCount > 0 && other.xCoords && other.yCoords) {
                xCoords = new float[other.pointCount];
                yCoords = new float[other.pointCount];
                std::memcpy(xCoords, other.xCoords, other.pointCount * sizeof(float));
                std::memcpy(yCoords, other.yCoords, other.pointCount * sizeof(float));
            } else {
                xCoords = nullptr;
                yCoords = nullptr;
            }
        }
        
        // 赋值运算符
        CurveData& operator=(const CurveData& other) {
            if (this != &other) {
                if (xCoords) delete[] xCoords;
                if (yCoords) delete[] yCoords;
                
                pointCount = other.pointCount;
                enabled = other.enabled;
                
                if (other.pointCount > 0 && other.xCoords && other.yCoords) {
                    xCoords = new float[other.pointCount];
                    yCoords = new float[other.pointCount];
                    std::memcpy(xCoords, other.xCoords, other.pointCount * sizeof(float));
                    std::memcpy(yCoords, other.yCoords, other.pointCount * sizeof(float));
                } else {
                    xCoords = nullptr;
                    yCoords = nullptr;
                }
            }
            return *this;
        }
        
        // 设置控制点
        void setPoints(int count, const float* x, const float* y) {
            if (xCoords) delete[] xCoords;
            if (yCoords) delete[] yCoords;
            
            pointCount = count;
            if (count > 0 && x && y) {
                xCoords = new float[count];
                yCoords = new float[count];
                std::memcpy(xCoords, x, count * sizeof(float));
                std::memcpy(yCoords, y, count * sizeof(float));
            } else {
                xCoords = nullptr;
                yCoords = nullptr;
            }
        }
    };
    
    CurveData rgbCurve;
    CurveData redCurve;
    CurveData greenCurve;
    CurveData blueCurve;
    
    ToneCurveParams() {}
};

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
    
    // 颜色调整
    float temperature;      // 色温（-100 到 +100）
    float tint;             // 色调（-100 到 +100）
    
    // 分级调整
    float gradingHighlightsTemp;
    float gradingHighlightsTint;
    float gradingMidtonesTemp;
    float gradingMidtonesTint;
    float gradingShadowsTemp;
    float gradingShadowsTint;
    float gradingBlending;
    float gradingBalance;
    
    // 效果调整
    float texture;          // 纹理（-100 到 +100）
    float dehaze;           // 去雾（-100 到 +100）
    float vignette;         // 晕影（-100 到 +100）
    float grain;            // 颗粒（0 到 100）
    
    // 细节调整
    float sharpening;       // 锐化（0 到 100）
    float noiseReduction;   // 降噪（0 到 100）
    
    // 曲线参数（指针，延迟初始化）
    ToneCurveParams* curveParams;
    
    // HSL 参数（指针，延迟初始化）
    HSLParams* hslParams;
    
    BasicAdjustmentParams()
        : globalExposure(0.0f),
          contrast(1.0f),
          saturation(1.0f),
          highlights(0.0f),
          shadows(0.0f),
          whites(0.0f),
          blacks(0.0f),
          clarity(0.0f),
          vibrance(0.0f),
          temperature(0.0f),
          tint(0.0f),
          gradingHighlightsTemp(0.0f),
          gradingHighlightsTint(0.0f),
          gradingMidtonesTemp(0.0f),
          gradingMidtonesTint(0.0f),
          gradingShadowsTemp(0.0f),
          gradingShadowsTint(0.0f),
          gradingBlending(50.0f),
          gradingBalance(0.0f),
          texture(0.0f),
          dehaze(0.0f),
          vignette(0.0f),
          grain(0.0f),
          sharpening(0.0f),
          noiseReduction(0.0f),
          curveParams(nullptr),
          hslParams(nullptr) {}
    
    ~BasicAdjustmentParams() {
        if (curveParams) {
            delete curveParams;
            curveParams = nullptr;
        }
        if (hslParams) {
            delete hslParams;
            hslParams = nullptr;
        }
    }
};

} // namespace filmtracker

#endif // FILMTRACKER_BASIC_ADJUSTMENT_PARAMS_H
