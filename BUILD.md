# 构建说明

> 💡 **详细开发流程请查看 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)**

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34
- NDK r25c 或更高版本
- CMake 3.22.1 或更高版本
- Kotlin 1.9.20
- JDK 17 或更高版本

## 快速开始

### 1. 打开项目
- 启动 Android Studio
- File → Open → 选择项目目录

### 2. 等待 Gradle Sync
- 首次打开会自动同步
- 如果失败，查看 `DEVELOPMENT_GUIDE.md` 的"常见问题排查"

### 3. 配置 NDK（如需要）
- File → Project Structure → SDK Location
- 确认 NDK 路径已自动检测

### 4. 运行应用
- 连接设备或启动模拟器
- 点击 Run 按钮（Shift+F10）

## 构建步骤

### 命令行构建

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 安装到设备
./gradlew installDebug
```

### Android Studio 构建

1. **Build → Make Project** (Ctrl+F9)
2. **Build → Clean Project**（清理）
3. **Build → Rebuild Project**（重建）

## 依赖说明

### 核心依赖
- **Compose BOM**: UI 框架
- **Coil**: 图像加载
- **Coroutines**: 异步处理

### Native 依赖
- **C++17**: 核心算法
- **Android STL**: C++ 标准库

## 已知限制

1. **RAW 解码**：当前为占位实现，实际应集成：
   - libraw
   - 或 Adobe DNG SDK

2. **Bayer 去马赛克**：当前为简化实现，应实现完整 AHD 算法

3. **参数传递**：当前仅传递部分参数，完整实现需要：
   - 完整的 JNI 结构体映射
   - 或使用 Protocol Buffers

## 下一步开发

1. 集成真实的 RAW 解码库
2. 实现完整的 AHD 去马赛克
3. 训练轻量 AI 模型（参数建议）
4. 完善参数传递机制
5. 添加更多胶片预设
6. 性能优化（SIMD、多线程）
