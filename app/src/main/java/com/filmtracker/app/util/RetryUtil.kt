package com.filmtracker.app.util

import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * 重试工具类
 * 
 * 提供带指数退避的重试机制
 * Requirements: 1.5
 */
object RetryUtil {
    
    /**
     * 重试配置
     * 
     * @param maxAttempts 最大尝试次数
     * @param initialDelayMs 初始延迟（毫秒）
     * @param maxDelayMs 最大延迟（毫秒）
     * @param factor 退避因子
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 100,
        val maxDelayMs: Long = 2000,
        val factor: Double = 2.0
    ) {
        companion object {
            /**
             * 默认配置：3 次尝试，指数退避
             */
            val DEFAULT = RetryConfig()
            
            /**
             * 快速重试：2 次尝试，短延迟
             */
            val FAST = RetryConfig(
                maxAttempts = 2,
                initialDelayMs = 50,
                maxDelayMs = 500
            )
            
            /**
             * 持久重试：5 次尝试，较长延迟
             */
            val PERSISTENT = RetryConfig(
                maxAttempts = 5,
                initialDelayMs = 200,
                maxDelayMs = 5000
            )
        }
    }
    
    /**
     * 重试结果
     */
    sealed class RetryResult<T> {
        data class Success<T>(
            val value: T,
            val attemptCount: Int
        ) : RetryResult<T>()
        
        data class Failure<T>(
            val error: Throwable,
            val attemptCount: Int,
            val allErrors: List<Throwable>
        ) : RetryResult<T>()
    }
    
    /**
     * 执行带重试的操作
     * 
     * @param config 重试配置
     * @param operation 要执行的操作
     * @return 重试结果
     */
    suspend fun <T> retry(
        config: RetryConfig = RetryConfig.DEFAULT,
        operation: suspend (attemptNumber: Int) -> T
    ): RetryResult<T> {
        val errors = mutableListOf<Throwable>()
        
        repeat(config.maxAttempts) { attempt ->
            try {
                val result = operation(attempt + 1)
                return RetryResult.Success(result, attempt + 1)
            } catch (e: Exception) {
                errors.add(e)
                
                // 如果不是最后一次尝试，等待后重试
                if (attempt < config.maxAttempts - 1) {
                    val delayMs = calculateDelay(
                        attempt = attempt,
                        initialDelay = config.initialDelayMs,
                        maxDelay = config.maxDelayMs,
                        factor = config.factor
                    )
                    delay(delayMs)
                }
            }
        }
        
        // 所有尝试都失败
        return RetryResult.Failure(
            error = errors.last(),
            attemptCount = config.maxAttempts,
            allErrors = errors
        )
    }
    
    /**
     * 计算指数退避延迟
     * 
     * @param attempt 当前尝试次数（从 0 开始）
     * @param initialDelay 初始延迟
     * @param maxDelay 最大延迟
     * @param factor 退避因子
     * @return 延迟时间（毫秒）
     */
    private fun calculateDelay(
        attempt: Int,
        initialDelay: Long,
        maxDelay: Long,
        factor: Double
    ): Long {
        val delay = (initialDelay * factor.pow(attempt.toDouble())).toLong()
        return delay.coerceAtMost(maxDelay)
    }
    
    /**
     * 执行带重试的操作（简化版，返回 Result）
     * 
     * @param config 重试配置
     * @param operation 要执行的操作
     * @return Result<T>
     */
    suspend fun <T> retryWithResult(
        config: RetryConfig = RetryConfig.DEFAULT,
        operation: suspend (attemptNumber: Int) -> T
    ): Result<T> {
        return when (val result = retry(config, operation)) {
            is RetryResult.Success -> Result.success(result.value)
            is RetryResult.Failure -> Result.failure(result.error)
        }
    }
}
