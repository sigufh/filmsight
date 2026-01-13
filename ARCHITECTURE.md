# FilmTracker 架构说明

## 架构设计原则

本项目采用**混合架构**，充分发挥各语言优势：

- **C++ / NDK**：核心图像处理算法（性能关键）
- **Kotlin / Compose**：UI 层和业务逻辑（现代化、素材丰富）

## 模块划分

### 1. C++ 核心层 (`app/src/main/cpp/`)

#### 职责
- RAW 图像解码与线性化
- 胶片银盐模拟算法
- 高性能像素处理

#### 关键模块
- `raw_processor.cpp`: RAW 解码、Bayer 去马赛克
- `film_engine.cpp`: 胶片模拟引擎
- `film_response_curve.cpp`: Toe/Linear/Shoulder 响应曲线
- `color_crosstalk.cpp`: 颜色猜色矩阵
- `grain_model.cpp`: 泊松颗粒模型
- `image_converter.cpp`: 线性到 sRGB 转换（仅输出阶段）

#### 设计约束
- ✅ 所有核心计算在线性光域
- ✅ 不使用 LUT、滤镜、预烘焙映射
- ✅ 不依赖 GPU（CPU SIMD 优先）

### 2. JNI 桥接层 (`app/src/main/cpp/jni_bridge.cpp`)

#### 职责
- Kotlin ↔ C++ 数据传递
- 内存管理
- 异常处理

### 3. Kotlin Native 封装层 (`app/src/main/java/com/filmtracker/app/native/`)

#### 职责
- 封装 JNI 调用
- 类型转换
- 资源管理

### 4. Kotlin 业务层

#### 数据模型 (`app/src/main/java/com/filmtracker/app/data/`)
- `FilmParams.kt`: 胶片参数数据类
- 预设管理（Portra 400, Kodak Gold 200 等）

#### AI 模块 (`app/src/main/java/com/filmtracker/app/ai/`)
- `AIParameterAdvisor.kt`: AI 参数建议
- **核心原则**：AI 只输出参数，不生成图像
- 轻量模型（<5MB），端侧推理

#### UI 层 (`app/src/main/java/com/filmtracker/app/ui/`)
- **Jetpack Compose** 现代化 UI
- Material 3 设计系统
- 丰富的参数控制组件

### 5. UI 组件优势

使用 Kotlin/Compose 的优势：

1. **丰富的组件库**
   - `Slider`: 参数调整
   - `Card`: 分组容器
   - `Switch`: 开关控制
   - Material 3 完整组件集

2. **图像处理库**
   - `Coil`: 异步图像加载
   - `Bitmap`: Android 原生支持
   - 完整的图像 I/O API

3. **现代化 UI**
   - 声明式 UI
   - 响应式布局
   - 动画支持
   - 主题系统

## 数据流

```
RAW 文件
  ↓
[Kotlin] 文件选择
  ↓
[JNI] 传递文件路径
  ↓
[C++] RAW 解码 → 线性 RGB
  ↓
[C++] 胶片模拟（猜色 → 响应曲线 → 颗粒）
  ↓
[C++] 线性 RGB（处理后）
  ↓
[JNI] 返回图像数据
  ↓
[Kotlin] 转换为 Bitmap
  ↓
[Compose] 显示预览
  ↓
[Kotlin] 参数调整（用户交互）
  ↓
[循环] 重新处理
  ↓
[Kotlin] 导出最终图像
```

## 性能策略

1. **预览模式**：低分辨率处理（快速反馈）
2. **导出模式**：全分辨率处理（最终输出）
3. **异步处理**：Kotlin Coroutines + Dispatchers.Default
4. **SIMD 优化**：C++ 层使用 AVX2/FMA

## 扩展性

- 新增胶片预设：在 `FilmParams.kt` 添加
- 新增 AI 模型：在 `AIParameterAdvisor.kt` 集成
- 新增 UI 组件：Compose 组件化设计
- 新增算法：C++ 层模块化设计

## 关键约束遵守

✅ **RAW 优先**：所有计算在线性域  
✅ **模型先于结果**：模拟物理过程  
✅ **AI 辅助**：只输出参数建议  
✅ **离线可用**：无网络依赖  
✅ **可解释性**：所有参数可调、可理解
