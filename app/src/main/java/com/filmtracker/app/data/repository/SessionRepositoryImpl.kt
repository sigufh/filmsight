package com.filmtracker.app.data.repository

import android.content.Context
import android.net.Uri
import com.filmtracker.app.domain.model.EditSession
import com.filmtracker.app.domain.model.SerializableEditSession
import com.filmtracker.app.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * 会话仓储实现
 * 使用文件系统存储编辑会话
 * 
 * 会话存储策略：
 * - last_session.json: 最后一次编辑的会话（用于应用重启恢复）
 * - sessions/{uri_hash}.json: 每个图像的会话（用于多图像切换）
 */
class SessionRepositoryImpl(
    private val context: Context
) : SessionRepository {
    
    /**
     * 会话目录
     */
    private val sessionsDir: File
        get() = File(context.filesDir, "sessions").apply {
            if (!exists()) mkdirs()
        }
    
    /**
     * 最后会话文件
     */
    private val lastSessionFile: File
        get() = File(context.filesDir, "last_session.json")
    
    /**
     * JSON 序列化器
     */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * 获取图像 URI 的哈希值（用作文件名）
     */
    private fun getUriHash(uri: Uri): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(uri.toString().toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }
    
    /**
     * 获取指定图像的会话文件
     */
    private fun getSessionFileForImage(imageUri: Uri): File {
        val hash = getUriHash(imageUri)
        return File(sessionsDir, "$hash.json")
    }
    
    override suspend fun saveSession(session: EditSession): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 转换为可序列化格式
            val serializableSession = SerializableEditSession.fromEditSession(session)
            val jsonString = json.encodeToString(serializableSession)
            
            // 1. 保存到最后会话文件（用于应用重启恢复）
            lastSessionFile.writeText(jsonString)
            
            // 2. 保存到图像特定的会话文件（用于多图像切换）
            val imageSessionFile = getSessionFileForImage(session.imageUri)
            imageSessionFile.writeText(jsonString)
            
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun loadLastSession(): Result<EditSession?> = withContext(Dispatchers.IO) {
        try {
            if (!lastSessionFile.exists()) {
                return@withContext Result.success(null)
            }
            
            // 读取文件并反序列化
            val jsonString = lastSessionFile.readText()
            val serializableSession = json.decodeFromString<SerializableEditSession>(jsonString)
            
            // 转换为领域模型
            val session = serializableSession.toEditSession()
            
            Result.success(session)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            // JSON 解析错误或其他异常
            Result.failure(e)
        }
    }
    
    override suspend fun loadSessionForImage(imageUri: Uri): Result<EditSession?> = withContext(Dispatchers.IO) {
        try {
            val sessionFile = getSessionFileForImage(imageUri)
            
            if (!sessionFile.exists()) {
                return@withContext Result.success(null)
            }
            
            // 读取文件并反序列化
            val jsonString = sessionFile.readText()
            val serializableSession = json.decodeFromString<SerializableEditSession>(jsonString)
            
            // 转换为领域模型
            val session = serializableSession.toEditSession()
            
            // 验证会话是否匹配图像 URI
            if (session.imageUri.toString() != imageUri.toString()) {
                // URI 不匹配，返回 null
                return@withContext Result.success(null)
            }
            
            Result.success(session)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            // JSON 解析错误或其他异常
            Result.failure(e)
        }
    }
    
    override suspend fun clearSession(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (lastSessionFile.exists()) {
                lastSessionFile.delete()
            }
            
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearSessionForImage(imageUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessionFile = getSessionFileForImage(imageUri)
            
            if (sessionFile.exists()) {
                sessionFile.delete()
            }
            
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
