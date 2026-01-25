#ifndef FILMTRACKER_BILATERAL_FILTER_OPTIMIZER_H
#define FILMTRACKER_BILATERAL_FILTER_OPTIMIZER_H

#include "raw_types.h"
#include <cstdint>

namespace filmtracker {

/**
 * 双边滤波器优化策略选择器
 * 
 * 根据图像尺寸、参数和设备能力自动选择最优的实现方式：
 * - STANDARD_CPU: 标准 CPU 多线程实现
 * - FAST_APPROXIMATION: 快速近似算法（降采样 + 标准滤波 + 上采样）
 * - GPU_VULKAN: GPU 加速实现（使用 Vulkan compute shader）
 * 
 * 决策规则：
 * 1. 如果 GPU 可用且图像 > 2MP，使用 GPU_VULKAN
 * 2. 否则，如果 spatialSigma > 5.0，使用 FAST_APPROXIMATION
 * 3. 否则，使用 STANDARD_CPU
 */
class BilateralFilterOptimizer {
public:
    /**
     * 实现方式枚举
     */
    enum class Implementation {
        STANDARD_CPU,        // 标准 CPU 多线程实现
        FAST_APPROXIMATION,  // 快速近似算法
        GPU_VULKAN          // GPU 加速（Vulkan）
    };
    
    /**
     * 选择最优实现方式
     * 
     * 根据图像尺寸、参数和设备能力自动选择最优的实现方式
     * 
     * @param width 图像宽度
     * @param height 图像高度
     * @param spatialSigma 空间域标准差
     * @param rangeSigma 强度域标准差
     * @param enableFastApproximation 是否启用快速近似算法
     * @param enableGPU 是否启用 GPU 加速
     * @return 选择的实现方式
     */
    static Implementation selectImplementation(
        uint32_t width,
        uint32_t height,
        float spatialSigma,
        float rangeSigma,
        bool enableFastApproximation = true,
        bool enableGPU = true
    );
    
    /**
     * 执行双边滤波（自动选择实现）
     * 
     * 根据 selectImplementation 的结果调用相应的实现
     * 
     * @param input 输入图像
     * @param output 输出图像（必须预先分配）
     * @param spatialSigma 空间域标准差
     * @param rangeSigma 强度域标准差
     * @param hint 实现方式提示（可选，如果提供则优先使用）
     * @param enableFastApproximation 是否启用快速近似算法
     * @param enableGPU 是否启用 GPU 加速
     * @return 实际使用的实现方式
     */
    static Implementation execute(
        const LinearImage& input,
        LinearImage& output,
        float spatialSigma,
        float rangeSigma,
        Implementation hint = Implementation::STANDARD_CPU,
        bool enableFastApproximation = true,
        bool enableGPU = true
    );
    
    /**
     * 获取实现方式的名称（用于日志）
     * 
     * @param impl 实现方式
     * @return 实现方式的字符串名称
     */
    static const char* getImplementationName(Implementation impl);
    
private:
    // 决策阈值常量
    static constexpr uint32_t SMALL_IMAGE_PIXELS = 500000;   // 0.5MP
    static constexpr uint32_t LARGE_IMAGE_PIXELS = 2000000;  // 2MP
    static constexpr float LARGE_SPATIAL_SIGMA = 5.0f;
    
    /**
     * 检查 GPU 是否可用
     * 
     * @return true 如果 GPU 已初始化且可用
     */
    static bool isGPUAvailable();
    
    /**
     * 执行标准 CPU 实现
     */
    static void executeStandardCPU(
        const LinearImage& input,
        LinearImage& output,
        float spatialSigma,
        float rangeSigma
    );
    
    /**
     * 执行快速近似算法
     */
    static void executeFastApproximation(
        const LinearImage& input,
        LinearImage& output,
        float spatialSigma,
        float rangeSigma
    );
    
    /**
     * 执行 GPU 加速
     * 
     * @return true 如果成功，false 如果失败（需要回退）
     */
    static bool executeGPU(
        const LinearImage& input,
        LinearImage& output,
        float spatialSigma,
        float rangeSigma
    );
};

} // namespace filmtracker

#endif // FILMTRACKER_BILATERAL_FILTER_OPTIMIZER_H
