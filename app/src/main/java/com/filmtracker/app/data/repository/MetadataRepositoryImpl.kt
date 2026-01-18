package com.filmtracker.app.data.repository

import android.content.Context
import android.util.Log
import com.filmtracker.app.domain.error.FileSystemError
import com.filmtracker.app.domain.model.ParameterMetadata
import com.filmtracker.app.domain.repository.MetadataRepository
import com.filmtracker.app.util.RetryUtil
import com.filmtracker.app.util.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 元数据仓储实现
 * 使用文件系统存储参数元数据
 * 
 * 增强的错误处理：
 * - 自动重试（指数退避）
 * - 存储空间检查
 * - 损坏文件备份
 * - 详细的错误日志
 * 
 * Requirements: 1.5, 3.1, 3.3, 3.4
 */
class MetadataRepositoryImpl(
    private val context: Context
) : MetadataRepository {
    
    companion object {
        private const val TAG = "MetadataRepository"
        private const val BACKUP_SUFFIX = ".backup"
        
        // 估算元数据文件大小（通常很小，< 10KB）
        private const val ESTIMATED_METADATA_SIZE = 10 * 1024L
    }
    
    /**
     * 元数据存储目录
     */
    private val metadataDir: File
        get() = File(context.filesDir, "metadata").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    
    /**
     * 保存元数据（带重试和错误处理）
     * 
     * Requirements: 1.5 - 处理元数据保存失败
     */
    override suspend fun saveMetadata(metadata: ParameterMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 检查存储空间
            if (!StorageUtil.hasEnoughSpace(ESTIMATED_METADATA_SIZE)) {
                val available = StorageUtil.getAvailableSpace()
                Log.e(TAG, "Insufficient storage space: available=${StorageUtil.formatBytes(available)}")
                return@withContext Result.failure(
                    FileSystemError.InsufficientStorage(
                        requiredBytes = ESTIMATED_METADATA_SIZE,
                        availableBytes = available,
                        operation = "保存元数据"
                    )
                )
            }
            
            val fileName = ParameterMetadata.getMetadataFileName(metadata.imagePath)
            val file = File(metadataDir, fileName)
            
            // 2. 使用重试机制保存
            val retryResult = RetryUtil.retry(
                config = RetryUtil.RetryConfig.DEFAULT
            ) { attemptNumber ->
                Log.d(TAG, "Saving metadata (attempt $attemptNumber): ${metadata.imagePath}")
                
                // 序列化为 JSON
                val jsonString = metadata.toJson()
                
                // 原子写入：先写入临时文件，然后重命名
                val tempFile = File(file.parent, "${file.name}.tmp")
                tempFile.writeText(jsonString)
                
                // 重命名为最终文件
                if (!tempFile.renameTo(file)) {
                    throw IOException("Failed to rename temp file to final file")
                }
                
                Log.d(TAG, "Metadata saved successfully: ${file.absolutePath}")
            }
            
            when (retryResult) {
                is RetryUtil.RetryResult.Success -> {
                    if (retryResult.attemptCount > 1) {
                        Log.w(TAG, "Metadata saved after ${retryResult.attemptCount} attempts")
                    }
                    Result.success(Unit)
                }
                is RetryUtil.RetryResult.Failure -> {
                    Log.e(TAG, "Failed to save metadata after ${retryResult.attemptCount} attempts", retryResult.error)
                    Result.failure(
                        FileSystemError.MetadataSaveFailure(
                            imagePath = metadata.imagePath,
                            attemptCount = retryResult.attemptCount,
                            cause = retryResult.error
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving metadata", e)
            Result.failure(
                FileSystemError.MetadataSaveFailure(
                    imagePath = metadata.imagePath,
                    attemptCount = 0,
                    cause = e
                )
            )
        }
    }
    
    /**
     * 加载元数据（带损坏文件处理）
     * 
     * Requirements: 3.4 - 处理损坏的元数据文件
     */
    override suspend fun loadMetadata(imagePath: String): Result<ParameterMetadata?> = withContext(Dispatchers.IO) {
        try {
            val fileName = ParameterMetadata.getMetadataFileName(imagePath)
            val file = File(metadataDir, fileName)
            
            if (!file.exists()) {
                Log.d(TAG, "Metadata file not found: $imagePath")
                return@withContext Result.success(null)
            }
            
            // 检查文件是否可读
            if (!file.canRead()) {
                Log.e(TAG, "Metadata file not readable: ${file.absolutePath}")
                return@withContext Result.failure(
                    FileSystemError.PermissionDenied(file.absolutePath)
                )
            }
            
            try {
                // 读取文件并反序列化
                val jsonString = file.readText()
                val metadata = ParameterMetadata.fromJson(jsonString)
                
                Log.d(TAG, "Metadata loaded successfully: $imagePath")
                Result.success(metadata)
                
            } catch (e: Exception) {
                // JSON 解析错误 - 文件可能损坏
                Log.e(TAG, "Corrupted metadata file: ${file.absolutePath}", e)
                
                // 备份损坏的文件
                backupCorruptedFile(file)
                
                // 返回 null（使用默认参数）
                Result.success(null)
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "IO error loading metadata: $imagePath", e)
            Result.failure(
                FileSystemError.MetadataLoadFailure(imagePath, e)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading metadata: $imagePath", e)
            Result.failure(
                FileSystemError.MetadataLoadFailure(imagePath, e)
            )
        }
    }
    
    /**
     * 备份损坏的文件
     */
    private fun backupCorruptedFile(file: File) {
        try {
            val backupFile = File(file.parent, "${file.name}$BACKUP_SUFFIX")
            file.copyTo(backupFile, overwrite = true)
            Log.d(TAG, "Corrupted file backed up: ${backupFile.absolutePath}")
            
            // 删除原文件
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup corrupted file", e)
        }
    }
    
    override suspend fun deleteMetadata(imagePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fileName = ParameterMetadata.getMetadataFileName(imagePath)
            val file = File(metadataDir, fileName)
            
            if (file.exists()) {
                file.delete()
            }
            
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun metadataExists(imagePath: String): Boolean = withContext(Dispatchers.IO) {
        val fileName = ParameterMetadata.getMetadataFileName(imagePath)
        val file = File(metadataDir, fileName)
        file.exists()
    }
    
    override suspend fun getAllMetadata(): Result<List<ParameterMetadata>> = withContext(Dispatchers.IO) {
        try {
            val metadataList = mutableListOf<ParameterMetadata>()
            
            // 遍历元数据目录中的所有文件
            metadataDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(ParameterMetadata.FILE_EXTENSION)) {
                    try {
                        val jsonString = file.readText()
                        val metadata = ParameterMetadata.fromJson(jsonString)
                        metadataList.add(metadata)
                    } catch (e: Exception) {
                        // 跳过损坏的文件,继续处理其他文件
                        // 可以在这里记录日志
                    }
                }
            }
            
            Result.success(metadataList)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
