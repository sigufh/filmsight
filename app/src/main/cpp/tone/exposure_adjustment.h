#ifndef FILMTRACKER_EXPOSURE_ADJUSTMENT_H
#define FILMTRACKER_EXPOSURE_ADJUSTMENT_H

namespace filmtracker {

/**
 * 曝光调整模块
 * 
 * 提供高质量的曝光调整，保护高光和暗部细节。
 * 使用动态范围压缩和自适应调整策略。
 */
class ExposureAdjustment {
public:
    /**
     * 应用曝光调整（带高光保护）
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     * @param exposureEV 曝光值（EV，-5.0 到 +5.0）
     */
    static void applyExposure(float& r, float& g, float& b, float exposureEV);
    
    /**
     * 应用曝光到单个值（带保护）
     * 
     * @param value 输入值
     * @param exposureEV 曝光值（EV）
     * @return 调整后的值
     */
    static float applyExposureToValue(float value, float exposureEV);

private:
    /**
     * 高光压缩函数
     * 当曝光增加时，压缩高光区域以保留细节
     */
    static float compressHighlights(float value, float amount);
    
    /**
     * 暗部提升函数
     * 当曝光增加时，适度提升暗部以保持对比度
     */
    static float liftShadows(float value, float amount);
};

} // namespace filmtracker

#endif // FILMTRACKER_EXPOSURE_ADJUSTMENT_H
