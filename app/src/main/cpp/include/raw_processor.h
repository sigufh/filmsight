#ifndef FILMTRACKER_RAW_PROCESSOR_H
#define FILMTRACKER_RAW_PROCESSOR_H

#include "raw_types.h"
#include <vector>
#include <cstdint>
#include <fstream>

namespace filmtracker {

/**
 * RAW 处理器
 * 
 * 职责：
 * 1. 读取 RAW/DNG 文件
 * 2. 黑电平校正
 * 3. 白电平归一化
 * 4. Bayer 去马赛克
 * 5. 输出线性 RGB（线性光域）
 */
class RawProcessor {
public:
    RawProcessor();
    ~RawProcessor();
    
    /**
     * 从文件路径加载 RAW 图像
     * 
     * @param filePath RAW/DNG 文件路径
     * @param metadata 输出的元数据
     * @return 线性 RGB 图像（线性光域，未应用 Gamma）
     */
    LinearImage loadRaw(const char* filePath, RawMetadata& metadata);
    
    /**
     * 从内存缓冲区加载 RAW 图像
     */
    LinearImage loadRawFromBuffer(const uint8_t* buffer, 
                                  size_t bufferSize,
                                  RawMetadata& metadata);
    
    /**
     * 应用黑电平校正
     */
    void applyBlackLevel(std::vector<uint16_t>& rawData, 
                        float blackLevel, 
                        uint32_t width, 
                        uint32_t height);
    
    /**
     * Bayer 去马赛克（使用归一化的float数据）
     * 
     * @param normalizedRawData 归一化的 Bayer 数据（float，0-1范围）
     * @param width 图像宽度
     * @param height 图像高度
     * @param cfaPattern CFA 模式（0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR）
     * @return 线性 RGB 图像
     */
    LinearImage demosaicBayerNormalized(const std::vector<float>& normalizedRawData,
                                       uint32_t width,
                                       uint32_t height,
                                       uint32_t cfaPattern);
};

} // namespace filmtracker

#endif // FILMTRACKER_RAW_PROCESSOR_H
