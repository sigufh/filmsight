# LibRaw 集成指南

## LibRaw 简介

LibRaw 是一个用于读取数码相机 RAW 文件的开源 C++ 库，支持超过 300 种相机品牌的 RAW 格式，包括：
- Sony ARW/SRF/SR2
- Canon CR2/CR3
- Nikon NEF
- Fujifilm RAF
- Olympus ORF
- Panasonic RW2
- 等等...

LibRaw 是 `rawpy` (Python) 的底层库，提供了高效的 RAW 解码功能。

## 集成方案

### 方案1：使用预编译的 LibRaw（推荐）

1. **下载 LibRaw**
   - 从 https://www.libraw.org/download 下载最新版本
   - 或使用 Git 克隆：`git clone https://github.com/LibRaw/LibRaw.git`

2. **编译 LibRaw for Android**
   ```bash
   cd LibRaw
   # 需要配置 Android NDK 工具链
   # 编译静态库或动态库
   ```

### 方案2：使用 CMake FetchContent（最简单）

在 `CMakeLists.txt` 中添加：

```cmake
include(FetchContent)

FetchContent_Declare(
  libraw
  GIT_REPOSITORY https://github.com/LibRaw/LibRaw.git
  GIT_TAG 0.21.1  # 使用最新稳定版本
)

FetchContent_MakeAvailable(libraw)

# 链接 LibRaw
target_link_libraries(filmtracker
    ${log-lib}
    ${jnigraphics-lib}
    LibRaw::raw  # LibRaw 主库
)
```

### 方案3：手动集成（如果方案2不可用）

1. 将 LibRaw 源码放到 `app/src/main/cpp/libraw/` 目录
2. 在 CMakeLists.txt 中添加：
```cmake
add_subdirectory(libraw)
target_link_libraries(filmtracker LibRaw::raw)
```

## 使用 LibRaw API

基本使用示例：

```cpp
#include <libraw/libraw.h>

LinearImage RawProcessor::loadRaw(const char* filePath, RawMetadata& metadata) {
    LibRaw rawProcessor;
    
    // 打开 RAW 文件
    int ret = rawProcessor.open_file(filePath);
    if (ret != LIBRAW_SUCCESS) {
        LOGE("Failed to open RAW file: %s", libraw_strerror(ret));
        throw std::runtime_error("Failed to open RAW file");
    }
    
    // 解包 RAW 数据
    ret = rawProcessor.unpack();
    if (ret != LIBRAW_SUCCESS) {
        LOGE("Failed to unpack RAW data: %s", libraw_strerror(ret));
        throw std::runtime_error("Failed to unpack RAW data");
    }
    
    // 获取图像信息
    libraw_image_t* img = rawProcessor.imgdata;
    metadata.width = img->sizes.width;
    metadata.height = img->sizes.height;
    metadata.iso = img->other.iso_speed;
    metadata.exposureTime = img->other.shutter;
    metadata.aperture = img->other.aperture;
    metadata.focalLength = img->other.focal_len;
    
    // 获取 RAW Bayer 数据
    uint16_t* rawData = img->rawdata.raw_image;
    uint32_t rawWidth = img->sizes.raw_width;
    uint32_t rawHeight = img->sizes.raw_height;
    
    // 应用黑电平校正
    float blackLevel = img->color.black;
    applyBlackLevel(rawData, blackLevel, rawWidth, rawHeight);
    
    // 归一化
    float whiteLevel = img->color.maximum;
    std::vector<float> normalizedData(rawWidth * rawHeight);
    for (size_t i = 0; i < normalizedData.size(); ++i) {
        normalizedData[i] = std::max(0.0f, std::min(1.0f, 
            static_cast<float>(rawData[i]) / whiteLevel));
    }
    
    // 去马赛克
    LinearImage result = demosaicBayerNormalized(normalizedData, rawWidth, rawHeight, 
                                                 img->sizes.filters);
    
    // 清理
    rawProcessor.recycle();
    
    return result;
}
```

## 优势

使用 LibRaw 的优势：
1. **支持广泛**：自动支持 300+ 种相机格式
2. **成熟稳定**：经过大量实际应用验证
3. **维护良好**：持续更新支持新相机
4. **性能优秀**：优化的解码算法
5. **API 简单**：易于集成和使用

## 注意事项

1. LibRaw 需要链接 `-lm` (数学库)
2. 某些相机可能需要特定的解码选项
3. 内存使用：LibRaw 会加载整个 RAW 文件到内存
