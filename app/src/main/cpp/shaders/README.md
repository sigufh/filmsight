# Vulkan Compute Shaders

This directory contains GLSL compute shaders for GPU-accelerated image processing.

## Bilateral Filter Shader

The `bilateral_filter.comp` shader implements the bilateral filter algorithm on the GPU using Vulkan compute shaders.

### Compiling the Shader

To compile the GLSL shader to SPIR-V bytecode, you need the Vulkan SDK installed with `glslangValidator`:

```bash
# Install Vulkan SDK (if not already installed)
# On Ubuntu/Debian:
sudo apt-get install vulkan-sdk

# On macOS:
brew install vulkan-sdk

# On Windows:
# Download from https://vulkan.lunarg.com/

# Compile the shader
glslangValidator -V bilateral_filter.comp -o bilateral_filter.spv

# Or use glslc (alternative compiler):
glslc bilateral_filter.comp -o bilateral_filter.spv
```

### Embedding SPIR-V in C++

After compilation, you can convert the SPIR-V binary to a C++ array:

```bash
# Convert SPIR-V to C++ header
xxd -i bilateral_filter.spv > bilateral_filter_spv.h
```

Or use this Python script:

```python
#!/usr/bin/env python3
import sys

def spv_to_cpp(spv_file, output_file):
    with open(spv_file, 'rb') as f:
        data = f.read()
    
    # Convert to uint32_t array
    words = []
    for i in range(0, len(data), 4):
        word = int.from_bytes(data[i:i+4], byteorder='little')
        words.append(word)
    
    with open(output_file, 'w') as f:
        f.write('// Auto-generated SPIR-V bytecode\n')
        f.write('#include <cstdint>\n\n')
        f.write('static const uint32_t BILATERAL_FILTER_SPV[] = {\n')
        for i, word in enumerate(words):
            if i % 8 == 0:
                f.write('    ')
            f.write(f'0x{word:08x}')
            if i < len(words) - 1:
                f.write(', ')
            if i % 8 == 7:
                f.write('\n')
        f.write('\n};\n\n')
        f.write(f'static const size_t BILATERAL_FILTER_SPV_SIZE = {len(words)};\n')

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(f'Usage: {sys.argv[0]} <input.spv> <output.h>')
        sys.exit(1)
    spv_to_cpp(sys.argv[1], sys.argv[2])
```

### Integration

The compiled SPIR-V bytecode should be embedded in `vulkan_bilateral_filter.cpp` or included as a separate header file.

## Shader Parameters

### Push Constants

The shader receives parameters via push constants:

- `width` (uint32_t): Image width in pixels
- `height` (uint32_t): Image height in pixels
- `spatialSigma` (float): Spatial domain standard deviation
- `rangeSigma` (float): Range domain standard deviation

### Buffers

- **Binding 0**: Input buffer (readonly) - RGB float data, layout: [R0, G0, B0, R1, G1, B1, ...]
- **Binding 1**: Output buffer (writeonly) - RGB float data, same layout as input

### Work Group Size

The shader uses an 8x8 work group size for optimal GPU utilization on mobile devices.

## Performance Considerations

- The shader uses local memory for efficient access patterns
- Boundary checks are performed to handle image edges
- The filter radius is calculated as `ceil(3.0 * spatialSigma)` for 99.7% coverage
- Weight normalization ensures energy conservation

## Testing

To test the shader compilation:

```bash
# Validate shader syntax
glslangValidator bilateral_filter.comp

# Compile and check for errors
glslangValidator -V bilateral_filter.comp -o bilateral_filter.spv

# Disassemble SPIR-V for inspection
spirv-dis bilateral_filter.spv
```
