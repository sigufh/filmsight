package com.filmtracker.app.domain.error

/**
 * 数据完整性错误类型
 * 
 * 定义了所有可能的数据完整性错误，用于错误处理和用户通知
 * Requirements: 11.4
 */
sealed class DataIntegrityError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * 元数据文件损坏
     * 
     * 当元数据文件无法解析时抛出
     */
    data class CorruptedMetadata(
        val imagePath: String,
        val parseError: String,
        override val cause: Throwable? = null
    ) : DataIntegrityError(
        message = "元数据文件损坏: $imagePath (错误: $parseError)",
        cause = cause
    )
    
    /**
     * 会话文件损坏
     * 
     * 当会话文件无法解析时抛出
     */
    data class CorruptedSession(
        val sessionType: String,
        val parseError: String,
        override val cause: Throwable? = null
    ) : DataIntegrityError(
        message = "会话文件损坏: $sessionType (错误: $parseError)",
        cause = cause
    )
    
    /**
     * 版本不匹配
     * 
     * 当元数据版本与当前应用版本不兼容时抛出
     */
    data class VersionMismatch(
        val metadataVersion: Int,
        val currentVersion: Int,
        val isNewer: Boolean
    ) : DataIntegrityError(
        message = if (isNewer) {
            "元数据版本过新: 元数据版本=$metadataVersion, 当前版本=$currentVersion"
        } else {
            "元数据版本过旧: 元数据版本=$metadataVersion, 当前版本=$currentVersion"
        }
    )
    
    /**
     * 参数验证失败
     * 
     * 当参数值超出有效范围时抛出
     */
    data class ParameterValidationFailure(
        val parameterName: String,
        val value: Any,
        val validRange: String,
        val action: ValidationAction
    ) : DataIntegrityError(
        message = "参数验证失败: $parameterName=$value (有效范围: $validRange, 操作: ${action.description})"
    ) {
        enum class ValidationAction(val description: String) {
            CLAMPED("已限制到有效范围"),
            REJECTED("已拒绝"),
            RESET_TO_DEFAULT("已重置为默认值")
        }
    }
    
    /**
     * 数据不一致
     * 
     * 当数据之间存在逻辑不一致时抛出
     */
    data class DataInconsistency(
        val description: String,
        val details: Map<String, Any> = emptyMap()
    ) : DataIntegrityError(
        message = "数据不一致: $description"
    )
    
    /**
     * 缺少必需字段
     * 
     * 当反序列化时缺少必需字段时抛出
     */
    data class MissingRequiredField(
        val fieldName: String,
        val dataType: String
    ) : DataIntegrityError(
        message = "缺少必需字段: $fieldName (数据类型: $dataType)"
    )
    
    /**
     * 无效的数据格式
     * 
     * 当数据格式不符合预期时抛出
     */
    data class InvalidDataFormat(
        val expectedFormat: String,
        val actualFormat: String,
        override val cause: Throwable? = null
    ) : DataIntegrityError(
        message = "无效的数据格式: 期望=$expectedFormat, 实际=$actualFormat",
        cause = cause
    )
}

/**
 * 参数验证器
 * 
 * 提供参数值验证和修正功能
 */
object ParameterValidator {
    
    /**
     * 验证并修正参数值
     * 
     * @param value 参数值
     * @param min 最小值
     * @param max 最大值
     * @param paramName 参数名称（用于日志）
     * @return 修正后的值
     */
    fun validateAndClamp(
        value: Float,
        min: Float,
        max: Float,
        paramName: String
    ): Float {
        return when {
            value < min -> {
                android.util.Log.w(
                    "ParameterValidator",
                    "Parameter $paramName=$value is below minimum $min, clamping"
                )
                min
            }
            value > max -> {
                android.util.Log.w(
                    "ParameterValidator",
                    "Parameter $paramName=$value is above maximum $max, clamping"
                )
                max
            }
            value.isNaN() -> {
                android.util.Log.w(
                    "ParameterValidator",
                    "Parameter $paramName is NaN, resetting to 0"
                )
                0f
            }
            value.isInfinite() -> {
                android.util.Log.w(
                    "ParameterValidator",
                    "Parameter $paramName is Infinite, resetting to 0"
                )
                0f
            }
            else -> value
        }
    }
    
    /**
     * 验证参数是否在有效范围内
     * 
     * @param value 参数值
     * @param min 最小值
     * @param max 最大值
     * @return 是否有效
     */
    fun isValid(value: Float, min: Float, max: Float): Boolean {
        return value in min..max && !value.isNaN() && !value.isInfinite()
    }
    
    /**
     * 验证整数参数
     */
    fun validateAndClamp(
        value: Int,
        min: Int,
        max: Int,
        paramName: String
    ): Int {
        return when {
            value < min -> {
                android.util.Log.w(
                    "ParameterValidator",
                    "Parameter $paramName=$value is below minimum $min, clamping"
                )
                min
            }
            value > max -> {
                android.util.Log.w(
                    "ParameterValidator",
                    "Parameter $paramName=$value is above maximum $max, clamping"
                )
                max
            }
            else -> value
        }
    }
}
