cmake_minimum_required(VERSION 3.22.1)
project("filmtracker")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")

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
    ${JNI_SOURCES}
)

# Link libraries
find_library(log-lib log)
target_link_libraries(filmtracker
    ${log-lib}
)
