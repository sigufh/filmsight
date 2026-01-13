#ifndef FILMTRACKER_FILM_ENGINE_H
#define FILMTRACKER_FILM_ENGINE_H

#include "raw_types.h"
#include "film_params.h"

namespace filmtracker {

/**
 * 胶片银盐模拟引擎
 * 
 * 核心思想：模拟胶片成像的物理过程，而非结果映射
 * 1. 颜色猜色（非对角矩阵）
 * 2. 非线性响应曲线（Toe/Linear/Shoulder）
 * 3. 颗粒模型（泊松统计，参与曝光）
 */
class FilmEngine {
public:
    FilmEngine();
    ~FilmEngine();
    
    /**
     * 处理线性 RAW 图像，应用胶片模拟
     * 
     * @param input 输入的线性 RGB 图像（线性光域）
     * @param params 胶片参数
     * @param metadata RAW 元数据（用于颗粒 ISO 耦合）
     * @return 处理后的线性图像（仍在线性域，但已应用胶片响应）
     */
    LinearImage process(const LinearImage& input, 
                       const FilmParams& params,
                       const RawMetadata& metadata);
    
    /**
     * 应用颜色猜色矩阵
     * 模拟胶片银盐对光谱的误判
     */
    void applyColorCrosstalk(LinearImage& image, const ColorCrosstalkMatrix& matrix);
    
    /**
     * 应用通道独立的非线性响应曲线
     */
    void applyResponseCurve(LinearImage& image, const FilmParams& params);
    
    /**
     * 应用颗粒模型（泊松统计，参与曝光）
     */
    void applyGrain(LinearImage& image, 
                   const GrainParams& grainParams,
                   const RawMetadata& metadata);

    /**
     * 应用基础色调调整（高光 / 阴影 / 白场 / 黑场 / 清晰度 / 自然饱和度）
     * 在线性域的亮度通道上工作，再映射回 RGB
     */
    void applyBasicTone(LinearImage& image, const BasicToneParams& toneParams);
    
    /**
     * 应用色调曲线（RGB 曲线 + 单通道曲线）
     */
    void applyToneCurves(LinearImage& image, const ToneCurveParams& curveParams);
    
    /**
     * 应用 HSL 调整（按色相分段）
     */
    void applyHSL(LinearImage& image, const HSLParams& hslParams);
    
private:
    /**
     * 计算单个通道的非线性响应
     * 实现 Toe / Linear / Shoulder 三段曲线
     */
    float computeResponse(float linearValue, const ChannelResponseParams& params);
    
    /**
     * 生成泊松分布的颗粒值
     */
    float generatePoissonGrain(float mean, float variation);

    /**
     * 对单个亮度值应用基础色调曲线
     */
    float applyToneCurve(float luminance, const BasicToneParams& toneParams) const;
    
    /**
     * 插值曲线值（使用 Catmull-Rom 样条）
     */
    float interpolateCurve(const float* curve, int numPoints, float x) const;
    
    /**
     * RGB 转 HSL
     */
    void rgbToHSL(float r, float g, float b, float& h, float& s, float& l) const;
    
    /**
     * HSL 转 RGB
     */
    void hslToRGB(float h, float s, float l, float& r, float& g, float& b) const;
    
    /**
     * 获取色相段索引（0-7）
     */
    int getHueSegment(float hue) const;
};

} // namespace filmtracker

#endif // FILMTRACKER_FILM_ENGINE_H
