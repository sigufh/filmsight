#ifndef PARALLEL_PROCESSOR_H
#define PARALLEL_PROCESSOR_H

#include "raw_types.h"
#include "basic_adjustment_params.h"

using namespace filmtracker;

/**
 * 并行图像处理器
 * 使用多线程 + ARM NEON SIMD 优化
 */
class ParallelProcessor {
public:
    ParallelProcessor();
    ~ParallelProcessor() = default;
    
    /**
     * 并行处理图像
     * @param input 输入线性图像
     * @param output 输出线性图像
     * @param params 调整参数
     */
    void process(
        const LinearImage& input,
        LinearImage& output,
        const BasicAdjustmentParams& params
    );
    
    /**
     * 获取线程数
     */
    int getNumThreads() const { return numThreads; }
    
private:
    int numThreads;
    
    /**
     * 处理图像块（单个线程）
     */
    void processBlock(
        const LinearImage& input,
        LinearImage& output,
        const BasicAdjustmentParams& params,
        int startRow,
        int endRow
    );
    
    /**
     * 使用 SIMD 处理单行
     */
    void processRowWithSIMD(
        const LinearImage& input,
        LinearImage& output,
        const BasicAdjustmentParams& params,
        int y
    );
    
    /**
     * 标量处理单个像素（用于剩余像素）
     */
    void processPixelScalar(
        const LinearImage& input,
        LinearImage& output,
        const BasicAdjustmentParams& params,
        int x,
        int y
    );
};

#endif // PARALLEL_PROCESSOR_H
