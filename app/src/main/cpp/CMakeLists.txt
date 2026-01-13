cmake_minimum_required(VERSION 3.22.1)
project("filmtracker")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")

# 集成 LibRaw 库（用于解析 RAW 文件）
include(FetchContent)

FetchContent_Declare(
    libraw
    GIT_REPOSITORY https://github.com/LibRaw/LibRaw.git
    GIT_TAG 0.21.1
)

# 设置 LibRaw 构建选项（在 FetchContent_Populate 之前设置）
set(LIBRAW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LIBRAW_BUILD_DOCS OFF CACHE BOOL "" FORCE)
set(LIBRAW_BUILD_DEMOSAIC_PACK_GPL2 OFF CACHE BOOL "" FORCE)
set(LIBRAW_BUILD_DEMOSAIC_PACK_GPL3 OFF CACHE BOOL "" FORCE)

# 获取 LibRaw 源码
FetchContent_GetProperties(libraw)
if(NOT libraw_POPULATED)
    FetchContent_Populate(libraw)
    set(libraw_SOURCE_DIR ${libraw_SOURCE_DIR} CACHE PATH "" FORCE)
    set(libraw_BINARY_DIR ${libraw_BINARY_DIR} CACHE PATH "" FORCE)
endif()

# 添加 LibRaw 子目录（这会执行 LibRaw 的 CMakeLists.txt）
add_subdirectory(${libraw_SOURCE_DIR} ${libraw_BINARY_DIR})

# Source files
set(RAW_PROCESSOR_SOURCES
    raw_processor.cpp
    raw_decoder.cpp
    bayer_demosaic.cpp
)

set(FILM_ENGINE_SOURCES
    film_engine.cpp
    film_response_curve.cpp
    color_crosstalk.cpp
    grain_model.cpp
    tone_curve.cpp
    hsl_adjust.cpp
)

set(IMAGE_CONVERTER_SOURCES
    image_converter.cpp
)

set(JNI_SOURCES
    jni_bridge.cpp
)

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Create shared library
add_library(filmtracker SHARED
    ${RAW_PROCESSOR_SOURCES}
    ${FILM_ENGINE_SOURCES}
    ${IMAGE_CONVERTER_SOURCES}
    ${JNI_SOURCES}
)

# Link libraries
find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(m-lib m)

# 链接 LibRaw
# LibRaw 通过 FetchContent_MakeAvailable 创建的目标名称通常是 "raw"
# 但某些版本可能使用不同的名称，我们需要检查并链接正确的目标
if(TARGET raw)
    target_link_libraries(filmtracker raw)
    target_include_directories(filmtracker PRIVATE ${libraw_SOURCE_DIR})
    message(STATUS "LibRaw: Linking to target 'raw'")
elseif(TARGET libraw)
    target_link_libraries(filmtracker libraw)
    target_include_directories(filmtracker PRIVATE ${libraw_SOURCE_DIR})
    message(STATUS "LibRaw: Linking to target 'libraw'")
elseif(TARGET LibRaw::raw)
    target_link_libraries(filmtracker LibRaw::raw)
    message(STATUS "LibRaw: Linking to target 'LibRaw::raw'")
else()
    # 如果找不到目标，尝试手动添加包含目录和链接库
    if(EXISTS ${libraw_SOURCE_DIR})
        target_include_directories(filmtracker PRIVATE ${libraw_SOURCE_DIR})
        message(STATUS "LibRaw: Added include directory ${libraw_SOURCE_DIR}")
        message(WARNING "LibRaw target not found, but include directory added. You may need to manually link the library.")
    else()
        message(FATAL_ERROR "LibRaw not found. Please check the FetchContent configuration.")
    endif()
endif()

target_link_libraries(filmtracker
    ${log-lib}
    ${jnigraphics-lib}
    ${m-lib}  # 数学库（LibRaw 需要）
)
