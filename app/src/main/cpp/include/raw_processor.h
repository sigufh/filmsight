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
     * 应用白电平归一化
     */
    void normalizeWhiteLevel(std::vector<float>& linearData,
                            float whiteLevel,
                            uint32_t pixelCount);
    
    /**
     * Bayer 去马赛克（AHD 算法）
     * 
     * @param rawData 原始 Bayer 数据
     * @param width 图像宽度
     * @param height 图像高度
     * @param cfaPattern CFA 模式（0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR）
     * @return 线性 RGB 图像
     */
    LinearImage demosaicBayer(const std::vector<uint16_t>& rawData,
                             uint32_t width,
                             uint32_t height,
                             uint32_t cfaPattern);
    
private:
    /**
     * 解析 DNG 标签（简化版）
     */
    void parseDngTags(const uint8_t* buffer, size_t size, RawMetadata& metadata);
    
    /**
     * AHD 去马赛克核心算法
     */
    void demosaicAHD(const std::vector<uint16_t>& rawData,
                    LinearImage& output,
                    uint32_t width,
                    uint32_t height,
                    uint32_t cfaPattern);
    
    /**
     * 加载ARW文件（Sony ARW格式）
     */
    LinearImage loadArwFile(std::ifstream& file, RawMetadata& metadata);
    
    /**
     * 解析TIFF IFD并提取EXIF信息
     */
    void parseTiffIfd(std::ifstream& file, 
                      uint32_t ifdOffset, 
                      bool isLittleEndian,
                      size_t fileSize,
                      RawMetadata& metadata);
    
    /**
     * 读取TIFF标签值（支持不同数据类型）
     */
    uint32_t readTiffValue(std::ifstream& file,
                          uint16_t dataType,
                          uint32_t count,
                          uint32_t valueOffset,
                          bool isLittleEndian,
                          size_t fileSize);
    
    /**
     * 读取TIFF字符串值
     */
    void readTiffString(std::ifstream& file,
                       uint32_t valueOffset,
                       uint32_t count,
                       bool isLittleEndian,
                       char* buffer,
                       size_t bufferSize);
    
    /**
     * 读取ARW文件的RAW数据条带
     */
    bool readArwRawData(std::ifstream& file,
                       uint32_t stripOffset,
                       uint32_t stripByteCount,
                       uint32_t width,
                       uint32_t height,
                       uint16_t bitsPerSample,
                       bool isLittleEndian,
                       std::vector<uint16_t>& rawData);
    
    /**
     * 解析ARW文件的RAW数据位置（StripOffsets和StripByteCounts）
     */
    bool findArwRawDataLocation(std::ifstream& file,
                               uint32_t ifdOffset,
                               bool isLittleEndian,
                               size_t fileSize,
                               uint32_t& stripOffset,
                               uint32_t& stripByteCount,
                               uint32_t& width,
                               uint32_t& height,
                               uint16_t& bitsPerSample);
};

} // namespace filmtracker

#endif // FILMTRACKER_RAW_PROCESSOR_H
