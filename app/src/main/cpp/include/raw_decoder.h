#ifndef RAW_DECODER_H
#define RAW_DECODER_H

#include "raw_types.h"
#include <string>
#include <vector>
#include <cstdint>

namespace filmtracker {

/**
 * 检测 RAW 文件格式
 */
std::string detectRawFormat(const char* filePath);

/**
 * 验证 RAW 文件头
 */
bool validateRawFile(const char* filePath);

/**
 * 获取 RAW 文件的快速预览信息
 */
bool getRawFileInfo(const char* filePath, uint32_t& width, uint32_t& height);

/**
 * 提取 RAW 文件的嵌入式 JPEG 预览
 */
bool extractRawPreview(const char* filePath, std::vector<uint8_t>& jpegData);

/**
 * 优化的 RAW 解码（针对特定相机型号）
 */
bool optimizedRawDecode(const char* filePath, 
                       const char* cameraModel,
                       LinearImage& output,
                       RawMetadata& metadata);

} // namespace filmtracker

#endif // RAW_DECODER_H
