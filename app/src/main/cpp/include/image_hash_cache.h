#ifndef FILMTRACKER_IMAGE_HASH_CACHE_H
#define FILMTRACKER_IMAGE_HASH_CACHE_H

#include "raw_types.h"
#include <unordered_map>
#include <mutex>
#include <chrono>
#include <cstdint>

namespace filmtracker {

/**
 * 图像哈希缓存系统
 * 
 * 使用 xxHash64 算法计算图像内容哈希，实现 LRU 缓存管理
 * 用于避免重复计算双边滤波结果
 */
class ImageHashCache {
public:
    /**
     * 哈希键（包含图像内容和参数）
     */
    struct HashKey {
        uint64_t imageHash;      // 图像内容哈希（xxHash64）
        float spatialSigma;
        float rangeSigma;
        
        bool operator==(const HashKey& other) const {
            return imageHash == other.imageHash &&
                   std::abs(spatialSigma - other.spatialSigma) < 0.001f &&
                   std::abs(rangeSigma - other.rangeSigma) < 0.001f;
        }
    };
    
    /**
     * 哈希函数
     */
    struct HashKeyHash {
        size_t operator()(const HashKey& key) const {
            // 组合哈希：使用 imageHash 作为主要哈希，参数作为次要哈希
            size_t h1 = std::hash<uint64_t>{}(key.imageHash);
            size_t h2 = std::hash<float>{}(key.spatialSigma);
            size_t h3 = std::hash<float>{}(key.rangeSigma);
            return h1 ^ (h2 << 1) ^ (h3 << 2);
        }
    };
    
    /**
     * 缓存条目
     */
    struct CacheEntry {
        LinearImage result;
        size_t memorySize;
        std::chrono::steady_clock::time_point lastAccess;
        
        CacheEntry(uint32_t width, uint32_t height)
            : result(width, height)
            , memorySize(width * height * 3 * sizeof(float))
            , lastAccess(std::chrono::steady_clock::now()) {
        }
    };
    
    /**
     * 获取单例
     */
    static ImageHashCache& getInstance();
    
    /**
     * 查找缓存
     * 
     * @param key 哈希键
     * @param output 输出图像（如果找到）
     * @return 是否找到缓存
     */
    bool find(const HashKey& key, LinearImage& output);
    
    /**
     * 插入缓存
     * 
     * @param key 哈希键
     * @param result 结果图像
     */
    void insert(const HashKey& key, const LinearImage& result);
    
    /**
     * 清除缓存
     */
    void clear();
    
    /**
     * 获取缓存统计
     */
    size_t size() const;
    size_t memoryUsage() const;
    
    /**
     * 配置
     */
    void setMaxSize(size_t maxSize);
    void setMaxMemoryMB(size_t maxMemoryMB);
    
    /**
     * 计算图像哈希（使用 xxHash64）
     * 
     * @param image 输入图像
     * @return 64位哈希值
     */
    static uint64_t computeImageHash(const LinearImage& image);
    
private:
    ImageHashCache() = default;
    ~ImageHashCache() = default;
    
    // 禁止拷贝和赋值
    ImageHashCache(const ImageHashCache&) = delete;
    ImageHashCache& operator=(const ImageHashCache&) = delete;
    
    /**
     * LRU 驱逐
     * 
     * 移除最久未使用的条目
     */
    void evictLRU();
    
    /**
     * 检查内存限制
     * 
     * 如果超过内存限制，驱逐条目直到满足限制
     */
    void enforceMemoryLimit();
    
    std::unordered_map<HashKey, CacheEntry, HashKeyHash> m_cache;
    size_t m_maxSize = 10;
    size_t m_maxMemoryBytes = 100 * 1024 * 1024;  // 100MB
    size_t m_currentMemoryBytes = 0;
    mutable std::mutex m_mutex;
};

} // namespace filmtracker

#endif // FILMTRACKER_IMAGE_HASH_CACHE_H
