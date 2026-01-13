# 快速开始指南

## 🚀 5 分钟上手

### 第一步：安装 Android Studio
1. 下载：https://developer.android.com/studio
2. 安装时选择 "Standard" 模式
3. 等待 SDK 组件下载完成

### 第二步：安装必要组件
打开 **Tools → SDK Manager → SDK Tools**，安装：
- ✅ NDK (Side by side)
- ✅ CMake
- ✅ LLDB

### 第三步：打开项目
1. **File → Open** → 选择 `filmtracker` 目录
2. 等待 **Gradle Sync** 完成（首次可能需要几分钟）

### 第四步：运行应用
1. 连接 Android 设备（API 26+）或启动模拟器
2. 点击工具栏 **Run** 按钮（▶️）
3. 等待应用安装和启动

## 📋 开发工作流

### 修改 C++ 代码
```
1. 编辑 app/src/main/cpp/ 下的文件
2. Build → Make Project (Ctrl+F9)
3. 重新运行应用
```

### 修改 Kotlin/Compose 代码
```
1. 编辑 app/src/main/java/ 下的文件
2. 使用 Compose Preview（右上角 Split/Design）
3. 或直接运行查看效果
```

### 调试
```
1. 设置断点（点击行号左侧）
2. Debug 模式运行（Shift+F9）
3. 查看 Variables、Logcat 窗口
```

## 🛠️ 常用操作

| 操作 | 快捷键/位置 |
|------|------------|
| 运行应用 | Shift+F10 |
| 调试运行 | Shift+F9 |
| 构建项目 | Ctrl+F9 |
| 查看日志 | View → Logcat |
| 性能分析 | View → Profiler |
| 格式化代码 | Ctrl+Alt+L |

## ❓ 遇到问题？

### Gradle Sync 失败
- File → Invalidate Caches → Invalidate and Restart
- 检查网络连接（需要下载依赖）

### NDK 未找到
- File → Project Structure → SDK Location
- 确认 NDK 路径正确

### 编译错误
- 查看 Build 窗口的详细错误
- 检查 C++ 语法和 CMakeLists.txt

### 运行时崩溃
- 查看 Logcat 的错误信息
- 使用 Debug 模式运行并设置断点

## 📚 详细文档

- **完整开发流程**：查看 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
- **架构说明**：查看 [ARCHITECTURE.md](ARCHITECTURE.md)
- **构建细节**：查看 [BUILD.md](BUILD.md)

## 💡 开发技巧

### Compose Preview（推荐）
- 打开 Compose 文件（如 `ProcessingScreen.kt`）
- 点击右上角 **Split** 或 **Design**
- 实时预览 UI，无需运行应用

### Native 调试
- Run → Edit Configurations
- Debugger: 选择 "Native" 或 "Hybrid"
- 可以在 C++ 代码中设置断点

### 性能分析
- View → Tool Windows → Profiler
- 录制 CPU/Memory 使用情况
- 识别性能瓶颈

---

**开始开发吧！** 🎉
