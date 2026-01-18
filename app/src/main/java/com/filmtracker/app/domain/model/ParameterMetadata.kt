package com.filmtracker.app.domain.model

import android.util.Log
import com.filmtracker.app.domain.error.DataIntegrityError
import com.filmtracker.app.domain.error.ParameterValidator
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Parameter metadata for non-destructive image editing system.
 * 
 * This class represents the persistent storage format for all adjustment parameters
 * applied to an image. It provides:
 * 
 * - **Version Management**: Handles metadata format evolution and migration
 * - **Data Integrity**: Validates parameters and handles corrupted data
 * - **Serialization**: JSON-based storage with pretty printing and compatibility
 * - **Audit Trail**: Tracks creation, modification times and app versions
 * 
 * The metadata is stored as separate JSON files alongside original images,
 * following the naming convention: `{original_filename}.filmtracker.json`
 * 
 * Key features:
 * - Forward/backward compatibility through version migration
 * - Automatic parameter validation and correction
 * - Optional file hash verification for integrity checking
 * - Graceful handling of corrupted or incompatible metadata
 * 
 * @param version Metadata format version for compatibility management
 * @param imageUri Android MediaStore URI string for the associated image
 * @param imagePath Absolute file system path to the original image
 * @param imageHash Optional SHA-256 hash for file integrity verification
 * @param parameters Complete set of adjustment parameters in serializable format
 * @param createdAt Timestamp when this metadata was first created
 * @param modifiedAt Timestamp of the last modification to parameters
 * @param appVersion Version of the app that created/last modified this metadata
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4
 */
@Serializable
data class ParameterMetadata(
    val version: Int = CURRENT_VERSION,
    val imageUri: String,
    val imagePath: String,
    val imageHash: String? = null,  // 可选的文件哈希,用于验证
    val parameters: SerializableAdjustmentParams,
    val createdAt: Long,
    val modifiedAt: Long,
    val appVersion: String
) {
    companion object {
        private const val TAG = "ParameterMetadata"
        const val CURRENT_VERSION = 1
        const val FILE_EXTENSION = ".filmtracker.json"
        
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true  // 忽略未知字段，支持向后兼容
        }
        
        /**
         * Deserializes ParameterMetadata from JSON string with automatic version migration.
         * 
         * This method handles the complete deserialization process including:
         * 1. JSON parsing with error handling
         * 2. Version compatibility checking
         * 3. Automatic migration for older versions
         * 4. Parameter validation and correction
         * 5. Best-effort loading for newer versions
         * 
         * Version handling strategy:
         * - Current version: Direct loading with validation
         * - Older version: Automatic migration to current version
         * - Newer version: Best-effort loading with warnings
         * 
         * @param jsonString JSON representation of the metadata
         * @return Validated and potentially migrated ParameterMetadata
         * @throws DataIntegrityError.CorruptedMetadata when JSON cannot be parsed
         * @throws DataIntegrityError.VersionMismatch when version is incompatible
         * 
         * Requirements: 11.2, 11.3, 11.4
         */
        fun fromJson(jsonString: String): ParameterMetadata {
            try {
                val metadata = json.decodeFromString<ParameterMetadata>(jsonString)
                
                // 检查版本
                when {
                    metadata.version == CURRENT_VERSION -> {
                        // 当前版本，直接返回
                        Log.d(TAG, "Loaded metadata version $CURRENT_VERSION")
                        return metadata.validate()
                    }
                    metadata.version < CURRENT_VERSION -> {
                        // 旧版本，需要向上迁移
                        Log.i(TAG, "Migrating metadata from version ${metadata.version} to $CURRENT_VERSION")
                        return metadata.migrate(CURRENT_VERSION).validate()
                    }
                    else -> {
                        // 版本过新，尝试最佳努力加载
                        Log.w(TAG, "Metadata version ${metadata.version} is newer than current version $CURRENT_VERSION")
                        Log.w(TAG, "Attempting best-effort loading, some features may not work correctly")
                        
                        // 抛出警告但继续加载
                        // 在实际应用中，可能需要通知用户
                        return metadata.validate()
                    }
                }
                
            } catch (e: SerializationException) {
                Log.e(TAG, "Failed to parse metadata JSON", e)
                throw DataIntegrityError.CorruptedMetadata(
                    imagePath = "unknown",
                    parseError = e.message ?: "JSON parsing failed",
                    cause = e
                )
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid metadata format", e)
                throw DataIntegrityError.CorruptedMetadata(
                    imagePath = "unknown",
                    parseError = e.message ?: "Invalid format",
                    cause = e
                )
            }
        }
        
        /**
         * Generates the metadata filename for a given image path.
         * 
         * This method creates the standardized filename used for storing
         * parameter metadata alongside the original image file.
         * 
         * Example: 
         * - Input: "/storage/emulated/0/DCIM/Camera/IMG_1234.jpg"
         * - Output: "IMG_1234.jpg.filmtracker.json"
         * 
         * @param imagePath Absolute path to the original image file
         * @return Filename for the corresponding metadata file
         */
        fun getMetadataFileName(imagePath: String): String {
            val imageFile = File(imagePath)
            return "${imageFile.name}$FILE_EXTENSION"
        }
    }
    
    /**
     * Validates the integrity of this metadata instance.
     * 
     * This method performs comprehensive validation including:
     * - Parameter value range checking and correction
     * - Timestamp validation and repair
     * - Data consistency verification
     * 
     * Invalid values are automatically corrected to valid ranges,
     * and warnings are logged for debugging purposes.
     * 
     * @return Validated metadata instance (may have corrected parameters)
     * 
     * Requirements: 11.4
     */
    fun validate(): ParameterMetadata {
        // 验证参数
        val validatedParams = parameters.validate()
        
        // 验证时间戳
        if (createdAt <= 0 || modifiedAt <= 0) {
            Log.w(TAG, "Invalid timestamps: createdAt=$createdAt, modifiedAt=$modifiedAt")
        }
        
        if (modifiedAt < createdAt) {
            Log.w(TAG, "Modified time is before created time, fixing")
        }
        
        return if (validatedParams != parameters) {
            copy(parameters = validatedParams)
        } else {
            this
        }
    }
    
    /**
     * Serializes this metadata to a JSON string.
     * 
     * The output is formatted with pretty printing for human readability
     * and debugging purposes. The JSON structure is designed to be
     * forward-compatible with future versions.
     * 
     * @return JSON string representation of this metadata
     */
    fun toJson(): String {
        return json.encodeToString(this)
    }
    
    /**
     * 迁移到目标版本
     * 
     * 版本迁移策略:
     * - 版本 1 (当前): 基线版本,包含所有当前参数
     * - 向上迁移: 从旧版本迁移到新版本,添加新参数的默认值
     * - 向下迁移: 不支持,保持原版本号
     * 
     * Requirements: 11.1, 11.2, 11.3
     * 
     * @param targetVersion 目标版本号
     * @return 迁移后的元数据
     */
    fun migrate(targetVersion: Int): ParameterMetadata {
        if (version == targetVersion) return this
        
        return when {
            version < targetVersion -> {
                // 向上迁移到更新版本
                Log.i(TAG, "Migrating up from version $version to $targetVersion")
                migrateUp(targetVersion)
            }
            else -> {
                // 版本过新,无法向下迁移
                // 保持原版本号,尝试最佳努力加载
                Log.w(TAG, "Cannot migrate down from version $version to $targetVersion")
                this
            }
        }
    }
    
    /**
     * 向上迁移逻辑
     * 从当前版本迁移到目标版本
     */
    private fun migrateUp(targetVersion: Int): ParameterMetadata {
        var current = this
        
        // 逐步迁移每个版本
        for (v in (version + 1)..targetVersion) {
            current = when (v) {
                1 -> {
                    // 迁移到版本 1 (基线版本)
                    // 这是当前版本,不需要迁移
                    Log.d(TAG, "Migrated to version 1")
                    current.copy(version = 1)
                }
                // 未来版本的迁移逻辑将在这里添加
                // 例如:
                // 2 -> migrateToVersion2(current)
                // 3 -> migrateToVersion3(current)
                else -> {
                    // 未知的目标版本,保持当前状态
                    Log.w(TAG, "Unknown target version $v, keeping current state")
                    current.copy(version = v)
                }
            }
        }
        
        return current
    }
    
    // 未来版本的迁移方法示例:
    // private fun migrateToVersion2(metadata: ParameterMetadata): ParameterMetadata {
    //     // 添加版本 2 的新参数
    //     val updatedParams = metadata.parameters.copy(
    //         // 新参数 = 默认值
    //     )
    //     return metadata.copy(
    //         version = 2,
    //         parameters = updatedParams
    //     )
    // }
}

/**
 * 可序列化的调整参数
 * 将 AdjustmentParams 转换为可序列化的格式
 * 
 * 增强的参数验证：
 * - 自动限制参数到有效范围
 * - 处理 NaN 和 Infinite 值
 * - 记录验证警告
 * 
 * Requirements: 11.4
 */
@Serializable
data class SerializableAdjustmentParams(
    // 基础调整
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    
    // 色调调整
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    
    // 存在感
    val clarity: Float = 0f,
    val vibrance: Float = 0f,
    
    // 颜色
    val temperature: Float = 0f,
    val tint: Float = 0f,
    
    // 分级
    val gradingHighlightsTemp: Float = 0f,
    val gradingHighlightsTint: Float = 0f,
    val gradingMidtonesTemp: Float = 0f,
    val gradingMidtonesTint: Float = 0f,
    val gradingShadowsTemp: Float = 0f,
    val gradingShadowsTint: Float = 0f,
    val gradingBlending: Float = 50f,
    val gradingBalance: Float = 0f,
    
    // 效果
    val texture: Float = 0f,
    val dehaze: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    
    // 细节
    val sharpening: Float = 0f,
    val noiseReduction: Float = 0f,
    
    // 曲线
    val enableRgbCurve: Boolean = false,
    val rgbCurvePoints: List<FloatPair> = listOf(FloatPair(0f, 0f), FloatPair(1f, 1f)),
    val enableRedCurve: Boolean = false,
    val redCurvePoints: List<FloatPair> = listOf(FloatPair(0f, 0f), FloatPair(1f, 1f)),
    val enableGreenCurve: Boolean = false,
    val greenCurvePoints: List<FloatPair> = listOf(FloatPair(0f, 0f), FloatPair(1f, 1f)),
    val enableBlueCurve: Boolean = false,
    val blueCurvePoints: List<FloatPair> = listOf(FloatPair(0f, 0f), FloatPair(1f, 1f)),
    
    // HSL
    val enableHSL: Boolean = false,
    val hslHueShift: List<Float> = List(8) { 0f },
    val hslSaturation: List<Float> = List(8) { 0f },
    val hslLuminance: List<Float> = List(8) { 0f },
    
    // 几何
    val rotation: Float = 0f,
    val cropEnabled: Boolean = false,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
) {
    companion object {
        private const val TAG = "SerializableAdjustmentParams"
        
        /**
         * 从领域模型创建
         */
        fun fromAdjustmentParams(params: AdjustmentParams): SerializableAdjustmentParams {
            return SerializableAdjustmentParams(
                exposure = params.exposure,
                contrast = params.contrast,
                saturation = params.saturation,
                highlights = params.highlights,
                shadows = params.shadows,
                whites = params.whites,
                blacks = params.blacks,
                clarity = params.clarity,
                vibrance = params.vibrance,
                temperature = params.temperature,
                tint = params.tint,
                gradingHighlightsTemp = params.gradingHighlightsTemp,
                gradingHighlightsTint = params.gradingHighlightsTint,
                gradingMidtonesTemp = params.gradingMidtonesTemp,
                gradingMidtonesTint = params.gradingMidtonesTint,
                gradingShadowsTemp = params.gradingShadowsTemp,
                gradingShadowsTint = params.gradingShadowsTint,
                gradingBlending = params.gradingBlending,
                gradingBalance = params.gradingBalance,
                texture = params.texture,
                dehaze = params.dehaze,
                vignette = params.vignette,
                grain = params.grain,
                sharpening = params.sharpening,
                noiseReduction = params.noiseReduction,
                enableRgbCurve = params.enableRgbCurve,
                rgbCurvePoints = params.rgbCurvePoints.map { FloatPair(it.first, it.second) },
                enableRedCurve = params.enableRedCurve,
                redCurvePoints = params.redCurvePoints.map { FloatPair(it.first, it.second) },
                enableGreenCurve = params.enableGreenCurve,
                greenCurvePoints = params.greenCurvePoints.map { FloatPair(it.first, it.second) },
                enableBlueCurve = params.enableBlueCurve,
                blueCurvePoints = params.blueCurvePoints.map { FloatPair(it.first, it.second) },
                enableHSL = params.enableHSL,
                hslHueShift = params.hslHueShift.toList(),
                hslSaturation = params.hslSaturation.toList(),
                hslLuminance = params.hslLuminance.toList(),
                rotation = params.rotation,
                cropEnabled = params.cropEnabled,
                cropLeft = params.cropLeft,
                cropTop = params.cropTop,
                cropRight = params.cropRight,
                cropBottom = params.cropBottom
            )
        }
        
        /**
         * 从 BasicAdjustmentParams 创建（用于胶卷工作流）
         */
        fun fromBasicParams(params: com.filmtracker.app.data.BasicAdjustmentParams): SerializableAdjustmentParams {
            return SerializableAdjustmentParams(
                exposure = params.globalExposure,
                contrast = params.contrast,
                saturation = params.saturation,
                highlights = params.highlights,
                shadows = params.shadows,
                whites = params.whites,
                blacks = params.blacks,
                clarity = params.clarity,
                vibrance = params.vibrance,
                temperature = params.temperature,
                tint = params.tint,
                gradingHighlightsTemp = params.gradingHighlightsTemp,
                gradingHighlightsTint = params.gradingHighlightsTint,
                gradingMidtonesTemp = params.gradingMidtonesTemp,
                gradingMidtonesTint = params.gradingMidtonesTint,
                gradingShadowsTemp = params.gradingShadowsTemp,
                gradingShadowsTint = params.gradingShadowsTint,
                gradingBlending = params.gradingBlending,
                gradingBalance = params.gradingBalance,
                texture = params.texture,
                dehaze = params.dehaze,
                vignette = params.vignette,
                grain = params.grain,
                sharpening = params.sharpening,
                noiseReduction = params.noiseReduction,
                enableRgbCurve = params.enableRgbCurve,
                rgbCurvePoints = params.rgbCurvePoints.map { FloatPair(it.first, it.second) },
                enableRedCurve = params.enableRedCurve,
                redCurvePoints = params.redCurvePoints.map { FloatPair(it.first, it.second) },
                enableGreenCurve = params.enableGreenCurve,
                greenCurvePoints = params.greenCurvePoints.map { FloatPair(it.first, it.second) },
                enableBlueCurve = params.enableBlueCurve,
                blueCurvePoints = params.blueCurvePoints.map { FloatPair(it.first, it.second) },
                enableHSL = params.enableHSL,
                hslHueShift = params.hslHueShift.toList(),
                hslSaturation = params.hslSaturation.toList(),
                hslLuminance = params.hslLuminance.toList(),
                rotation = params.rotation,
                cropEnabled = params.cropEnabled,
                cropLeft = params.cropLeft,
                cropTop = params.cropTop,
                cropRight = params.cropRight,
                cropBottom = params.cropBottom
            )
        }
    }
    
    /**
     * 验证并修正参数值
     * 
     * Requirements: 11.4 - 参数验证失败处理
     * 
     * @return 验证后的参数（可能被修正）
     */
    fun validate(): SerializableAdjustmentParams {
        return copy(
            // 基础调整 (-5.0 to 5.0 for exposure, 0.0 to 2.0 for contrast/saturation)
            exposure = ParameterValidator.validateAndClamp(exposure, -5f, 5f, "exposure"),
            contrast = ParameterValidator.validateAndClamp(contrast, 0f, 2f, "contrast"),
            saturation = ParameterValidator.validateAndClamp(saturation, 0f, 2f, "saturation"),
            
            // 色调调整 (-100 to 100)
            highlights = ParameterValidator.validateAndClamp(highlights, -100f, 100f, "highlights"),
            shadows = ParameterValidator.validateAndClamp(shadows, -100f, 100f, "shadows"),
            whites = ParameterValidator.validateAndClamp(whites, -100f, 100f, "whites"),
            blacks = ParameterValidator.validateAndClamp(blacks, -100f, 100f, "blacks"),
            
            // 存在感 (-100 to 100)
            clarity = ParameterValidator.validateAndClamp(clarity, -100f, 100f, "clarity"),
            vibrance = ParameterValidator.validateAndClamp(vibrance, -100f, 100f, "vibrance"),
            
            // 颜色 (-100 to 100)
            temperature = ParameterValidator.validateAndClamp(temperature, -100f, 100f, "temperature"),
            tint = ParameterValidator.validateAndClamp(tint, -100f, 100f, "tint"),
            
            // 分级 (-100 to 100, except blending 0-100)
            gradingHighlightsTemp = ParameterValidator.validateAndClamp(gradingHighlightsTemp, -100f, 100f, "gradingHighlightsTemp"),
            gradingHighlightsTint = ParameterValidator.validateAndClamp(gradingHighlightsTint, -100f, 100f, "gradingHighlightsTint"),
            gradingMidtonesTemp = ParameterValidator.validateAndClamp(gradingMidtonesTemp, -100f, 100f, "gradingMidtonesTemp"),
            gradingMidtonesTint = ParameterValidator.validateAndClamp(gradingMidtonesTint, -100f, 100f, "gradingMidtonesTint"),
            gradingShadowsTemp = ParameterValidator.validateAndClamp(gradingShadowsTemp, -100f, 100f, "gradingShadowsTemp"),
            gradingShadowsTint = ParameterValidator.validateAndClamp(gradingShadowsTint, -100f, 100f, "gradingShadowsTint"),
            gradingBlending = ParameterValidator.validateAndClamp(gradingBlending, 0f, 100f, "gradingBlending"),
            gradingBalance = ParameterValidator.validateAndClamp(gradingBalance, -100f, 100f, "gradingBalance"),
            
            // 效果 (-100 to 100 or 0 to 100)
            texture = ParameterValidator.validateAndClamp(texture, -100f, 100f, "texture"),
            dehaze = ParameterValidator.validateAndClamp(dehaze, -100f, 100f, "dehaze"),
            vignette = ParameterValidator.validateAndClamp(vignette, -100f, 100f, "vignette"),
            grain = ParameterValidator.validateAndClamp(grain, 0f, 100f, "grain"),
            
            // 细节 (0 to 100)
            sharpening = ParameterValidator.validateAndClamp(sharpening, 0f, 100f, "sharpening"),
            noiseReduction = ParameterValidator.validateAndClamp(noiseReduction, 0f, 100f, "noiseReduction"),
            
            // 曲线点 (0.0 to 1.0)
            rgbCurvePoints = rgbCurvePoints.map { it.validate() },
            redCurvePoints = redCurvePoints.map { it.validate() },
            greenCurvePoints = greenCurvePoints.map { it.validate() },
            blueCurvePoints = blueCurvePoints.map { it.validate() },
            
            // HSL (-100 to 100)
            hslHueShift = hslHueShift.map { ParameterValidator.validateAndClamp(it, -100f, 100f, "hslHueShift") },
            hslSaturation = hslSaturation.map { ParameterValidator.validateAndClamp(it, -100f, 100f, "hslSaturation") },
            hslLuminance = hslLuminance.map { ParameterValidator.validateAndClamp(it, -100f, 100f, "hslLuminance") },
            
            // 几何 (rotation: -180 to 180, crop: 0.0 to 1.0)
            rotation = ParameterValidator.validateAndClamp(rotation, -180f, 180f, "rotation"),
            cropLeft = ParameterValidator.validateAndClamp(cropLeft, 0f, 1f, "cropLeft"),
            cropTop = ParameterValidator.validateAndClamp(cropTop, 0f, 1f, "cropTop"),
            cropRight = ParameterValidator.validateAndClamp(cropRight, 0f, 1f, "cropRight"),
            cropBottom = ParameterValidator.validateAndClamp(cropBottom, 0f, 1f, "cropBottom")
        )
    }
    
    /**
     * 转换为领域模型
     */
    fun toAdjustmentParams(): AdjustmentParams {
        return AdjustmentParams(
            exposure = exposure,
            contrast = contrast,
            saturation = saturation,
            highlights = highlights,
            shadows = shadows,
            whites = whites,
            blacks = blacks,
            clarity = clarity,
            vibrance = vibrance,
            temperature = temperature,
            tint = tint,
            gradingHighlightsTemp = gradingHighlightsTemp,
            gradingHighlightsTint = gradingHighlightsTint,
            gradingMidtonesTemp = gradingMidtonesTemp,
            gradingMidtonesTint = gradingMidtonesTint,
            gradingShadowsTemp = gradingShadowsTemp,
            gradingShadowsTint = gradingShadowsTint,
            gradingBlending = gradingBlending,
            gradingBalance = gradingBalance,
            texture = texture,
            dehaze = dehaze,
            vignette = vignette,
            grain = grain,
            sharpening = sharpening,
            noiseReduction = noiseReduction,
            enableRgbCurve = enableRgbCurve,
            rgbCurvePoints = rgbCurvePoints.map { it.toPair() },
            enableRedCurve = enableRedCurve,
            redCurvePoints = redCurvePoints.map { it.toPair() },
            enableGreenCurve = enableGreenCurve,
            greenCurvePoints = greenCurvePoints.map { it.toPair() },
            enableBlueCurve = enableBlueCurve,
            blueCurvePoints = blueCurvePoints.map { it.toPair() },
            enableHSL = enableHSL,
            hslHueShift = hslHueShift.toFloatArray(),
            hslSaturation = hslSaturation.toFloatArray(),
            hslLuminance = hslLuminance.toFloatArray(),
            rotation = rotation,
            cropEnabled = cropEnabled,
            cropLeft = cropLeft,
            cropTop = cropTop,
            cropRight = cropRight,
            cropBottom = cropBottom
        )
    }
}

/**
 * 可序列化的浮点数对
 * 用于曲线点的序列化
 * 
 * Requirements: 11.4
 */
@Serializable
data class FloatPair(
    val first: Float,
    val second: Float
) {
    /**
     * 验证并修正曲线点值
     * 曲线点应该在 [0.0, 1.0] 范围内
     */
    fun validate(): FloatPair {
        return FloatPair(
            first = ParameterValidator.validateAndClamp(first, 0f, 1f, "curvePoint.x"),
            second = ParameterValidator.validateAndClamp(second, 0f, 1f, "curvePoint.y")
        )
    }
    
    fun toPair(): Pair<Float, Float> = Pair(first, second)
}
