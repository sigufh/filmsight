package com.filmtracker.app.domain.repository

import android.net.Uri
import com.filmtracker.app.domain.model.EditSession

/**
 * 会话仓储接口
 * 负责编辑会话的持久化存储和加载
 */
interface SessionRepository {
    
    /**
     * 保存编辑会话
     * @param session 要保存的会话
     * @return 保存结果
     */
    suspend fun saveSession(session: EditSession): Result<Unit>
    
    /**
     * 加载最后一次编辑会话
     * @return 会话,如果不存在则返回 null
     */
    suspend fun loadLastSession(): Result<EditSession?>
    
    /**
     * 加载指定图像的编辑会话
     * @param imageUri 图像 URI
     * @return 会话,如果不存在则返回 null
     */
    suspend fun loadSessionForImage(imageUri: Uri): Result<EditSession?>
    
    /**
     * 清除会话
     * @return 清除结果
     */
    suspend fun clearSession(): Result<Unit>
    
    /**
     * 清除指定图像的会话
     * @param imageUri 图像 URI
     * @return 清除结果
     */
    suspend fun clearSessionForImage(imageUri: Uri): Result<Unit>
}
