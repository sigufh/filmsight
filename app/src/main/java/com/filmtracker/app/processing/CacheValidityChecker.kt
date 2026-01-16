package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import java.security.MessageDigest

/**
 * 缓存有效性检查器
 * 
 * 负责验证缓存条目的有效性，包括：
 * 1. 参数哈希验证 - 确保缓存的参数与当前参数匹配
 * 2. 输入图像哈希验证 - 确保缓存的输入图像与当前输入匹配
 * 3. 缓存条目完整性验证 - 确保缓存的 Bitmap 未被回收
 * 
 * Requirements: 5.3, 5.6
 */
class CacheValidityChecker {
    
    companion object {
        private const val TAG = "CacheValidityChecker"
        
        // 图像哈希采样点数量（平衡精度和性能）
        private const val IMAGE_HASH_SAMPLE_SIZE = 32
        
        // 图像哈希采样步长（用于更均匀的采样）
        private const val IMAGE_HASH_SAMPLE_STEP = 7
        
        @Volatile
        private var instance: CacheValidityChecker? = null
        
        fun getInstance(): CacheValidityChecker {
            return instance ?: synchronized(this) {
                instance ?: CacheValidityChecker().also { instance = it }
            }
        }
    }
    
    /**
     * 缓存有效性检查结果
     */
    data class ValidityResult(
        val isValid: Boolean,
        val reason: InvalidityReason? = null,
        val details: String? = null
    ) {
        companion object {
            fun valid() = ValidityResult(isValid = true)
            
            fun invalid(reason: InvalidityReason, details: String? = null) = 
                ValidityResult(isValid = false, reason = reason, details = details)
        }
    }
    
    /**
     * 缓存无效原因
     */
    enum class InvalidityReason {
        /** 缓存条目不存在 */
        ENTRY_NOT_FOUND,
        
        /** Bitmap 已被回收 */
        BITMAP_RECYCLED,
        
        /** 参数哈希不匹配 */
        PARAM_HASH_MISMATCH,
        
        /** 输入图像哈希不匹配 */
        INPUT_HASH_MISMATCH,
        
        /** 缓存已过期 */
        CACHE_EXPIRED,
        
        /** 图像尺寸不匹配 */
        SIZE_MISMATCH,
        
        /** 未知错误 */
        UNKNOWN_ERROR
    }
    
    /**
     * 检查缓存条目是否有效
     * 
     * @param entry 缓存条目
     * @param expectedParamHash 期望的参数哈希
     * @param expectedInputHash 期望的输入图像哈希
     * @return 有效性检查结果
     */
    fun checkValidity(
        entry: StageCache.CacheEntry?,
        expectedParamHash: String,
        expectedInputHash: String
    ): ValidityResult {
        // 检查条目是否存在
        if (entry == null) {
            return ValidityResult.invalid(
                InvalidityReason.ENTRY_NOT_FOUND,
                "Cache entry does not exist"
            )
        }
        
        // 检查 Bitmap 是否已被回收
        if (entry.bitmap.isRecycled) {
            return ValidityResult.invalid(
                InvalidityReason.BITMAP_RECYCLED,
                "Cached bitmap has been recycled"
            )
        }
        
        // 检查参数哈希
        if (entry.key.paramHash != expectedParamHash) {
            return ValidityResult.invalid(
                InvalidityReason.PARAM_HASH_MISMATCH,
                "Parameter hash mismatch: expected=$expectedParamHash, actual=${entry.key.paramHash}"
            )
        }
        
        // 检查输入图像哈希
        if (entry.key.inputHash != expectedInputHash) {
            return ValidityResult.invalid(
                InvalidityReason.INPUT_HASH_MISMATCH,
                "Input hash mismatch: expected=$expectedInputHash, actual=${entry.key.inputHash}"
            )
        }
        
        return ValidityResult.valid()
    }
    
    /**
     * 检查缓存条目的 Bitmap 完整性
     * 
     * @param entry 缓存条目
     * @return 是否完整
     */
    fun checkBitmapIntegrity(entry: StageCache.CacheEntry?): Boolean {
        if (entry == null) return false
        if (entry.bitmap.isRecycled) return false
        
        return try {
            // 尝试访问 Bitmap 的基本属性来验证完整性
            entry.bitmap.width > 0 && entry.bitmap.height > 0
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap integrity check failed", e)
            false
        }
    }
    
    /**
     * 验证参数哈希是否匹配
     * 
     * @param stage 处理阶段
     * @param params 当前参数
     * @param cachedParamHash 缓存的参数哈希
     * @return 是否匹配
     */
    fun validateParameterHash(
        stage: ProcessingStage,
        params: BasicAdjustmentParams,
        cachedParamHash: String
    ): Boolean {
        val currentHash = computeParameterHash(stage, params)
        val matches = currentHash == cachedParamHash
        
        if (!matches) {
            try {
                Log.d(TAG, "Parameter hash mismatch for stage ${stage.name}: " +
                        "current=$currentHash, cached=$cachedParamHash")
            } catch (e: Exception) {
                // Ignore logging errors in test environment
            }
        }
        
        return matches
    }
    
    /**
     * 验证输入图像哈希是否匹配
     * 
     * @param inputBitmap 当前输入图像
     * @param cachedInputHash 缓存的输入图像哈希
     * @return 是否匹配
     */
    fun validateInputHash(
        inputBitmap: Bitmap,
        cachedInputHash: String
    ): Boolean {
        val currentHash = computeImageHash(inputBitmap)
        val matches = currentHash == cachedInputHash
        
        if (!matches) {
            try {
                Log.d(TAG, "Input hash mismatch: current=$currentHash, cached=$cachedInputHash")
            } catch (e: Exception) {
                // Ignore logging errors in test environment
            }
        }
        
        return matches
    }
    
    /**
     * 计算参数哈希
     * 
     * 只计算与指定阶段相关的参数的哈希值
     * 
     * @param stage 处理阶段
     * @param params 参数
     * @return 参数哈希字符串
     */
    fun computeParameterHash(stage: ProcessingStage, params: BasicAdjustmentParams): String {
        val sb = StringBuilder()
        sb.append(stage.name).append(":")
        
        when (stage) {
            ProcessingStage.TONE_BASE -> {
                sb.append("exp:").append(formatFloat(params.globalExposure))
                sb.append(",con:").append(formatFloat(params.contrast))
                sb.append(",hi:").append(formatFloat(params.highlights))
                sb.append(",sh:").append(formatFloat(params.shadows))
                sb.append(",wh:").append(formatFloat(params.whites))
                sb.append(",bl:").append(formatFloat(params.blacks))
            }
            ProcessingStage.CURVES -> {
                sb.append("rgb:").append(params.enableRgbCurve)
                sb.append(",rgbPts:").append(hashCurvePoints(params.rgbCurvePoints))
                sb.append(",red:").append(params.enableRedCurve)
                sb.append(",redPts:").append(hashCurvePoints(params.redCurvePoints))
                sb.append(",green:").append(params.enableGreenCurve)
                sb.append(",greenPts:").append(hashCurvePoints(params.greenCurvePoints))
                sb.append(",blue:").append(params.enableBlueCurve)
                sb.append(",bluePts:").append(hashCurvePoints(params.blueCurvePoints))
            }
            ProcessingStage.COLOR -> {
                sb.append("temp:").append(formatFloat(params.temperature))
                sb.append(",tint:").append(formatFloat(params.tint))
                sb.append(",sat:").append(formatFloat(params.saturation))
                sb.append(",vib:").append(formatFloat(params.vibrance))
                sb.append(",hsl:").append(params.enableHSL)
                sb.append(",hslH:").append(hashFloatArray(params.hslHueShift))
                sb.append(",hslS:").append(hashFloatArray(params.hslSaturation))
                sb.append(",hslL:").append(hashFloatArray(params.hslLuminance))
                sb.append(",ghT:").append(formatFloat(params.gradingHighlightsTemp))
                sb.append(",ghTi:").append(formatFloat(params.gradingHighlightsTint))
                sb.append(",gmT:").append(formatFloat(params.gradingMidtonesTemp))
                sb.append(",gmTi:").append(formatFloat(params.gradingMidtonesTint))
                sb.append(",gsT:").append(formatFloat(params.gradingShadowsTemp))
                sb.append(",gsTi:").append(formatFloat(params.gradingShadowsTint))
                sb.append(",gBl:").append(formatFloat(params.gradingBlending))
                sb.append(",gBa:").append(formatFloat(params.gradingBalance))
            }
            ProcessingStage.EFFECTS -> {
                sb.append("cla:").append(formatFloat(params.clarity))
                sb.append(",tex:").append(formatFloat(params.texture))
                sb.append(",deh:").append(formatFloat(params.dehaze))
                sb.append(",vig:").append(formatFloat(params.vignette))
                sb.append(",gra:").append(formatFloat(params.grain))
            }
            ProcessingStage.DETAILS -> {
                sb.append("sha:").append(formatFloat(params.sharpening))
                sb.append(",noi:").append(formatFloat(params.noiseReduction))
            }
        }
        
        return computeMD5Hash(sb.toString())
    }
    
    /**
     * 计算图像哈希
     * 
     * 使用采样方式计算哈希，平衡精度和性能
     * 
     * @param bitmap 输入图像
     * @return 图像哈希字符串
     */
    fun computeImageHash(bitmap: Bitmap): String {
        if (bitmap.isRecycled) {
            return "recycled"
        }
        
        val width = bitmap.width
        val height = bitmap.height
        
        val sb = StringBuilder()
        sb.append(width).append("x").append(height).append(":")
        sb.append(bitmap.config?.name ?: "unknown").append(":")
        
        // 采样像素计算哈希
        val pixels = IntArray(IMAGE_HASH_SAMPLE_SIZE)
        var pixelIndex = 0
        
        for (i in 0 until IMAGE_HASH_SAMPLE_SIZE) {
            // 使用质数步长进行更均匀的采样
            val linearIndex = (i * IMAGE_HASH_SAMPLE_STEP * width * height / IMAGE_HASH_SAMPLE_SIZE) % (width * height)
            val x = linearIndex % width
            val y = linearIndex / width
            
            try {
                pixels[pixelIndex++] = bitmap.getPixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
            } catch (e: Exception) {
                pixels[pixelIndex++] = 0
            }
        }
        
        // 将采样像素转换为哈希
        for (pixel in pixels) {
            sb.append(Integer.toHexString(pixel))
        }
        
        return computeMD5Hash(sb.toString())
    }
    
    /**
     * 快速检查两个图像是否可能相同
     * 
     * 只检查尺寸和少量采样点，用于快速排除明显不同的图像
     * 
     * @param bitmap1 第一个图像
     * @param bitmap2 第二个图像
     * @return 是否可能相同
     */
    fun quickImageComparison(bitmap1: Bitmap, bitmap2: Bitmap): Boolean {
        // 检查尺寸
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return false
        }
        
        // 检查配置
        if (bitmap1.config != bitmap2.config) {
            return false
        }
        
        // 快速采样比较（只检查 4 个角和中心）
        val checkPoints = listOf(
            Pair(0, 0),
            Pair(bitmap1.width - 1, 0),
            Pair(0, bitmap1.height - 1),
            Pair(bitmap1.width - 1, bitmap1.height - 1),
            Pair(bitmap1.width / 2, bitmap1.height / 2)
        )
        
        for ((x, y) in checkPoints) {
            try {
                if (bitmap1.getPixel(x, y) != bitmap2.getPixel(x, y)) {
                    return false
                }
            } catch (e: Exception) {
                return false
            }
        }
        
        return true
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 格式化浮点数（保留 4 位小数，避免浮点精度问题）
     */
    private fun formatFloat(value: Float): String {
        return String.format("%.4f", value)
    }
    
    /**
     * 计算曲线点列表的哈希
     */
    private fun hashCurvePoints(points: List<Pair<Float, Float>>): String {
        val sb = StringBuilder()
        for ((x, y) in points) {
            sb.append(formatFloat(x)).append(",").append(formatFloat(y)).append(";")
        }
        return computeMD5Hash(sb.toString()).take(8)
    }
    
    /**
     * 计算浮点数组的哈希
     */
    private fun hashFloatArray(array: FloatArray): String {
        val sb = StringBuilder()
        for (value in array) {
            sb.append(formatFloat(value)).append(",")
        }
        return computeMD5Hash(sb.toString()).take(8)
    }
    
    /**
     * 计算 MD5 哈希
     */
    private fun computeMD5Hash(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            try {
                Log.e(TAG, "MD5 hash computation failed", e)
            } catch (logError: Exception) {
                // Ignore logging errors in test environment
            }
            input.hashCode().toString(16)
        }
    }
    
    /**
     * 安全日志方法（用于测试环境兼容）
     */
    private fun safeLog(message: String) {
        try {
            Log.d(TAG, message)
        } catch (e: Exception) {
            // Ignore logging errors in test environment
        }
    }
}
