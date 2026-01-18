package com.filmtracker.app.domain.repository

import com.filmtracker.app.domain.model.ParameterMetadata

/**
 * 元数据仓储接口
 * 负责参数元数据的持久化存储和加载
 */
interface MetadataRepository {
    
    /**
     * 保存参数元数据
     * @param metadata 要保存的元数据
     * @return 保存结果
     */
    suspend fun saveMetadata(metadata: ParameterMetadata): Result<Unit>
    
    /**
     * 加载参数元数据
     * @param imagePath 图像文件路径
     * @return 元数据,如果不存在则返回 null
     */
    suspend fun loadMetadata(imagePath: String): Result<ParameterMetadata?>
    
    /**
     * 删除参数元数据
     * @param imagePath 图像文件路径
     * @return 删除结果
     */
    suspend fun deleteMetadata(imagePath: String): Result<Unit>
    
    /**
     * 检查元数据是否存在
     * @param imagePath 图像文件路径
     * @return 是否存在
     */
    suspend fun metadataExists(imagePath: String): Boolean
    
    /**
     * 获取所有元数据
     * @return 所有元数据列表
     */
    suspend fun getAllMetadata(): Result<List<ParameterMetadata>>
}
