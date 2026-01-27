# FilmTracker - Android RAW 胶片影调 AI 摄影软件

> 专业级 RAW/DNG 胶片影调处理应用 | 基于线性光域处理与胶片银盐模拟

## 🚀 快速开始

### 使用 Android Studio 开发

1. **打开项目**
   - 启动 Android Studio
   - File → Open → 选择项目目录

2. **等待 Gradle Sync**
   - 首次打开会自动同步依赖

3. **运行应用**
   - 连接设备或启动模拟器
   - 点击 Run 按钮（Shift+F10）

📖 **详细开发流程**：查看 [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)  
⚡ **快速参考**：查看 [QUICK_START.md](QUICK_START.md)

---

## ✨ 核心功能

- ✅ **完整修图调色**：曝光、对比度、高光/阴影、白场/黑场、清晰度、自然饱和度
- ✅ **色调曲线**：RGB 总曲线 + 单通道曲线（16 控制点，Catmull-Rom 插值）
- ✅ **HSL 调整**：8 个色相段独立调整（色相、饱和度、亮度）
- ✅ **AI 美颜**：人脸检测、皮肤分析、参数建议、一键应用
- ✅ **胶片模拟**：颜色猜色矩阵、通道独立响应曲线、泊松颗粒模型
- ✅ **非破坏性编辑**：原始图像永不修改，所有调整以参数形式保存，支持完整的撤销/重做历史

📖 **详细功能清单**：查看 [FEATURES.md](FEATURES.md)

## 📋 项目概述

FilmSight 是一款面向 Android 平台的专业级 RAW/DNG 摄影后期处理软件。产品以"数字暗房"为核心理念，围绕 RAW 图像的线性光域处理，结合胶片银盐成像机理模拟与 AI 辅助参数决策，为摄影师提供高度可控、可解释、可定制的影像风格构建工具。

### 核心特性

- **RAW 优先**：所有核心计算在 RAW 线性域完成
- **模型先于结果**：模拟成像过程，而非结果映射
- **AI 辅助而非替代**：AI 输出建议参数，不直接输出颜色
- **离线可用**：无网络情况下具备完整功能
- **专业工作流**：符合摄影后期软件使用习惯
- **非破坏性编辑**：原始图像永不修改，所有调整可随时撤销和修改

---

## 🎯 非破坏性编辑系统

FilmTracker 实现了类似 Lightroom 的非破坏性编辑工作流，确保原始图像文件始终保持完整，所有调整操作都以参数形式记录。

### 核心优势

- **无损编辑**：原始文件永不被修改，可随时恢复
- **灵活调整**：任何时候都可以修改或撤销任何操作
- **高效存储**：参数文件体积远小于图像文件
- **批量一致性**：同一组参数可应用到多张照片
- **RAW 潜力最大化**：直接处理传感器原始数据，保留最大动态范围

### 工作流程

```
1. 导入图像 → 原始文件保持只读
2. 调整参数 → 参数自动保存为 JSON 元数据
3. 实时预览 → 基于参数动态计算
4. 撤销/重做 → 完整的编辑历史记录
5. 导出图像 → 仅在导出时应用所有参数到原始数据
```

### 元数据文件

所有调整参数保存在独立的 JSON 文件中：

```
/storage/emulated/0/DCIM/Camera/
    IMG_1234.jpg                          # 原始图像（只读）
    
/data/data/com.filmtracker.app/files/
    metadata/
        IMG_1234.jpg.filmtracker.json     # 参数元数据
    last_session.json                      # 最后编辑会话
```

**元数据文件格式**：查看 [docs/NON_DESTRUCTIVE_EDITING.md](docs/NON_DESTRUCTIVE_EDITING.md)

### 会话管理

- **自动保存**：参数修改后自动保存到元数据文件
- **会话恢复**：应用重启后自动恢复上次编辑状态
- **多图像切换**：每张图像独立保存编辑状态
- **历史记录**：支持无限次撤销/重做操作

### 导出选项

导出时将所有参数一次性应用到原始数据：

- **格式**：JPEG、PNG、TIFF
- **质量**：可调节压缩质量（JPEG）
- **位深度**：8-bit 或 16-bit（TIFF/PNG）
- **色彩空间**：sRGB、Adobe RGB、ProPhoto RGB
- **分辨率**：完整原始分辨率

📖 **详细文档**：查看 [docs/NON_DESTRUCTIVE_EDITING.md](docs/NON_DESTRUCTIVE_EDITING.md)

---

## 🏗️ 架构设计

本项目采用 **Clean Architecture** 分层架构，确保代码的可维护性、可测试性和可扩展性。

### 架构层次

```
┌─────────────────────────────────────────┐
│          UI Layer (Compose)             │
│  ┌─────────────┐    ┌─────────────┐    │
│  │  ViewModel  │    │   Screen    │    │
│  └─────────────┘    └─────────────┘    │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│         Domain Layer (Kotlin)           │
│  ┌─────────────┐    ┌─────────────┐    │
│  │   UseCase   │    │    Model    │    │
│  └─────────────┘    └─────────────┘    │
│  ┌─────────────────────────────────┐   │
│  │      Repository Interface       │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│          Data Layer (Kotlin)            │
│  ┌─────────────┐    ┌─────────────┐    │
│  │ Repository  │    │   Mapper    │    │
│  │    Impl     │    │             │    │
│  └─────────────┘    └─────────────┘    │
│  ┌─────────────┐    ┌─────────────┐    │
│  │   Native    │    │    Local    │    │
│  │   Source    │    │   Source    │    │
│  └─────────────┘    └─────────────┘    │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│        Native Layer (C++/JNI)           │
│  ┌─────────────┐    ┌─────────────┐    │
│  │ RAW Decoder │    │   Image     │    │
│  │             │    │  Processor  │    │
│  └─────────────┘    └─────────────┘    │
│  ┌─────────────┐    ┌─────────────┐    │
│  │   Bayer     │    │    Tone     │    │
│  │  Demosaic   │    │    Curve    │    │
│  └─────────────┘    └─────────────┘    │
└─────────────────────────────────────────┘
```

### 模块说明

#### UI Layer
- **ViewModel**: 管理 UI 状态和业务逻辑
- **Screen**: Jetpack Compose UI 组件
- **ViewModelFactory**: ViewModel 创建工厂（临时方案，未来将使用 DI）

#### Domain Layer
- **UseCase**: 封装业务逻辑用例
- **Model**: 领域模型，独立于具体实现
- **Repository Interface**: 数据访问接口定义

#### Data Layer
- **Repository Implementation**: 仓储接口实现
- **Mapper**: 数据模型转换
- **Data Source**: 数据源封装（Native、Local）

#### Native Layer
- **RAW Decoder**: RAW 文件解码（LibRaw）
- **Image Processor**: 图像处理引擎
- **Bayer Demosaic**: 拜耳阵列去马赛克
- **Tone Curve**: 色调曲线处理

### 数据流

```
User Input → ViewModel → UseCase → Repository → Data Source → Native Code
                ↓           ↓           ↓            ↓             ↓
            UI State ← Domain Model ← Data Model ← JNI ← C++ Processing
```

### 依赖规则

- **UI Layer** 依赖 **Domain Layer**
- **Data Layer** 依赖 **Domain Layer**
- **Domain Layer** 不依赖任何其他层（纯业务逻辑）
- **Native Layer** 被 **Data Layer** 调用

📖 **详细架构文档**：查看 [docs/architecture/REFACTORING_GUIDE.md](docs/architecture/REFACTORING_GUIDE.md)

---

## 🛠️ 开发指南

### 环境要求

- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **Gradle**: 8.0+
- **NDK**: 27.0.12077973
- **Kotlin**: 1.9.0+
- **Java**: 17+
- **CMake**: 3.22.1+

### 项目结构

```
filmsight/
├── app/
│   ├── src/main/
│   │   ├── cpp/                    # C++ Native 代码
│   │   │   ├── jni/               # JNI 接口层
│   │   │   ├── include/           # 头文件
│   │   │   └── *.cpp              # 实现文件
│   │   └── java/com/filmtracker/app/
│   │       ├── domain/            # 领域层
│   │       │   ├── model/         # 领域模型
│   │       │   ├── repository/    # 仓储接口
│   │       │   └── usecase/       # 用例
│   │       ├── data/              # 数据层
│   │       │   ├── repository/    # 仓储实现
│   │       │   ├── source/        # 数据源
│   │       │   └── mapper/        # 映射器
│   │       ├── ui/                # UI 层
│   │       │   ├── viewmodel/     # ViewModel
│   │       │   ├── screens/       # 界面
│   │       │   └── theme/         # 主题
│   │       ├── native/            # Native 接口
│   │       └── util/              # 工具类
│   └── build.gradle.kts
├── docs/                          # 文档
│   └── architecture/              # 架构文档
├── .kiro/                         # Kiro 配置
│   └── specs/                     # 规格文档
└── README.md
```

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd filmsight

# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease

# 运行测试
./gradlew test

# 安装到设备
./gradlew installDebug
```

### 代码规范

- **Kotlin**: 遵循 [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **C++**: 遵循 C++17 标准
- **命名规范**:
  - Kotlin: camelCase (变量/函数), PascalCase (类)
  - C++: snake_case (变量/函数), PascalCase (类)
- **注释**: 所有公共 API 必须有 KDoc/Doxygen 注释

### 迁移指南

如果你正在从旧代码迁移到新架构：

1. 查看 [docs/architecture/MIGRATION_CHECKLIST.md](docs/architecture/MIGRATION_CHECKLIST.md)
2. 使用新的 ViewModel 而不是直接调用 ImageProcessor
3. 通过 UseCase 访问业务逻辑
4. 使用 Repository 访问数据

---
