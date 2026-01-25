#include "raw_processor.h"
#include <libraw.h>
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
 * 从文件加载 RAW（使用 LibRaw 库）
 * 支持所有 LibRaw 支持的 RAW 格式（ARW, CR2, NEF, RAF, ORF, RW2 等）
 */
LinearImage RawProcessor::loadRaw(const char* filePath, RawMetadata& metadata) {
    LOGI("loadRaw: Starting with LibRaw, filePath=%s", filePath);
    
    if (!filePath) {
        LOGE("loadRaw: File path is null");
        throw std::runtime_error("File path is null");
    }
    
    // 创建 LibRaw 处理器
    LibRaw rawProcessor;
    
    // 打开 RAW 文件
    int ret = rawProcessor.open_file(filePath);
    if (ret != LIBRAW_SUCCESS) {
        LOGE("loadRaw: Failed to open RAW file: %s, error: %s", 
             filePath, libraw_strerror(ret));
        throw std::runtime_error("Failed to open RAW file");
    }
    
    LOGI("loadRaw: File opened successfully");
    
    // 解包 RAW 数据
    ret = rawProcessor.unpack();
    if (ret != LIBRAW_SUCCESS) {
        LOGE("loadRaw: Failed to unpack RAW data: %s", libraw_strerror(ret));
        rawProcessor.recycle();
        throw std::runtime_error("Failed to unpack RAW data");
    }
    
    LOGI("loadRaw: RAW data unpacked successfully");
    
    // 获取图像数据（imgdata 是结构体，不是指针）
    libraw_data_t& imgdata = rawProcessor.imgdata;
    
    // 提取元数据
    metadata.width = imgdata.sizes.width;
    metadata.height = imgdata.sizes.height;
    metadata.iso = imgdata.other.iso_speed;
    metadata.exposureTime = imgdata.other.shutter;
    metadata.aperture = imgdata.other.aperture;
    metadata.focalLength = imgdata.other.focal_len;
    
    // 相机信息
    std::strncpy(metadata.cameraModel, imgdata.idata.make, sizeof(metadata.cameraModel) - 1);
    std::strncat(metadata.cameraModel, " ", sizeof(metadata.cameraModel) - std::strlen(metadata.cameraModel) - 1);
    std::strncat(metadata.cameraModel, imgdata.idata.model, sizeof(metadata.cameraModel) - std::strlen(metadata.cameraModel) - 1);
    
    // 白平衡（只使用前两个值）
    metadata.whiteBalance[0] = imgdata.color.cam_mul[0];
    metadata.whiteBalance[1] = imgdata.color.cam_mul[1];
    
    // 黑电平和白电平
    metadata.blackLevel = static_cast<float>(imgdata.color.black);
    metadata.whiteLevel = static_cast<float>(imgdata.color.maximum);
    
    // Bits per sample
    metadata.bitsPerSample = imgdata.sizes.raw_pitch ? 16 : 14;
    
    // 颜色空间
    std::strncpy(metadata.colorSpace, "sRGB", sizeof(metadata.colorSpace) - 1);
    
    LOGI("loadRaw: Image dimensions: %dx%d, ISO=%.0f, Exposure=%.3fs, Aperture=f/%.1f, Focal=%.1fmm",
         metadata.width, metadata.height, metadata.iso, metadata.exposureTime,
         metadata.aperture, metadata.focalLength);
    LOGI("loadRaw: Camera: %s", metadata.cameraModel);
    LOGI("loadRaw: Black level=%.0f, White level=%.0f, Bits per sample=%d",
         metadata.blackLevel, metadata.whiteLevel, metadata.bitsPerSample);
    
    // 获取 RAW Bayer 数据
    uint16_t* rawData = imgdata.rawdata.raw_image;
    if (!rawData) {
        LOGE("loadRaw: RAW image data is null");
        rawProcessor.recycle();
        throw std::runtime_error("RAW image data is null");
    }
    
    uint32_t rawWidth = imgdata.sizes.raw_width;
    uint32_t rawHeight = imgdata.sizes.raw_height;
    uint32_t rawPixels = rawWidth * rawHeight;
    
    LOGI("loadRaw: RAW dimensions: %dx%d (%u pixels)", rawWidth, rawHeight, rawPixels);
    
    // 获取 CFA 模式
    uint32_t cfaPattern = 0;  // 默认 RGGB
    if (imgdata.idata.filters > 0) {
        uint32_t filter = imgdata.idata.filters;
        if ((filter & 0x0000FF00) == 0x00009400) {
            cfaPattern = 0;  // RGGB
        } else if ((filter & 0x0000FF00) == 0x00004900) {
            cfaPattern = 1;  // GRBG
        } else if ((filter & 0x0000FF00) == 0x00006100) {
            cfaPattern = 2;  // GBRG
        } else if ((filter & 0x0000FF00) == 0x00001600) {
            cfaPattern = 3;  // BGGR
        }
        LOGI("loadRaw: CFA pattern: %u (filter=0x%08x)", cfaPattern, filter);
    }
    
    // 将 RAW 数据复制到 vector（用于黑电平校正）
    std::vector<uint16_t> rawBayerData(rawPixels);
    std::memcpy(rawBayerData.data(), rawData, rawPixels * sizeof(uint16_t));
    
    // 应用黑电平校正
    applyBlackLevel(rawBayerData, metadata.blackLevel, rawWidth, rawHeight);
    
    // 归一化到 0-1 范围
    std::vector<float> normalizedRawData(rawPixels);
    float whiteLevel = metadata.whiteLevel;
    for (size_t i = 0; i < rawPixels; ++i) {
        normalizedRawData[i] = std::max(0.0f, std::min(1.0f, 
            static_cast<float>(rawBayerData[i]) / whiteLevel));
    }
    
    LOGI("loadRaw: Applied black level correction and normalization");
    
    // 去马赛克（Bayer 转 RGB）
    LinearImage demosaiced = demosaicBayerNormalized(normalizedRawData, rawWidth, rawHeight, cfaPattern);
    
    LOGI("loadRaw: Demosaicing completed, output size: %dx%d", demosaiced.width, demosaiced.height);
    
    // 如果 RAW 尺寸与输出尺寸不匹配，需要缩放
    if (demosaiced.width != metadata.width || demosaiced.height != metadata.height) {
        LOGI("loadRaw: Resizing from %dx%d to %dx%d", 
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
        
        rawProcessor.recycle();
        LOGI("loadRaw: Completed successfully with LibRaw");
        return resized;
    }
    
    // 清理 LibRaw 资源
    rawProcessor.recycle();
    
    LOGI("loadRaw: Completed successfully with LibRaw");
    return demosaiced;
}

LinearImage RawProcessor::loadRawFromBuffer(const uint8_t* buffer, 
                                           size_t bufferSize,
                                           RawMetadata& metadata) {
    if (!buffer || bufferSize == 0) {
        LOGE("loadRawFromBuffer: Invalid buffer");
        throw std::runtime_error("Invalid buffer");
    }
    
    LOGI("loadRawFromBuffer: Starting with LibRaw, buffer size = %zu bytes", bufferSize);
    
    // 创建 LibRaw 处理器
    LibRaw rawProcessor;
    
    // 从缓冲区打开 RAW 数据（LibRaw 支持从内存读取）
    int ret = rawProcessor.open_buffer(const_cast<uint8_t*>(buffer), bufferSize);
    if (ret != LIBRAW_SUCCESS) {
        LOGE("loadRawFromBuffer: Failed to open RAW buffer, error: %s", libraw_strerror(ret));
        throw std::runtime_error("Failed to open RAW buffer");
    }
    
    LOGI("loadRawFromBuffer: Buffer opened successfully");
    
    // 解包 RAW 数据
    ret = rawProcessor.unpack();
    if (ret != LIBRAW_SUCCESS) {
        LOGE("loadRawFromBuffer: Failed to unpack RAW data: %s", libraw_strerror(ret));
        rawProcessor.recycle();
        throw std::runtime_error("Failed to unpack RAW data");
    }
    
    LOGI("loadRawFromBuffer: RAW data unpacked successfully");
    
    // 获取图像数据（imgdata 是结构体，不是指针）
    libraw_data_t& imgdata = rawProcessor.imgdata;
    
    // 提取元数据（与loadRaw相同的逻辑）
    metadata.width = imgdata.sizes.width;
    metadata.height = imgdata.sizes.height;
    metadata.iso = imgdata.other.iso_speed;
    metadata.exposureTime = imgdata.other.shutter;
    metadata.aperture = imgdata.other.aperture;
    metadata.focalLength = imgdata.other.focal_len;
    
    std::strncpy(metadata.cameraModel, imgdata.idata.make, sizeof(metadata.cameraModel) - 1);
    std::strncat(metadata.cameraModel, " ", sizeof(metadata.cameraModel) - std::strlen(metadata.cameraModel) - 1);
    std::strncat(metadata.cameraModel, imgdata.idata.model, sizeof(metadata.cameraModel) - std::strlen(metadata.cameraModel) - 1);
    
    metadata.whiteBalance[0] = imgdata.color.cam_mul[0];
    metadata.whiteBalance[1] = imgdata.color.cam_mul[1];
    
    metadata.blackLevel = static_cast<float>(imgdata.color.black);
    metadata.whiteLevel = static_cast<float>(imgdata.color.maximum);
    metadata.bitsPerSample = imgdata.sizes.raw_pitch ? 16 : 14;
    std::strncpy(metadata.colorSpace, "sRGB", sizeof(metadata.colorSpace) - 1);
    
    // 获取 RAW Bayer 数据
    uint16_t* rawData = imgdata.rawdata.raw_image;
    if (!rawData) {
        LOGE("loadRawFromBuffer: RAW image data is null");
        rawProcessor.recycle();
        throw std::runtime_error("RAW image data is null");
    }
    
    uint32_t rawWidth = imgdata.sizes.raw_width;
    uint32_t rawHeight = imgdata.sizes.raw_height;
    uint32_t rawPixels = rawWidth * rawHeight;
    
    // 获取 CFA 模式
    uint32_t cfaPattern = 0;
    if (imgdata.idata.filters > 0) {
        uint32_t filter = imgdata.idata.filters;
        if ((filter & 0x0000FF00) == 0x00009400) {
            cfaPattern = 0;  // RGGB
        } else if ((filter & 0x0000FF00) == 0x00004900) {
            cfaPattern = 1;  // GRBG
        } else if ((filter & 0x0000FF00) == 0x00006100) {
            cfaPattern = 2;  // GBRG
        } else if ((filter & 0x0000FF00) == 0x00001600) {
            cfaPattern = 3;  // BGGR
        }
    }
    
    // 复制 RAW 数据
    std::vector<uint16_t> rawBayerData(rawPixels);
    std::memcpy(rawBayerData.data(), rawData, rawPixels * sizeof(uint16_t));
    
    // 应用黑电平校正
    applyBlackLevel(rawBayerData, metadata.blackLevel, rawWidth, rawHeight);
    
    // 归一化
    std::vector<float> normalizedRawData(rawPixels);
    float whiteLevel = metadata.whiteLevel;
    for (size_t i = 0; i < rawPixels; ++i) {
        normalizedRawData[i] = std::max(0.0f, std::min(1.0f, 
            static_cast<float>(rawBayerData[i]) / whiteLevel));
    }
    
    // 去马赛克
    LinearImage demosaiced = demosaicBayerNormalized(normalizedRawData, rawWidth, rawHeight, cfaPattern);
    
    // 如果需要缩放
    if (demosaiced.width != metadata.width || demosaiced.height != metadata.height) {
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
        
        rawProcessor.recycle();
        return resized;
    }
    
    rawProcessor.recycle();
    return demosaiced;
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

} // namespace filmtracker
