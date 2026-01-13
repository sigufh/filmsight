#include "film_engine.h"
#include <algorithm>
#include <cmath>

namespace filmtracker {

FilmEngine::FilmEngine() {
    // 初始化引擎
}

FilmEngine::~FilmEngine() {
    // 清理资源
}

/**
 * 在亮度通道上应用基础色调曲线，并按比例回写到 RGB
 */
void FilmEngine::applyBasicTone(LinearImage& image, const BasicToneParams& toneParams) {
    const uint32_t pixelCount = image.width * image.height;

    // 如果所有参数都是 0，则直接返回
    if (std::fabs(toneParams.highlights) < 1e-5f &&
        std::fabs(toneParams.shadows)   < 1e-5f &&
        std::fabs(toneParams.whites)    < 1e-5f &&
        std::fabs(toneParams.blacks)    < 1e-5f &&
        std::fabs(toneParams.clarity)   < 1e-5f &&
        std::fabs(toneParams.vibrance)  < 1e-5f) {
        return;
    }

    // 简化：不在这里做真正的空间域清晰度滤波，只做亮度曲线 + 自然饱和度
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];

        // 计算亮度（在线性域）
        float luminance = 0.299f * r + 0.587f * g + 0.114f * b;

        // 应用基础色调曲线
        float newLuminance = applyToneCurve(luminance, toneParams);

        // 避免除零
        float eps = 1e-5f;
        float scale = (luminance > eps) ? (newLuminance / luminance) : 1.0f;

        // 对 RGB 按比例缩放，保持色相
        r *= scale;
        g *= scale;
        b *= scale;

        // 自然饱和度调整（vibrance）
        if (std::fabs(toneParams.vibrance) > 1e-5f) {
            float maxC = std::max(r, std::max(g, b));
            float minC = std::min(r, std::min(g, b));
            float saturation = (maxC > 0.0f) ? (maxC - minC) / maxC : 0.0f;

            // 低饱和区域提升更多，高饱和区域提升更少
            float vib = toneParams.vibrance; // [-1,1]
            float factor = 1.0f + vib * (1.0f - saturation);

            float avg = (r + g + b) / 3.0f;
            r = avg + (r - avg) * factor;
            g = avg + (g - avg) * factor;
            b = avg + (b - avg) * factor;
        }

        image.r[i] = std::max(0.0f, r);
        image.g[i] = std::max(0.0f, g);
        image.b[i] = std::max(0.0f, b);
    }
}

/**
 * 对单个亮度值应用基础色调曲线
 *
 * 设计目标：
 * - highlights：对高亮区域做压缩（肩部更软），避免死白
 * - shadows：对暗部做抬升，保留层次
 * - whites / blacks：整体移动白场 / 黑场
 *
 * 所有操作都在线性域完成，使用平滑的分段函数而非 LUT。
 */
float FilmEngine::applyToneCurve(float luminance, const BasicToneParams& toneParams) const {
    float x = std::max(0.0f, std::min(1.0f, luminance));

    auto smoothstep = [](float edge0, float edge1, float v) {
        float t = std::max(0.0f, std::min(1.0f, (v - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    };

    float result = x;

    // 高光调整：在亮度 > 0.6 区域压缩或提升
    if (std::fabs(toneParams.highlights) > 1e-5f) {
        float h = toneParams.highlights; // [-1,1]
        float w = smoothstep(0.5f, 1.0f, x);
        // 正值：压缩高光，负值：推高光
        float target = (h > 0.0f) ? (x - h * w * (x - 0.5f)) : (x - h * w * (1.0f - x));
        result = result + (target - result) * w;
    }

    // 阴影调整：在亮度 < 0.4 区域抬升或压暗
    if (std::fabs(toneParams.shadows) > 1e-5f) {
        float s = toneParams.shadows; // [-1,1]
        float w = 1.0f - smoothstep(0.2f, 0.6f, x);
        float target = (s > 0.0f) ? (x + s * w * (0.5f - x)) : (x + s * w * x);
        result = result + (target - result) * w;
    }

    // 白场调整：整体抬高或压低亮部
    if (std::fabs(toneParams.whites) > 1e-5f) {
        float wAdj = toneParams.whites; // [-1,1]
        float w = smoothstep(0.5f, 1.0f, x);
        result += wAdj * w * 0.2f;
    }

    // 黑场调整：整体抬升或压低暗部
    if (std::fabs(toneParams.blacks) > 1e-5f) {
        float bAdj = toneParams.blacks; // [-1,1]
        float w = 1.0f - smoothstep(0.0f, 0.4f, x);
        result += bAdj * w * 0.2f;
    }

    // 限制在 [0,1]
    result = std::max(0.0f, std::min(1.0f, result));
    return result;
}

/**
 * 主处理流程：应用完整的胶片模拟
 * 
 * 处理顺序（重要）：
 * 1. 颜色猜色（在线性域）
 * 2. 非线性响应曲线
 * 3. 颗粒（参与曝光）
 * 4. 基础色调调整（高光/阴影/白/黑/自然饱和度）
 */
LinearImage FilmEngine::process(const LinearImage& input, 
                               const FilmParams& params,
                               const RawMetadata& metadata) {
    // 创建输出图像副本
    LinearImage output(input.width, input.height);
    output.r = input.r;
    output.g = input.g;
    output.b = input.b;
    
    // 步骤 1: 应用颜色猜色矩阵（在线性域）
    applyColorCrosstalk(output, params.crosstalk);
    
    // 步骤 2: 应用非线性响应曲线（通道独立）
    applyResponseCurve(output, params);
    
    // 步骤 3: 应用颗粒（参与曝光，在线性域）
    applyGrain(output, params.grain, metadata);

    // 步骤 4: 应用基础色调调整
    applyBasicTone(output, params.basicTone);
    
    // 步骤 5: 应用色调曲线
    applyToneCurves(output, params.toneCurve);
    
    // 步骤 6: 应用 HSL 调整
    applyHSL(output, params.hsl);
    
    // 应用全局饱和度（在响应曲线和基础色调之后）
    if (params.saturation != 1.0f) {
        const uint32_t pixelCount = output.width * output.height;
        for (uint32_t i = 0; i < pixelCount; ++i) {
            float luminance = 0.299f * output.r[i] + 
                             0.587f * output.g[i] + 
                             0.114f * output.b[i];
            output.r[i] = luminance + (output.r[i] - luminance) * params.saturation;
            output.g[i] = luminance + (output.g[i] - luminance) * params.saturation;
            output.b[i] = luminance + (output.b[i] - luminance) * params.saturation;
        }
    }
    
    return output;
}

} // namespace filmtracker
