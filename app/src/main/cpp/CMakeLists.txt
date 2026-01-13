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

target_link_libraries(filmtracker
    ${log-lib}
    ${jnigraphics-lib}
    ${m-lib}  # 数学库（LibRaw 需要）
    LibRaw::raw  # LibRaw 库
)
