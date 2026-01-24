package com.filmtracker.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.filmtracker.app.ai.AISettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 主体分割器
 * 使用分割模型实现智能抠图
 * 
 * 支持两种模式：
 * - 云端模式：使用视觉大模型（推荐）
 * - 本地模式：使用本地 TFLite 模型（待实现）
 */
class SubjectSegmenter(private val context: Context) {
    
    companion object {
        private const val TAG = "SubjectSegmenter"
        private const val MODEL_NAME = "segmentation_model.tflite"
    }
    
    private val settingsManager = AISettingsManager(context)
    private var cloudSegmenter: CloudVisionSegmenter? = null
    
    /**
     * 自动分割主体
     * @param bitmap 输入图像
     * @param useCloud 是否使用云端模型（默认 true）
     * @return 分割蒙版（黑白图，白色为主体）
     */
    suspend fun segmentAuto(bitmap: Bitmap, useCloud: Boolean = true): Bitmap = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Starting auto segmentation for ${bitmap.width}x${bitmap.height} image (cloud: $useCloud)")
            
            if (useCloud) {
                // 使用云端视觉模型
                val aiConfig = settingsManager.getAPIConfig()
                if (aiConfig != null) {
                    if (cloudSegmenter == null) {
                        cloudSegmenter = CloudVisionSegmenter(context, aiConfig)
                    }
                    val result = cloudSegmenter!!.segmentAuto(bitmap)
                    return@withContext cloudSegmenter!!.generateMask(
                        result,
                        bitmap.width,
                        bitmap.height
                    )
                } else {
                    Log.w(TAG, "AI config not found, falling back to local mode")
                }
            }
            
            // 降级到本地模式
            segmentAutoLocal(bitmap)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to segment", e)
            // 返回模拟蒙版
            generateMockMask(bitmap)
        }
    }
    
    /**
     * 基于点击位置分割
     * @param bitmap 输入图像
     * @param points 用户点击的点（归一化坐标 0-1）
     * @param useCloud 是否使用云端模型（默认 true）
     * @return 分割蒙版（黑白图，白色为选中区域）
     */
    suspend fun segmentWithPoints(
        bitmap: Bitmap,
        points: List<Pair<Float, Float>>,
        useCloud: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Starting point-based segmentation with ${points.size} points (cloud: $useCloud)")
            
            if (useCloud && points.isNotEmpty()) {
                // 使用云端视觉模型（使用第一个点）
                val aiConfig = settingsManager.getAPIConfig()
                if (aiConfig != null) {
                    if (cloudSegmenter == null) {
                        cloudSegmenter = CloudVisionSegmenter(context, aiConfig)
                    }
                    val result = cloudSegmenter!!.segmentWithPoint(
                        bitmap,
                        points[0].first,
                        points[0].second
                    )
                    return@withContext cloudSegmenter!!.generateMask(
                        result,
                        bitmap.width,
                        bitmap.height
                    )
                } else {
                    Log.w(TAG, "AI config not found, falling back to local mode")
                }
            }
            
            // 降级到本地模式
            segmentWithPointsLocal(bitmap, points)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to segment with points", e)
            // 返回基于点的模拟蒙版
            generateMockMaskWithPoints(bitmap, points)
        }
    }
    
    /**
     * 本地自动分割（待实现）
     */
    private suspend fun segmentAutoLocal(bitmap: Bitmap): Bitmap {
        // TODO: 集成 TensorFlow Lite 模型
        Log.d(TAG, "Local segmentation not yet implemented, using mock")
        return generateMockMask(bitmap)
    }
    
    /**
     * 本地点击分割（待实现）
     */
    private suspend fun segmentWithPointsLocal(
        bitmap: Bitmap,
        points: List<Pair<Float, Float>>
    ): Bitmap {
        // TODO: 集成 TensorFlow Lite 模型
        Log.d(TAG, "Local point-based segmentation not yet implemented, using mock")
        return generateMockMaskWithPoints(bitmap, points)
    }
    
    /**
     * 应用抠图蒙版
     * @param bitmap 原始图像
     * @param mask 分割蒙版
     * @param backgroundColor 背景颜色（null表示透明）
     * @return 抠图后的图像
     */
    suspend fun applyCutout(
        bitmap: Bitmap,
        mask: Bitmap,
        backgroundColor: Int? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Applying cutout mask")
            
            val width = bitmap.width
            val height = bitmap.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val maskPixels = IntArray(width * height)
            mask.getPixels(maskPixels, 0, width, 0, 0, width, height)
            
            for (i in pixels.indices) {
                val maskValue = (maskPixels[i] and 0xFF) / 255f
                
                if (maskValue > 0.5f) {
                    // 保留原像素
                    // 不需要修改
                } else {
                    // 背景区域
                    pixels[i] = backgroundColor ?: Color.TRANSPARENT
                }
            }
            
            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply cutout", e)
            throw e
        }
    }
    
    /**
     * 优化蒙版边缘
     * 使用羽化和边缘平滑
     */
    suspend fun refineMask(mask: Bitmap, featherRadius: Int = 5): Bitmap = 
        withContext(Dispatchers.Default) {
            try {
                Log.d(TAG, "Refining mask with feather radius $featherRadius")
                
                val width = mask.width
                val height = mask.height
                val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                val pixels = IntArray(width * height)
                mask.getPixels(pixels, 0, width, 0, 0, width, height)
                
                val refined = IntArray(width * height)
                
                // 应用高斯模糊进行羽化
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val idx = y * width + x
                        
                        // 计算邻域平均值
                        var sum = 0f
                        var count = 0
                        
                        for (dy in -featherRadius..featherRadius) {
                            for (dx in -featherRadius..featherRadius) {
                                val nx = x + dx
                                val ny = y + dy
                                
                                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                    val nidx = ny * width + nx
                                    val value = (pixels[nidx] and 0xFF) / 255f
                                    
                                    // 高斯权重
                                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                                    val weight = Math.exp((-distance * distance / (2 * featherRadius * featherRadius)).toDouble()).toFloat()
                                    
                                    sum += value * weight
                                    count++
                                }
                            }
                        }
                        
                        val avgValue = (sum / count * 255).toInt().coerceIn(0, 255)
                        refined[idx] = (0xFF shl 24) or (avgValue shl 16) or (avgValue shl 8) or avgValue
                    }
                }
                
                result.setPixels(refined, 0, width, 0, 0, width, height)
                result
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refine mask", e)
                mask // 返回原蒙版
            }
        }
    
    /**
     * 生成模拟蒙版（用于测试）
     * 使用中心椭圆作为主体
     */
    private fun generateMockMask(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radiusX = width * 0.3f
        val radiusY = height * 0.4f
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - centerX) / radiusX
                val dy = (y - centerY) / radiusY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
                
                val value = if (distance < 1.0) {
                    // 内部：白色（主体）
                    255
                } else if (distance < 1.2) {
                    // 边缘：渐变
                    ((1.2 - distance) / 0.2 * 255).toInt().coerceIn(0, 255)
                } else {
                    // 外部：黑色（背景）
                    0
                }
                
                val color = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
                mask.setPixel(x, y, color)
            }
        }
        
        return mask
    }
    
    /**
     * 生成基于点的模拟蒙版
     */
    private fun generateMockMaskWithPoints(
        bitmap: Bitmap,
        points: List<Pair<Float, Float>>
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        if (points.isEmpty()) {
            return mask
        }
        
        // 计算所有点的中心
        val centerX = points.map { it.first }.average().toFloat() * width
        val centerY = points.map { it.second }.average().toFloat() * height
        
        // 使用第一个点到中心的距离作为半径
        val firstX = points[0].first * width
        val firstY = points[0].second * height
        val radius = Math.sqrt(
            ((firstX - centerX) * (firstX - centerX) + 
             (firstY - centerY) * (firstY - centerY)).toDouble()
        ).toFloat() * 2f
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                
                val value = if (distance < radius) {
                    // 内部：白色（选中区域）
                    255
                } else if (distance < radius * 1.2f) {
                    // 边缘：渐变
                    ((radius * 1.2f - distance) / (radius * 0.2f) * 255).toInt().coerceIn(0, 255)
                } else {
                    // 外部：黑色（背景）
                    0
                }
                
                val color = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
                mask.setPixel(x, y, color)
            }
        }
        
        return mask
    }
}
