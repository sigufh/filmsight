# Android Studio 开发全流程指南

## 一、环境准备

### 1.1 安装 Android Studio

1. **下载 Android Studio**
   - 访问：https://developer.android.com/studio
   - 下载最新稳定版（Hedgehog 2023.1.1 或更高）

2. **安装步骤**
   ```
   - 运行安装程序
   - 选择 "Standard" 安装类型
   - 等待 SDK 组件下载完成
   ```

### 1.2 安装必要组件

打开 Android Studio → **Tools → SDK Manager**

#### SDK Platforms
- ✅ Android 14.0 (API 34) - 必须
- ✅ Android 8.0 (API 26) - 最低支持

#### SDK Tools
- ✅ Android SDK Build-Tools 34.0.0
- ✅ Android SDK Command-line Tools
- ✅ Android SDK Platform-Tools
- ✅ CMake 3.22.1 或更高
- ✅ NDK (Side by side) - 选择最新稳定版（推荐 r25c+）
- ✅ LLDB - C++ 调试工具

### 1.3 验证安装

打开 **Tools → SDK Manager → SDK Tools**，确认：
- ✅ NDK 已安装
- ✅ CMake 已安装
- ✅ LLDB 已安装

## 二、项目导入与配置

### 2.1 打开项目

1. **启动 Android Studio**
2. **选择 "Open"**
3. **选择项目目录**：`/home/asus/filmtracker`
4. **等待 Gradle Sync**

### 2.2 首次同步配置

#### 如果 Gradle Sync 失败：

1. **检查 Gradle 版本**
   - File → Project Structure → Project
   - Gradle Version: 8.0+
   - Android Gradle Plugin: 8.2.0+

2. **配置 Gradle JDK**
   - File → Settings → Build → Build Tools → Gradle
   - Gradle JDK: 选择 JDK 17 或更高

3. **配置 NDK 路径**
   - File → Project Structure → SDK Location
   - Android NDK location: 自动检测或手动指定
   - 示例：`C:\Users\YourName\AppData\Local\Android\Sdk\ndk\25.x.x`

### 2.3 项目结构检查

确认以下目录存在：
```
filmtracker/
├── app/
│   ├── build.gradle.kts          ✅
│   ├── src/main/
│   │   ├── cpp/                   ✅ C++ 源码
│   │   ├── java/                  ✅ Kotlin 源码
│   │   └── res/                   ✅ 资源文件
│   └── CMakeLists.txt             ✅
├── build.gradle.kts               ✅
└── settings.gradle.kts            ✅
```

## 三、编译与运行

### 3.1 首次编译

#### 方法一：使用 GUI
1. **点击工具栏 "Build" → "Make Project"** (Ctrl+F9)
2. **等待编译完成**
3. **查看 Build 窗口**，确认无错误

#### 方法二：使用命令行
```bash
# 在项目根目录
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

### 3.2 常见编译问题解决

#### 问题 1: NDK 未找到
```
Error: NDK not found
```
**解决**：
- File → Project Structure → SDK Location
- 设置正确的 NDK 路径

#### 问题 2: CMake 版本不匹配
```
Error: CMake 3.22.1 or higher is required
```
**解决**：
- SDK Manager → SDK Tools → CMake
- 安装 3.22.1 或更高版本

#### 问题 3: C++ 编译错误
```
Error: undefined reference to...
```
**解决**：
- 检查 `CMakeLists.txt` 中的源文件列表
- 确认所有 `.cpp` 文件都已包含

### 3.3 运行应用

#### 3.3.1 连接设备

**选项 A：使用物理设备**
1. 启用开发者选项：
   - 设置 → 关于手机 → 连续点击"版本号" 7 次
2. 启用 USB 调试：
   - 设置 → 开发者选项 → USB 调试
3. 连接手机到电脑
4. 确认设备出现在设备列表中

**选项 B：使用模拟器**
1. Tools → Device Manager
2. Create Device
3. 选择设备（推荐 Pixel 6，API 34）
4. 下载系统镜像（API 34）
5. Finish

#### 3.3.2 运行应用

1. **选择运行配置**
   - 工具栏下拉菜单选择 "app"
   - 选择目标设备

2. **点击运行按钮** (Shift+F10)
   - 或点击工具栏绿色播放按钮

3. **等待安装和启动**
   - 首次运行会较慢（编译 + 安装）
   - 后续运行会使用增量编译

## 四、开发工作流

### 4.1 修改 C++ 代码

#### 步骤：
1. **编辑 C++ 文件**
   - `app/src/main/cpp/` 目录下的文件

2. **重新编译**
   - Build → Make Project
   - 或使用快捷键 Ctrl+F9

3. **重新运行应用**
   - 点击运行按钮
   - 或使用快捷键 Shift+F10

#### 提示：
- C++ 代码修改后必须重新编译
- 可以使用 **Build → Clean Project** 清理缓存

### 4.2 修改 Kotlin/Compose 代码

#### 步骤：
1. **编辑 Kotlin 文件**
   - `app/src/main/java/` 目录下的文件

2. **使用 Instant Run（如果支持）**
   - 修改后点击 "Apply Changes" 按钮
   - 或使用快捷键 Ctrl+F10

3. **或重新运行**
   - 点击运行按钮

#### Compose Preview（推荐）
1. **打开 Compose 文件**
   - 例如：`ProcessingScreen.kt`

2. **点击右上角 "Split" 或 "Design"**
   - 可以看到实时预览

3. **修改代码后**
   - 点击预览窗口的刷新按钮
   - 或使用快捷键 Ctrl+Shift+R

### 4.3 调试技巧

#### 4.3.1 Kotlin 调试

1. **设置断点**
   - 点击代码行号左侧
   - 红色圆点表示断点

2. **以 Debug 模式运行**
   - 点击工具栏 "Debug" 按钮 (Shift+F9)
   - 或 Run → Debug 'app'

3. **调试窗口**
   - Variables: 查看变量值
   - Watches: 监视表达式
   - Call Stack: 调用栈
   - Console: 日志输出

#### 4.3.2 C++ 调试（NDK 调试）

1. **配置 Native 调试**
   - Run → Edit Configurations
   - Debugger: 选择 "Native"
   - 或使用 "Hybrid"（同时调试 Java/Kotlin 和 C++）

2. **设置 C++ 断点**
   - 在 C++ 文件中设置断点
   - 确保 LLDB 已安装

3. **以 Debug 模式运行**
   - 点击 Debug 按钮
   - 等待 LLDB 连接

4. **查看 Native 变量**
   - Debug 窗口 → LLDB 标签
   - 可以执行 C++ 表达式

#### 4.3.3 日志调试

**Kotlin 日志**：
```kotlin
import android.util.Log

Log.d("TAG", "Debug message")
Log.e("TAG", "Error message")
```

**C++ 日志**：
```cpp
#include <android/log.h>

__android_log_print(ANDROID_LOG_DEBUG, "TAG", "Debug message");
__android_log_print(ANDROID_LOG_ERROR, "TAG", "Error message");
```

**查看日志**：
- View → Tool Windows → Logcat
- 或点击底部 "Logcat" 标签

### 4.4 性能分析

#### 4.4.1 CPU Profiler

1. **启动应用**
2. **View → Tool Windows → Profiler**
3. **点击 CPU 行**
4. **录制性能数据**
   - 点击 Record
   - 执行操作
   - 点击 Stop

5. **分析结果**
   - 查看函数调用时间
   - 识别性能瓶颈

#### 4.4.2 Memory Profiler

1. **打开 Profiler**
2. **点击 Memory 行**
3. **查看内存使用**
   - 堆内存
   - Native 内存
   - 内存泄漏检测

## 五、常用操作

### 5.1 清理和重建

```bash
# 清理构建产物
Build → Clean Project

# 重建项目
Build → Rebuild Project
```

### 5.2 查看项目依赖

1. **View → Tool Windows → Gradle**
2. **展开项目 → app → Tasks → help**
3. **双击 "dependencies"**
4. **查看依赖树**

### 5.3 代码格式化

- **格式化当前文件**：Ctrl+Alt+L
- **格式化整个项目**：Code → Reformat Code

### 5.4 代码检查

- **Analyze → Inspect Code**
- 选择检查范围
- 查看检查结果

### 5.5 Git 集成

1. **VCS → Enable Version Control Integration**
2. **选择 Git**
3. **提交更改**：Ctrl+K
4. **推送更改**：Ctrl+Shift+K

## 六、项目特定配置

### 6.1 NDK 配置检查

**确认 `app/build.gradle.kts` 中的配置**：
```kotlin
externalNativeBuild {
    cmake {
        path = file("src/main/cpp/CMakeLists.txt")
        version = "3.22.1"
    }
}

ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a")
}
```

### 6.2 CMake 配置检查

**确认 `app/src/main/cpp/CMakeLists.txt` 存在且正确**

### 6.3 JNI 接口检查

**确认 Native 方法声明**：
- Kotlin 文件中的 `external fun` 方法
- 对应的 C++ JNI 函数实现

## 七、开发最佳实践

### 7.1 代码组织

```
app/src/main/
├── cpp/                    # C++ 核心算法
│   ├── include/           # 头文件
│   └── *.cpp              # 实现文件
├── java/                  # Kotlin 代码
│   ├── native/           # JNI 封装
│   ├── data/             # 数据模型
│   ├── ui/               # UI 组件
│   └── util/             # 工具类
└── res/                   # 资源文件
```

### 7.2 编译优化

**Debug 模式**：
- 快速编译
- 包含调试信息
- 不优化代码

**Release 模式**：
- 代码优化（-O3）
- 移除调试信息
- 启用 ProGuard

切换方式：
- Build Variants → 选择 debug/release

### 7.3 测试流程

1. **单元测试**
   - `app/src/test/` - 本地单元测试
   - `app/src/androidTest/` - 设备测试

2. **运行测试**
   - 右键测试文件 → Run
   - 或 Run → Run Tests

### 7.4 版本管理

**版本号配置**（`app/build.gradle.kts`）：
```kotlin
defaultConfig {
    versionCode = 1
    versionName = "1.0.0"
}
```

## 八、常见问题排查

### 8.1 Gradle Sync 失败

**检查**：
1. 网络连接（需要下载依赖）
2. Gradle 版本兼容性
3. JDK 版本（需要 JDK 17+）

**解决**：
- File → Invalidate Caches → Invalidate and Restart

### 8.2 NDK 编译错误

**检查**：
1. NDK 版本是否正确
2. CMakeLists.txt 语法
3. C++ 标准设置（C++17）

**解决**：
- 查看 Build 窗口的详细错误信息
- 检查 C++ 代码语法

### 8.3 运行时崩溃

**检查**：
1. Logcat 中的错误信息
2. Native 崩溃：查看 tombstone 文件
3. JNI 调用是否正确

**调试**：
- 使用 Debug 模式运行
- 设置断点追踪
- 查看调用栈

### 8.4 性能问题

**检查**：
1. 使用 Profiler 分析
2. 检查是否有内存泄漏
3. 优化 C++ 算法（SIMD、多线程）

## 九、推荐插件

### 9.1 必装插件

1. **Kotlin** - Kotlin 语言支持（通常已内置）
2. **Android NDK Support** - NDK 开发支持

### 9.2 推荐插件

1. **Rainbow Brackets** - 括号高亮
2. **CodeGlance** - 代码缩略图
3. **GitToolBox** - Git 增强
4. **Material Theme UI** - Material 主题

安装方式：
- File → Settings → Plugins → Marketplace

## 十、快速参考

### 10.1 快捷键（Windows/Linux）

| 功能 | 快捷键 |
|------|--------|
| 运行 | Shift+F10 |
| 调试 | Shift+F9 |
| 构建 | Ctrl+F9 |
| 格式化 | Ctrl+Alt+L |
| 查找 | Ctrl+F |
| 全局搜索 | Shift+Shift |
| 打开文件 | Ctrl+Shift+N |
| 最近文件 | Ctrl+E |
| 代码补全 | Ctrl+Space |
| 快速修复 | Alt+Enter |

### 10.2 常用菜单路径

- **Build Project**: Build → Make Project
- **Clean Project**: Build → Clean Project
- **Rebuild Project**: Build → Rebuild Project
- **Sync Project**: File → Sync Project with Gradle Files
- **Invalidate Caches**: File → Invalidate Caches

### 10.3 重要窗口

- **Logcat**: View → Tool Windows → Logcat
- **Profiler**: View → Tool Windows → Profiler
- **Terminal**: View → Tool Windows → Terminal
- **Gradle**: View → Tool Windows → Gradle
- **Build**: View → Tool Windows → Build

## 十一、下一步

完成基础配置后，可以：

1. **运行示例代码**
   - 确保项目可以编译运行

2. **集成真实 RAW 解码库**
   - 参考 `BUILD.md` 中的说明

3. **完善算法实现**
   - 实现完整的 AHD 去马赛克
   - 优化性能

4. **开发 UI**
   - 使用 Compose Preview 快速迭代
   - 添加更多功能

5. **测试和优化**
   - 使用 Profiler 分析性能
   - 修复 bug

---

**需要帮助？**
- 查看 `ARCHITECTURE.md` 了解架构
- 查看 `BUILD.md` 了解构建细节
- 查看 Android Studio 官方文档
