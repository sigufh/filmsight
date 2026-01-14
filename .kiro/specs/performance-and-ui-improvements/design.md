# Design Document

## Overview

本设计文档描述 FilmSight 应用的性能优化和 UI 改进方案，主要解决：处理速度慢、UI 遮挡、色彩偏差、缺少导入界面等问题。

## Architecture

### 三层缓存架构

```
用户调整参数
    
参数防抖 (150ms)
    
检查缓存层
    
 L1: 预览图缓存 (内存, 1200x1200)
 L2: 处理步骤缓存 (中间结果)
 L3: 原始图像缓存 (LinearImage)
    
增量处理 (只处理变化的步骤)
    
更新预览
```

### 新增组件

1. **ImageImportScreen** - 图片导入界面
2. **ImageCache** - 多级图像缓存管理器
3. **ProcessingPipeline** - 增量处理管道
4. **PresetManager** - 参数预设管理器

## Components and Interfaces

### 1. ImageCache (Kotlin)

```kotlin
class ImageCache {
    // L1: 预览图缓存
    private val previewCache = LruCache<String, Bitmap>(maxSize = 50MB)
    
    // L2: 处理步骤缓存
    private val stepCache = mutableMapOf<String, LinearImageNative>()
    
    // L3: 原始图像缓存
    private var sourceImage: LinearImageNative? = null
    
    fun getPreview(key: String): Bitmap?
    fun putPreview(key: String, bitmap: Bitmap)
    fun getStepResult(step: String): LinearImageNative?
    fun putStepResult(step: String, image: LinearImageNative)
    fun clear()
}
```

### 2. ProcessingPipeline (Kotlin)

```kotlin
class ProcessingPipeline(private val cache: ImageCache) {
    // 处理步骤枚举
    enum class Step {
        COLOR_CROSSTALK,
        RESPONSE_CURVE,
        GRAIN,
        BASIC_TONE,
        TONE_CURVES,
        HSL,
        SATURATION
    }
    
    // 检测哪些步骤需要重新处理
    fun detectChangedSteps(oldParams: FilmParams, newParams: FilmParams): List<Step>
    
    // 增量处理
    suspend fun processIncremental(
        params: FilmParams,
        changedSteps: List<Step>
    ): Bitmap
}
```

### 3. ImageImportScreen (Compose)

```kotlin
@Composable
fun ImageImportScreen(
    onImageSelected: (String) -> Unit,
    onNavigateToEdit: () -> Unit
) {
    // 显示：
    // - 导入按钮
    // - 最近图片网格 (缩略图)
    // - 图片信息 (文件名、尺寸、格式)
}
```

### 4. ColorManager (C++)

```cpp
class ColorManager {
    // 修复 sRGB 到线性空间转换
    static LinearImage correctSRGBToLinear(const uint8_t* data, int w, int h);
    
    // 应用相机色彩配置文件 (RAW)
    static void applyCameraProfile(LinearImage& img, const RawMetadata& meta);
    
    // 白平衡调整
    static void applyWhiteBalance(LinearImage& img, float temp, float tint);
};
```

### 5. PresetManager (Kotlin)

```kotlin
data class Preset(
    val name: String,
    val params: FilmParams,
    val thumbnail: Bitmap? = null
)

class PresetManager(context: Context) {
    fun savePreset(name: String, params: FilmParams)
    fun loadPreset(name: String): FilmParams?
    fun listPresets(): List<Preset>
    fun deletePreset(name: String)
    
    // 内置预设
    fun getBuiltInPresets(): List<Preset>
}
```

## Data Models

### ProcessingState

```kotlin
data class ProcessingState(
    val isProcessing: Boolean = false,
    val currentStep: String? = null,
    val progress: Float = 0f,
    val canCancel: Boolean = false
)
```

### ImageInfo

```kotlin
data class ImageInfo(
    val uri: String,
    val fileName: String,
    val width: Int,
    val height: Int,
    val format: String,
    val fileSize: Long,
    val thumbnail: Bitmap?
)
```

## Correctness Properties

*属性是系统应该在所有有效执行中保持为真的特征或行为，作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### Property 1: 缓存一致性
*对于任何* 参数组合，如果缓存命中，返回的结果应该与重新处理的结果相同
**Validates: Requirements 2.2**

### Property 2: 增量处理正确性
*对于任何* 参数变化，增量处理的最终结果应该与完整处理的结果相同
**Validates: Requirements 2.1**

### Property 3: 预览响应时间
*对于任何* 参数调整，预览更新时间应该小于 500ms（在缓存命中时）
**Validates: Requirements 1.1**

### Property 4: 色彩空间一致性
*对于任何* 图像，处理管道中的色彩空间转换应该保持可逆性（在精度范围内）
**Validates: Requirements 4.3**

### Property 5: 内存安全性
*对于任何* 图像大小，系统应该在内存不足时自动降级而不是崩溃
**Validates: Requirements 7.2**

## Error Handling

### 内存不足
- 自动降低预览分辨率（1200  800  600）
- 清理最旧的缓存
- 显示警告提示

### 处理失败
- 捕获异常并显示错误消息
- 保留上一次成功的预览
- 提供重试选项

### 文件访问失败
- 显示友好的错误提示
- 从最近列表中移除无效文件
- 建议重新导入

## Testing Strategy

### Unit Tests
- `ImageCache` 的缓存逻辑
- `ProcessingPipeline` 的步骤检测
- `ColorManager` 的色彩转换
- `PresetManager` 的序列化/反序列化

### Property-Based Tests
- 使用 Kotest 进行属性测试
- 每个测试运行 100 次迭代
- 生成随机参数组合测试缓存一致性
- 生成随机图像测试色彩转换可逆性

### Integration Tests
- 完整的图片导入流程
- 参数调整到预览更新的端到端流程
- 预设保存和加载
- 内存压力测试

### Performance Tests
- 测量预览响应时间（目标 < 500ms）
- 测量内存使用（目标 < 200MB for 42MP RAW）
- 测量缓存命中率（目标 > 80%）

## Implementation Notes

### 性能优化关键点

1. **预览分辨率** - 默认 1200x1200，可根据内存动态调整
2. **防抖延迟** - 150ms，平衡响应速度和处理频率
3. **缓存大小** - L1: 50MB, L2: 3个步骤, L3: 1个原图
4. **线程策略** - 使用 Dispatchers.Default 进行图像处理

### UI 改进关键点

1. **导入界面** - 独立的 Screen，显示网格缩略图
2. **参数面板** - 使用 ModalBottomSheet，不遮挡预览
3. **进度反馈** - 处理超过 1 秒显示进度条
4. **预设快捷访问** - 底部工具栏添加预设按钮

### 色彩管理关键点

1. **RAW 处理** - 使用 LibRaw 的相机配置文件
2. **sRGB 转换** - 正确应用 2.4 gamma
3. **白平衡** - 提供色温和色调调整
4. **色彩空间标记** - 在整个管道中保持一致

