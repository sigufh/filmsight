#include "raw_processor.h"
#include <fstream>
#include <cmath>
#include <algorithm>
#include <cctype>
#include <string>
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
    // 简化实现：读取基本信息和占位数据
    // 实际应解析TIFF IFD结构
    
    // 读取文件大小
    file.seekg(0, std::ios::end);
    size_t fileSize = file.tellg();
    file.seekg(0, std::ios::beg);
    LOGI("loadArwFile: File size = %zu bytes", fileSize);
    
    // 限制图像尺寸，避免创建过大的图像导致内存问题
    // 使用较小的尺寸作为预览（进一步减小以提高性能）
    uint32_t maxWidth = 1200;
    uint32_t maxHeight = 1200;
    
    // 默认ARW参数（使用较小的预览尺寸）
    metadata.width = maxWidth;
    metadata.height = maxHeight;
    metadata.iso = 400.0f;
    metadata.exposureTime = 1.0f / 125.0f;
    metadata.blackLevel = 512.0f;  // ARW典型黑电平
    metadata.whiteLevel = 16383.0f;  // 14位RAW
    
    LOGI("loadArwFile: Creating image %dx%d", metadata.width, metadata.height);
    
    // 设置CFA模式（ARW通常使用RGGB）
    metadata.cfaPattern[0] = 0;  // R
    metadata.cfaPattern[1] = 1;  // G
    metadata.cfaPattern[2] = 1;  // G
    metadata.cfaPattern[3] = 2;  // B
    
    // 创建线性图像
    LOGI("loadArwFile: Allocating LinearImage memory");
    LinearImage image(metadata.width, metadata.height);
    LOGI("loadArwFile: LinearImage allocated successfully");
    
    // 生成测试图案（渐变）以便调试，而不是纯灰色
    // 这样可以看到图像是否正确加载和处理
    const uint32_t pixelCount = image.width * image.height;
    LOGI("loadArwFile: Filling %u pixels with test pattern", pixelCount);
    
    // 创建渐变测试图案
    for (uint32_t y = 0; y < image.height; ++y) {
        for (uint32_t x = 0; x < image.width; ++x) {
            uint32_t idx = y * image.width + x;
            
            // 创建渐变测试图案
            float fx = static_cast<float>(x) / static_cast<float>(image.width);
            float fy = static_cast<float>(y) / static_cast<float>(image.height);
            
            // RGB渐变：左上角偏红，右上角偏绿，左下角偏蓝，右下角偏白
            image.r[idx] = fx * 0.8f + 0.2f;  // 0.2-1.0
            image.g[idx] = (1.0f - fx) * 0.6f + fy * 0.4f + 0.2f;  // 0.2-1.0
            image.b[idx] = fy * 0.8f + 0.2f;  // 0.2-1.0
            
            // 应用曝光调整（模拟RAW数据，使其看起来更真实）
            image.r[idx] *= 0.5f;
            image.g[idx] *= 0.5f;
            image.b[idx] *= 0.5f;
        }
    }
    
    LOGI("loadArwFile: Completed successfully with test pattern");
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

} // namespace filmtracker
