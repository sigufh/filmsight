package com.filmtracker.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.filmtracker.app.ai.AIConfig
import com.filmtracker.app.ai.AISettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 深度估计器
 * 使用深度估计模型生成深度图
 * 
 * 支持两种模式：
 * - 云端模式：使用视觉大模型（推荐）
 * - 本地模式：使用本地 TFLite 模型（待实现）
 */
class DepthEstimator(private val context: Context) {
    
    companion object {
        private const val TAG = "DepthEstimator"
        private const val MODEL_NAME = "depth_estimation_model.tflite"
    }
    
    private val settingsManager = AISettingsManager(context)
    private var cloudEstimator: CloudVisionDepthEstimator? = null
    
    /**
     * 估计图像深度（返回深度图和主体蒙版）
     * @param bitmap 输入图像
     * @param useCloud 是否使用云端模型（默认 true）
     * @return Pair<深度图, 主体蒙版>
     */
    suspend fun estimateWithMask(bitmap: Bitmap, useCloud: Boolean = true): Pair<Bitmap, Bitmap> = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Starting depth estimation with mask for ${bitmap.width}x${bitmap.height} image (cloud: $useCloud)")
            
            if (useCloud) {
                // 使用云端视觉模型
                val aiConfig = settingsManager.getAPIConfig()
                if (aiConfig != null) {
                    if (cloudEstimator == null) {
                        cloudEstimator = CloudVisionDepthEstimator(context, aiConfig)
                    }
                    val analysis = cloudEstimator!!.analyzeDepth(bitmap)
                    val depthMap = cloudEstimator!!.generateDepthMap(analysis, bitmap.width, bitmap.height)
                    val subjectMask = cloudEstimator!!.generateSubjectMask(analysis, bitmap.width, bitmap.height)
                    return@withContext Pair(depthMap, subjectMask)
                } else {
                    Log.w(TAG, "AI config not found, falling back to local mode")
                }
            }
            
            // 降级到本地模式
            val depthMap = estimateLocal(bitmap)
            val mockMask = generateMockMask(bitmap)
            Pair(depthMap, mockMask)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to estimate depth with mask", e)
            // 返回模拟深度图和蒙版
            val depthMap = generateMockDepthMap(bitmap)
            val mockMask = generateMockMask(bitmap)
            Pair(depthMap, mockMask)
        }
    }
    
    /**
     * 估计图像深度（仅深度图，保持向后兼容）
     * @param bitmap 输入图像
     * @param useCloud 是否使用云端模型（默认 true）
     * @return 深度图（灰度图，值越大表示越远）
     */
    suspend fun estimate(bitmap: Bitmap, useCloud: Boolean = true): Bitmap = withContext(Dispatchers.Default) {
        val (depthMap, _) = estimateWithMask(bitmap, useCloud)
        depthMap
    }
    
    /**
     * 本地深度估计（待实现）
     */
    private suspend fun estimateLocal(bitmap: Bitmap): Bitmap {
        // TODO: 集成 TensorFlow Lite 模型
        Log.d(TAG, "Local depth estimation not yet implemented, using mock")
        return generateMockDepthMap(bitmap)
    }
    
    /**
     * 应用景深效果
     * @param bitmap 原始图像
     * @param depthMap 深度图
     * @param blurAmount 模糊强度 (0-100)
     * @param focusX 焦点X坐标 (0-1)
     * @param focusY 焦点Y坐标 (0-1)
     * @param focusRadius 焦点范围 (0-1)
     * @return 应用景深效果后的图像
     */
    suspend fun applyDepthOfField(
        bitmap: Bitmap,
        depthMap: Bitmap,
        blurAmount: Float,
        focusX: Float,
        focusY: Float,
        focusRadius: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Applying depth of field effect: blur=$blurAmount, focus=($focusX, $focusY), radius=$focusRadius")
            
            val width = bitmap.width
            val height = bitmap.height
            
            // 计算焦点位置（像素坐标）
            val focusPx = (focusX * width).toInt()
            val focusPy = (focusY * height).toInt()
            
            // 获取焦点处的深度值
            val focusDepth = getDepthAt(depthMap, focusPx, focusPy)
            
            // 获取像素数据
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val depthPixels = IntArray(width * height)
            depthMap.getPixels(depthPixels, 0, width, 0, 0, width, height)
            
            // 创建模糊强度图
            val blurMap = FloatArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val idx = y * width + x
                    val depth = (depthPixels[idx] and 0xFF) / 255f
                    
                    // 计算与焦点的深度差异（增强对比度）
                    val depthDiff = Math.abs(depth - focusDepth)
                    
                    // 计算与焦点的距离
                    val dx = (x - focusPx).toFloat() / width
                    val dy = (y - focusPy).toFloat() / height
                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    
                    // 优化模糊强度计算，模拟真实大光圈效果
                    val strength = if (distance < focusRadius) {
                        // 焦点范围内，主要根据深度差异模糊
                        // 使用平方函数增强模糊效果
                        val normalizedDiff = (depthDiff * 3f).coerceIn(0f, 1f)
                        normalizedDiff * normalizedDiff * blurAmount / 100f
                    } else {
                        // 焦点范围外，强烈模糊背景
                        val distanceFactor = ((distance - focusRadius) / (1f - focusRadius)).coerceIn(0f, 1f)
                        val depthFactor = (depthDiff * 2f).coerceIn(0f, 1f)
                        // 综合距离和深度，使用更激进的模糊
                        (distanceFactor * 0.7f + depthFactor * 0.3f + 0.3f) * blurAmount / 100f
                    }
                    
                    blurMap[idx] = strength.coerceIn(0f, 1f)
                }
            }
            
            // 应用可变模糊，增加最大模糊半径以模拟大光圈
            // 根据图像尺寸动态计算最大半径
            val maxRadius = ((blurAmount / 100f) * Math.min(width, height) * 0.05f).toInt().coerceIn(5, 50)
            val result = applyVariableBlur(bitmap, blurMap, maxRadius)
            
            Log.d(TAG, "Depth of field effect applied successfully with maxRadius=$maxRadius")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply depth of field", e)
            throw e
        }
    }
    
    /**
     * 应用可变模糊（优化版，使用多次盒式模糊近似高斯模糊）
     * @param bitmap 原始图像
     * @param blurMap 模糊强度图 (0-1)
     * @param maxRadius 最大模糊半径
     */
    private fun applyVariableBlur(bitmap: Bitmap, blurMap: FloatArray, maxRadius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val resultPixels = IntArray(width * height)
        
        // 对每个像素应用模糊
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val blurStrength = blurMap[idx]
                
                if (blurStrength < 0.05f) {
                    // 模糊强度太小，不需要模糊
                    resultPixels[idx] = pixels[idx]
                } else {
                    // 计算实际模糊半径
                    val radius = (blurStrength * maxRadius).toInt().coerceAtLeast(1)
                    
                    // 使用改进的模糊算法
                    resultPixels[idx] = if (radius <= 3) {
                        // 小半径使用简单盒式模糊
                        boxBlur(pixels, width, height, x, y, radius)
                    } else {
                        // 大半径使用多次盒式模糊近似高斯模糊
                        gaussianApproximateBlur(pixels, width, height, x, y, radius)
                    }
                }
            }
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * 盒式模糊（简单快速的模糊算法）
     */
    private fun boxBlur(pixels: IntArray, width: Int, height: Int, cx: Int, cy: Int, radius: Int): Int {
        var r = 0
        var g = 0
        var b = 0
        var count = 0
        
        val x1 = (cx - radius).coerceAtLeast(0)
        val x2 = (cx + radius).coerceAtMost(width - 1)
        val y1 = (cy - radius).coerceAtLeast(0)
        val y2 = (cy + radius).coerceAtMost(height - 1)
        
        for (y in y1..y2) {
            for (x in x1..x2) {
                val pixel = pixels[y * width + x]
                r += (pixel shr 16) and 0xFF
                g += (pixel shr 8) and 0xFF
                b += pixel and 0xFF
                count++
            }
        }
        
        if (count == 0) return pixels[cy * width + cx]
        
        r /= count
        g /= count
        b /= count
        
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    
    /**
     * 高斯近似模糊（使用多次盒式模糊）
     * 根据中心极限定理，多次盒式模糊可以近似高斯模糊
     */
    private fun gaussianApproximateBlur(pixels: IntArray, width: Int, height: Int, cx: Int, cy: Int, radius: Int): Int {
        // 使用 3 次盒式模糊近似高斯模糊
        // 每次使用较小的半径
        val r1 = (radius * 0.6f).toInt().coerceAtLeast(1)
        val r2 = (radius * 0.8f).toInt().coerceAtLeast(1)
        val r3 = radius
        
        // 第一次模糊
        var result = boxBlur(pixels, width, height, cx, cy, r1)
        
        // 创建临时像素数组用于迭代模糊
        val tempPixels = pixels.clone()
        val idx = cy * width + cx
        if (idx >= 0 && idx < tempPixels.size) {
            tempPixels[idx] = result
        }
        
        // 第二次模糊
        result = boxBlur(tempPixels, width, height, cx, cy, r2)
        if (idx >= 0 && idx < tempPixels.size) {
            tempPixels[idx] = result
        }
        
        // 第三次模糊
        result = boxBlur(tempPixels, width, height, cx, cy, r3)
        
        return result
    }
    
    /**
     * 获取指定位置的深度值
     */
    private fun getDepthAt(depthMap: Bitmap, x: Int, y: Int): Float {
        val px = x.coerceIn(0, depthMap.width - 1)
        val py = y.coerceIn(0, depthMap.height - 1)
        val pixel = depthMap.getPixel(px, py)
        return (pixel and 0xFF) / 255f
    }
    
    /**
     * 从深度图提取主体蒙版（基于 AI 返回的深度值）
     * @param depthMap 深度图
     * @param targetDepth AI 返回的目标深度值 (0-255)
     * @param focusX 焦点X坐标 (0-1) - 用于日志
     * @param focusY 焦点Y坐标 (0-1) - 用于日志
     * @return 主体蒙版（白色为主体）
     */
    fun extractSubjectMaskByDepth(
        depthMap: Bitmap,
        targetDepth: Int,
        focusX: Float = 0.5f,
        focusY: Float = 0.5f
    ): Bitmap {
        Log.d(TAG, "=== EXTRACTING SUBJECT MASK BY AI DEPTH ===")
        
        val width = depthMap.width
        val height = depthMap.height
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 将 AI 返回的深度值转换为 0-1 范围
        val targetDepthNormalized = targetDepth / 255f
        
        // 检测是否为人物（AI 返回的深度值 < 80）
        val isPerson = targetDepth < 80
        
        // 使用非常小的深度容差
        val depthTolerance = if (isPerson) {
            0.10f  // 人物：约 25/255
        } else {
            0.15f
        }
        
        val minDepth = (targetDepthNormalized - depthTolerance).coerceIn(0f, 1f)
        val maxDepth = (targetDepthNormalized + depthTolerance).coerceIn(0f, 1f)
        
        Log.d(TAG, "Focus position: ($focusX, $focusY)")
        Log.d(TAG, "AI target depth: $targetDepthNormalized ($targetDepth/255)")
        Log.d(TAG, "Is person: $isPerson")
        Log.d(TAG, "Depth tolerance: $depthTolerance (${(depthTolerance * 255).toInt()}/255)")
        Log.d(TAG, "Depth range: [$minDepth, $maxDepth] -> [${(minDepth * 255).toInt()}, ${(maxDepth * 255).toInt()}]/255")
        
        // 采样深度图以检查深度分布
        val samplePoints = listOf(
            Pair(width / 4, height / 4),
            Pair(width / 2, height / 4),
            Pair(3 * width / 4, height / 4),
            Pair(width / 4, height / 2),
            Pair(width / 2, height / 2),
            Pair(3 * width / 4, height / 2),
            Pair(width / 4, 3 * height / 4),
            Pair(width / 2, 3 * height / 4),
            Pair(3 * width / 4, 3 * height / 4)
        )
        
        Log.d(TAG, "Depth map sample (9 points):")
        samplePoints.forEach { (x, y) ->
            val depth = getDepthAt(depthMap, x, y)
            Log.d(TAG, "  ($x, $y): $depth (${(depth * 255).toInt()}/255)")
        }
        
        // 第一遍：纯粹基于深度值识别
        val rawMask = BooleanArray(width * height)
        var subjectPixelCount = 0
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val depth = getDepthAt(depthMap, x, y)
                
                // 纯粹基于深度值判断
                val isSubject = depth >= minDepth && depth <= maxDepth
                
                rawMask[y * width + x] = isSubject
                if (isSubject) subjectPixelCount++
            }
        }
        
        val initialPercentage = subjectPixelCount * 100 / (width * height)
        Log.d(TAG, "Initial subject pixels: $subjectPixelCount / ${width * height} ($initialPercentage%)")
        
        // 第二遍：形态学处理
        val refinedMask = if (isPerson) {
            morphologicalRefine(rawMask, width, height, radius = 2)
        } else {
            morphologicalRefine(rawMask, width, height, radius = 1)
        }
        
        // 第三遍：边缘平滑
        val smoothedMask = edgeSmoothing(refinedMask, width, height)
        
        // 统计最终像素数
        var finalPixelCount = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (smoothedMask[y * width + x]) finalPixelCount++
                val value = if (smoothedMask[y * width + x]) 255 else 0
                val color = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
                mask.setPixel(x, y, color)
            }
        }
        
        val finalPercentage = finalPixelCount * 100 / (width * height)
        Log.d(TAG, "Final subject pixels: $finalPixelCount / ${width * height} ($finalPercentage%)")
        Log.d(TAG, "Pixel change: $initialPercentage% -> $finalPercentage%")
        Log.d(TAG, "=== SUBJECT MASK EXTRACTION COMPLETE ===")
        
        return mask
    }
    
    /**
     * 从深度图提取主体蒙版（优化版，纯基于深度值）
     * @param depthMap 深度图
     * @param focusX 焦点X坐标 (0-1)
     * @param focusY 焦点Y坐标 (0-1)
     * @param focusRadius 焦点范围 (0-1) - 不再使用
     * @return 主体蒙版（白色为主体）
     */
    fun extractSubjectMask(
        depthMap: Bitmap,
        focusX: Float = 0.5f,
        focusY: Float = 0.5f,
        focusRadius: Float = 0.3f
    ): Bitmap {
        Log.d(TAG, "=== EXTRACTING SUBJECT MASK ===")
        
        val width = depthMap.width
        val height = depthMap.height
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 获取焦点处的深度值
        val focusPx = (focusX * width).toInt().coerceIn(0, width - 1)
        val focusPy = (focusY * height).toInt().coerceIn(0, height - 1)
        val focusDepth = getDepthAt(depthMap, focusPx, focusPy)
        
        // 检测是否为人物
        val isPerson = focusDepth < 0.31f  // 80/255 ≈ 0.31
        
        // 使用非常小的深度容差，纯粹基于深度值识别
        val depthTolerance = if (isPerson) {
            0.10f  // 人物：约 25/255
        } else {
            0.15f
        }
        
        val minDepth = (focusDepth - depthTolerance).coerceIn(0f, 1f)
        val maxDepth = (focusDepth + depthTolerance).coerceIn(0f, 1f)
        
        Log.d(TAG, "Focus position: ($focusX, $focusY) -> pixel ($focusPx, $focusPy)")
        Log.d(TAG, "Focus depth: $focusDepth (${(focusDepth * 255).toInt()}/255)")
        Log.d(TAG, "Is person: $isPerson")
        Log.d(TAG, "Depth tolerance: $depthTolerance (${(depthTolerance * 255).toInt()}/255)")
        Log.d(TAG, "Depth range: [$minDepth, $maxDepth] -> [${(minDepth * 255).toInt()}, ${(maxDepth * 255).toInt()}]/255")
        
        // 采样深度图以检查深度分布
        val samplePoints = listOf(
            Pair(width / 4, height / 4),
            Pair(width / 2, height / 4),
            Pair(3 * width / 4, height / 4),
            Pair(width / 4, height / 2),
            Pair(width / 2, height / 2),
            Pair(3 * width / 4, height / 2),
            Pair(width / 4, 3 * height / 4),
            Pair(width / 2, 3 * height / 4),
            Pair(3 * width / 4, 3 * height / 4)
        )
        
        Log.d(TAG, "Depth map sample (9 points):")
        samplePoints.forEach { (x, y) ->
            val depth = getDepthAt(depthMap, x, y)
            Log.d(TAG, "  ($x, $y): $depth (${(depth * 255).toInt()}/255)")
        }
        
        // 第一遍：纯粹基于深度值识别（不考虑距离）
        val rawMask = BooleanArray(width * height)
        var subjectPixelCount = 0
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val depth = getDepthAt(depthMap, x, y)
                
                // 纯粹基于深度值判断
                val isSubject = depth >= minDepth && depth <= maxDepth
                
                rawMask[y * width + x] = isSubject
                if (isSubject) subjectPixelCount++
            }
        }
        
        val initialPercentage = subjectPixelCount * 100 / (width * height)
        Log.d(TAG, "Initial subject pixels: $subjectPixelCount / ${width * height} ($initialPercentage%)")
        
        // 第二遍：形态学处理
        val refinedMask = if (isPerson) {
            // 人物：轻度形态学处理
            morphologicalRefine(rawMask, width, height, radius = 2)
        } else {
            morphologicalRefine(rawMask, width, height, radius = 1)
        }
        
        // 第三遍：边缘平滑
        val smoothedMask = edgeSmoothing(refinedMask, width, height)
        
        // 统计最终像素数
        var finalPixelCount = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (smoothedMask[y * width + x]) finalPixelCount++
                val value = if (smoothedMask[y * width + x]) 255 else 0
                val color = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
                mask.setPixel(x, y, color)
            }
        }
        
        val finalPercentage = finalPixelCount * 100 / (width * height)
        Log.d(TAG, "Final subject pixels: $finalPixelCount / ${width * height} ($finalPercentage%)")
        Log.d(TAG, "Pixel change: $initialPercentage% -> $finalPercentage%")
        Log.d(TAG, "=== SUBJECT MASK EXTRACTION COMPLETE ===")
        
        return mask
    }
    
    /**
     * 形态学处理：先腐蚀后膨胀（开运算），去除噪点
     * 然后先膨胀后腐蚀（闭运算），填充空洞
     */
    private fun morphologicalRefine(mask: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
        // 开运算：去除小的噪点
        var result = erode(mask, width, height, radius)
        result = dilate(result, width, height, radius)
        
        // 闭运算：填充小的空洞
        result = dilate(result, width, height, radius)
        result = erode(result, width, height, radius)
        
        return result
    }
    
    /**
     * 腐蚀操作
     */
    private fun erode(mask: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
        val result = BooleanArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 检查邻域内是否全部为 true
                var allTrue = true
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        if (!mask[ny * width + nx]) {
                            allTrue = false
                            break
                        }
                    }
                    if (!allTrue) break
                }
                result[y * width + x] = allTrue
            }
        }
        
        return result
    }
    
    /**
     * 膨胀操作
     */
    private fun dilate(mask: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
        val result = BooleanArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 检查邻域内是否有任何 true
                var anyTrue = false
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        if (mask[ny * width + nx]) {
                            anyTrue = true
                            break
                        }
                    }
                    if (anyTrue) break
                }
                result[y * width + x] = anyTrue
            }
        }
        
        return result
    }
    
    /**
     * 边缘平滑：使用简单的中值滤波
     */
    private fun edgeSmoothing(mask: BooleanArray, width: Int, height: Int): BooleanArray {
        val result = BooleanArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 统计 3x3 邻域内 true 的数量
                var trueCount = 0
                var totalCount = 0
                
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            if (mask[ny * width + nx]) trueCount++
                            totalCount++
                        }
                    }
                }
                
                // 如果超过一半的邻居是 true，则该像素为 true
                result[y * width + x] = trueCount > totalCount / 2
            }
        }
        
        return result
    }
    
    /**
     * 生成模拟深度图（用于测试）
     * 使用径向渐变模拟深度
     */
    private fun generateMockDepthMap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val depthMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val maxDistance = Math.sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val depth = (distance / maxDistance * 255).toInt().coerceIn(0, 255)
                val color = (0xFF shl 24) or (depth shl 16) or (depth shl 8) or depth
                depthMap.setPixel(x, y, color)
            }
        }
        
        return depthMap
    }
    
    /**
     * 生成模拟主体蒙版（用于测试）
     * 使用中心圆形区域作为主体
     */
    private fun generateMockMask(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(width, height) / 3f
        val featherSize = radius * 0.2f
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                
                val value = if (distance < radius) {
                    255  // 主体内部
                } else if (distance < radius + featherSize) {
                    // 羽化边缘
                    ((1f - (distance - radius) / featherSize) * 255).toInt().coerceIn(0, 255)
                } else {
                    0  // 背景
                }
                
                val color = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
                mask.setPixel(x, y, color)
            }
        }
        
        return mask
    }
}
