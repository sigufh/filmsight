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

# 获取 LibRaw 源码并添加到构建
FetchContent_MakeAvailable(libraw)

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

# 添加 LibRaw 包含目录（如果已获取）
if(libraw_SOURCE_DIR)
    include_directories(${libraw_SOURCE_DIR})
endif()

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
# LibRaw 通过 FetchContent_MakeAvailable 创建的目标
# 检查常见的目标名称
set(LIBRAW_TARGET_FOUND FALSE)
if(TARGET raw)
    target_link_libraries(filmtracker raw)
    set(LIBRAW_TARGET_FOUND TRUE)
    message(STATUS "LibRaw: Successfully linked to target 'raw'")
elseif(TARGET libraw)
    target_link_libraries(filmtracker libraw)
    set(LIBRAW_TARGET_FOUND TRUE)
    message(STATUS "LibRaw: Successfully linked to target 'libraw'")
elseif(TARGET LibRaw::raw)
    target_link_libraries(filmtracker LibRaw::raw)
    set(LIBRAW_TARGET_FOUND TRUE)
    message(STATUS "LibRaw: Successfully linked to target 'LibRaw::raw'")
endif()

if(NOT LIBRAW_TARGET_FOUND)
    # 如果 LibRaw 的 CMake 不支持 Android，尝试手动编译
    message(WARNING "LibRaw CMake target not found. Attempting manual compilation...")
    
    if(libraw_SOURCE_DIR AND EXISTS ${libraw_SOURCE_DIR}/libraw/libraw.h)
        # 手动添加 LibRaw 源文件
        file(GLOB LIBRAW_SOURCES 
            "${libraw_SOURCE_DIR}/libraw/*.cpp"
            "${libraw_SOURCE_DIR}/internal/*.cpp"
        )
        
        # 创建 LibRaw 静态库
        add_library(libraw_static STATIC ${LIBRAW_SOURCES})
        target_include_directories(libraw_static PUBLIC ${libraw_SOURCE_DIR})
        target_compile_definitions(libraw_static PRIVATE LIBRAW_NODLL)
        
        # 链接到主库
        target_link_libraries(filmtracker libraw_static)
        message(STATUS "LibRaw: Manually compiled and linked")
    else()
        message(FATAL_ERROR "LibRaw source not found. Please check FetchContent configuration.")
    endif()
endif()

target_link_libraries(filmtracker
    ${log-lib}
    ${jnigraphics-lib}
    ${m-lib}  # 数学库（LibRaw 需要）
)
