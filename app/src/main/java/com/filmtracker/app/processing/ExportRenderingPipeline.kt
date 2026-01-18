package com.filmtracker.app.processing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.filmtracker.app.data.BasicAdjustmentParams
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import com.filmtracker.app.domain.error.FileSystemError
import com.filmtracker.app.domain.model.AdjustmentParams
import com.filmtracker.app.util.StorageUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * 导出渲染管线
 * 
 * 负责将调整参数应用到原始图像并导出为最终文件。
 * 与预览渲染不同，导出渲染：
 * 1. 使用完整分辨率（不降采样）
 * 2. 不使用预览缓存
 * 3. 按固定顺序处理所有阶段：GEOMETRY → TONE_BASE → CURVES → COLOR → EFFECTS → DETAILS
 * 4. 支持多种输出格式（JPEG、PNG、TIFF）
 * 5. 支持质量和位深度设置
 * 
 * 增强的错误处理：
 * - 存储空间检查
 * - 原始图像验证
 * - 详细的错误信息
 * 
 * Requirements: 1.5, 6.1, 6.2, 6.3, 6.4
 */
class ExportRenderingPipeline(
    private val context: Context,
    private val stageProcessorFactory: StageProcessorFactory = StageProcessorFactory,
    private val paramsMapper: AdjustmentParamsMapper = AdjustmentParamsMapper()
) {
    
    companion object {
        private const val TAG = "ExportRenderingPipeline"
    }
    
    /**
     * 导出格式
     */
    enum class ExportFormat {
        JPEG,
        PNG,
        TIFF
    }
    
    /**
     * 位深度
     */
    enum class BitDepth {
        BIT_8,
        BIT_16
    }
    
    /**
     * 色彩空间
     */
    enum class ColorSpace {
        SRGB,
        ADOBE_RGB,
        PROPHOTO_RGB
    }
    
    /**
     * 导出配置
     * 
     * @param format 输出格式
     * @param quality JPEG 质量（0-100），仅对 JPEG 有效
     * @param bitDepth 位深度（8-bit 或 16-bit）
     * @param colorSpace 色彩空间
     * @param outputPath 输出文件路径（已废弃，保留用于兼容性）
     * @param saveToGallery 是否保存到相册（默认 true）
     * @param displayName 显示名称（保存到相册时使用）
     */
    data class ExportConfig(
        val format: ExportFormat = ExportFormat.JPEG,
        val quality: Int = 95,
        val bitDepth: BitDepth = BitDepth.BIT_8,
        val colorSpace: ColorSpace = ColorSpace.SRGB,
        val outputPath: String = "",
        val saveToGallery: Boolean = true,
        val displayName: String = "FilmSight_${System.currentTimeMillis()}"
    ) {
        init {
            require(quality in 0..100) { "Quality must be between 0 and 100" }
        }
    }
    
    /**
     * 导出结果
     */
    sealed class ExportResult {
        data class Success(
            val outputFile: File? = null,  // 文件路径（如果保存到文件系统）
            val outputUri: Uri? = null,    // URI（如果保存到相册）
            val totalTimeMs: Long,
            val stageResults: List<StageExecutionResult>
        ) : ExportResult()
        
        data class Failure(
            val error: Throwable,
            val message: String,
            val stageResults: List<StageExecutionResult> = emptyList()
        ) : ExportResult()
    }
    
    /**
     * 导出图像
     * 
     * 完整流程：
     * 1. 验证原始图像存在
     * 2. 检查存储空间
     * 3. 加载原始图像（完整分辨率）
     * 4. 按顺序处理所有阶段
     * 5. 编码为输出格式
     * 6. 保存到指定路径
     * 
     * Requirements: 1.5, 6.1, 6.2, 6.3, 6.4
     * 
     * @param imagePath 原始图像路径
     * @param params 调整参数（领域层模型）
     * @param config 导出配置
     * @return 导出结果
     */
    suspend fun export(
        imagePath: String,
        params: AdjustmentParams,
        config: ExportConfig
    ): ExportResult {
        val startTime = System.currentTimeMillis()
        val stageResults = mutableListOf<StageExecutionResult>()
        
        Log.d(TAG, "Starting export: $imagePath -> ${config.outputPath}")
        Log.d(TAG, "Export config: format=${config.format}, quality=${config.quality}, " +
                "bitDepth=${config.bitDepth}, colorSpace=${config.colorSpace}")
        
        var currentBitmap: Bitmap? = null
        
        try {
            // 1. 验证原始图像存在
            // Requirement 1.5: Handle original image not found
            if (!StorageUtil.isFileAccessible(imagePath)) {
                Log.e(TAG, "Original image not found or not accessible: $imagePath")
                return ExportResult.Failure(
                    error = FileSystemError.ImageNotFound(imagePath),
                    message = "原始图像未找到或无法访问: $imagePath"
                )
            }
            
            // 2. 检查存储空间
            // Requirement 1.5: Handle insufficient storage space
            val imageFile = File(imagePath)
            val estimatedSize = StorageUtil.estimateExportSize(
                width = 4000,  // 估算值，实际会在加载后更新
                height = 3000,
                format = config.format.name,
                quality = config.quality
            )
            
            if (!StorageUtil.hasEnoughSpace(estimatedSize, config.outputPath)) {
                val available = StorageUtil.getAvailableSpace(config.outputPath)
                Log.e(TAG, "Insufficient storage space: required=${StorageUtil.formatBytes(estimatedSize)}, " +
                        "available=${StorageUtil.formatBytes(available)}")
                return ExportResult.Failure(
                    error = FileSystemError.InsufficientStorage(
                        requiredBytes = estimatedSize,
                        availableBytes = available,
                        operation = "导出图像"
                    ),
                    message = "存储空间不足: 需要 ${StorageUtil.formatBytes(estimatedSize)}，" +
                            "可用 ${StorageUtil.formatBytes(available)}"
                )
            }
            
            // 3. 加载原始图像（完整分辨率，不降采样）
            // Requirement 6.1: Export Full Resolution
            currentBitmap = loadOriginalImage(imagePath)
            if (currentBitmap == null) {
                Log.e(TAG, "Failed to load image: $imagePath")
                return ExportResult.Failure(
                    error = FileSystemError.ImageNotFound(imagePath),
                    message = "无法加载图像: $imagePath"
                )
            }
            
            Log.d(TAG, "Loaded original image: ${currentBitmap.width}x${currentBitmap.height}")
            
            // 更新存储空间估算（使用实际尺寸）
            val actualEstimatedSize = StorageUtil.estimateExportSize(
                width = currentBitmap.width,
                height = currentBitmap.height,
                format = config.format.name,
                quality = config.quality
            )
            
            if (!StorageUtil.hasEnoughSpace(actualEstimatedSize, config.outputPath)) {
                val available = StorageUtil.getAvailableSpace(config.outputPath)
                Log.e(TAG, "Insufficient storage space (actual): required=${StorageUtil.formatBytes(actualEstimatedSize)}, " +
                        "available=${StorageUtil.formatBytes(available)}")
                currentBitmap.recycle()
                return ExportResult.Failure(
                    error = FileSystemError.InsufficientStorage(
                        requiredBytes = actualEstimatedSize,
                        availableBytes = available,
                        operation = "导出图像"
                    ),
                    message = "存储空间不足: 需要 ${StorageUtil.formatBytes(actualEstimatedSize)}，" +
                            "可用 ${StorageUtil.formatBytes(available)}"
                )
            }
            
            // 转换参数到数据层模型
            val basicParams = paramsMapper.toData(params)
            
            // 4. 按固定顺序处理所有阶段
            // Requirement 6.2: Export Pipeline Independence (不使用预览缓存)
            val orderedStages = ProcessingStage.getOrderedStages()
            
            for (stage in orderedStages) {
                val stageStartTime = System.currentTimeMillis()
                val processor = stageProcessorFactory.getProcessor(stage)
                
                Log.d(TAG, "Processing stage: ${stage.name}")
                
                // 检查是否需要执行此阶段
                if (!processor.shouldExecute(basicParams)) {
                    stageResults.add(StageExecutionResult.success(
                        stage = stage,
                        executionTimeMs = 0,
                        fromCache = false
                    ))
                    Log.d(TAG, "Stage ${stage.name}: skipped (default params)")
                    continue
                }
                
                // 执行阶段处理
                val result = processor.process(currentBitmap!!, basicParams)
                val stageTime = System.currentTimeMillis() - stageStartTime
                
                if (result == null) {
                    Log.e(TAG, "Stage ${stage.name} failed")
                    stageResults.add(StageExecutionResult.failure(
                        stage = stage,
                        executionTimeMs = stageTime,
                        errorMessage = "Processing failed"
                    ))
                    currentBitmap.recycle()
                    return ExportResult.Failure(
                        error = FileSystemError.ExportFailure(
                            outputPath = config.outputPath,
                            stage = stage.name
                        ),
                        message = "导出失败于阶段: ${stage.name}",
                        stageResults = stageResults
                    )
                }
                
                // 更新当前位图（释放旧的）
                if (currentBitmap != result) {
                    currentBitmap.recycle()
                    currentBitmap = result
                }
                
                stageResults.add(StageExecutionResult.success(
                    stage = stage,
                    executionTimeMs = stageTime,
                    fromCache = false
                ))
                
                Log.d(TAG, "Stage ${stage.name}: completed in ${stageTime}ms")
            }
            
            // 5. 编码并保存到输出格式
            // Requirements: 6.3, 6.4
            val outputFile = File(config.outputPath)
            val encodeSuccess = encodeAndSave(currentBitmap!!, outputFile, config)
            
            if (!encodeSuccess) {
                currentBitmap.recycle()
                return ExportResult.Failure(
                    error = FileSystemError.ExportFailure(config.outputPath),
                    message = "无法保存图像到: ${config.outputPath}",
                    stageResults = stageResults
                )
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Export completed successfully in ${totalTime}ms")
            
            return ExportResult.Success(
                outputFile = outputFile,
                totalTimeMs = totalTime,
                stageResults = stageResults
            )
            
        } catch (e: FileSystemError) {
            Log.e(TAG, "File system error during export", e)
            return ExportResult.Failure(
                error = e,
                message = e.message,
                stageResults = stageResults
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during export", e)
            return ExportResult.Failure(
                error = FileSystemError.ExportFailure(config.outputPath, cause = e),
                message = "导出失败: ${e.message}",
                stageResults = stageResults
            )
        } finally {
            // 清理资源
            currentBitmap?.recycle()
        }
    }
    
    /**
     * 从位图导出图像
     * 
     * 与 export() 方法类似，但直接使用已加载的位图而不是从文件加载。
     * 适用于图像来源是 content:// URI 或其他非文件路径的情况。
     * 
     * @param originalBitmap 原始图像位图（完整分辨率）
     * @param params 调整参数（领域层模型）
     * @param config 导出配置
     * @return 导出结果
     */
    suspend fun exportFromBitmap(
        originalBitmap: Bitmap,
        params: AdjustmentParams,
        config: ExportConfig
    ): ExportResult {
        val startTime = System.currentTimeMillis()
        val stageResults = mutableListOf<StageExecutionResult>()
        
        Log.d(TAG, "Starting export from bitmap: ${originalBitmap.width}x${originalBitmap.height} -> ${config.outputPath}")
        Log.d(TAG, "Export config: format=${config.format}, quality=${config.quality}, " +
                "bitDepth=${config.bitDepth}, colorSpace=${config.colorSpace}")
        
        var currentBitmap: Bitmap? = null
        
        try {
            // 1. 检查存储空间
            val estimatedSize = StorageUtil.estimateExportSize(
                width = originalBitmap.width,
                height = originalBitmap.height,
                format = config.format.name,
                quality = config.quality
            )
            
            // 当保存到相册时，检查外部存储空间；否则检查指定路径
            val checkPath = if (config.saveToGallery) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                config.outputPath
            }
            
            if (!StorageUtil.hasEnoughSpace(estimatedSize, checkPath)) {
                val available = StorageUtil.getAvailableSpace(checkPath)
                Log.e(TAG, "Insufficient storage space: required=${StorageUtil.formatBytes(estimatedSize)}, " +
                        "available=${StorageUtil.formatBytes(available)}")
                return ExportResult.Failure(
                    error = FileSystemError.InsufficientStorage(
                        requiredBytes = estimatedSize,
                        availableBytes = available,
                        operation = "导出图像"
                    ),
                    message = "存储空间不足: 需要 ${StorageUtil.formatBytes(estimatedSize)}，" +
                            "可用 ${StorageUtil.formatBytes(available)}"
                )
            }
            
            // 2. 使用原始位图
            currentBitmap = originalBitmap
            Log.d(TAG, "Using original bitmap: ${currentBitmap.width}x${currentBitmap.height}")
            
            // 转换参数到数据层模型
            val basicParams = paramsMapper.toData(params)
            
            // 3. 按固定顺序处理所有阶段
            val orderedStages = ProcessingStage.getOrderedStages()
            
            for (stage in orderedStages) {
                val stageStartTime = System.currentTimeMillis()
                val processor = stageProcessorFactory.getProcessor(stage)
                
                Log.d(TAG, "Processing stage: ${stage.name}")
                
                // 检查是否需要执行此阶段
                if (!processor.shouldExecute(basicParams)) {
                    stageResults.add(StageExecutionResult.success(
                        stage = stage,
                        executionTimeMs = 0,
                        fromCache = false
                    ))
                    Log.d(TAG, "Stage ${stage.name}: skipped (default params)")
                    continue
                }
                
                // 执行阶段处理
                val result = processor.process(currentBitmap!!, basicParams)
                val stageTime = System.currentTimeMillis() - stageStartTime
                
                if (result == null) {
                    Log.e(TAG, "Stage ${stage.name} failed")
                    stageResults.add(StageExecutionResult.failure(
                        stage = stage,
                        executionTimeMs = stageTime,
                        errorMessage = "Processing failed"
                    ))
                    // 注意：不回收 originalBitmap，因为它不是我们创建的
                    if (currentBitmap != originalBitmap) {
                        currentBitmap?.recycle()
                    }
                    return ExportResult.Failure(
                        error = FileSystemError.ExportFailure(
                            outputPath = config.outputPath,
                            stage = stage.name
                        ),
                        message = "导出失败于阶段: ${stage.name}",
                        stageResults = stageResults
                    )
                }
                
                // 更新当前位图（释放旧的，但不释放原始位图）
                if (currentBitmap != result && currentBitmap != originalBitmap) {
                    currentBitmap?.recycle()
                }
                currentBitmap = result
                
                stageResults.add(StageExecutionResult.success(
                    stage = stage,
                    executionTimeMs = stageTime,
                    fromCache = false
                ))
                
                Log.d(TAG, "Stage ${stage.name}: completed in ${stageTime}ms")
            }
            
            // 4. 保存图像（到相册或文件）
            val (outputUri, outputFile) = if (config.saveToGallery) {
                // 保存到相册
                saveToGallery(currentBitmap!!, config)
            } else {
                // 保存到文件
                val file = File(config.outputPath)
                val success = encodeAndSave(currentBitmap!!, file, config)
                if (success) {
                    Pair(null, file)
                } else {
                    Pair(null, null)
                }
            }
            
            if (outputUri == null && outputFile == null) {
                // 清理（不回收原始位图）
                if (currentBitmap != originalBitmap) {
                    currentBitmap?.recycle()
                }
                return ExportResult.Failure(
                    error = FileSystemError.ExportFailure(config.outputPath),
                    message = "无法保存图像",
                    stageResults = stageResults
                )
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Export completed successfully in ${totalTime}ms")
            
            return ExportResult.Success(
                outputFile = outputFile,
                outputUri = outputUri,
                totalTimeMs = totalTime,
                stageResults = stageResults
            )
            
        } catch (e: FileSystemError) {
            Log.e(TAG, "File system error during export", e)
            return ExportResult.Failure(
                error = e,
                message = e.message,
                stageResults = stageResults
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during export", e)
            return ExportResult.Failure(
                error = FileSystemError.ExportFailure(config.outputPath, cause = e),
                message = "导出失败: ${e.message}",
                stageResults = stageResults
            )
        } finally {
            // 清理资源（不回收原始位图，因为它不是我们创建的）
            if (currentBitmap != null && currentBitmap != originalBitmap) {
                currentBitmap.recycle()
            }
        }
    }
    
    /**
     * 加载原始图像（完整分辨率）
     * 
     * Requirement 6.1: 不降采样，保持原始分辨率
     */
    private fun loadOriginalImage(imagePath: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                // 不降采样，加载完整分辨率
                inSampleSize = 1
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }
            
            BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: $imagePath", e)
            null
        }
    }
    
    /**
     * 编码并保存图像
     * 
     * Requirements: 6.3, 6.4
     * 
     * @param bitmap 要保存的位图
     * @param outputFile 输出文件
     * @param config 导出配置
     * @return 是否成功
     */
    private fun encodeAndSave(
        bitmap: Bitmap,
        outputFile: File,
        config: ExportConfig
    ): Boolean {
        return try {
            // 确保输出目录存在
            outputFile.parentFile?.mkdirs()
            
            // 根据格式编码
            FileOutputStream(outputFile).use { outputStream ->
                when (config.format) {
                    ExportFormat.JPEG -> {
                        // JPEG 格式：支持质量设置
                        // Requirement 6.4: Export Quality Settings
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            config.quality,
                            outputStream
                        )
                    }
                    ExportFormat.PNG -> {
                        // PNG 格式：无损压缩
                        // Requirement 6.3: 支持多种输出格式
                        bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,  // PNG 忽略质量参数
                            outputStream
                        )
                    }
                    ExportFormat.TIFF -> {
                        // TIFF 格式：目前 Android 不直接支持，使用 PNG 作为替代
                        // 注意：完整的 TIFF 支持需要额外的库（如 libtiff）
                        Log.w(TAG, "TIFF format not directly supported, using PNG instead")
                        bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            outputStream
                        )
                    }
                }
            }
            
            Log.d(TAG, "Image saved to: ${outputFile.absolutePath}")
            Log.d(TAG, "File size: ${outputFile.length() / 1024} KB")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image", e)
            false
        }
    }
    
    /**
     * 保存图像到相册
     * 
     * 使用 MediaStore API 将图像保存到系统相册
     * 
     * @param bitmap 要保存的位图
     * @param config 导出配置
     * @return Pair<Uri?, File?> - URI（相册）和 File（null）
     */
    private fun saveToGallery(
        bitmap: Bitmap,
        config: ExportConfig
    ): Pair<Uri?, File?> {
        return try {
            // 确定 MIME 类型和文件扩展名
            val (mimeType, extension) = when (config.format) {
                ExportFormat.JPEG -> Pair("image/jpeg", ".jpg")
                ExportFormat.PNG -> Pair("image/png", ".png")
                ExportFormat.TIFF -> Pair("image/png", ".png")  // TIFF 使用 PNG 替代
            }
            
            val displayName = "${config.displayName}$extension"
            
            Log.d(TAG, "Saving to gallery: $displayName")
            
            // 使用 MediaStore API
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                
                // Android Q (API 29) 及以上
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FilmSight")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            
            // 插入到 MediaStore
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val imageUri = resolver.insert(collection, contentValues)
            
            if (imageUri == null) {
                Log.e(TAG, "Failed to create MediaStore entry")
                return Pair(null, null)
            }
            
            // 写入图像数据
            resolver.openOutputStream(imageUri)?.use { outputStream ->
                val compressFormat = when (config.format) {
                    ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ExportFormat.PNG, ExportFormat.TIFF -> Bitmap.CompressFormat.PNG
                }
                
                val quality = if (config.format == ExportFormat.JPEG) config.quality else 100
                
                bitmap.compress(compressFormat, quality, outputStream)
            }
            
            // Android Q 及以上：标记为完成
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            
            Log.d(TAG, "Image saved to gallery: $imageUri")
            
            Pair(imageUri, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to gallery", e)
            Pair(null, null)
        }
    }
}
