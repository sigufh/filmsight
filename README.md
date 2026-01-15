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

📖 **详细功能清单**：查看 [FEATURES.md](FEATURES.md)

## 📋 项目概述

FilmSight 是一款面向 Android 平台的专业级 RAW/DNG 摄影后期处理软件。产品以"数字暗房"为核心理念，围绕 RAW 图像的线性光域处理，结合胶片银盐成像机理模拟与 AI 辅助参数决策，为摄影师提供高度可控、可解释、可定制的影像风格构建工具。

### 核心特性

- **RAW 优先**：所有核心计算在 RAW 线性域完成
- **模型先于结果**：模拟成像过程，而非结果映射
- **AI 辅助而非替代**：AI 输出建议参数，不直接输出颜色
- **离线可用**：无网络情况下具备完整功能
- **专业工作流**：符合摄影后期软件使用习惯

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

## 📋 项目概述（详细）
一、项目概述
本项目是一款面向 Android 平台的专业级 RAW / DNG 摄影后期处理软件。产品以“数字暗房”为核心理念，围绕 RAW 图像的线性光域处理，结合胶片银盐成像机理模拟与AI 辅助参数决策，为摄影师提供高度可控、可解释、可定制的影像风格构建工具。
本软件不依赖系统相机，仅处理用户相册中的 RAW 图像；不以 GPU 为主要算力依赖，核心算法以 CPU / NPU 为主；AI 不直接生成影像，而是作为“理解 RAW 与约束参数空间的暗房助手”。

二、设计目标与原则
2.1 设计目标
1.提供完整的 RAW → 胶片影调 → 输出工作流
2.实现区别于 LUT / 滤镜的胶片银盐模拟方法
3.引入 AI 提升效率，但不剥夺摄影师控制权
4.支持高度参数化与用户自定义
5.在中端 Android 设备上稳定运行
2.2 核心设计原则
RAW 优先：所有核心计算在 RAW 线性域完成
模型先于结果：模拟成像过程，而非结果映射
AI 辅助而非替代：AI 输出建议参数，不直接输出颜色
离线可用：无网络情况下具备完整功能
专业工作流一致性：符合摄影后期软件使用习惯

三、系统总体架构
系统采用分层、模块化架构，分为本地端与云端协同两部分。
3.1 本地端（Android）
UI 与交互层（Kotlin / Compose）
RAW 导入与解析模块
RAW 解码与线性化模块（C++ / NDK）
胶片银盐模拟引擎（C++）
AI 边缘推理模块（轻量模型）
图像输出与导出模块
3.2 云端（可选增强）
参数分布学习服务
胶片人格模型更新
高复杂度模型反演与分析
云端不参与实时处理，不作为功能依赖。

四、RAW 图像处理流程
4.1 RAW 导入
通过 Android MediaStore / SAF 访问相册
支持批量选择 DNG / RAW 文件
4.2 RAW 解码与线性化
处理流程：
1.读取 RAW 数据与 DNG Tags
2.黑电平校正
3.白电平归一化
4.Bayer 去马赛克（AHD / Malvar）
5.得到线性 RGB 数据
该阶段不进行 Gamma、Tone Mapping 或风格化处理。

五、元数据驱动系统
系统解析并利用以下关键元数据：
ISO：噪声模型、颗粒密度参考
曝光时间：高光压缩与肩部宽度
白平衡信息：猜色基准
相机型号：传感器色彩偏向
元数据用于初始化胶片参数与 AI 建议区间，而非直接决定结果。

六、胶片银盐模拟引擎（核心）
6.1 核心思想
模拟胶片成像过程中的不确定性与非线性响应，而非简单颜色映射。
6.2 猜色权重模型
将线性 RGB 映射为伪光谱能量分布
引入非对角权重矩阵模拟颜色误判
权重随曝光强度、邻域信息变化
6.3 银盐非线性响应曲线
每个伪光谱通道具备独立的：
Toe（暗部抬升）
Linear（中间调）
Shoulder（高光压缩）
曲线参数可由用户或 AI 建议调整。
6.4 颗粒模型
基于泊松统计的颗粒生成
颗粒与亮度、ISO、颜色耦合
颗粒参与曝光，而非后期叠加

七、AI 系统设计
7.1 AI 的角色定位
AI 不直接修改像素，而是承担：
1.RAW 内容感知
2.参数建议与风险提示
3.可选的 RAW 域降噪
7.2 边缘推理模块
小型 CNN / 回归模型
模型体积 < 5MB
输入为低分辨率线性 RGB + 元数据
输出为参数区间与推荐模板
7.3 云端 AI 模块（增强）
学习用户参数选择分布
优化胶片人格初始参数
反演真实胶片扫描特征

八、专业工作流与 UI 设计
8.1 工作流顺序
1.RAW 导入
2.基础曝光与白平衡
3.胶片建模（核心）
4.颗粒与质感
5.AI 辅助建议
6.输出与导出
8.2 交互设计要点
参数以“推荐区间 + 当前值”形式呈现
支持参数快照、历史回溯、A/B 对比
提供可视化辅助图层（猜色权重、颗粒分布）

九、性能与算力策略
核心算法以 CPU SIMD 实现
AI 推理优先使用 CPU / NPU
GPU 仅用于 UI 预览（非必需）
全分辨率处理仅在导出阶段执行

十、端云协同与数据安全
所有 RAW 本地处理
云端仅上传匿名参数统计（可关闭）
明确隐私与数据使用说明

十一、开发流程与里程碑（简述）
1.PC 端算法原型与验证
2.AI 模型训练与轻量化
3.C++ 工程化
4.Android 集成与 UI 开发
5.性能与摄影级验证
6.测试与上线

十二、项目总结
本项目通过 RAW 线性光域处理、胶片银盐成像模型与 AI 辅助决策的深度融合，构建了一套区别于传统滤镜与 AI 直出风格的移动端专业摄影解决方案，兼具技术创新性与实际可用性。