package com.filmtracker.app.domain.error

import java.io.IOException

/**
 * 文件系统错误类型
 * 
 * 定义了所有可能的文件系统错误，用于错误处理和用户通知
 * Requirements: 1.5
 */
sealed class FileSystemError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * 原始图像未找到
     * 
     * 当用户尝试加载或处理的原始图像文件不存在时抛出
     */
    data class ImageNotFound(
        val imagePath: String,
        override val cause: Throwable? = null
    ) : FileSystemError(
        message = "原始图像未找到: $imagePath",
        cause = cause
    )
    
    /**
     * 元数据保存失败
     * 
     * 当无法将参数元数据写入文件系统时抛出
     */
    data class MetadataSaveFailure(
        val imagePath: String,
        val attemptCount: Int = 0,
        override val cause: Throwable? = null
    ) : FileSystemError(
        message = "元数据保存失败 (尝试 $attemptCount 次): $imagePath",
        cause = cause
    )
    
    /**
     * 元数据加载失败
     * 
     * 当无法从文件系统读取元数据时抛出
     */
    data class MetadataLoadFailure(
        val imagePath: String,
        override val cause: Throwable? = null
    ) : FileSystemError(
        message = "元数据加载失败: $imagePath",
        cause = cause
    )
    
    /**
     * 存储空间不足
     * 
     * 当设备存储空间不足以完成操作时抛出
     */
    data class InsufficientStorage(
        val requiredBytes: Long,
        val availableBytes: Long,
        val operation: String
    ) : FileSystemError(
        message = "存储空间不足: 需要 ${requiredBytes / 1024 / 1024}MB，可用 ${availableBytes / 1024 / 1024}MB (操作: $operation)"
    )
    
    /**
     * 文件访问权限错误
     * 
     * 当应用没有足够的权限访问文件时抛出
     */
    data class PermissionDenied(
        val filePath: String,
        override val cause: Throwable? = null
    ) : FileSystemError(
        message = "文件访问权限被拒绝: $filePath",
        cause = cause
    )
    
    /**
     * 导出失败
     * 
     * 当图像导出过程中发生错误时抛出
     */
    data class ExportFailure(
        val outputPath: String,
        val stage: String? = null,
        override val cause: Throwable? = null
    ) : FileSystemError(
        message = if (stage != null) {
            "导出失败 (阶段: $stage): $outputPath"
        } else {
            "导出失败: $outputPath"
        },
        cause = cause
    )
    
    /**
     * 会话保存失败
     * 
     * 当无法保存编辑会话时抛出
     */
    data class SessionSaveFailure(
        val sessionType: String,
        override val cause: Throwable? = null
    ) : FileSystemError(
        message = "会话保存失败: $sessionType",
        cause = cause
    )
    
    /**
     * 会话加载失败
     * 
     * 当无法加载编辑会话时抛出
     */
    data class SessionLoadFailure(
        val sessionType: String,
        override val cause: Throwable? = null
    ) : FileSystemError(
        message = "会话加载失败: $sessionType",
        cause = cause
    )
    
    /**
     * 通用 IO 错误
     * 
     * 其他未分类的 IO 错误
     */
    data class GenericIOError(
        val operation: String,
        override val cause: Throwable? = null
    ) : FileSystemError(
        message = "IO 错误: $operation",
        cause = cause
    )
}

/**
 * 将通用异常转换为文件系统错误
 */
fun Throwable.toFileSystemError(context: String): FileSystemError {
    return when (this) {
        is FileSystemError -> this
        is IOException -> FileSystemError.GenericIOError(context, this)
        is SecurityException -> FileSystemError.PermissionDenied(context, this)
        else -> FileSystemError.GenericIOError(context, this)
    }
}
