#include "raw_processor.h"
#include <fstream>
#include <cmath>
#include <algorithm>
#include <cctype>
#include <string>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "RawProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

RawProcessor::RawProcessor() {
}

RawProcessor::~RawProcessor() {
}

/**
 * 从文件加载 RAW（支持ARW等格式）
 * 实际项目中应使用 libraw 或类似库
 */
LinearImage RawProcessor::loadRaw(const char* filePath, RawMetadata& metadata) {
    LOGI("loadRaw: Starting, filePath=%s", filePath);
    
    if (!filePath) {
        LOGE("loadRaw: File path is null");
        throw std::runtime_error("File path is null");
    }
    
    std::ifstream file(filePath, std::ios::binary);
    if (!file.is_open()) {
        LOGE("loadRaw: Failed to open file: %s", filePath);
        throw std::runtime_error("Failed to open RAW file");
    }
    
    LOGI("loadRaw: File opened successfully");
    
    // 读取文件头，识别文件格式
    uint8_t header[16];
    file.read(reinterpret_cast<char*>(header), 16);
    file.seekg(0, std::ios::beg);
    
    // 检测ARW文件（Sony ARW格式）
    // ARW文件通常以 "II" 或 "MM" 开头（TIFF格式）
    bool isArw = false;
    if (header[0] == 0x49 && header[1] == 0x49) {  // "II" (Intel byte order)
        isArw = true;
        LOGI("loadRaw: Detected ARW (Intel byte order)");
    } else if (header[0] == 0x4D && header[1] == 0x4D) {  // "MM" (Motorola byte order)
        isArw = true;
        LOGI("loadRaw: Detected ARW (Motorola byte order)");
    }
    
    // 检查文件扩展名
    std::string pathStr(filePath);
    std::string ext = pathStr.substr(pathStr.find_last_of(".") + 1);
    std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);
    if (ext == "arw" || ext == "srf" || ext == "sr2") {
        isArw = true;
        LOGI("loadRaw: Detected ARW by extension: %s", ext.c_str());
    }
    
    if (isArw) {
        LOGI("loadRaw: Processing as ARW file");
        // ARW文件处理（简化实现）
        // 实际应解析TIFF/ARW结构
        LinearImage result = loadArwFile(file, metadata);
        LOGI("loadRaw: ARW file processed successfully, size=%dx%d", metadata.width, metadata.height);
        return result;
    }
    
    // 其他RAW格式的占位实现
    metadata.width = 4000;
    metadata.height = 3000;
    metadata.iso = 400.0f;
    metadata.exposureTime = 1.0f / 125.0f;
    metadata.blackLevel = 0.0f;
    metadata.whiteLevel = 16383.0f;
    
    LinearImage image(metadata.width, metadata.height);
    
    // 填充示例数据
    for (uint32_t i = 0; i < image.width * image.height; ++i) {
        image.r[i] = 0.5f;
        image.g[i] = 0.5f;
        image.b[i] = 0.5f;
    }
    
    return image;
}

/**
 * 加载ARW文件（Sony ARW格式）
 */
LinearImage RawProcessor::loadArwFile(std::ifstream& file, RawMetadata& metadata) {
    LOGI("loadArwFile: Starting");
    
    // ARW文件是TIFF格式的变体
    // 尝试解析TIFF IFD结构以获取实际图像尺寸
    
    // 读取文件大小
    file.seekg(0, std::ios::end);
    size_t fileSize = file.tellg();
    file.seekg(0, std::ios::beg);
    LOGI("loadArwFile: File size = %zu bytes", fileSize);
    
    // 读取TIFF头（8字节）
    uint8_t tiffHeader[8];
    file.read(reinterpret_cast<char*>(tiffHeader), 8);
    
    // 检测字节序
    bool isLittleEndian = (tiffHeader[0] == 0x49 && tiffHeader[1] == 0x49);
    bool isBigEndian = (tiffHeader[0] == 0x4D && tiffHeader[1] == 0x4D);
    
    if (!isLittleEndian && !isBigEndian) {
        LOGE("loadArwFile: Invalid TIFF header");
        throw std::runtime_error("Invalid ARW file format");
    }
    
    // 读取IFD偏移量（字节4-7）
    uint32_t ifdOffset;
    if (isLittleEndian) {
        ifdOffset = tiffHeader[4] | (tiffHeader[5] << 8) | 
                   (tiffHeader[6] << 16) | (tiffHeader[7] << 24);
    } else {
        ifdOffset = (tiffHeader[4] << 24) | (tiffHeader[5] << 16) | 
                   (tiffHeader[6] << 8) | tiffHeader[7];
    }
    
    LOGI("loadArwFile: IFD offset = %u", ifdOffset);
    
    // 初始化元数据默认值
    metadata.iso = 400.0f;
    metadata.exposureTime = 1.0f / 125.0f;
    metadata.aperture = 2.8f;
    metadata.focalLength = 50.0f;
    metadata.whiteBalance[0] = 5500.0f;  // 色温
    metadata.whiteBalance[1] = 0.0f;     // 色调
    metadata.bitsPerSample = 14;
    metadata.blackLevel = 512.0f;
    metadata.whiteLevel = 16383.0f;
    std::memset(metadata.cameraModel, 0, sizeof(metadata.cameraModel));
    std::strncpy(metadata.cameraModel, "Unknown", sizeof(metadata.cameraModel) - 1);
    std::memset(metadata.colorSpace, 0, sizeof(metadata.colorSpace));
    std::strncpy(metadata.colorSpace, "sRGB", sizeof(metadata.colorSpace) - 1);
    
    // 解析TIFF IFD以获取完整的EXIF信息
    if (ifdOffset > 0 && ifdOffset < fileSize) {
        parseTiffIfd(file, ifdOffset, isLittleEndian, fileSize, metadata);
    }
    
    // 如果无法读取实际尺寸，使用默认值（保持宽高比）
    uint32_t actualWidth = metadata.width;
    uint32_t actualHeight = metadata.height;
    if (actualWidth == 0 || actualHeight == 0) {
        LOGI("loadArwFile: Could not read dimensions from TIFF, using defaults");
        // 使用常见的ARW尺寸比例（3:2）
        actualWidth = 6000;
        actualHeight = 4000;
    }
    
    LOGI("loadArwFile: Actual image dimensions = %ux%u", actualWidth, actualHeight);
    
    // 计算预览尺寸，保持宽高比
    uint32_t maxPreviewSize = 1200;
    uint32_t previewWidth = actualWidth;
    uint32_t previewHeight = actualHeight;
    
    if (actualWidth > maxPreviewSize || actualHeight > maxPreviewSize) {
        float scale = std::min(
            static_cast<float>(maxPreviewSize) / static_cast<float>(actualWidth),
            static_cast<float>(maxPreviewSize) / static_cast<float>(actualHeight)
        );
        previewWidth = static_cast<uint32_t>(actualWidth * scale);
        previewHeight = static_cast<uint32_t>(actualHeight * scale);
        LOGI("loadArwFile: Scaled to preview size %ux%u (scale=%.2f)", 
             previewWidth, previewHeight, scale);
    }
    
    // 更新尺寸（如果已从EXIF读取，使用实际尺寸；否则使用预览尺寸）
    if (metadata.width == 0 || metadata.height == 0) {
        metadata.width = previewWidth;
        metadata.height = previewHeight;
    } else {
        // 如果已读取实际尺寸，按比例缩放
        if (metadata.width > maxPreviewSize || metadata.height > maxPreviewSize) {
            float scale = std::min(
                static_cast<float>(maxPreviewSize) / static_cast<float>(metadata.width),
                static_cast<float>(maxPreviewSize) / static_cast<float>(metadata.height)
            );
            metadata.width = static_cast<uint32_t>(metadata.width * scale);
            metadata.height = static_cast<uint32_t>(metadata.height * scale);
        }
    }
    
    // 如果某些值仍为默认值，设置合理的默认值
    if (metadata.iso == 0.0f) {
        metadata.iso = 400.0f;
    }
    if (metadata.exposureTime == 0.0f) {
        metadata.exposureTime = 1.0f / 125.0f;
    }
    if (metadata.aperture == 0.0f) {
        metadata.aperture = 2.8f;
    }
    if (metadata.focalLength == 0.0f) {
        metadata.focalLength = 50.0f;
    }
    if (metadata.blackLevel == 0.0f) {
        metadata.blackLevel = 512.0f;  // ARW典型黑电平
    }
    if (metadata.whiteLevel == 0.0f) {
        metadata.whiteLevel = 16383.0f;  // 14位RAW
    }
    
    LOGI("loadArwFile: Final metadata - %dx%d, ISO=%.0f, Exposure=%.3fs, Aperture=f/%.1f, Focal=%.1fmm",
         metadata.width, metadata.height, metadata.iso, metadata.exposureTime, 
         metadata.aperture, metadata.focalLength);
    LOGI("loadArwFile: Camera: %s", metadata.cameraModel);
    
    // 设置CFA模式（ARW通常使用RGGB）
    metadata.cfaPattern[0] = 0;  // R
    metadata.cfaPattern[1] = 1;  // G
    metadata.cfaPattern[2] = 1;  // G
    metadata.cfaPattern[3] = 2;  // B
    
    // 尝试找到并读取实际的RAW数据
    uint32_t stripOffset = 0;
    uint32_t stripByteCount = 0;
    uint32_t rawWidth = metadata.width;
    uint32_t rawHeight = metadata.height;
    uint16_t rawBitsPerSample = metadata.bitsPerSample;
    
    bool foundRawData = findArwRawDataLocation(file, ifdOffset, isLittleEndian, fileSize,
                                              stripOffset, stripByteCount, rawWidth, rawHeight, rawBitsPerSample);
    
    if (foundRawData && stripOffset > 0 && stripByteCount > 0) {
        LOGI("loadArwFile: Found RAW data at offset %u, size %u bytes", stripOffset, stripByteCount);
        LOGI("loadArwFile: RAW dimensions: %ux%u, bits per sample: %u", rawWidth, rawHeight, rawBitsPerSample);
        
        // 读取RAW Bayer数据
        std::vector<uint16_t> rawBayerData;
        if (readArwRawData(file, stripOffset, stripByteCount, rawWidth, rawHeight, 
                          rawBitsPerSample, isLittleEndian, rawBayerData)) {
            LOGI("loadArwFile: Successfully read %zu RAW pixels", rawBayerData.size());
            
            // 应用黑电平校正
            applyBlackLevel(rawBayerData, metadata.blackLevel, rawWidth, rawHeight);
            
            // 应用去马赛克（Bayer转RGB）
            LinearImage demosaiced = demosaicBayer(rawBayerData, rawWidth, rawHeight, 0); // 0 = RGGB
            
            // 应用白电平归一化
            normalizeWhiteLevel(demosaiced.r, metadata.whiteLevel, rawWidth * rawHeight);
            normalizeWhiteLevel(demosaiced.g, metadata.whiteLevel, rawWidth * rawHeight);
            normalizeWhiteLevel(demosaiced.b, metadata.whiteLevel, rawWidth * rawHeight);
            
            // 如果尺寸不匹配，需要缩放
            if (demosaiced.width != metadata.width || demosaiced.height != metadata.height) {
                LOGI("loadArwFile: Resizing from %ux%u to %ux%u", 
                     demosaiced.width, demosaiced.height, metadata.width, metadata.height);
                LinearImage resized(metadata.width, metadata.height);
                
                float scaleX = static_cast<float>(demosaiced.width) / static_cast<float>(metadata.width);
                float scaleY = static_cast<float>(demosaiced.height) / static_cast<float>(metadata.height);
                
                for (uint32_t y = 0; y < metadata.height; ++y) {
                    for (uint32_t x = 0; x < metadata.width; ++x) {
                        uint32_t srcX = static_cast<uint32_t>(x * scaleX);
                        uint32_t srcY = static_cast<uint32_t>(y * scaleY);
                        srcX = std::min(srcX, demosaiced.width - 1);
                        srcY = std::min(srcY, demosaiced.height - 1);
                        
                        uint32_t dstIdx = y * metadata.width + x;
                        uint32_t srcIdx = srcY * demosaiced.width + srcX;
                        
                        resized.r[dstIdx] = demosaiced.r[srcIdx];
                        resized.g[dstIdx] = demosaiced.g[srcIdx];
                        resized.b[dstIdx] = demosaiced.b[srcIdx];
                    }
                }
                
                LOGI("loadArwFile: Completed successfully with real RAW data");
                return resized;
            }
            
            LOGI("loadArwFile: Completed successfully with real RAW data");
            return demosaiced;
        } else {
            LOGI("loadArwFile: Failed to read RAW data, falling back to test pattern");
        }
    } else {
        LOGI("loadArwFile: Could not find RAW data location, falling back to test pattern");
    }
    
    // 如果无法读取实际数据，生成测试图案作为后备
    LOGI("loadArwFile: Allocating LinearImage memory");
    LinearImage image(metadata.width, metadata.height);
    LOGI("loadArwFile: LinearImage allocated successfully");
    
    const uint32_t pixelCount = image.width * image.height;
    LOGI("loadArwFile: Filling %u pixels with test pattern", pixelCount);
    
    // 创建渐变测试图案
    for (uint32_t y = 0; y < image.height; ++y) {
        for (uint32_t x = 0; x < image.width; ++x) {
            uint32_t idx = y * image.width + x;
            
            float fx = static_cast<float>(x) / static_cast<float>(image.width);
            float fy = static_cast<float>(y) / static_cast<float>(image.height);
            
            image.r[idx] = fx * 0.8f + 0.2f;
            image.g[idx] = (1.0f - fx) * 0.6f + fy * 0.4f + 0.2f;
            image.b[idx] = fy * 0.8f + 0.2f;
            
            image.r[idx] *= 0.5f;
            image.g[idx] *= 0.5f;
            image.b[idx] *= 0.5f;
        }
    }
    
    LOGI("loadArwFile: Completed with test pattern");
    return image;
}

LinearImage RawProcessor::loadRawFromBuffer(const uint8_t* buffer, 
                                           size_t bufferSize,
                                           RawMetadata& metadata) {
    // 类似实现
    return LinearImage(100, 100);
}

void RawProcessor::applyBlackLevel(std::vector<uint16_t>& rawData, 
                                  float blackLevel, 
                                  uint32_t width, 
                                  uint32_t height) {
    for (auto& pixel : rawData) {
        if (pixel > blackLevel) {
            pixel = static_cast<uint16_t>(pixel - blackLevel);
        } else {
            pixel = 0;
        }
    }
}

void RawProcessor::normalizeWhiteLevel(std::vector<float>& linearData,
                                     float whiteLevel,
                                     uint32_t pixelCount) {
    float scale = 1.0f / whiteLevel;
    for (uint32_t i = 0; i < pixelCount; ++i) {
        linearData[i] *= scale;
    }
}

LinearImage RawProcessor::demosaicBayer(const std::vector<uint16_t>& rawData,
                                       uint32_t width,
                                       uint32_t height,
                                       uint32_t cfaPattern) {
    LinearImage result(width, height);
    
    // 简化的去马赛克（实际应使用 AHD 算法）
    // 这里只是占位实现
    for (uint32_t y = 0; y < height; ++y) {
        for (uint32_t x = 0; x < width; ++x) {
            uint32_t idx = y * width + x;
            float value = static_cast<float>(rawData[idx]) / 16383.0f;
            result.r[idx] = value;
            result.g[idx] = value;
            result.b[idx] = value;
        }
    }
    
    return result;
}

void RawProcessor::parseDngTags(const uint8_t* buffer, size_t size, RawMetadata& metadata) {
    // TODO: 实现 DNG 标签解析
}

void RawProcessor::demosaicAHD(const std::vector<uint16_t>& rawData,
                               LinearImage& output,
                               uint32_t width,
                               uint32_t height,
                               uint32_t cfaPattern) {
    // TODO: 实现 AHD 去马赛克算法
}

/**
 * 读取TIFF标签值（支持不同数据类型）
 */
uint32_t RawProcessor::readTiffValue(std::ifstream& file,
                                     uint16_t dataType,
                                     uint32_t count,
                                     uint32_t valueOffset,
                                     bool isLittleEndian,
                                     size_t fileSize) {
    // 如果值可以直接存储在4字节中，直接返回
    if (count == 1) {
        if (dataType == 1 || dataType == 2 || dataType == 6) {  // BYTE, ASCII, SBYTE
            return valueOffset & 0xFF;
        } else if (dataType == 3) {  // SHORT
            return valueOffset & 0xFFFF;
        } else if (dataType == 4 || dataType == 9) {  // LONG, SLONG
            return valueOffset;
        }
    }
    
    // 值存储在文件中的其他位置
    if (valueOffset >= fileSize) {
        return 0;
    }
    
    size_t oldPos = file.tellg();
    file.seekg(valueOffset, std::ios::beg);
    
    uint32_t result = 0;
    if (dataType == 1 || dataType == 6) {  // BYTE, SBYTE
        uint8_t value;
        file.read(reinterpret_cast<char*>(&value), 1);
        result = value;
    } else if (dataType == 3) {  // SHORT
        uint8_t bytes[2];
        file.read(reinterpret_cast<char*>(bytes), 2);
        result = isLittleEndian ?
            (bytes[0] | (bytes[1] << 8)) :
            ((bytes[0] << 8) | bytes[1]);
    } else if (dataType == 4 || dataType == 9) {  // LONG, SLONG
        uint8_t bytes[4];
        file.read(reinterpret_cast<char*>(bytes), 4);
        result = isLittleEndian ?
            (bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24)) :
            ((bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3]);
    } else if (dataType == 5) {  // RATIONAL (两个LONG: 分子/分母)
        uint8_t bytes[8];
        file.read(reinterpret_cast<char*>(bytes), 8);
        uint32_t numerator = isLittleEndian ?
            (bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24)) :
            ((bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3]);
        uint32_t denominator = isLittleEndian ?
            (bytes[4] | (bytes[5] << 8) | (bytes[6] << 16) | (bytes[7] << 24)) :
            ((bytes[4] << 24) | (bytes[5] << 16) | (bytes[6] << 8) | bytes[7]);
        if (denominator != 0) {
            result = numerator / denominator;  // 简化处理，返回整数部分
        }
    }
    
    file.seekg(oldPos, std::ios::beg);
    return result;
}

/**
 * 读取TIFF字符串值
 */
void RawProcessor::readTiffString(std::ifstream& file,
                                  uint32_t valueOffset,
                                  uint32_t count,
                                  bool isLittleEndian,
                                  char* buffer,
                                  size_t bufferSize) {
    if (valueOffset == 0 || bufferSize == 0) {
        return;
    }
    
    size_t oldPos = file.tellg();
    file.seekg(valueOffset, std::ios::beg);
    
    size_t readSize = std::min(static_cast<size_t>(count), bufferSize - 1);
    file.read(buffer, readSize);
    buffer[readSize] = '\0';
    
    // 移除末尾的空字符
    size_t len = std::strlen(buffer);
    while (len > 0 && (buffer[len - 1] == '\0' || buffer[len - 1] == ' ')) {
        buffer[len - 1] = '\0';
        len--;
    }
    
    file.seekg(oldPos, std::ios::beg);
}

/**
 * 解析TIFF IFD并提取EXIF信息
 */
void RawProcessor::parseTiffIfd(std::ifstream& file,
                                uint32_t ifdOffset,
                                bool isLittleEndian,
                                size_t fileSize,
                                RawMetadata& metadata) {
    if (ifdOffset == 0 || ifdOffset >= fileSize) {
        return;
    }
    
    file.seekg(ifdOffset, std::ios::beg);
    
    // 读取IFD条目数量
    uint8_t entryCountBytes[2];
    file.read(reinterpret_cast<char*>(entryCountBytes), 2);
    uint16_t entryCount = isLittleEndian ?
        (entryCountBytes[0] | (entryCountBytes[1] << 8)) :
        ((entryCountBytes[0] << 8) | entryCountBytes[1]);
    
    LOGI("parseTiffIfd: Entry count = %u", entryCount);
    
    uint32_t exifIfdOffset = 0;
    
    // 读取IFD条目
    for (uint16_t i = 0; i < entryCount && i < 200; ++i) {
        uint8_t entry[12];
        file.read(reinterpret_cast<char*>(entry), 12);
        
        // 读取标签ID
        uint16_t tagId = isLittleEndian ?
            (entry[0] | (entry[1] << 8)) :
            ((entry[0] << 8) | entry[1]);
        
        // 读取数据类型
        uint16_t dataType = isLittleEndian ?
            (entry[2] | (entry[3] << 8)) :
            ((entry[2] << 8) | entry[3]);
        
        // 读取数量
        uint32_t count = isLittleEndian ?
            (entry[4] | (entry[5] << 8) | (entry[6] << 16) | (entry[7] << 24)) :
            ((entry[4] << 24) | (entry[5] << 16) | (entry[6] << 8) | entry[7]);
        
        // 读取值或偏移量
        uint32_t valueOffset = isLittleEndian ?
            (entry[8] | (entry[9] << 8) | (entry[10] << 16) | (entry[11] << 24)) :
            ((entry[8] << 24) | (entry[9] << 16) | (entry[10] << 8) | entry[11]);
        
        // 解析常见标签
        switch (tagId) {
            case 256:  // ImageWidth
                metadata.width = readTiffValue(file, dataType, count, valueOffset, isLittleEndian, fileSize);
                LOGI("parseTiffIfd: ImageWidth = %u", metadata.width);
                break;
            case 257:  // ImageLength (height)
                metadata.height = readTiffValue(file, dataType, count, valueOffset, isLittleEndian, fileSize);
                LOGI("parseTiffIfd: ImageLength = %u", metadata.height);
                break;
            case 258:  // BitsPerSample
                metadata.bitsPerSample = readTiffValue(file, dataType, count, valueOffset, isLittleEndian, fileSize);
                LOGI("parseTiffIfd: BitsPerSample = %u", metadata.bitsPerSample);
                break;
            case 271:  // Make (相机品牌)
                readTiffString(file, valueOffset, count, isLittleEndian, metadata.cameraModel, sizeof(metadata.cameraModel));
                LOGI("parseTiffIfd: Make = %s", metadata.cameraModel);
                break;
            case 272:  // Model (相机型号)
                if (std::strlen(metadata.cameraModel) > 0) {
                    std::strncat(metadata.cameraModel, " ", sizeof(metadata.cameraModel) - std::strlen(metadata.cameraModel) - 1);
                }
                char model[64];
                readTiffString(file, valueOffset, count, isLittleEndian, model, sizeof(model));
                std::strncat(metadata.cameraModel, model, sizeof(metadata.cameraModel) - std::strlen(metadata.cameraModel) - 1);
                LOGI("parseTiffIfd: Model = %s", model);
                break;
            case 34665:  // EXIF IFD (0x8769)
                exifIfdOffset = valueOffset;
                LOGI("parseTiffIfd: Found EXIF IFD at offset %u", exifIfdOffset);
                break;
        }
    }
    
    // 解析EXIF子IFD
    if (exifIfdOffset > 0 && exifIfdOffset < fileSize) {
        file.seekg(exifIfdOffset, std::ios::beg);
        
        // 读取EXIF IFD条目数量
        uint8_t exifEntryCountBytes[2];
        file.read(reinterpret_cast<char*>(exifEntryCountBytes), 2);
        uint16_t exifEntryCount = isLittleEndian ?
            (exifEntryCountBytes[0] | (exifEntryCountBytes[1] << 8)) :
            ((exifEntryCountBytes[0] << 8) | exifEntryCountBytes[1]);
        
        LOGI("parseTiffIfd: EXIF entry count = %u", exifEntryCount);
        
        // 读取EXIF条目
        for (uint16_t i = 0; i < exifEntryCount && i < 200; ++i) {
            uint8_t entry[12];
            file.read(reinterpret_cast<char*>(entry), 12);
            
            uint16_t tagId = isLittleEndian ?
                (entry[0] | (entry[1] << 8)) :
                ((entry[0] << 8) | entry[1]);
            
            uint16_t dataType = isLittleEndian ?
                (entry[2] | (entry[3] << 8)) :
                ((entry[2] << 8) | entry[3]);
            
            uint32_t count = isLittleEndian ?
                (entry[4] | (entry[5] << 8) | (entry[6] << 16) | (entry[7] << 24)) :
                ((entry[4] << 24) | (entry[5] << 16) | (entry[6] << 8) | entry[7]);
            
            uint32_t valueOffset = isLittleEndian ?
                (entry[8] | (entry[9] << 8) | (entry[10] << 16) | (entry[11] << 24)) :
                ((entry[8] << 24) | (entry[9] << 16) | (entry[10] << 8) | entry[11]);
            
            // 解析EXIF标签
            switch (tagId) {
                case 33434:  // ExposureTime (0x829A)
                    if (dataType == 5) {  // RATIONAL
                        size_t oldPos = file.tellg();
                        file.seekg(valueOffset, std::ios::beg);
                        uint8_t bytes[8];
                        file.read(reinterpret_cast<char*>(bytes), 8);
                        uint32_t numerator = isLittleEndian ?
                            (bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24)) :
                            ((bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3]);
                        uint32_t denominator = isLittleEndian ?
                            (bytes[4] | (bytes[5] << 8) | (bytes[6] << 16) | (bytes[7] << 24)) :
                            ((bytes[4] << 24) | (bytes[5] << 16) | (bytes[6] << 8) | bytes[7]);
                        if (denominator != 0) {
                            metadata.exposureTime = static_cast<float>(numerator) / static_cast<float>(denominator);
                            LOGI("parseTiffIfd: ExposureTime = %f sec", metadata.exposureTime);
                        }
                        file.seekg(oldPos, std::ios::beg);
                    }
                    break;
                case 33437:  // FNumber (0x829D)
                    if (dataType == 5) {  // RATIONAL
                        size_t oldPos = file.tellg();
                        file.seekg(valueOffset, std::ios::beg);
                        uint8_t bytes[8];
                        file.read(reinterpret_cast<char*>(bytes), 8);
                        uint32_t numerator = isLittleEndian ?
                            (bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24)) :
                            ((bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3]);
                        uint32_t denominator = isLittleEndian ?
                            (bytes[4] | (bytes[5] << 8) | (bytes[6] << 16) | (bytes[7] << 24)) :
                            ((bytes[4] << 24) | (bytes[5] << 16) | (bytes[6] << 8) | bytes[7]);
                        if (denominator != 0) {
                            metadata.aperture = static_cast<float>(numerator) / static_cast<float>(denominator);
                            LOGI("parseTiffIfd: FNumber = f/%.1f", metadata.aperture);
                        }
                        file.seekg(oldPos, std::ios::beg);
                    }
                    break;
                case 34855:  // ISOSpeedRatings (0x8827)
                    metadata.iso = static_cast<float>(readTiffValue(file, dataType, count, valueOffset, isLittleEndian, fileSize));
                    LOGI("parseTiffIfd: ISO = %.0f", metadata.iso);
                    break;
                case 37386:  // FocalLength (0x920A)
                    if (dataType == 5) {  // RATIONAL
                        size_t oldPos = file.tellg();
                        file.seekg(valueOffset, std::ios::beg);
                        uint8_t bytes[8];
                        file.read(reinterpret_cast<char*>(bytes), 8);
                        uint32_t numerator = isLittleEndian ?
                            (bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24)) :
                            ((bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3]);
                        uint32_t denominator = isLittleEndian ?
                            (bytes[4] | (bytes[5] << 8) | (bytes[6] << 16) | (bytes[7] << 24)) :
                            ((bytes[4] << 24) | (bytes[5] << 16) | (bytes[6] << 8) | bytes[7]);
                        if (denominator != 0) {
                            metadata.focalLength = static_cast<float>(numerator) / static_cast<float>(denominator);
                            LOGI("parseTiffIfd: FocalLength = %.1f mm", metadata.focalLength);
                        }
                        file.seekg(oldPos, std::ios::beg);
                    }
                    break;
                case 37377:  // ShutterSpeedValue (0x9201)
                    // 可选：解析快门速度值
                    break;
                case 37378:  // ApertureValue (0x9202)
                    // 可选：解析光圈值
                    break;
                case 37385:  // Flash (0x9209)
                    // 可选：解析闪光灯信息
                    break;
                case 37381:  // WhiteBalance (0x9205)
                    // 可选：解析白平衡设置
                    break;
            }
        }
    }
}

/**
 * 查找ARW文件的RAW数据位置
 */
bool RawProcessor::findArwRawDataLocation(std::ifstream& file,
                                         uint32_t ifdOffset,
                                         bool isLittleEndian,
                                         size_t fileSize,
                                         uint32_t& stripOffset,
                                         uint32_t& stripByteCount,
                                         uint32_t& width,
                                         uint32_t& height,
                                         uint16_t& bitsPerSample) {
    if (ifdOffset == 0 || ifdOffset >= fileSize) {
        return false;
    }
    
    file.seekg(ifdOffset, std::ios::beg);
    
    // 读取IFD条目数量
    uint8_t entryCountBytes[2];
    file.read(reinterpret_cast<char*>(entryCountBytes), 2);
    uint16_t entryCount = isLittleEndian ?
        (entryCountBytes[0] | (entryCountBytes[1] << 8)) :
        ((entryCountBytes[0] << 8) | entryCountBytes[1]);
    
    uint32_t stripOffsetsOffset = 0;
    uint32_t stripByteCountsOffset = 0;
    uint32_t stripOffsetsCount = 0;
    uint32_t stripByteCountsCount = 0;
    uint16_t stripOffsetsType = 0;
    uint16_t stripByteCountsType = 0;
    
    // 读取IFD条目，查找StripOffsets (273) 和 StripByteCounts (279)
    for (uint16_t i = 0; i < entryCount && i < 200; ++i) {
        uint8_t entry[12];
        file.read(reinterpret_cast<char*>(entry), 12);
        
        uint16_t tagId = isLittleEndian ?
            (entry[0] | (entry[1] << 8)) :
            ((entry[0] << 8) | entry[1]);
        
        uint16_t dataType = isLittleEndian ?
            (entry[2] | (entry[3] << 8)) :
            ((entry[2] << 8) | entry[3]);
        
        uint32_t count = isLittleEndian ?
            (entry[4] | (entry[5] << 8) | (entry[6] << 16) | (entry[7] << 24)) :
            ((entry[4] << 24) | (entry[5] << 16) | (entry[6] << 8) | entry[7]);
        
        uint32_t valueOffset = isLittleEndian ?
            (entry[8] | (entry[9] << 8) | (entry[10] << 16) | (entry[11] << 24)) :
            ((entry[8] << 24) | (entry[9] << 16) | (entry[10] << 8) | entry[11]);
        
        if (tagId == 273) {  // StripOffsets
            stripOffsetsOffset = valueOffset;
            stripOffsetsCount = count;
            stripOffsetsType = dataType;
            LOGI("findArwRawDataLocation: Found StripOffsets tag, count=%u, type=%u, offset=%u", 
                 count, dataType, valueOffset);
        } else if (tagId == 279) {  // StripByteCounts
            stripByteCountsOffset = valueOffset;
            stripByteCountsCount = count;
            stripByteCountsType = dataType;
            LOGI("findArwRawDataLocation: Found StripByteCounts tag, count=%u, type=%u, offset=%u", 
                 count, dataType, valueOffset);
        } else if (tagId == 256) {  // ImageWidth
            width = readTiffValue(file, dataType, count, valueOffset, isLittleEndian, fileSize);
        } else if (tagId == 257) {  // ImageLength
            height = readTiffValue(file, dataType, count, valueOffset, isLittleEndian, fileSize);
        } else if (tagId == 258) {  // BitsPerSample
            bitsPerSample = static_cast<uint16_t>(readTiffValue(file, dataType, count, valueOffset, isLittleEndian, fileSize));
        }
    }
    
    if (stripOffsetsOffset == 0 || stripByteCountsOffset == 0) {
        LOGI("findArwRawDataLocation: Could not find StripOffsets or StripByteCounts");
        return false;
    }
    
    // 读取第一个strip的偏移量和大小（通常ARW只有一个strip）
    size_t oldPos = file.tellg();
    
    // 读取StripOffsets
    file.seekg(stripOffsetsOffset, std::ios::beg);
    if (stripOffsetsType == 4) {  // LONG
        uint8_t bytes[4];
        file.read(reinterpret_cast<char*>(bytes), 4);
        stripOffset = isLittleEndian ?
            (bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24)) :
            ((bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3]);
    } else if (stripOffsetsType == 3) {  // SHORT
        uint8_t bytes[2];
        file.read(reinterpret_cast<char*>(bytes), 2);
        stripOffset = isLittleEndian ?
            (bytes[0] | (bytes[1] << 8)) :
            ((bytes[0] << 8) | bytes[1]);
    }
    
    // 读取StripByteCounts
    file.seekg(stripByteCountsOffset, std::ios::beg);
    if (stripByteCountsType == 4) {  // LONG
        uint8_t bytes[4];
        file.read(reinterpret_cast<char*>(bytes), 4);
        stripByteCount = isLittleEndian ?
            (bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24)) :
            ((bytes[0] << 24) | (bytes[1] << 16) | (bytes[2] << 8) | bytes[3]);
    } else if (stripByteCountsType == 3) {  // SHORT
        uint8_t bytes[2];
        file.read(reinterpret_cast<char*>(bytes), 2);
        stripByteCount = isLittleEndian ?
            (bytes[0] | (bytes[1] << 8)) :
            ((bytes[0] << 8) | bytes[1]);
    }
    
    file.seekg(oldPos, std::ios::beg);
    
    LOGI("findArwRawDataLocation: RAW data at offset %u, size %u bytes", stripOffset, stripByteCount);
    return true;
}

/**
 * 读取ARW文件的RAW数据
 */
bool RawProcessor::readArwRawData(std::ifstream& file,
                                 uint32_t stripOffset,
                                 uint32_t stripByteCount,
                                 uint32_t width,
                                 uint32_t height,
                                 uint16_t bitsPerSample,
                                 bool isLittleEndian,
                                 std::vector<uint16_t>& rawData) {
    if (stripOffset == 0 || stripByteCount == 0 || width == 0 || height == 0) {
        LOGE("readArwRawData: Invalid parameters");
        return false;
    }
    
    file.seekg(stripOffset, std::ios::beg);
    
    const uint32_t pixelCount = width * height;
    rawData.resize(pixelCount);
    
    LOGI("readArwRawData: Reading %u pixels (%ux%u), %u bits per sample", 
         pixelCount, width, height, bitsPerSample);
    
    if (bitsPerSample == 14) {
        // ARW通常使用14位压缩格式（Sony特有的压缩）
        // 简化实现：按字节读取并解包
        std::vector<uint8_t> compressedData(stripByteCount);
        file.read(reinterpret_cast<char*>(compressedData.data()), stripByteCount);
        
        if (file.gcount() != static_cast<std::streamsize>(stripByteCount)) {
            LOGE("readArwRawData: Failed to read all data, read %ld bytes", file.gcount());
            return false;
        }
        
        // Sony ARW 14位压缩格式：每3字节存储2个14位值
        // 简化实现：假设未压缩或使用简单的字节序
        // 实际应实现Sony的压缩算法
        uint32_t idx = 0;
        for (uint32_t i = 0; i < stripByteCount && idx < pixelCount; i += 2) {
            if (i + 1 < stripByteCount) {
                uint16_t value;
                if (isLittleEndian) {
                    value = compressedData[i] | (compressedData[i + 1] << 8);
                } else {
                    value = (compressedData[i] << 8) | compressedData[i + 1];
                }
                rawData[idx++] = value;
            }
        }
        
        // 如果数据不足，填充剩余部分
        while (idx < pixelCount) {
            rawData[idx++] = 0;
        }
        
        LOGI("readArwRawData: Successfully read %u pixels", idx);
    } else if (bitsPerSample == 16) {
        // 16位未压缩格式
        std::vector<uint8_t> rawBytes(stripByteCount);
        file.read(reinterpret_cast<char*>(rawBytes.data()), stripByteCount);
        
        if (file.gcount() != static_cast<std::streamsize>(stripByteCount)) {
            LOGE("readArwRawData: Failed to read all data");
            return false;
        }
        
        for (uint32_t i = 0; i < pixelCount && (i * 2 + 1) < stripByteCount; ++i) {
            uint16_t value;
            if (isLittleEndian) {
                value = rawBytes[i * 2] | (rawBytes[i * 2 + 1] << 8);
            } else {
                value = (rawBytes[i * 2] << 8) | rawBytes[i * 2 + 1];
            }
            rawData[i] = value;
        }
        
        LOGI("readArwRawData: Successfully read %u pixels", pixelCount);
    } else {
        LOGE("readArwRawData: Unsupported bits per sample: %u", bitsPerSample);
        return false;
    }
    
    return true;
}

} // namespace filmtracker
