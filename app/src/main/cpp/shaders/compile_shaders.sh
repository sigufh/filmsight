#!/bin/bash
# Compile GLSL shaders to SPIR-V and generate C++ headers

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Compiling Vulkan Compute Shaders ==="
echo

# Check if glslangValidator is available
if ! command -v glslangValidator &> /dev/null; then
    echo "Error: glslangValidator not found"
    echo "Please install the Vulkan SDK:"
    echo "  - Ubuntu/Debian: sudo apt-get install vulkan-sdk"
    echo "  - macOS: brew install vulkan-sdk"
    echo "  - Windows: Download from https://vulkan.lunarg.com/"
    exit 1
fi

# Check if Python 3 is available
if ! command -v python3 &> /dev/null; then
    echo "Error: python3 not found"
    echo "Please install Python 3"
    exit 1
fi

# Compile bilateral filter shader
echo "Compiling bilateral_filter.comp..."
glslangValidator -V bilateral_filter.comp -o bilateral_filter.spv

if [ $? -eq 0 ]; then
    echo "✓ Shader compiled successfully"
    
    # Convert to C++ header
    echo "Converting SPIR-V to C++ header..."
    python3 spv_to_cpp.py bilateral_filter.spv bilateral_filter_spv.h
    
    if [ $? -eq 0 ]; then
        echo "✓ C++ header generated successfully"
        echo
        echo "=== Compilation Complete ==="
        echo
        echo "Next steps:"
        echo "1. Include bilateral_filter_spv.h in vulkan_bilateral_filter.cpp"
        echo "2. Replace the placeholder SPIR-V in compileShader() with BILATERAL_FILTER_SPV"
        echo
        echo "Example:"
        echo "  spirvCode.assign(BILATERAL_FILTER_SPV,"
        echo "                   BILATERAL_FILTER_SPV + BILATERAL_FILTER_SPV_SIZE);"
    else
        echo "✗ Failed to generate C++ header"
        exit 1
    fi
else
    echo "✗ Shader compilation failed"
    exit 1
fi
