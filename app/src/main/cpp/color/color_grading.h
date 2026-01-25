#ifndef FILMTRACKER_COLOR_GRADING_H
#define FILMTRACKER_COLOR_GRADING_H

#include "raw_types.h"

namespace filmtracker {

/**
 * 色彩分级模块
 * 
 * 实现高质量的三区域色彩分级（高光、中间调、阴影）
 * 使用高斯权重函数实现平滑过渡
 * 参考 Adobe Camera RAW / DaVinci Resolve 的色彩分级算法
 */
class ColorGrading {
public:
    /**
     * 色彩分级参数
     */
    struct GradingParams {
        // 高光色彩调整（RGB 偏移，-1.0 到 +1.0）
        float highlightR = 0.0f;
        float highlightG = 0.0f;
        float highlightB = 0.0f;
        
        // 中间调色彩调整（RGB 偏移，-1.0 到 +1.0）
        float midtoneR = 0.0f;
        float midtoneG = 0.0f;
        float midtoneB = 0.0f;
        
        // 阴影色彩调整（RGB 偏移，-1.0 到 +1.0）
        float shadowR = 0.0f;
        float shadowG = 0.0f;
        float shadowB = 0.0f;
        
        // 整体强度控制（0.0 到 1.0）
        float blending = 1.0f;
        
        // 区域边界调整（-1.0 到 +1.0）
        // 负值：扩大阴影区域，缩小高光区域
        // 正值：扩大高光区域，缩小阴影区域
        float balance = 0.0f;
    };
    
    /**
     * 应用色彩分级
     * 
     * @param image 输入/输出图像（就地修改）
     * @param params 分级参数
     */
    static void applyGrading(LinearImage& image, const GradingParams& params);
    
    /**
     * 计算高斯权重
     * 
     * 使用高斯函数计算三个区域的权重，确保平滑过渡
     * 
     * @param luminance 亮度值（0.0 到 1.0）
     * @param balance 区域边界调整（-1.0 到 +1.0）
     * @param shadowWeight 输出：阴影权重
     * @param midtoneWeight 输出：中间调权重
     * @param highlightWeight 输出：高光权重
     */
    static void calculateGaussianWeights(float luminance, 
                                         float balance,
                                         float& shadowWeight,
                                         float& midtoneWeight,
                                         float& highlightWeight);
    
    /**
     * 将 RGB 转换到 LMS 色彩空间
     * 
     * LMS 是基于人眼锥细胞响应的色彩空间，
     * 在此空间中进行色彩分级可以获得更自然的效果
     * 
     * @param r 红色通道（线性）
     * @param g 绿色通道（线性）
     * @param b 蓝色通道（线性）
     * @param l 输出：L 通道（长波）
     * @param m 输出：M 通道（中波）
     * @param s 输出：S 通道（短波）
     */
    static void rgbToLMS(float r, float g, float b, float& l, float& m, float& s);
    
    /**
     * 将 LMS 转换回 RGB 色彩空间
     * 
     * @param l L 通道（长波）
     * @param m M 通道（中波）
     * @param s S 通道（短波）
     * @param r 输出：红色通道（线性）
     * @param g 输出：绿色通道（线性）
     * @param b 输出：蓝色通道（线性）
     */
    static void lmsToRGB(float l, float m, float s, float& r, float& g, float& b);
    
private:
    /**
     * 高斯函数
     * 
     * @param x 输入值
     * @param center 中心位置
     * @param width 宽度（标准差）
     * @return 高斯权重（0.0 到 1.0）
     */
    static float gaussian(float x, float center, float width);
};

} // namespace filmtracker

#endif // FILMTRACKER_COLOR_GRADING_H
