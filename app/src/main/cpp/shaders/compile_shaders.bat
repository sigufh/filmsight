@echo off
REM Compile GLSL shaders to SPIR-V and generate C++ headers

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo === Compiling Vulkan Compute Shaders ===
echo.

REM Check if glslangValidator is available
where glslangValidator >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: glslangValidator not found
    echo Please install the Vulkan SDK from https://vulkan.lunarg.com/
    exit /b 1
)

REM Check if Python 3 is available
where python >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: python not found
    echo Please install Python 3 from https://www.python.org/
    exit /b 1
)

REM Compile bilateral filter shader
echo Compiling bilateral_filter.comp...
glslangValidator -V bilateral_filter.comp -o bilateral_filter.spv

if %ERRORLEVEL% EQU 0 (
    echo [OK] Shader compiled successfully
    
    REM Convert to C++ header
    echo Converting SPIR-V to C++ header...
    python spv_to_cpp.py bilateral_filter.spv bilateral_filter_spv.h
    
    if !ERRORLEVEL! EQU 0 (
        echo [OK] C++ header generated successfully
        echo.
        echo === Compilation Complete ===
        echo.
        echo Next steps:
        echo 1. Include bilateral_filter_spv.h in vulkan_bilateral_filter.cpp
        echo 2. Replace the placeholder SPIR-V in compileShader^(^) with BILATERAL_FILTER_SPV
        echo.
        echo Example:
        echo   spirvCode.assign^(BILATERAL_FILTER_SPV,
        echo                    BILATERAL_FILTER_SPV + BILATERAL_FILTER_SPV_SIZE^);
    ) else (
        echo [ERROR] Failed to generate C++ header
        exit /b 1
    )
) else (
    echo [ERROR] Shader compilation failed
    exit /b 1
)

endlocal
