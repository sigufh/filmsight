cmake_minimum_required(VERSION 3.22.1)
project("filmtracker")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")

# 集成 LibRaw 库（用于解析 RAW 文件）
include(FetchContent)

# 设置 LibRaw 构建选项（必须在 FetchContent_Declare 之前设置）
set(LIBRAW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LIBRAW_BUILD_DOCS OFF CACHE BOOL "" FORCE)
set(LIBRAW_BUILD_DEMOSAIC_PACK_GPL2 OFF CACHE BOOL "" FORCE)
set(LIBRAW_BUILD_DEMOSAIC_PACK_GPL3 OFF CACHE BOOL "" FORCE)

FetchContent_Declare(
    libraw
    GIT_REPOSITORY https://github.com/LibRaw/LibRaw.git
    GIT_TAG 0.21.1
)

# 获取 LibRaw 源码（但不自动添加到构建）
FetchContent_GetProperties(libraw)
if(NOT libraw_POPULATED)
    FetchContent_Populate(libraw)
    message(STATUS "LibRaw source directory: ${libraw_SOURCE_DIR}")
endif()

# Source files
set(RAW_PROCESSOR_SOURCES
    raw_processor.cpp
    raw_decoder.cpp
    bayer_demosaic.cpp
)

set(IMAGE_PROCESSOR_SOURCES
    image_processor_engine.cpp
    error_diffusion_dithering.cpp
    dynamic_range_protection.cpp
    adobe_tone_adjustment.cpp
    contrast_adjustment.cpp
    color_temperature.cpp
    color_grading.cpp
    bilateral_filter.cpp
)

set(IMAGE_CONVERTER_SOURCES
    image_converter.cpp
)

set(JNI_SOURCES
    jni/jni_common.h
    jni/jni_raw_processor.cpp
    jni/jni_converter.cpp
    jni/jni_image_processor.cpp
    jni/jni_parameters.cpp
)

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${libraw_SOURCE_DIR}
)

# 手动编译 LibRaw（因为其 CMake 配置可能不支持 Android）
# 收集 LibRaw 源文件
file(GLOB LIBRAW_SOURCES 
    "${libraw_SOURCE_DIR}/src/*.cpp"
    "${libraw_SOURCE_DIR}/src/decoders/*.cpp"
    "${libraw_SOURCE_DIR}/src/demosaic/*.cpp"
    "${libraw_SOURCE_DIR}/src/integration/*.cpp"
    "${libraw_SOURCE_DIR}/src/metadata/*.cpp"
    "${libraw_SOURCE_DIR}/src/postprocessing/*.cpp"
    "${libraw_SOURCE_DIR}/src/preprocessing/*.cpp"
    "${libraw_SOURCE_DIR}/src/tables/*.cpp"
    "${libraw_SOURCE_DIR}/src/utils/*.cpp"
    "${libraw_SOURCE_DIR}/src/write/*.cpp"
    "${libraw_SOURCE_DIR}/src/x3f/*.cpp"
)

# 排除 postprocessing_ph.cpp，因为它包含了其他文件的内联实现
list(FILTER LIBRAW_SOURCES EXCLUDE REGEX ".*postprocessing_ph\\.cpp$")

# 创建 LibRaw 静态库
if(LIBRAW_SOURCES)
    add_library(libraw_static STATIC ${LIBRAW_SOURCES})
    target_include_directories(libraw_static PUBLIC 
        ${libraw_SOURCE_DIR}
        ${libraw_SOURCE_DIR}/libraw
    )
    target_compile_definitions(libraw_static PRIVATE 
        LIBRAW_NODLL
        LIBRAW_NOTHREADS  # 禁用多线程以简化 Android 构建
    )
    message(STATUS "LibRaw: Found ${CMAKE_MATCH_COUNT} source files, building static library")
else()
    message(FATAL_ERROR "LibRaw: No source files found in ${libraw_SOURCE_DIR}/src")
endif()

# Create shared library
add_library(filmtracker SHARED
    ${RAW_PROCESSOR_SOURCES}
    ${IMAGE_PROCESSOR_SOURCES}
    ${IMAGE_CONVERTER_SOURCES}
    ${JNI_SOURCES}
)

# Link libraries
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(m-lib m)

# 链接 LibRaw 静态库
target_link_libraries(filmtracker
    libraw_static
    ${log-lib}
    ${jnigraphics-lib}
    ${m-lib}  # 数学库（LibRaw 需要）
)
