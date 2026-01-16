package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.util.Log
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 阶段级缓存
 * 
 * 实现阶段级缓存存储，支持：
 * - 缓存有效性检查（基于参数哈希和输入图像哈希）
 * - LRU 淘汰策略
 * - 内存使用监控
 * 
 * Requirements: 5.1, 5.2, 5.5
 */
class StageCache(
    private val maxMemoryBytes: Long = DEFAULT_MAX_MEMORY_BYTES
) {
    companion object {
        private const val TAG = "StageCache"
        
        // 默认最大缓存大小：100MB
        const val DEFAULT_MAX_MEMORY_BYTES = 100L * 1024 * 1024
        
        // 最小缓存大小：10MB
        const val MIN_MEMORY_BYTES = 10L * 1024 * 1024
        
        // 内存警告阈值（80%）
        const val MEMORY_WARNING_THRESHOLD = 0.8f
        
        // 内存临界阈值（95%）
        const val MEMORY_CRITICAL_THRESHOLD = 0.95f
    }
    
    // 缓存有效性检查器
    private val validityChecker = CacheValidityChecker.getInstance()
    
    /**
     * 缓存键
     */
    data class CacheKey(
        val stage: ProcessingStage,
        val paramHash: String,
        val inputHash: String
    ) {
        override fun toString(): String {
            return "${stage.name}:${paramHash.take(8)}:${inputHash.take(8)}"
        }
    }
    
    /**
     * 缓存条目
     */
    data class CacheEntry(
        val key: CacheKey,
        val bitmap: Bitmap,
        val sizeBytes: Long,
        val createdAt: Long,
        var lastAccessedAt: Long,
        var accessCount: Int = 1
    ) {
        fun updateAccess() {
            lastAccessedAt = System.currentTimeMillis()
            accessCount++
        }
    }
    
    /**
     * 缓存统计
     */
    data class CacheStats(
        val totalEntries: Int,
        val memoryUsageBytes: Long,
        val maxMemoryBytes: Long,
        val hitCount: Long,
        val missCount: Long,
        val evictionCount: Long
    ) {
        val hitRate: Float
            get() = if (hitCount + missCount > 0) {
                hitCount.toFloat() / (hitCount + missCount)
            } else {
                0f
            }
        
        val memoryUsagePercent: Float
            get() = if (maxMemoryBytes > 0) {
                (memoryUsageBytes.toFloat() / maxMemoryBytes) * 100
            } else {
                0f
            }
    }
    
    // 缓存存储（线程安全）
    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()
    
    // 当前内存使用
    private val currentMemoryUsage = AtomicLong(0)
    
    // 统计计数器
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)
    
    /**
     * 获取缓存
     * 
     * @param key 缓存键
     * @return 缓存的 Bitmap，如果不存在或已失效则返回 null
     */
    fun get(key: CacheKey): Bitmap? {
        val entry = cache[key]
        
        // 使用有效性检查器验证缓存
        val validityResult = validityChecker.checkValidity(entry, key.paramHash, key.inputHash)
        
        return if (validityResult.isValid) {
            entry!!.updateAccess()
            hitCount.incrementAndGet()
            Log.d(TAG, "Cache hit: $key")
            entry.bitmap
        } else {
            missCount.incrementAndGet()
            if (entry != null) {
                // 条目存在但无效，移除它
                remove(key)
                Log.d(TAG, "Cache invalid (${validityResult.reason}), removed: $key")
            } else {
                Log.d(TAG, "Cache miss: $key")
            }
            null
        }
    }
    
    /**
     * 获取缓存并验证有效性
     * 
     * @param key 缓存键
     * @param currentParams 当前参数（用于验证）
     * @param currentInputBitmap 当前输入图像（用于验证）
     * @return 缓存的 Bitmap，如果不存在或已失效则返回 null
     */
    fun getWithValidation(
        key: CacheKey,
        currentParams: com.filmtracker.app.data.BasicAdjustmentParams,
        currentInputBitmap: Bitmap
    ): Bitmap? {
        val entry = cache[key]
        
        if (entry == null) {
            missCount.incrementAndGet()
            Log.d(TAG, "Cache miss: $key")
            return null
        }
        
        // 验证参数哈希
        if (!validityChecker.validateParameterHash(key.stage, currentParams, key.paramHash)) {
            missCount.incrementAndGet()
            remove(key)
            Log.d(TAG, "Cache invalid (param hash mismatch), removed: $key")
            return null
        }
        
        // 验证输入图像哈希
        if (!validityChecker.validateInputHash(currentInputBitmap, key.inputHash)) {
            missCount.incrementAndGet()
            remove(key)
            Log.d(TAG, "Cache invalid (input hash mismatch), removed: $key")
            return null
        }
        
        // 验证 Bitmap 完整性
        if (!validityChecker.checkBitmapIntegrity(entry)) {
            missCount.incrementAndGet()
            remove(key)
            Log.d(TAG, "Cache invalid (bitmap integrity), removed: $key")
            return null
        }
        
        entry.updateAccess()
        hitCount.incrementAndGet()
        Log.d(TAG, "Cache hit (validated): $key")
        return entry.bitmap
    }
    
    /**
     * 存储缓存
     * 
     * @param key 缓存键
     * @param bitmap 要缓存的 Bitmap
     */
    fun put(key: CacheKey, bitmap: Bitmap) {
        // 只缓存需要缓存的阶段
        if (!key.stage.shouldCache) {
            Log.d(TAG, "Stage ${key.stage} does not require caching, skipping")
            return
        }
        
        val sizeBytes = calculateBitmapSize(bitmap)
        
        // 如果单个条目超过最大缓存大小，不缓存
        if (sizeBytes > maxMemoryBytes) {
            Log.w(TAG, "Bitmap too large to cache: $sizeBytes bytes > $maxMemoryBytes bytes")
            return
        }
        
        // 确保有足够空间
        ensureCapacity(sizeBytes)
        
        // 如果已存在相同键的条目，先移除
        remove(key)
        
        // 创建新条目
        val entry = CacheEntry(
            key = key,
            bitmap = bitmap.copy(bitmap.config, false),
            sizeBytes = sizeBytes,
            createdAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis()
        )
        
        cache[key] = entry
        currentMemoryUsage.addAndGet(sizeBytes)
        
        Log.d(TAG, "Cache put: $key, size: ${sizeBytes / 1024}KB, total: ${currentMemoryUsage.get() / 1024}KB")
    }
    
    /**
     * 移除缓存条目
     */
    fun remove(key: CacheKey): Boolean {
        val entry = cache.remove(key)
        return if (entry != null) {
            currentMemoryUsage.addAndGet(-entry.sizeBytes)
            if (!entry.bitmap.isRecycled) {
                entry.bitmap.recycle()
            }
            Log.d(TAG, "Cache removed: $key")
            true
        } else {
            false
        }
    }
    
    /**
     * 移除指定阶段的所有缓存
     */
    fun removeStage(stage: ProcessingStage) {
        val keysToRemove = cache.keys.filter { it.stage == stage }
        keysToRemove.forEach { remove(it) }
        Log.d(TAG, "Removed ${keysToRemove.size} entries for stage $stage")
    }
    
    /**
     * 使指定阶段及其后续阶段的缓存失效
     * 用于参数变化时的增量失效
     */
    fun invalidateFromStage(stage: ProcessingStage) {
        val stagesToInvalidate = ProcessingStage.getOrderedStages()
            .filter { it.order >= stage.order }
        
        stagesToInvalidate.forEach { removeStage(it) }
        Log.d(TAG, "Invalidated stages from $stage: ${stagesToInvalidate.map { it.name }}")
    }
    
    /**
     * 清空所有缓存
     */
    fun clear() {
        cache.values.forEach { entry ->
            if (!entry.bitmap.isRecycled) {
                entry.bitmap.recycle()
            }
        }
        cache.clear()
        currentMemoryUsage.set(0)
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * 获取缓存统计
     */
    fun getStats(): CacheStats {
        return CacheStats(
            totalEntries = cache.size,
            memoryUsageBytes = currentMemoryUsage.get(),
            maxMemoryBytes = maxMemoryBytes,
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            evictionCount = evictionCount.get()
        )
    }
    
    /**
     * 重置统计计数器
     */
    fun resetStats() {
        hitCount.set(0)
        missCount.set(0)
        evictionCount.set(0)
    }
    
    /**
     * 检查是否有指定阶段的有效缓存
     */
    fun hasValidCache(stage: ProcessingStage, paramHash: String, inputHash: String): Boolean {
        val key = CacheKey(stage, paramHash, inputHash)
        val entry = cache[key]
        return entry != null && isEntryValid(entry)
    }
    
    /**
     * 获取指定阶段的缓存（如果存在）
     */
    fun getForStage(stage: ProcessingStage, paramHash: String, inputHash: String): Bitmap? {
        val key = CacheKey(stage, paramHash, inputHash)
        return get(key)
    }
    
    /**
     * 存储指定阶段的缓存
     */
    fun putForStage(stage: ProcessingStage, paramHash: String, inputHash: String, bitmap: Bitmap) {
        val key = CacheKey(stage, paramHash, inputHash)
        put(key, bitmap)
    }
    
    /**
     * 获取缓存有效性检查器
     */
    fun getValidityChecker(): CacheValidityChecker = validityChecker
    
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
     * 检查是否接近内存限制
     */
    fun isNearMemoryLimit(): Boolean {
        return getMemoryUsageRatio() >= MEMORY_WARNING_THRESHOLD
    }
    
    /**
     * 检查是否达到内存临界值
     */
    fun isAtCriticalMemory(): Boolean {
        return getMemoryUsageRatio() >= MEMORY_CRITICAL_THRESHOLD
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 检查缓存条目是否有效
     */
    private fun isEntryValid(entry: CacheEntry): Boolean {
        return validityChecker.checkBitmapIntegrity(entry)
    }
    
    /**
     * 计算 Bitmap 大小
     */
    private fun calculateBitmapSize(bitmap: Bitmap): Long {
        return bitmap.allocationByteCount.toLong()
    }
    
    /**
     * 确保有足够的缓存空间
     * 使用 LRU 策略淘汰最少使用的条目
     */
    private fun ensureCapacity(requiredBytes: Long) {
        while (currentMemoryUsage.get() + requiredBytes > maxMemoryBytes && cache.isNotEmpty()) {
            evictLeastRecentlyUsed()
        }
    }
    
    /**
     * 淘汰最少使用的缓存条目（LRU）
     * 
     * 使用综合评分策略：
     * - 最近访问时间（recency）
     * - 访问频率（frequency）
     * - 阶段优先级（stage priority）
     */
    private fun evictLeastRecentlyUsed() {
        // 计算每个条目的 LRU 分数
        val lruEntry = cache.values
            .minByOrNull { entry ->
                calculateLruScore(entry)
            }
        
        if (lruEntry != null) {
            remove(lruEntry.key)
            evictionCount.incrementAndGet()
            Log.d(TAG, "LRU eviction: ${lruEntry.key}, score: ${calculateLruScore(lruEntry)}")
        }
    }
    
    /**
     * 计算 LRU 分数
     * 
     * 分数越低，越容易被淘汰
     * 考虑因素：
     * 1. 最近访问时间（越久越容易淘汰）
     * 2. 访问频率（越少越容易淘汰）
     * 3. 阶段优先级（DETAILS > EFFECTS，因为计算更密集）
     */
    private fun calculateLruScore(entry: CacheEntry): Double {
        val recency = System.currentTimeMillis() - entry.lastAccessedAt
        val frequency = entry.accessCount.toDouble()
        
        // 阶段优先级：DETAILS 最高，EFFECTS 次之
        val stagePriority = when (entry.key.stage) {
            ProcessingStage.DETAILS -> 2.0
            ProcessingStage.EFFECTS -> 1.0
            else -> 0.0
        }
        
        // 综合分数：频率 * 100 - 时间衰减 + 阶段优先级 * 1000
        return (frequency * 100.0) - (recency / 1000.0) + (stagePriority * 1000.0)
    }
    
    /**
     * 批量淘汰缓存条目
     * 
     * @param count 要淘汰的条目数量
     */
    fun evictMultiple(count: Int) {
        repeat(count.coerceAtMost(cache.size)) {
            evictLeastRecentlyUsed()
        }
    }
    
    /**
     * 淘汰到目标内存使用率
     * 
     * @param targetRatio 目标内存使用率（0.0 - 1.0）
     */
    fun trimToRatio(targetRatio: Float) {
        val targetBytes = (maxMemoryBytes * targetRatio).toLong()
        while (currentMemoryUsage.get() > targetBytes && cache.isNotEmpty()) {
            evictLeastRecentlyUsed()
        }
        Log.d(TAG, "Trimmed to ratio $targetRatio, current usage: ${currentMemoryUsage.get() / 1024}KB")
    }
    
    /**
     * 响应系统低内存警告
     */
    fun onLowMemory() {
        Log.w(TAG, "System low memory warning received")
        // 紧急清理：保留最少条目
        trimToRatio(0.3f)
    }
    
    /**
     * 获取按 LRU 分数排序的条目列表（用于调试）
     */
    fun getEntriesByLruScore(): List<Pair<CacheKey, Double>> {
        return cache.values
            .map { entry -> entry.key to calculateLruScore(entry) }
            .sortedBy { it.second }
    }
}

/**
 * 参数哈希计算工具
 */
object ParameterHasher {
    
    /**
     * 计算参数的哈希值
     * 用于缓存键的生成
     */
    fun hashParameters(
        stage: ProcessingStage,
        params: com.filmtracker.app.data.BasicAdjustmentParams
    ): String {
        val relevantParams = getRelevantParameterValues(stage, params)
        return computeHash(relevantParams)
    }
    
    /**
     * 计算 Bitmap 的哈希值
     * 使用采样方式计算，避免全图扫描
     */
    fun hashBitmap(bitmap: Bitmap): String {
        // 采样计算哈希，避免全图扫描
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 16 // 采样点数
        
        val sb = StringBuilder()
        sb.append("${width}x${height}:")
        
        for (i in 0 until sampleSize) {
            val x = (width * i / sampleSize).coerceIn(0, width - 1)
            val y = (height * i / sampleSize).coerceIn(0, height - 1)
            val pixel = bitmap.getPixel(x, y)
            sb.append(pixel.toString(16))
        }
        
        return computeHash(sb.toString())
    }
    
    /**
     * 获取阶段相关的参数值字符串
     */
    private fun getRelevantParameterValues(
        stage: ProcessingStage,
        params: com.filmtracker.app.data.BasicAdjustmentParams
    ): String {
        val sb = StringBuilder()
        sb.append(stage.name).append(":")
        
        when (stage) {
            ProcessingStage.TONE_BASE -> {
                // 舍入到 1 位小数
                sb.append(roundToDecimal(params.globalExposure, 1))
                sb.append(roundToDecimal(params.contrast, 1))
                sb.append(roundToDecimal(params.highlights, 1))
                sb.append(roundToDecimal(params.shadows, 1))
                sb.append(roundToDecimal(params.whites, 1))
                sb.append(roundToDecimal(params.blacks, 1))
            }
            ProcessingStage.CURVES -> {
                sb.append(params.enableRgbCurve)
                sb.append(params.rgbCurvePoints.hashCode())
                sb.append(params.enableRedCurve)
                sb.append(params.redCurvePoints.hashCode())
                sb.append(params.enableGreenCurve)
                sb.append(params.greenCurvePoints.hashCode())
                sb.append(params.enableBlueCurve)
                sb.append(params.blueCurvePoints.hashCode())
            }
            ProcessingStage.COLOR -> {
                // 舍入到 1 位小数
                sb.append(roundToDecimal(params.temperature, 1))
                sb.append(roundToDecimal(params.tint, 1))
                sb.append(roundToDecimal(params.saturation, 1))
                sb.append(roundToDecimal(params.vibrance, 1))
                sb.append(params.enableHSL)
                sb.append(params.hslHueShift.contentHashCode())
                sb.append(params.hslSaturation.contentHashCode())
                sb.append(params.hslLuminance.contentHashCode())
                sb.append(roundToDecimal(params.gradingHighlightsTemp, 1))
                sb.append(roundToDecimal(params.gradingHighlightsTint, 1))
                sb.append(roundToDecimal(params.gradingMidtonesTemp, 1))
                sb.append(roundToDecimal(params.gradingMidtonesTint, 1))
                sb.append(roundToDecimal(params.gradingShadowsTemp, 1))
                sb.append(roundToDecimal(params.gradingShadowsTint, 1))
                sb.append(roundToDecimal(params.gradingBlending, 1))
                sb.append(roundToDecimal(params.gradingBalance, 1))
            }
            ProcessingStage.EFFECTS -> {
                // 舍入到 1 位小数，避免微小变化导致缓存失效
                sb.append(roundToDecimal(params.clarity, 1))
                sb.append(roundToDecimal(params.texture, 1))
                sb.append(roundToDecimal(params.dehaze, 1))
                sb.append(roundToDecimal(params.vignette, 1))
                sb.append(roundToDecimal(params.grain, 1))
            }
            ProcessingStage.DETAILS -> {
                // 舍入到 1 位小数
                sb.append(roundToDecimal(params.sharpening, 1))
                sb.append(roundToDecimal(params.noiseReduction, 1))
            }
        }
        
        return sb.toString()
    }
    
    /**
     * 将浮点数舍入到指定小数位数
     * 用于避免微小的浮点数差异导致缓存失效
     */
    private fun roundToDecimal(value: Float, decimals: Int): Float {
        val multiplier = Math.pow(10.0, decimals.toDouble()).toFloat()
        return Math.round(value * multiplier) / multiplier
    }
    
    /**
     * 计算字符串的 MD5 哈希
     */
    private fun computeHash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
