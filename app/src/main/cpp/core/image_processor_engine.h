#ifndef FILMTRACKER_IMAGE_PROCESSOR_ENGINE_H
#define FILMTRACKER_IMAGE_PROCESSOR_ENGINE_H

#include "raw_types.h"
#include "basic_adjustment_params.h"

namespace filmtracker {

/**
 * 图像处理引擎 - 纯粹的基础调整
 * 
 * 遵循 Adobe Camera RAW / Lightroom 的工作流程：
 * 1. 导入：只解析和显示，不应用任何调整
 * 2. 调整：每个模块独立，用户主动控制
 * 3. 非破坏性：所有调整只修改参数，不修改原始数据
 * 
 * 注意：此引擎只包含基础调整功能，不包含任何胶片模拟效果
 */
class ImageProcessorEngine {
public:
    ImageProcessorEngine();
    ~ImageProcessorEngine();
    
    // ========== 基础调整模块 ==========
    
    /**
     * 应用基础调整：曝光、对比度、饱和度
     * 这些是最基础的调整，直接作用于线性 RGB 数据
     * 
     * @param image 输入/输出图像（就地修改）
     * @param exposure 曝光调整（EV，-5 到 +5）
     * @param contrast 对比度（0.5 到 2.0，1.0 为不变）
     * @param saturation 饱和度（0.0 到 2.0，1.0 为不变）
     */
    void applyBasicAdjustments(LinearImage& image, 
                              float exposure, 
                              float contrast, 
                              float saturation);
    
    /**
     * 应用高级色调调整：高光、阴影、白场、黑场
     * 类似 Lightroom 的 Tone 面板
     * 
     * @param image 输入/输出图像
     * @param highlights 高光调整（-100 到 +100）
     * @param shadows 阴影调整（-100 到 +100）
     * @param whites 白场调整（-100 到 +100）
     * @param blacks 黑场调整（-100 到 +100）
     */
    void applyToneAdjustments(LinearImage& image,
                             float highlights,
                             float shadows,
                             float whites,
                             float blacks);
    
    /**
     * 应用清晰度和自然饱和度
     * 
     * @param image 输入/输出图像
     * @param clarity 清晰度（-100 到 +100）
     * @param vibrance 自然饱和度（-100 到 +100）
     */
    void applyPresence(LinearImage& image,
                      float clarity,
                      float vibrance);
    
    // ========== 色调曲线模块 ==========
    
    /**
     * 应用色调曲线
     * 
     * @param image 输入/输出图像
     * @param curveParams 曲线参数
     */
    void applyToneCurves(LinearImage& image, const ToneCurveParams& curveParams);
    
    // ========== HSL 调整模块 ==========
    
    /**
     * 应用 HSL 调整
     * 
     * @param image 输入/输出图像
     * @param hslParams HSL 参数
     */
    void applyHSL(LinearImage& image, const HSLParams& hslParams);
    
    // ========== 颜色调整模块 ==========
    
    /**
     * 应用颜色调整（色温、色调、分级）
     * 
     * @param image 输入/输出图像
     * @param params 调整参数
     */
    void applyColorAdjustments(LinearImage& image, const BasicAdjustmentParams& params);
    
    // ========== 效果模块 ==========
    
    /**
     * 应用效果（纹理、去雾、晕影、颗粒）
     * 
     * @param image 输入/输出图像
     * @param params 调整参数
     */
    void applyEffects(LinearImage& image, const BasicAdjustmentParams& params);
    
    // ========== 细节模块 ==========
    
    /**
     * 应用细节（锐化、降噪）
     * 
     * @param image 输入/输出图像
     * @param params 调整参数
     */
    void applyDetails(LinearImage& image, const BasicAdjustmentParams& params);
    
private:
    // 曲线相关辅助函数
    void buildLUTFromControlPoints(const ToneCurveParams::CurveData& curveData, float* lut, int lutSize) const;
    float interpolateHermiteSpline(const float* xCoords, const float* yCoords, int pointCount, float x) const;
    float applyLUT(const float* lut, int lutSize, float x) const;
    
    // HSL 相关辅助函数
    void rgbToHSL(float r, float g, float b, float& h, float& s, float& l) const;
    void hslToRGB(float h, float s, float l, float& r, float& g, float& b) const;
    int getHueSegment(float hue) const;
};

} // namespace filmtracker

#endif // FILMTRACKER_IMAGE_PROCESSOR_ENGINE_H
