#include "raw_processor.h"
#include <fstream>
#include <cmath>
#include <algorithm>
#include <cctype>
#include <string>
#include <cstring>
#include <cstdlib>
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
    
    try {
        // ARW文件是TIFF格式的变体
        // 尝试解析TIFF IFD结构以获取实际图像尺寸
        
        // 读取文件大小
        LOGI("loadArwFile: Seeking to end of file");
        file.seekg(0, std::ios::end);
        if (file.fail()) {
            LOGE("loadArwFile: Failed to seek to end of file");
            throw std::runtime_error("Failed to seek to end of file");
        }
        
        size_t fileSize = file.tellg();
        if (file.fail()) {
            LOGE("loadArwFile: Failed to get file size");
            throw std::runtime_error("Failed to get file size");
        }
        
        LOGI("loadArwFile: File size = %zu bytes", fileSize);
        
        LOGI("loadArwFile: Seeking to beginning of file");
        file.seekg(0, std::ios::beg);
        if (file.fail()) {
            LOGE("loadArwFile: Failed to seek to beginning of file");
            throw std::runtime_error("Failed to seek to beginning of file");
        }
        
        // 读取TIFF头（8字节）
        LOGI("loadArwFile: Reading TIFF header (8 bytes)");
        uint8_t tiffHeader[8];
        file.read(reinterpret_cast<char*>(tiffHeader), 8);
        
        size_t bytesRead = file.gcount();
        LOGI("loadArwFile: Read %zu bytes from file", bytesRead);
        
        if (bytesRead != 8) {
            LOGE("loadArwFile: Failed to read TIFF header, only read %zu bytes", bytesRead);
            throw std::runtime_error("Failed to read TIFF header");
        }
        
        if (file.fail() && !file.eof()) {
            LOGE("loadArwFile: File read error (not EOF)");
            throw std::runtime_error("File read error");
        }
        
        LOGI("loadArwFile: TIFF header bytes: %02x %02x %02x %02x %02x %02x %02x %02x",
             tiffHeader[0], tiffHeader[1], tiffHeader[2], tiffHeader[3],
             tiffHeader[4], tiffHeader[5], tiffHeader[6], tiffHeader[7]);
        
        // 检测字节序
        bool isLittleEndian = (tiffHeader[0] == 0x49 && tiffHeader[1] == 0x49);
        bool isBigEndian = (tiffHeader[0] == 0x4D && tiffHeader[1] == 0x4D);
        
        LOGI("loadArwFile: Byte order - isLittleEndian=%s, isBigEndian=%s", 
             isLittleEndian ? "true" : "false", isBigEndian ? "true" : "false");
        
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
        
        LOGI("loadArwFile: IFD offset = %u (0x%08x)", ifdOffset, ifdOffset);
        
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
        LOGI("loadArwFile: About to parse TIFF IFD, ifdOffset=%u, fileSize=%zu", ifdOffset, fileSize);
        if (ifdOffset > 0 && ifdOffset < fileSize) {
            LOGI("loadArwFile: Calling parseTiffIfd");
            parseTiffIfd(file, ifdOffset, isLittleEndian, fileSize, metadata);
            LOGI("loadArwFile: parseTiffIfd completed, metadata.width=%u, metadata.height=%u", 
                 metadata.width, metadata.height);
        } else {
            LOGE("loadArwFile: Invalid IFD offset, skipping parseTiffIfd");
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
        LOGI("loadArwFile: About to search for RAW data, ifdOffset=%u, fileSize=%zu", ifdOffset, fileSize);
        
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
        
        LOGI("loadArwFile: Attempting to find RAW data location, ifdOffset=%u, fileSize=%zu", ifdOffset, fileSize);
        bool foundRawData = findArwRawDataLocation(file, ifdOffset, isLittleEndian, fileSize,
                                                  stripOffset, stripByteCount, rawWidth, rawHeight, rawBitsPerSample);
        
        LOGI("loadArwFile: findArwRawDataLocation returned %s, stripOffset=%u, stripByteCount=%u", 
             foundRawData ? "true" : "false", stripOffset, stripByteCount);
        
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
                
                // 将RAW数据转换为归一化的float数组（0-1范围）
                // 这样去马赛克算法可以使用归一化的值
                std::vector<float> normalizedRawData(rawBayerData.size());
                float whiteLevel = metadata.whiteLevel;
                for (size_t i = 0; i < rawBayerData.size(); ++i) {
                    normalizedRawData[i] = std::max(0.0f, std::min(1.0f, 
                        static_cast<float>(rawBayerData[i]) / whiteLevel));
                }
                
                // 应用去马赛克（Bayer转RGB），使用归一化的数据
                LinearImage demosaiced = demosaicBayerNormalized(normalizedRawData, rawWidth, rawHeight, 0); // 0 = RGGB
                
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
    } catch (const std::exception& e) {
        LOGE("loadArwFile: Exception caught: %s", e.what());
        // 如果发生异常，返回一个默认图像
        metadata.width = 1200;
        metadata.height = 1200;
        LinearImage image(metadata.width, metadata.height);
        for (uint32_t i = 0; i < image.width * image.height; ++i) {
            image.r[i] = 0.5f;
            image.g[i] = 0.5f;
            image.b[i] = 0.5f;
        }
        LOGE("loadArwFile: Returning default image due to exception");
        return image;
    } catch (...) {
        LOGE("loadArwFile: Unknown exception caught");
        // 如果发生未知异常，返回一个默认图像
        metadata.width = 1200;
        metadata.height = 1200;
        LinearImage image(metadata.width, metadata.height);
        for (uint32_t i = 0; i < image.width * image.height; ++i) {
            image.r[i] = 0.5f;
            image.g[i] = 0.5f;
            image.b[i] = 0.5f;
        }
        LOGE("loadArwFile: Returning default image due to unknown exception");
        return image;
    }
}

LinearImage RawProcessor::loadRawFromBuffer(const uint8_t* buffer, 
                                           size_t bufferSize,
                                           RawMetadata& metadata) {
    if (!buffer || bufferSize < 16) {
        LOGE("loadRawFromBuffer: Invalid buffer");
        throw std::runtime_error("Invalid buffer");
    }
    
    LOGI("loadRawFromBuffer: Starting, buffer size = %zu bytes", bufferSize);
    
    // 检测文件格式（通过文件头）
    bool isArw = false;
    if (buffer[0] == 0x49 && buffer[1] == 0x49) {  // "II" (Intel byte order)
        isArw = true;
        LOGI("loadRawFromBuffer: Detected ARW (Intel byte order)");
    } else if (buffer[0] == 0x4D && buffer[1] == 0x4D) {  // "MM" (Motorola byte order)
        isArw = true;
        LOGI("loadRawFromBuffer: Detected ARW (Motorola byte order)");
    }
    
    if (isArw) {
        // 将缓冲区写入临时文件，然后使用现有的loadRaw实现
        // 或者直接解析内存中的数据
        // 简化实现：创建一个临时文件
        const char* tempFile = "/data/local/tmp/temp_raw.arw";
        std::ofstream tempOut(tempFile, std::ios::binary);
        if (tempOut.is_open()) {
            tempOut.write(reinterpret_cast<const char*>(buffer), bufferSize);
            tempOut.close();
            
            LinearImage result = loadRaw(tempFile, metadata);
            
            // 删除临时文件
            std::remove(tempFile);
            
            return result;
        } else {
            LOGE("loadRawFromBuffer: Failed to create temp file");
            throw std::runtime_error("Failed to create temp file");
        }
    }
    
    // 其他格式：返回占位图像
    LOGE("loadRawFromBuffer: Unsupported format, returning placeholder");
    metadata.width = 100;
    metadata.height = 100;
    metadata.iso = 400.0f;
    metadata.exposureTime = 1.0f / 125.0f;
    metadata.blackLevel = 0.0f;
    metadata.whiteLevel = 16383.0f;
    
    LinearImage image(100, 100);
    for (uint32_t i = 0; i < 100 * 100; ++i) {
        image.r[i] = 0.5f;
        image.g[i] = 0.5f;
        image.b[i] = 0.5f;
    }
    
    return image;
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
    
    // 基本双线性插值去马赛克算法（RGGB模式）
    // CFA模式：0=RGGB, 1=GRBG, 2=GBRG, 3=BGGR
    // 简化实现：假设RGGB模式（Sony ARW通常使用RGGB）
    
    for (uint32_t y = 0; y < height; ++y) {
        for (uint32_t x = 0; x < width; ++x) {
            uint32_t idx = y * width + x;
            float r = 0.0f, g = 0.0f, b = 0.0f;
            
            // 确定当前像素在Bayer模式中的位置
            bool isRedRow = (y % 2 == 0);
            bool isRedCol = (x % 2 == 0);
            
            if (isRedRow && isRedCol) {
                // R位置：直接使用R值，G和B需要插值
                r = static_cast<float>(rawData[idx]);
                
                // G值：取上下左右四个G像素的平均值
                float gSum = 0.0f;
                int gCount = 0;
                if (x > 0) { gSum += rawData[idx - 1]; gCount++; }
                if (x < width - 1) { gSum += rawData[idx + 1]; gCount++; }
                if (y > 0) { gSum += rawData[idx - width]; gCount++; }
                if (y < height - 1) { gSum += rawData[idx + width]; gCount++; }
                g = gCount > 0 ? gSum / gCount : 0.0f;
                
                // B值：取四个角的B像素的平均值
                float bSum = 0.0f;
                int bCount = 0;
                if (x > 0 && y > 0) { bSum += rawData[idx - width - 1]; bCount++; }
                if (x < width - 1 && y > 0) { bSum += rawData[idx - width + 1]; bCount++; }
                if (x > 0 && y < height - 1) { bSum += rawData[idx + width - 1]; bCount++; }
                if (x < width - 1 && y < height - 1) { bSum += rawData[idx + width + 1]; bCount++; }
                b = bCount > 0 ? bSum / bCount : 0.0f;
            } else if (isRedRow && !isRedCol) {
                // G位置（R行的G列）：直接使用G值，R和B需要插值
                g = static_cast<float>(rawData[idx]);
                
                // R值：取左右R像素的平均值
                float rSum = 0.0f;
                int rCount = 0;
                if (x > 0) { rSum += rawData[idx - 1]; rCount++; }
                if (x < width - 1) { rSum += rawData[idx + 1]; rCount++; }
                r = rCount > 0 ? rSum / rCount : 0.0f;
                
                // B值：取上下B像素的平均值
                float bSum = 0.0f;
                int bCount = 0;
                if (y > 0) { bSum += rawData[idx - width]; bCount++; }
                if (y < height - 1) { bSum += rawData[idx + width]; bCount++; }
                b = bCount > 0 ? bSum / bCount : 0.0f;
            } else if (!isRedRow && isRedCol) {
                // G位置（B行的G列）：直接使用G值，R和B需要插值
                g = static_cast<float>(rawData[idx]);
                
                // R值：取上下R像素的平均值
                float rSum = 0.0f;
                int rCount = 0;
                if (y > 0) { rSum += rawData[idx - width]; rCount++; }
                if (y < height - 1) { rSum += rawData[idx + width]; rCount++; }
                r = rCount > 0 ? rSum / rCount : 0.0f;
                
                // B值：取左右B像素的平均值
                float bSum = 0.0f;
                int bCount = 0;
                if (x > 0) { bSum += rawData[idx - 1]; bCount++; }
                if (x < width - 1) { bSum += rawData[idx + 1]; bCount++; }
                b = bCount > 0 ? bSum / bCount : 0.0f;
            } else {
                // B位置：直接使用B值，G和R需要插值
                b = static_cast<float>(rawData[idx]);
                
                // G值：取上下左右四个G像素的平均值
                float gSum = 0.0f;
                int gCount = 0;
                if (x > 0) { gSum += rawData[idx - 1]; gCount++; }
                if (x < width - 1) { gSum += rawData[idx + 1]; gCount++; }
                if (y > 0) { gSum += rawData[idx - width]; gCount++; }
                if (y < height - 1) { gSum += rawData[idx + width]; gCount++; }
                g = gCount > 0 ? gSum / gCount : 0.0f;
                
                // R值：取四个角的R像素的平均值
                float rSum = 0.0f;
                int rCount = 0;
                if (x > 0 && y > 0) { rSum += rawData[idx - width - 1]; rCount++; }
                if (x < width - 1 && y > 0) { rSum += rawData[idx - width + 1]; rCount++; }
                if (x > 0 && y < height - 1) { rSum += rawData[idx + width - 1]; rCount++; }
                if (x < width - 1 && y < height - 1) { rSum += rawData[idx + width + 1]; rCount++; }
                r = rCount > 0 ? rSum / rCount : 0.0f;
            }
            
            result.r[idx] = r;
            result.g[idx] = g;
            result.b[idx] = b;
        }
    }
    
    LOGI("demosaicBayer: Completed demosaicing %ux%u image", width, height);
    return result;
}

LinearImage RawProcessor::demosaicBayerNormalized(const std::vector<float>& normalizedRawData,
                                                  uint32_t width,
                                                  uint32_t height,
                                                  uint32_t cfaPattern) {
    LOGI("demosaicBayerNormalized: Starting demosaicing for %ux%u", width, height);
    LinearImage result(width, height);
    
    // 基本双线性插值去马赛克算法（RGGB模式）
    // 使用归一化的float数据（0-1范围）
    
    for (uint32_t y = 0; y < height; ++y) {
        for (uint32_t x = 0; x < width; ++x) {
            uint32_t idx = y * width + x;
            float r = 0.0f, g = 0.0f, b = 0.0f;
            
            // 确定当前像素在Bayer模式中的位置
            bool isRedRow = (y % 2 == 0);
            bool isRedCol = (x % 2 == 0);
            
            // 辅助函数：安全获取归一化的RAW值
            auto getNormalizedValue = [&](int32_t px, int32_t py) -> float {
                if (px < 0 || px >= static_cast<int32_t>(width) || 
                    py < 0 || py >= static_cast<int32_t>(height)) {
                    return 0.0f;
                }
                return normalizedRawData[py * width + px];
            };
            
            if (isRedRow && isRedCol) {
                // R位置：直接使用R值，G和B需要插值
                r = normalizedRawData[idx];
                
                // G值：取上下左右四个G像素的平均值
                g = (getNormalizedValue(x - 1, y) +
                     getNormalizedValue(x + 1, y) +
                     getNormalizedValue(x, y - 1) +
                     getNormalizedValue(x, y + 1)) / 4.0f;
                
                // B值：取四个角的B像素的平均值
                b = (getNormalizedValue(x - 1, y - 1) +
                     getNormalizedValue(x + 1, y - 1) +
                     getNormalizedValue(x - 1, y + 1) +
                     getNormalizedValue(x + 1, y + 1)) / 4.0f;
            } else if (isRedRow && !isRedCol) {
                // G位置（R行的G列）：直接使用G值，R和B需要插值
                g = normalizedRawData[idx];
                
                // R值：取左右R像素的平均值
                r = (getNormalizedValue(x - 1, y) +
                     getNormalizedValue(x + 1, y)) / 2.0f;
                
                // B值：取上下B像素的平均值
                b = (getNormalizedValue(x, y - 1) +
                     getNormalizedValue(x, y + 1)) / 2.0f;
            } else if (!isRedRow && isRedCol) {
                // G位置（B行的G列）：直接使用G值，R和B需要插值
                g = normalizedRawData[idx];
                
                // R值：取上下R像素的平均值
                r = (getNormalizedValue(x, y - 1) +
                     getNormalizedValue(x, y + 1)) / 2.0f;
                
                // B值：取左右B像素的平均值
                b = (getNormalizedValue(x - 1, y) +
                     getNormalizedValue(x + 1, y)) / 2.0f;
            } else {
                // B位置：直接使用B值，G和R需要插值
                b = normalizedRawData[idx];
                
                // G值：取上下左右四个G像素的平均值
                g = (getNormalizedValue(x - 1, y) +
                     getNormalizedValue(x + 1, y) +
                     getNormalizedValue(x, y - 1) +
                     getNormalizedValue(x, y + 1)) / 4.0f;
                
                // R值：取四个角的R像素的平均值
                r = (getNormalizedValue(x - 1, y - 1) +
                     getNormalizedValue(x + 1, y - 1) +
                     getNormalizedValue(x - 1, y + 1) +
                     getNormalizedValue(x + 1, y + 1)) / 4.0f;
            }
            
            // 确保值在有效范围内
            result.r[idx] = std::max(0.0f, std::min(1.0f, r));
            result.g[idx] = std::max(0.0f, std::min(1.0f, g));
            result.b[idx] = std::max(0.0f, std::min(1.0f, b));
        }
    }
    
    LOGI("demosaicBayerNormalized: Demosaicing completed");
    return result;
}

void RawProcessor::parseDngTags(const uint8_t* buffer, size_t size, RawMetadata& metadata) {
    if (!buffer || size < 8) {
        LOGE("parseDngTags: Invalid buffer");
        return;
    }
    
    LOGI("parseDngTags: Starting, buffer size = %zu bytes", size);
    
    // DNG文件也是基于TIFF格式的
    // 检测字节序
    bool isLittleEndian = (buffer[0] == 0x49 && buffer[1] == 0x49);
    bool isBigEndian = (buffer[0] == 0x4D && buffer[1] == 0x4D);
    
    if (!isLittleEndian && !isBigEndian) {
        LOGE("parseDngTags: Invalid TIFF header");
        return;
    }
    
    // 读取IFD偏移量（字节4-7）
    uint32_t ifdOffset;
    if (isLittleEndian) {
        ifdOffset = buffer[4] | (buffer[5] << 8) | 
                   (buffer[6] << 16) | (buffer[7] << 24);
    } else {
        ifdOffset = (buffer[4] << 24) | (buffer[5] << 16) | 
                   (buffer[6] << 8) | buffer[7];
    }
    
    LOGI("parseDngTags: IFD offset = %u", ifdOffset);
    
    // 创建一个临时的文件流来复用现有的TIFF解析逻辑
    // 或者直接解析内存中的数据
    // 简化实现：将缓冲区写入临时文件
    const char* tempFile = "/data/local/tmp/temp_dng.dng";
    std::ofstream tempOut(tempFile, std::ios::binary);
    if (tempOut.is_open()) {
        tempOut.write(reinterpret_cast<const char*>(buffer), size);
        tempOut.close();
        
        std::ifstream file(tempFile, std::ios::binary);
        if (file.is_open()) {
            parseTiffIfd(file, ifdOffset, isLittleEndian, size, metadata);
            file.close();
        }
        
        // 删除临时文件
        std::remove(tempFile);
    } else {
        LOGE("parseDngTags: Failed to create temp file");
    }
    
    LOGI("parseDngTags: Completed");
}

void RawProcessor::demosaicAHD(const std::vector<uint16_t>& rawData,
                               LinearImage& output,
                               uint32_t width,
                               uint32_t height,
                               uint32_t cfaPattern) {
    // AHD (Adaptive Homogeneity-Directed) 去马赛克算法
    // 这是一个高质量的算法，但实现复杂
    // 当前使用改进的双线性插值作为基础实现
    // 未来可以升级为完整的AHD算法
    
    LOGI("demosaicAHD: Starting AHD demosaicing for %ux%u image", width, height);
    
    // 使用现有的demosaicBayer实现作为基础
    LinearImage tempResult = demosaicBayer(rawData, width, height, cfaPattern);
    
    // 应用AHD算法的改进：边缘感知插值
    // 简化版：在边缘区域使用更好的插值策略
    for (uint32_t y = 1; y < height - 1; ++y) {
        for (uint32_t x = 1; x < width - 1; ++x) {
            uint32_t idx = y * width + x;
            
            // 计算局部梯度（用于边缘检测）
            float gradH = std::abs(tempResult.r[idx - 1] - tempResult.r[idx + 1]) +
                         std::abs(tempResult.g[idx - 1] - tempResult.g[idx + 1]) +
                         std::abs(tempResult.b[idx - 1] - tempResult.b[idx + 1]);
            
            float gradV = std::abs(tempResult.r[idx - width] - tempResult.r[idx + width]) +
                         std::abs(tempResult.g[idx - width] - tempResult.g[idx + width]) +
                         std::abs(tempResult.b[idx - width] - tempResult.b[idx + width]);
            
            // 如果检测到边缘，使用方向性插值
            if (gradH > gradV * 1.2f) {
                // 垂直边缘：使用水平插值
                tempResult.r[idx] = (tempResult.r[idx - 1] + tempResult.r[idx + 1]) * 0.5f;
                tempResult.b[idx] = (tempResult.b[idx - 1] + tempResult.b[idx + 1]) * 0.5f;
            } else if (gradV > gradH * 1.2f) {
                // 水平边缘：使用垂直插值
                tempResult.r[idx] = (tempResult.r[idx - width] + tempResult.r[idx + width]) * 0.5f;
                tempResult.b[idx] = (tempResult.b[idx - width] + tempResult.b[idx + width]) * 0.5f;
            }
            // 否则保持原有的插值结果
        }
    }
    
    // 复制结果到输出
    output.r = tempResult.r;
    output.g = tempResult.g;
    output.b = tempResult.b;
    
    LOGI("demosaicAHD: Completed AHD demosaicing");
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
    LOGI("findArwRawDataLocation: Starting, ifdOffset=%u, fileSize=%zu", ifdOffset, fileSize);
    
    if (ifdOffset == 0 || ifdOffset >= fileSize) {
        LOGE("findArwRawDataLocation: Invalid IFD offset %u (fileSize=%zu)", ifdOffset, fileSize);
        return false;
    }
    
    file.seekg(ifdOffset, std::ios::beg);
    if (file.fail()) {
        LOGE("findArwRawDataLocation: Failed to seek to IFD offset %u", ifdOffset);
        return false;
    }
    
    // 读取IFD条目数量
    uint8_t entryCountBytes[2];
    file.read(reinterpret_cast<char*>(entryCountBytes), 2);
    if (file.gcount() != 2) {
        LOGE("findArwRawDataLocation: Failed to read IFD entry count");
        return false;
    }
    
    uint16_t entryCount = isLittleEndian ?
        (entryCountBytes[0] | (entryCountBytes[1] << 8)) :
        ((entryCountBytes[0] << 8) | entryCountBytes[1]);
    
    LOGI("findArwRawDataLocation: IFD entry count = %u", entryCount);
    
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
    
    LOGI("findArwRawDataLocation: After scanning IFD - stripOffsetsOffset=%u, stripByteCountsOffset=%u", 
         stripOffsetsOffset, stripByteCountsOffset);
    
    if (stripOffsetsOffset == 0 || stripByteCountsOffset == 0) {
        LOGE("findArwRawDataLocation: Could not find StripOffsets (offset=%u) or StripByteCounts (offset=%u)", 
             stripOffsetsOffset, stripByteCountsOffset);
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
        // ARW通常使用14位格式
        // 检查是否是压缩格式：如果是压缩格式，字节数应该约为 pixelCount * 1.5
        // 如果是未压缩格式，字节数应该约为 pixelCount * 2
        uint32_t expectedCompressedBytes = (pixelCount * 3) / 2;
        uint32_t expectedUncompressedBytes = pixelCount * 2;
        
        LOGI("readArwRawData: Expected compressed=%u, uncompressed=%u, actual=%u", 
             expectedCompressedBytes, expectedUncompressedBytes, stripByteCount);
        
        std::vector<uint8_t> rawBytes(stripByteCount);
        file.read(reinterpret_cast<char*>(rawBytes.data()), stripByteCount);
        
        if (file.gcount() != static_cast<std::streamsize>(stripByteCount)) {
            LOGE("readArwRawData: Failed to read all data, read %ld bytes", file.gcount());
            return false;
        }
        
        // 判断格式：如果字节数接近压缩格式，使用压缩解析；否则使用未压缩解析
        bool isCompressed = (stripByteCount < expectedUncompressedBytes * 0.9f);
        
        if (isCompressed && stripByteCount >= expectedCompressedBytes * 0.8f) {
            // Sony ARW 14位压缩格式：每3字节存储2个14位值
            // 格式：Byte0[7:0] Byte1[5:0] Byte2[7:0] Byte3[5:0] ...
            //       Pixel0[13:6] Pixel0[5:0] Pixel1[13:6] Pixel1[5:0] ...
            LOGI("readArwRawData: Using compressed 14-bit format");
            uint32_t idx = 0;
            for (uint32_t i = 0; i + 2 < stripByteCount && idx < pixelCount; i += 3) {
                // 第一个14位值：Byte0[7:0] + Byte1[5:0] << 8
                uint16_t pixel0 = rawBytes[i] | ((rawBytes[i + 1] & 0x3F) << 8);
                rawData[idx++] = pixel0;
                
                if (idx < pixelCount) {
                    // 第二个14位值：Byte1[7:6] << 12 + Byte2[7:0] << 2
                    uint16_t pixel1 = ((rawBytes[i + 1] & 0xC0) >> 6) | (rawBytes[i + 2] << 2);
                    rawData[idx++] = pixel1;
                }
            }
            
            // 如果还有剩余字节但不足3字节，尝试读取最后一个像素
            uint32_t remaining = stripByteCount % 3;
            if (remaining >= 2 && idx < pixelCount) {
                uint16_t pixel = rawBytes[stripByteCount - remaining] | 
                               ((rawBytes[stripByteCount - remaining + 1] & 0x3F) << 8);
                rawData[idx++] = pixel;
            }
            
            LOGI("readArwRawData: Successfully read %u pixels from %u bytes (14-bit compressed)", idx, stripByteCount);
        } else {
            // 未压缩14位格式：每2字节一个14位值（低14位有效）
            LOGI("readArwRawData: Using uncompressed 14-bit format");
            for (uint32_t i = 0; i < pixelCount && (i * 2 + 1) < stripByteCount; ++i) {
                uint16_t value;
                if (isLittleEndian) {
                    value = rawBytes[i * 2] | ((rawBytes[i * 2 + 1] & 0x3F) << 8);
                } else {
                    value = ((rawBytes[i * 2] & 0x3F) << 8) | rawBytes[i * 2 + 1];
                }
                rawData[i] = value;
            }
            LOGI("readArwRawData: Successfully read %u pixels from %u bytes (14-bit uncompressed)", pixelCount, stripByteCount);
        }
        
        // 如果数据不足，填充剩余部分
        for (uint32_t i = 0; i < pixelCount; ++i) {
            if (rawData[i] == 0 && i > 0) {
                rawData[i] = rawData[i - 1]; // 使用前一个像素值填充
            }
        }
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
