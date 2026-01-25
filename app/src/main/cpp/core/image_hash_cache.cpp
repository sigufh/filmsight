#include "image_hash_cache.h"
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "ImageHashCache"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

// xxHash64 常量
static constexpr uint64_t PRIME64_1 = 0x9E3779B185EBCA87ULL;
static constexpr uint64_t PRIME64_2 = 0xC2B2AE3D27D4EB4FULL;
static constexpr uint64_t PRIME64_3 = 0x165667B19E3779F9ULL;
static constexpr uint64_t PRIME64_4 = 0x85EBCA77C2B2AE63ULL;
static constexpr uint64_t PRIME64_5 = 0x27D4EB2F165667C5ULL;

/**
 * xxHash64 辅助函数
 */
static inline uint64_t rotl64(uint64_t x, int r) {
    return (x << r) | (x >> (64 - r));
}

static inline uint64_t xxh64_round(uint64_t acc, uint64_t input) {
    acc += input * PRIME64_2;
    acc = rotl64(acc, 31);
    acc *= PRIME64_1;
    return acc;
}

static inline uint64_t xxh64_mergeRound(uint64_t acc, uint64_t val) {
    val = xxh64_round(0, val);
    acc ^= val;
    acc = acc * PRIME64_1 + PRIME64_4;
    return acc;
}

/**
 * xxHash64 实现
 * 
 * 基于 xxHash 算法的快速哈希函数
 * 参考：https://github.com/Cyan4973/xxHash
 */
static uint64_t xxhash64(const void* data, size_t len, uint64_t seed = 0) {
    const uint8_t* p = static_cast<const uint8_t*>(data);
    const uint8_t* const end = p + len;
    uint64_t h64;
    
    if (len >= 32) {
        const uint8_t* const limit = end - 32;
        uint64_t v1 = seed + PRIME64_1 + PRIME64_2;
        uint64_t v2 = seed + PRIME64_2;
        uint64_t v3 = seed + 0;
        uint64_t v4 = seed - PRIME64_1;
        
        do {
            v1 = xxh64_round(v1, *reinterpret_cast<const uint64_t*>(p)); p += 8;
            v2 = xxh64_round(v2, *reinterpret_cast<const uint64_t*>(p)); p += 8;
            v3 = xxh64_round(v3, *reinterpret_cast<const uint64_t*>(p)); p += 8;
            v4 = xxh64_round(v4, *reinterpret_cast<const uint64_t*>(p)); p += 8;
        } while (p <= limit);
        
        h64 = rotl64(v1, 1) + rotl64(v2, 7) + rotl64(v3, 12) + rotl64(v4, 18);
        h64 = xxh64_mergeRound(h64, v1);
        h64 = xxh64_mergeRound(h64, v2);
        h64 = xxh64_mergeRound(h64, v3);
        h64 = xxh64_mergeRound(h64, v4);
    } else {
        h64 = seed + PRIME64_5;
    }
    
    h64 += static_cast<uint64_t>(len);
    
    while (p + 8 <= end) {
        uint64_t k1 = xxh64_round(0, *reinterpret_cast<const uint64_t*>(p));
        h64 ^= k1;
        h64 = rotl64(h64, 27) * PRIME64_1 + PRIME64_4;
        p += 8;
    }
    
    if (p + 4 <= end) {
        h64 ^= static_cast<uint64_t>(*reinterpret_cast<const uint32_t*>(p)) * PRIME64_1;
        h64 = rotl64(h64, 23) * PRIME64_2 + PRIME64_3;
        p += 4;
    }
    
    while (p < end) {
        h64 ^= static_cast<uint64_t>(*p) * PRIME64_5;
        h64 = rotl64(h64, 11) * PRIME64_1;
        p++;
    }
    
    h64 ^= h64 >> 33;
    h64 *= PRIME64_2;
    h64 ^= h64 >> 29;
    h64 *= PRIME64_3;
    h64 ^= h64 >> 32;
    
    return h64;
}

ImageHashCache& ImageHashCache::getInstance() {
    static ImageHashCache instance;
    return instance;
}

uint64_t ImageHashCache::computeImageHash(const LinearImage& image) {
    // 计算图像数据的哈希
    // 我们对 R、G、B 通道分别计算哈希，然后组合
    const size_t pixelCount = image.width * image.height;
    const size_t dataSize = pixelCount * sizeof(float);
    
    uint64_t hashR = xxhash64(image.r.data(), dataSize, 0);
    uint64_t hashG = xxhash64(image.g.data(), dataSize, hashR);
    uint64_t hashB = xxhash64(image.b.data(), dataSize, hashG);
    
    // 组合三个通道的哈希
    return hashB;
}

bool ImageHashCache::find(const HashKey& key, LinearImage& output) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    auto it = m_cache.find(key);
    if (it != m_cache.end()) {
        // 缓存命中
        CacheEntry& entry = it->second;
        
        // 更新最后访问时间
        entry.lastAccess = std::chrono::steady_clock::now();
        
        // 复制结果到输出
        const uint32_t pixelCount = entry.result.width * entry.result.height;
        
        // 确保输出图像大小正确
        if (output.width != entry.result.width || output.height != entry.result.height) {
            output = LinearImage(entry.result.width, entry.result.height);
        }
        
        // 复制数据
        std::copy(entry.result.r.begin(), entry.result.r.end(), output.r.begin());
        std::copy(entry.result.g.begin(), entry.result.g.end(), output.g.begin());
        std::copy(entry.result.b.begin(), entry.result.b.end(), output.b.begin());
        
        LOGI("Cache hit: hash=0x%016llx, spatialSigma=%.2f, rangeSigma=%.2f",
             static_cast<unsigned long long>(key.imageHash), key.spatialSigma, key.rangeSigma);
        
        return true;
    }
    
    // 缓存未命中
    LOGI("Cache miss: hash=0x%016llx, spatialSigma=%.2f, rangeSigma=%.2f",
         static_cast<unsigned long long>(key.imageHash), key.spatialSigma, key.rangeSigma);
    
    return false;
}

void ImageHashCache::insert(const HashKey& key, const LinearImage& result) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    // 检查是否已存在
    if (m_cache.find(key) != m_cache.end()) {
        LOGI("Cache entry already exists, updating");
        m_cache.erase(key);
        m_currentMemoryBytes -= result.width * result.height * 3 * sizeof(float);
    }
    
    // 创建新条目
    CacheEntry entry(result.width, result.height);
    
    // 复制数据
    std::copy(result.r.begin(), result.r.end(), entry.result.r.begin());
    std::copy(result.g.begin(), result.g.end(), entry.result.g.begin());
    std::copy(result.b.begin(), result.b.end(), entry.result.b.begin());
    
    // 检查内存限制
    while (m_currentMemoryBytes + entry.memorySize > m_maxMemoryBytes && !m_cache.empty()) {
        LOGW("Memory limit exceeded, evicting LRU entry");
        evictLRU();
    }
    
    // 检查大小限制
    while (m_cache.size() >= m_maxSize && !m_cache.empty()) {
        LOGW("Size limit exceeded, evicting LRU entry");
        evictLRU();
    }
    
    // 插入新条目
    m_currentMemoryBytes += entry.memorySize;
    m_cache.emplace(key, std::move(entry));
    
    LOGI("Cache insert: hash=0x%016llx, spatialSigma=%.2f, rangeSigma=%.2f, size=%zu, memory=%zu MB",
         static_cast<unsigned long long>(key.imageHash), key.spatialSigma, key.rangeSigma,
         m_cache.size(), m_currentMemoryBytes / (1024 * 1024));
}

void ImageHashCache::clear() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    LOGI("Clearing cache: %zu entries, %zu MB", m_cache.size(), m_currentMemoryBytes / (1024 * 1024));
    
    m_cache.clear();
    m_currentMemoryBytes = 0;
}

size_t ImageHashCache::size() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_cache.size();
}

size_t ImageHashCache::memoryUsage() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_currentMemoryBytes;
}

void ImageHashCache::setMaxSize(size_t maxSize) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_maxSize = maxSize;
    
    // 如果当前大小超过新限制，驱逐条目
    while (m_cache.size() > m_maxSize && !m_cache.empty()) {
        evictLRU();
    }
}

void ImageHashCache::setMaxMemoryMB(size_t maxMemoryMB) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_maxMemoryBytes = maxMemoryMB * 1024 * 1024;
    
    // 如果当前内存使用超过新限制，驱逐条目
    enforceMemoryLimit();
}

void ImageHashCache::evictLRU() {
    if (m_cache.empty()) {
        return;
    }
    
    // 找到最久未使用的条目
    auto oldestIt = m_cache.begin();
    auto oldestTime = oldestIt->second.lastAccess;
    
    for (auto it = m_cache.begin(); it != m_cache.end(); ++it) {
        if (it->second.lastAccess < oldestTime) {
            oldestTime = it->second.lastAccess;
            oldestIt = it;
        }
    }
    
    // 移除最久未使用的条目
    LOGI("Evicting LRU entry: hash=0x%016llx, spatialSigma=%.2f, rangeSigma=%.2f",
         static_cast<unsigned long long>(oldestIt->first.imageHash),
         oldestIt->first.spatialSigma, oldestIt->first.rangeSigma);
    
    m_currentMemoryBytes -= oldestIt->second.memorySize;
    m_cache.erase(oldestIt);
}

void ImageHashCache::enforceMemoryLimit() {
    while (m_currentMemoryBytes > m_maxMemoryBytes && !m_cache.empty()) {
        evictLRU();
    }
}

} // namespace filmtracker
