#ifndef FILMTRACKER_RAW_TYPES_H
#define FILMTRACKER_RAW_TYPES_H

#include <cstdint>
#include <vector>

namespace filmtracker {

// RAW 图像元数据结构
struct RawMetadata {
    uint32_t width;
    uint32_t height;
    uint32_t bitsPerSample;
    float iso;
    float exposureTime;  // 秒
    float aperture;
    float focalLength;
    float whiteBalance[2];  // [temperature, tint]
    char cameraModel[64];
    char colorSpace[16];
    
    // DNG tags
    float blackLevel;
    float whiteLevel;
    uint32_t cfaPattern[4];  // RGGB, GRBG, GBRG, BGGR
    float colorMatrix[9];    // 3x3 color matrix
};

// 线性 RGB 图像数据（32位浮点，线性光域）
struct LinearImage {
    std::vector<float> r;
    std::vector<float> g;
    std::vector<float> b;
    uint32_t width;
    uint32_t height;
    
    LinearImage(uint32_t w, uint32_t h) 
        : width(w), height(h) {
        r.resize(w * h);
        g.resize(w * h);
        b.resize(w * h);
    }
    
    // 获取像素值（线性域，范围通常 0.0 - 1.0+）
    float getR(uint32_t x, uint32_t y) const { return r[y * width + x]; }
    float getG(uint32_t x, uint32_t y) const { return g[y * width + x]; }
    float getB(uint32_t x, uint32_t y) const { return b[y * width + x]; }
    
    void setR(uint32_t x, uint32_t y, float val) { r[y * width + x] = val; }
    void setG(uint32_t x, uint32_t y, float val) { g[y * width + x] = val; }
    void setB(uint32_t x, uint32_t y, float val) { b[y * width + x] = val; }
};

// 输出图像（sRGB，8位）
struct OutputImage {
    std::vector<uint8_t> data;  // RGBA
    uint32_t width;
    uint32_t height;
    
    OutputImage(uint32_t w, uint32_t h) 
        : width(w), height(h) {
        data.resize(w * h * 4);
    }
};

} // namespace filmtracker

#endif // FILMTRACKER_RAW_TYPES_H
