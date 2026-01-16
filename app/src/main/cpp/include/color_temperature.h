#ifndef FILMTRACKER_COLOR_TEMPERATURE_H
#define FILMTRACKER_COLOR_TEMPERATURE_H

namespace filmtracker {

/**
 * 色温调整模块
 * 
 * 实现基于 Planckian Locus（黑体辐射曲线）的精确色温调整。
 * 支持 1000K 到 100000K 的色温范围（符合专业调色软件标准），
 * 使用 CIE xy 色度空间进行计算。
 * 
 * 参考标准：
 * - DaVinci Resolve: 1000K ~ 10000K（基础）/ 无上限（手动输入）
 * - Adobe Lightroom/ACR: 2000K ~ 10000K（滑块）/ 1000K ~ 100000K（手动）
 * - Capture One: 1800K ~ 10000K（滑块）/ 800K ~ 20000K+（手动）
 */
class ColorTemperature {
public:
    /**
     * 色温到 CIE xy 色度坐标的转换
     * 
     * 使用 Planckian Locus 公式计算黑体辐射的色度坐标。
     * 
     * 算法基于：
     * - Hernández-Andrés et al. (1999) 的改进多项式近似
     * - 适用于 1000K 到 100000K 的范围（专业调色软件标准）
     * - 分段多项式以提高高色温区域的精度
     * 
     * @param temperature 色温（开尔文，1000-100000）
     * @param x 输出：CIE x 色度坐标
     * @param y 输出：CIE y 色度坐标
     */
    static void temperatureToCIExy(float temperature, float& x, float& y);
    
    /**
     * 计算色温调整的 RGB 缩放因子
     * 
     * 根据色温变化计算 RGB 通道的缩放因子。
     * 
     * @param temperatureShift 色温偏移（-100 到 +100）
     * @param rScale 输出：红色通道缩放因子
     * @param gScale 输出：绿色通道缩放因子
     * @param bScale 输出：蓝色通道缩放因子
     */
    static void calculateTemperatureScale(float temperatureShift, 
                                          float& rScale, 
                                          float& gScale, 
                                          float& bScale);
    
    /**
     * 计算色调调整的 RGB 缩放因子
     * 
     * 色调调整沿着绿-品红轴进行。
     * 
     * @param tintShift 色调偏移（-100 到 +100）
     * @param rScale 输出：红色通道缩放因子
     * @param gScale 输出：绿色通道缩放因子
     * @param bScale 输出：蓝色通道缩放因子
     */
    static void calculateTintScale(float tintShift,
                                   float& rScale,
                                   float& gScale,
                                   float& bScale);
    
    /**
     * 应用色温和色调调整
     * 
     * 同时应用色温和色调调整，保持亮度不变。
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     * @param temperatureShift 色温偏移（-100 到 +100）
     * @param tintShift 色调偏移（-100 到 +100）
     */
    static void applyColorTemperature(float& r, float& g, float& b,
                                     float temperatureShift,
                                     float tintShift);

private:
    /**
     * CIE xy 到 RGB 的转换矩阵计算
     * 
     * 计算从 CIE xy 色度坐标到 RGB 的转换。
     * 
     * @param x CIE x 色度坐标
     * @param y CIE y 色度坐标
     * @param r 输出：红色分量
     * @param g 输出：绿色分量
     * @param b 输出：蓝色分量
     */
    static void xyToRGB(float x, float y, float& r, float& g, float& b);
    
    /**
     * 归一化 RGB 值以保持亮度
     * 
     * @param r 红色通道（输入/输出）
     * @param g 绿色通道（输入/输出）
     * @param b 蓝色通道（输入/输出）
     */
    static void normalizeLuminance(float& r, float& g, float& b);
};

} // namespace filmtracker

#endif // FILMTRACKER_COLOR_TEMPERATURE_H
