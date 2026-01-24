package com.filmtracker.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import com.filmtracker.app.ai.AIAssistantService
import com.filmtracker.app.ai.AIConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 使用视觉大模型进行图像分割
 * 
 * 直接复用现有的 AIAssistantService
 */
class CloudVisionSegmenter(
    private val context: Context,
    private val aiConfig: AIConfig
) {
    private val aiService = AIAssistantService(aiConfig)
    
    @Serializable
    data class SegmentationResult(
        val subject: String,
        val bounding_box: BoundingBox,
        val contour_points: List<Point> = emptyList(),
        val mask_grid: List<List<Int>>? = null,  // 新增：像素级蒙版网格
        val confidence: Float = 0.9f
    )
    
    @Serializable
    data class BoundingBox(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )
    
    @Serializable
    data class Point(val x: Float, val y: Float)
    
    companion object {
        private const val TAG = "CloudVisionSegmenter"
        private const val MASK_GRID_SIZE = 64  // 64x64 蒙版网格，比深度图的 16x16 精度高 4 倍
    }
    
    /**
     * 自动识别主体
     */
    suspend fun segmentAuto(bitmap: Bitmap): SegmentationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Segmenting with vision model")
            
            val prompt = """
识别图片中的主要物体/人物，返回分割信息。

**重要规则**：
1. **优先识别人物**：如果图片中有人物，应该识别人物作为主体
2. 如果有多个人物，识别最突出/最清晰的人物
3. 如果没有人物，再识别其他主要物体

返回 JSON 格式：
{
  "subject": "物体名称（如：人物、猫、建筑等）",
  "bounding_box": {
    "x": 0.3,      // 左上角 X 坐标（0-1，归一化）
    "y": 0.2,      // 左上角 Y 坐标（0-1，归一化）
    "width": 0.4,  // 宽度（0-1，归一化）
    "height": 0.6  // 高度（0-1，归一化）
  },
  "contour_points": [
    // 主体轮廓的关键点（20-30个点，归一化坐标 0-1）
    // 按顺时针或逆时针顺序排列
    {"x": 0.35, "y": 0.25},
    {"x": 0.40, "y": 0.22},
    {"x": 0.45, "y": 0.21},
    {"x": 0.50, "y": 0.20},
    ...
  ],
  "confidence": 0.95
}

注意：
1. 只返回 JSON，不要其他文字
2. **如果有人物，必须识别人物**
3. 轮廓点要尽可能准确地描述主体边缘
4. 如果无法精确描述轮廓，可以只返回 bounding_box
""".trimIndent()
            
            val response = aiService.sendMessage(
                message = prompt,
                conversationHistory = emptyList(),
                imageBitmap = bitmap,
                userPreferences = null,
                onChunk = null
            )
            
            parseSegmentationResponse(response.message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to segment", e)
            // 返回默认值（整个图像）
            generateDefaultSegmentation()
        }
    }
    
    /**
     * 基于点击位置分割
     */
    suspend fun segmentWithPoint(
        bitmap: Bitmap,
        clickX: Float,
        clickY: Float
    ): SegmentationResult = withContext(Dispatchers.IO) {
        try {
            val prompt = """
用户点击了图片的 (${"%.2f".format(clickX)}, ${"%.2f".format(clickY)}) 位置（归一化坐标，0-1）。
识别该位置的物体，返回分割信息。

返回 JSON 格式（同上）。
只返回 JSON，不要其他文字。
""".trimIndent()
            
            val response = aiService.sendMessage(
                message = prompt,
                conversationHistory = emptyList(),
                imageBitmap = bitmap,
                userPreferences = null,
                onChunk = null
            )
            
            parseSegmentationResponse(response.message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to segment with point", e)
            // 返回点击位置周围的区域
            generatePointBasedSegmentation(clickX, clickY)
        }
    }
    
    /**
     * 解析分割响应
     */
    private fun parseSegmentationResponse(response: String): SegmentationResult {
        try {
            val jsonText = extractJson(response)
            
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            
            val result = json.decodeFromString<SegmentationResult>(jsonText)
            
            Log.d(TAG, "Segmentation successful: ${result.subject}, confidence: ${result.confidence}")
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse segmentation response", e)
            throw e
        }
    }
    
    /**
     * 从响应中提取 JSON
     */
    private fun extractJson(response: String): String {
        // 尝试提取 JSON 代码块
        val jsonBlockRegex = "```json\\s*([\\s\\S]*?)```".toRegex()
        val match = jsonBlockRegex.find(response)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        // 尝试提取 {} 包裹的内容
        val jsonRegex = "\\{[\\s\\S]*\\}".toRegex()
        val jsonMatch = jsonRegex.find(response)
        if (jsonMatch != null) {
            return jsonMatch.value
        }
        
        // 直接返回原文
        return response.trim()
    }
    
    /**
     * 生成默认分割（整个图像）
     */
    private fun generateDefaultSegmentation(): SegmentationResult {
        return SegmentationResult(
            subject = "主体",
            bounding_box = BoundingBox(0.1f, 0.1f, 0.8f, 0.8f),
            contour_points = emptyList(),
            confidence = 0.5f
        )
    }
    
    /**
     * 生成基于点击的分割
     */
    private fun generatePointBasedSegmentation(clickX: Float, clickY: Float): SegmentationResult {
        val size = 0.3f
        return SegmentationResult(
            subject = "选中区域",
            bounding_box = BoundingBox(
                x = (clickX - size / 2).coerceIn(0f, 1f),
                y = (clickY - size / 2).coerceIn(0f, 1f),
                width = size,
                height = size
            ),
            contour_points = emptyList(),
            confidence = 0.7f
        )
    }
    
    /**
     * 生成蒙版
     */
    fun generateMask(
        result: SegmentationResult,
        width: Int,
        height: Int
    ): Bitmap {
        Log.d(TAG, "Generating mask for ${result.subject}: ${width}x${height}")
        
        // 如果有轮廓点，使用轮廓生成
        if (result.contour_points.isNotEmpty()) {
            return generateMaskFromContour(result.contour_points, width, height)
        }
        
        // 否则使用边界框
        return generateMaskFromBoundingBox(result.bounding_box, width, height)
    }
    
    /**
     * 从边界框生成蒙版
     */
    private fun generateMaskFromBoundingBox(
        bbox: BoundingBox,
        width: Int,
        height: Int
    ): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val left = (bbox.x * width).toInt()
        val top = (bbox.y * height).toInt()
        val right = ((bbox.x + bbox.width) * width).toInt()
        val bottom = ((bbox.y + bbox.height) * height).toInt()
        
        // 创建带羽化的矩形蒙版
        val featherSize = 10  // 羽化像素
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = if (x in left..right && y in top..bottom) {
                    // 内部区域
                    255
                } else {
                    // 计算到边界的距离，实现羽化
                    val distToLeft = Math.abs(x - left)
                    val distToRight = Math.abs(x - right)
                    val distToTop = Math.abs(y - top)
                    val distToBottom = Math.abs(y - bottom)
                    
                    val minDist = minOf(distToLeft, distToRight, distToTop, distToBottom)
                    
                    if (minDist < featherSize) {
                        ((minDist.toFloat() / featherSize) * 255).toInt()
                    } else {
                        0
                    }
                }
                
                val color = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
                mask.setPixel(x, y, color)
            }
        }
        
        return mask
    }
    
    /**
     * 从轮廓点生成蒙版
     */
    private fun generateMaskFromContour(
        points: List<Point>,
        width: Int,
        height: Int
    ): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        
        // 转换为像素坐标并创建路径
        val path = Path()
        if (points.isNotEmpty()) {
            val firstPoint = points[0]
            path.moveTo(firstPoint.x * width, firstPoint.y * height)
            
            for (i in 1 until points.size) {
                val point = points[i]
                path.lineTo(point.x * width, point.y * height)
            }
            
            path.close()
        }
        
        // 绘制填充的多边形
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawPath(path, paint)
        
        return mask
    }
}
