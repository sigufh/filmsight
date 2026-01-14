/**
 * RAW 解码器实现
 * 
 * 注意：主要的 RAW 解码功能已经在 raw_processor.cpp 中通过 LibRaw 实现
 * 本文件提供辅助函数和特定格式的解码器扩展
 * 
 * 当前实现：
 * - raw_processor.cpp 中的 loadRaw() 使用 LibRaw 处理所有主流 RAW 格式
 * - raw_processor.cpp 中的 loadArwFile() 提供 Sony ARW 的特殊处理
 * - raw_processor.cpp 中的 loadRawFromBuffer() 支持从内存缓冲区加载
 * 
 * 未来扩展：
 * - 特定相机型号的优化解码器
 * - 自定义 RAW 格式支持
 * - 解码性能优化
 */

#include "raw_processor.h"
#include <libraw.h>
#include <android/log.h>
#include <fstream>
#include <vector>
#include <cstring>

#define LOG_TAG "RawDecoder"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

/**
 * 检测 RAW 文件格式
 * 
 * @param filePath RAW 文件路径
 * @return 格式字符串（"ARW", "CR2", "NEF", "DNG" 等）
 */
std::string detectRawFormat(const char* filePath) {
    if (!filePath) {
        return "UNKNOWN";
    }
    
    std::string path(filePath);
    size_t dotPos = path.find_last_of('.');
    if (dotPos == std::string::npos) {
        return "UNKNOWN";
    }
    
    std::string extension = path.substr(dotPos + 1);
    
    // 转换为大写
    for (char& c : extension) {
        c = std::toupper(c);
    }
    
    LOGI("detectRawFormat: Detected format '%s' from file '%s'", extension.c_str(), filePath);
    return extension;
}

/**
 * 验证 RAW 文件头
 * 
 * @param filePath RAW 文件路径
 * @return true 如果是有效的 RAW 文件
 */
bool validateRawFile(const char* filePath) {
    if (!filePath) {
        LOGE("validateRawFile: File path is null");
        return false;
    }
    
    std::ifstream file(filePath, std::ios::binary);
    if (!file.is_open()) {
        LOGE("validateRawFile: Cannot open file '%s'", filePath);
        return false;
    }
    
    // 读取文件头（前 8 字节）
    uint8_t header[8];
    file.read(reinterpret_cast<char*>(header), 8);
    
    if (file.gcount() < 8) {
        LOGE("validateRawFile: File too small");
        return false;
    }
    
    // 检查 TIFF 头（大多数 RAW 格式基于 TIFF）
    bool isTiff = (header[0] == 0x49 && header[1] == 0x49) ||  // Little-endian
                  (header[0] == 0x4D && header[1] == 0x4D);     // Big-endian
    
    if (isTiff) {
        LOGI("validateRawFile: Valid TIFF-based RAW file");
        return true;
    }
    
    // 检查其他 RAW 格式的魔数
    // DNG: 通常是 TIFF 格式
    // CR2 (Canon): TIFF 格式
    // NEF (Nikon): TIFF 格式
    // ARW (Sony): TIFF 格式
    // RAF (Fuji): "FUJIFILMCCD-RAW"
    if (header[0] == 'F' && header[1] == 'U' && header[2] == 'J' && header[3] == 'I') {
        LOGI("validateRawFile: Valid Fuji RAF file");
        return true;
    }
    
    LOGE("validateRawFile: Unknown file format");
    return false;
}

/**
 * 获取 RAW 文件的快速预览信息
 * 
 * @param filePath RAW 文件路径
 * @param width 输出：图像宽度
 * @param height 输出：图像高度
 * @return true 如果成功获取信息
 */
bool getRawFileInfo(const char* filePath, uint32_t& width, uint32_t& height) {
    if (!filePath) {
        LOGE("getRawFileInfo: File path is null");
        return false;
    }
    
    // 使用 LibRaw 快速获取信息（不解码完整图像）
    LibRaw rawProcessor;
    
    int ret = rawProcessor.open_file(filePath);
    if (ret != LIBRAW_SUCCESS) {
        LOGE("getRawFileInfo: Failed to open file: %s", libraw_strerror(ret));
        return false;
    }
    
    // 使用原始图像尺寸（未裁剪的完整传感器数据）
    width = rawProcessor.imgdata.sizes.raw_width;
    height = rawProcessor.imgdata.sizes.raw_height;
    
    // 如果 raw_width/raw_height 为 0，使用 iwidth/iheight（解码后的图像尺寸）
    if (width == 0 || height == 0) {
        width = rawProcessor.imgdata.sizes.iwidth;
        height = rawProcessor.imgdata.sizes.iheight;
    }
    
    rawProcessor.recycle();
    
    LOGI("getRawFileInfo: Image size = %ux%u", width, height);
    return true;
}

/**
 * 提取 RAW 文件的嵌入式 JPEG 预览
 * 
 * @param filePath RAW 文件路径
 * @param jpegData 输出：JPEG 数据
 * @return true 如果成功提取预览
 */
bool extractRawPreview(const char* filePath, std::vector<uint8_t>& jpegData) {
    if (!filePath) {
        LOGE("extractRawPreview: File path is null");
        return false;
    }
    
    LibRaw rawProcessor;
    
    int ret = rawProcessor.open_file(filePath);
    if (ret != LIBRAW_SUCCESS) {
        LOGE("extractRawPreview: Failed to open file: %s", libraw_strerror(ret));
        return false;
    }
    
    ret = rawProcessor.unpack_thumb();
    if (ret != LIBRAW_SUCCESS) {
        LOGE("extractRawPreview: Failed to unpack thumbnail: %s", libraw_strerror(ret));
        rawProcessor.recycle();
        return false;
    }
    
    libraw_processed_image_t* thumb = rawProcessor.dcraw_make_mem_thumb(&ret);
    if (!thumb || ret != LIBRAW_SUCCESS) {
        LOGE("extractRawPreview: Failed to create thumbnail: %s", libraw_strerror(ret));
        rawProcessor.recycle();
        return false;
    }
    
    // 复制 JPEG 数据
    jpegData.resize(thumb->data_size);
    std::memcpy(jpegData.data(), thumb->data, thumb->data_size);
    
    LibRaw::dcraw_clear_mem(thumb);
    rawProcessor.recycle();
    
    LOGI("extractRawPreview: Extracted %zu bytes of JPEG data", jpegData.size());
    return true;
}

/**
 * 优化的 RAW 解码（针对特定相机型号）
 * 
 * 这个函数可以根据相机型号应用特定的优化
 */
bool optimizedRawDecode(const char* filePath, 
                       const char* cameraModel,
                       LinearImage& output,
                       RawMetadata& metadata) {
    // 当前实现：使用标准 LibRaw 解码
    // 未来可以添加针对特定相机的优化
    
    LOGI("optimizedRawDecode: Using standard LibRaw decode for camera '%s'", 
         cameraModel ? cameraModel : "unknown");
    
    RawProcessor processor;
    output = processor.loadRaw(filePath, metadata);
    
    return true;
}

} // namespace filmtracker
