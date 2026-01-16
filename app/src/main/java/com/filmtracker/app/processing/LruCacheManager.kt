package com.filmtracker.app.processing

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * LRU 缓存管理器
 * 
 * 实现智能 LRU 缓存淘汰策略：
 * 1. 监控内存使用
 * 2. 自动清理最少使用的缓存
 * 3. 支持优先级保护（保护最近使用和最常使用的缓存）
 * 
 * Requirements: 5.5, 9.2, 9.3
 */
class LruCacheManager<K, V>(
    private val maxMemoryBytes: Long,
    private val sizeCalculator: (V) -> Long,
    private val onEvict: ((K, V) -> Unit)? = null
) {
    companion object {
        private const val TAG = "LruCacheManager"
        
        // 默认淘汰批次大小
        private const val DEFAULT_EVICTION_BATCH_SIZE = 3
        
        // 内存警告阈值（80%）
        const val MEMORY_WARNING_THRESHOLD = 0.8f
        
        // 内存临界阈值（95%）
        const val MEMORY_CRITICAL_THRESHOLD = 0.95f
        
        // 紧急清理阈值（98%）
        const val MEMORY_EMERGENCY_THRESHOLD = 0.98f
        
        // 最小保留条目数
        private const val MIN_ENTRIES_TO_KEEP = 2
    }
    
    /**
     * 缓存条目包装器
     */
    data class CacheEntryWrapper<K, V>(
        val key: K,
        val value: V,
        val sizeBytes: Long,
        val createdAt: Long = System.currentTimeMillis(),
        var lastAccessedAt: Long = System.currentTimeMillis(),
        var accessCount: Int = 1,
        var priority: Int = 0  // 优先级，越高越不容易被淘汰
    ) {
        fun updateAccess() {
            lastAccessedAt = System.currentTimeMillis()
            accessCount++
        }
        
        /**
         * 计算 LRU 分数（越低越容易被淘汰）
         */
        fun calculateLruScore(): Double {
            val recency = System.currentTimeMillis() - lastAccessedAt
            val frequency = accessCount.toDouble()
            val priorityBonus = priority * 1000.0
            
            // 综合考虑：最近访问时间、访问频率、优先级
            // 分数越高，越不容易被淘汰
            return (frequency * 100.0) - (recency / 1000.0) + priorityBonus
        }
    }
    
    /**
     * 淘汰统计
     */
    data class EvictionStats(
        val totalEvictions: Long,
        val evictedBytes: Long,
        val lastEvictionTime: Long,
        val evictionsByReason: Map<EvictionReason, Long>
    )
    
    /**
     * 淘汰原因
     */
    enum class EvictionReason {
        MEMORY_PRESSURE,      // 内存压力
        CAPACITY_LIMIT,       // 容量限制
        MANUAL_CLEAR,         // 手动清理
        ENTRY_EXPIRED,        // 条目过期
        SYSTEM_LOW_MEMORY     // 系统低内存
    }
    
    // 缓存存储
    private val cache = ConcurrentHashMap<K, CacheEntryWrapper<K, V>>()
    
    // 读写锁（用于淘汰操作）
    private val lock = ReentrantReadWriteLock()
    
    // 当前内存使用
    private val currentMemoryUsage = AtomicLong(0)
    
    // 淘汰统计
    private val totalEvictions = AtomicLong(0)
    private val evictedBytes = AtomicLong(0)
    private var lastEvictionTime = 0L
    private val evictionsByReason = ConcurrentHashMap<EvictionReason, AtomicLong>()
    
    // 内存监控回调
    private var memoryWarningCallback: (() -> Unit)? = null
    private var memoryCriticalCallback: (() -> Unit)? = null
    
    /**
     * 获取缓存条目
     */
    fun get(key: K): V? {
        return lock.read {
            cache[key]?.also { it.updateAccess() }?.value
        }
    }
    
    /**
     * 存储缓存条目
     */
    fun put(key: K, value: V, priority: Int = 0): Boolean {
        val sizeBytes = sizeCalculator(value)
        
        // 检查单个条目是否超过最大缓存大小
        if (sizeBytes > maxMemoryBytes) {
            Log.w(TAG, "Entry too large to cache: $sizeBytes bytes > $maxMemoryBytes bytes")
            return false
        }
        
        return lock.write {
            // 确保有足够空间
            ensureCapacity(sizeBytes)
            
            // 如果已存在相同键的条目，先移除
            remove(key)
            
            // 创建新条目
            val entry = CacheEntryWrapper(
                key = key,
                value = value,
                sizeBytes = sizeBytes,
                priority = priority
            )
            
            cache[key] = entry
            currentMemoryUsage.addAndGet(sizeBytes)
            
            // 检查内存状态
            checkMemoryStatus()
            
            Log.d(TAG, "Cache put: $key, size: ${sizeBytes / 1024}KB, " +
                    "total: ${currentMemoryUsage.get() / 1024}KB")
            true
        }
    }
    
    /**
     * 移除缓存条目
     */
    fun remove(key: K): Boolean {
        return lock.write {
            val entry = cache.remove(key)
            if (entry != null) {
                currentMemoryUsage.addAndGet(-entry.sizeBytes)
                onEvict?.invoke(key, entry.value)
                Log.d(TAG, "Cache removed: $key")
                true
            } else {
                false
            }
        }
    }
    
    /**
     * 清空所有缓存
     */
    fun clear() {
        lock.write {
            cache.values.forEach { entry ->
                onEvict?.invoke(entry.key, entry.value)
            }
            cache.clear()
            currentMemoryUsage.set(0)
            recordEviction(EvictionReason.MANUAL_CLEAR, 0)
            Log.d(TAG, "Cache cleared")
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun size(): Int = cache.size
    
    /**
     * 获取当前内存使用
     */
    fun getMemoryUsage(): Long = currentMemoryUsage.get()
    
    /**
     * 获取内存使用率
     */
    fun getMemoryUsageRatio(): Float {
        return if (maxMemoryBytes > 0) {
            currentMemoryUsage.get().toFloat() / maxMemoryBytes
        } else {
            0f
        }
    }
    
    /**
     * 检查是否包含指定键
     */
    fun containsKey(key: K): Boolean = cache.containsKey(key)
    
    /**
     * 获取所有键
     */
    fun keys(): Set<K> = cache.keys.toSet()
    
    /**
     * 设置内存警告回调
     */
    fun setMemoryWarningCallback(callback: () -> Unit) {
        memoryWarningCallback = callback
    }
    
    /**
     * 设置内存临界回调
     */
    fun setMemoryCriticalCallback(callback: () -> Unit) {
        memoryCriticalCallback = callback
    }
    
    /**
     * 手动触发内存清理
     * 
     * @param targetRatio 目标内存使用率
     */
    fun trimToRatio(targetRatio: Float) {
        lock.write {
            val targetBytes = (maxMemoryBytes * targetRatio).toLong()
            while (currentMemoryUsage.get() > targetBytes && cache.size > MIN_ENTRIES_TO_KEEP) {
                evictLeastValuable(EvictionReason.MANUAL_CLEAR)
            }
        }
    }
    
    /**
     * 响应系统低内存警告
     */
    fun onLowMemory() {
        Log.w(TAG, "System low memory warning received")
        lock.write {
            // 紧急清理：保留最少条目
            while (cache.size > MIN_ENTRIES_TO_KEEP) {
                evictLeastValuable(EvictionReason.SYSTEM_LOW_MEMORY)
            }
        }
    }
    
    /**
     * 获取淘汰统计
     */
    fun getEvictionStats(): EvictionStats {
        return EvictionStats(
            totalEvictions = totalEvictions.get(),
            evictedBytes = evictedBytes.get(),
            lastEvictionTime = lastEvictionTime,
            evictionsByReason = evictionsByReason.mapValues { it.value.get() }
        )
    }
    
    /**
     * 更新条目优先级
     */
    fun updatePriority(key: K, priority: Int) {
        cache[key]?.priority = priority
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 确保有足够的缓存空间
     */
    private fun ensureCapacity(requiredBytes: Long) {
        while (currentMemoryUsage.get() + requiredBytes > maxMemoryBytes && 
               cache.size > MIN_ENTRIES_TO_KEEP) {
            evictLeastValuable(EvictionReason.CAPACITY_LIMIT)
        }
    }
    
    /**
     * 淘汰最不值得保留的条目
     */
    private fun evictLeastValuable(reason: EvictionReason) {
        // 找到 LRU 分数最低的条目
        val lruEntry = cache.values
            .minByOrNull { it.calculateLruScore() }
        
        if (lruEntry != null) {
            cache.remove(lruEntry.key)
            currentMemoryUsage.addAndGet(-lruEntry.sizeBytes)
            onEvict?.invoke(lruEntry.key, lruEntry.value)
            recordEviction(reason, lruEntry.sizeBytes)
            Log.d(TAG, "LRU eviction ($reason): ${lruEntry.key}, " +
                    "score: ${lruEntry.calculateLruScore()}")
        }
    }
    
    /**
     * 记录淘汰统计
     */
    private fun recordEviction(reason: EvictionReason, bytes: Long) {
        totalEvictions.incrementAndGet()
        evictedBytes.addAndGet(bytes)
        lastEvictionTime = System.currentTimeMillis()
        evictionsByReason.getOrPut(reason) { AtomicLong(0) }.incrementAndGet()
    }
    
    /**
     * 检查内存状态并触发回调
     */
    private fun checkMemoryStatus() {
        val ratio = getMemoryUsageRatio()
        
        when {
            ratio >= MEMORY_EMERGENCY_THRESHOLD -> {
                Log.w(TAG, "Memory emergency: ${(ratio * 100).toInt()}%")
                // 紧急清理
                while (getMemoryUsageRatio() > MEMORY_WARNING_THRESHOLD && 
                       cache.size > MIN_ENTRIES_TO_KEEP) {
                    evictLeastValuable(EvictionReason.MEMORY_PRESSURE)
                }
                memoryCriticalCallback?.invoke()
            }
            ratio >= MEMORY_CRITICAL_THRESHOLD -> {
                Log.w(TAG, "Memory critical: ${(ratio * 100).toInt()}%")
                memoryCriticalCallback?.invoke()
            }
            ratio >= MEMORY_WARNING_THRESHOLD -> {
                Log.d(TAG, "Memory warning: ${(ratio * 100).toInt()}%")
                memoryWarningCallback?.invoke()
            }
        }
    }
}
