#include "film_engine.h"
#include <cmath>

namespace filmtracker {

/**
 * 应用颜色猜色/串扰矩阵
 * 
 * 模拟胶片银盐对光谱的误判：不同波长的光可能被错误的银盐层响应
 * 这是胶片与数字传感器的重要区别之一
 */
void FilmEngine::applyColorCrosstalk(LinearImage& image, const ColorCrosstalkMatrix& matrix) {
    const uint32_t pixelCount = image.width * image.height;
    
    #pragma omp parallel for
    for (uint32_t i = 0; i < pixelCount; ++i) {
        float r = image.r[i];
        float g = image.g[i];
        float b = image.b[i];
        
        // 应用 3x3 矩阵变换（非对角，允许颜色串扰）
        image.r[i] = matrix.matrix[0] * r + matrix.matrix[1] * g + matrix.matrix[2] * b;
        image.g[i] = matrix.matrix[3] * r + matrix.matrix[4] * g + matrix.matrix[5] * b;
        image.b[i] = matrix.matrix[6] * r + matrix.matrix[7] * g + matrix.matrix[8] * b;
        
        // 确保值在有效范围内
        image.r[i] = std::max(0.0f, image.r[i]);
        image.g[i] = std::max(0.0f, image.g[i]);
        image.b[i] = std::max(0.0f, image.b[i]);
    }
}

} // namespace filmtracker
