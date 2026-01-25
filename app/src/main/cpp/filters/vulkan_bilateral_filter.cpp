#include "vulkan_bilateral_filter.h"
#include <android/log.h>
#include <vector>
#include <cstring>

// Include Vulkan headers
#ifdef __ANDROID__
#define VK_USE_PLATFORM_ANDROID_KHR
#endif
#include <vulkan/vulkan.h>

#define LOG_TAG "VulkanBilateralFilter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace filmtracker {

// GLSL Compute Shader 源码
// 实现双边滤波算法
static const char* BILATERAL_FILTER_SHADER = R"(
#version 450

// 工作组大小（8x8 线程）
layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;

// 输入输出缓冲区（存储图像数据）
layout(binding = 0) readonly buffer InputBuffer {
    float inputData[];
};

layout(binding = 1) writeonly buffer OutputBuffer {
    float outputData[];
};

// Push constants（滤波参数）
layout(push_constant) uniform PushConstants {
    uint width;           // 图像宽度
    uint height;          // 图像高度
    float spatialSigma;   // 空间域标准差
    float rangeSigma;     // 强度域标准差
} params;

// 计算高斯权重
float gaussianWeight(float distance, float sigma) {
    return exp(-(distance * distance) / (2.0 * sigma * sigma));
}

// 获取像素索引
uint getPixelIndex(uint x, uint y, uint channel) {
    return (y * params.width + x) * 3 + channel;
}

void main() {
    // 获取当前像素坐标
    uint x = gl_GlobalInvocationID.x;
    uint y = gl_GlobalInvocationID.y;
    
    // 边界检查
    if (x >= params.width || y >= params.height) {
        return;
    }
    
    // 计算滤波器半径（基于 spatialSigma）
    int radius = int(ceil(3.0 * params.spatialSigma));
    
    // 获取中心像素值
    float centerR = inputData[getPixelIndex(x, y, 0)];
    float centerG = inputData[getPixelIndex(x, y, 1)];
    float centerB = inputData[getPixelIndex(x, y, 2)];
    
    // 累加器
    float sumR = 0.0;
    float sumG = 0.0;
    float sumB = 0.0;
    float sumWeight = 0.0;
    
    // 遍历邻域
    for (int dy = -radius; dy <= radius; dy++) {
        for (int dx = -radius; dx <= radius; dx++) {
            // 计算邻域像素坐标
            int nx = int(x) + dx;
            int ny = int(y) + dy;
            
            // 边界检查
            if (nx < 0 || nx >= int(params.width) || ny < 0 || ny >= int(params.height)) {
                continue;
            }
            
            // 获取邻域像素值
            float neighborR = inputData[getPixelIndex(uint(nx), uint(ny), 0)];
            float neighborG = inputData[getPixelIndex(uint(nx), uint(ny), 1)];
            float neighborB = inputData[getPixelIndex(uint(nx), uint(ny), 2)];
            
            // 计算空间距离
            float spatialDist = sqrt(float(dx * dx + dy * dy));
            
            // 计算强度差异（使用 RGB 欧氏距离）
            float dr = neighborR - centerR;
            float dg = neighborG - centerG;
            float db = neighborB - centerB;
            float rangeDist = sqrt(dr * dr + dg * dg + db * db);
            
            // 计算权重（空间权重 × 强度权重）
            float spatialWeight = gaussianWeight(spatialDist, params.spatialSigma);
            float rangeWeight = gaussianWeight(rangeDist, params.rangeSigma);
            float weight = spatialWeight * rangeWeight;
            
            // 累加加权像素值
            sumR += neighborR * weight;
            sumG += neighborG * weight;
            sumB += neighborB * weight;
            sumWeight += weight;
        }
    }
    
    // 归一化并写入输出
    if (sumWeight > 0.0) {
        outputData[getPixelIndex(x, y, 0)] = sumR / sumWeight;
        outputData[getPixelIndex(x, y, 1)] = sumG / sumWeight;
        outputData[getPixelIndex(x, y, 2)] = sumB / sumWeight;
    } else {
        // 如果权重为 0（不应该发生），保持原值
        outputData[getPixelIndex(x, y, 0)] = centerR;
        outputData[getPixelIndex(x, y, 1)] = centerG;
        outputData[getPixelIndex(x, y, 2)] = centerB;
    }
}
)";

// 静态成员初始化
VulkanBilateralFilter::VulkanResources VulkanBilateralFilter::s_resources;
bool VulkanBilateralFilter::s_initialized = false;
bool VulkanBilateralFilter::s_initializationAttempted = false;

/**
 * 初始化 Vulkan
 */
bool VulkanBilateralFilter::initialize() {
    // 避免重复初始化
    if (s_initializationAttempted) {
        return s_initialized;
    }
    
    s_initializationAttempted = true;
    
    LOGI("initialize: Starting Vulkan initialization");
    
    // 创建 Vulkan 实例
    if (!createInstance()) {
        LOGE("initialize: Failed to create Vulkan instance");
        return false;
    }
    
    // 选择物理设备
    if (!selectPhysicalDevice()) {
        LOGE("initialize: Failed to select physical device");
        cleanup();
        return false;
    }
    
    // 创建逻辑设备
    if (!createDevice()) {
        LOGE("initialize: Failed to create device");
        cleanup();
        return false;
    }
    
    // 创建命令池
    if (!createCommandPool()) {
        LOGE("initialize: Failed to create command pool");
        cleanup();
        return false;
    }
    
    // 创建描述符池
    if (!createDescriptorPool()) {
        LOGE("initialize: Failed to create descriptor pool");
        cleanup();
        return false;
    }
    
    // 创建计算管线
    if (!createComputePipeline()) {
        LOGE("initialize: Failed to create compute pipeline");
        cleanup();
        return false;
    }
    
    s_initialized = true;
    LOGI("initialize: Vulkan initialization successful");
    return true;
}

/**
 * 清理 Vulkan 资源
 */
void VulkanBilateralFilter::cleanup() {
    LOGI("cleanup: Cleaning up Vulkan resources");
    
    if (s_resources.device != VK_NULL_HANDLE) {
        // 等待设备空闲
        vkDeviceWaitIdle(s_resources.device);
        
        // 销毁计算着色器模块
        if (s_resources.computeShader != VK_NULL_HANDLE) {
            vkDestroyShaderModule(s_resources.device, s_resources.computeShader, nullptr);
            s_resources.computeShader = VK_NULL_HANDLE;
        }
        
        // 销毁管线
        if (s_resources.pipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(s_resources.device, s_resources.pipeline, nullptr);
            s_resources.pipeline = VK_NULL_HANDLE;
        }
        
        // 销毁管线布局
        if (s_resources.pipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(s_resources.device, s_resources.pipelineLayout, nullptr);
            s_resources.pipelineLayout = VK_NULL_HANDLE;
        }
        
        // 销毁描述符集布局
        if (s_resources.descriptorSetLayout != VK_NULL_HANDLE) {
            vkDestroyDescriptorSetLayout(s_resources.device, s_resources.descriptorSetLayout, nullptr);
            s_resources.descriptorSetLayout = VK_NULL_HANDLE;
        }
        
        // 销毁描述符池
        if (s_resources.descriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(s_resources.device, s_resources.descriptorPool, nullptr);
            s_resources.descriptorPool = VK_NULL_HANDLE;
        }
        
        // 销毁命令池
        if (s_resources.commandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(s_resources.device, s_resources.commandPool, nullptr);
            s_resources.commandPool = VK_NULL_HANDLE;
        }
        
        // 销毁逻辑设备
        vkDestroyDevice(s_resources.device, nullptr);
        s_resources.device = VK_NULL_HANDLE;
    }
    
    // 销毁实例
    if (s_resources.instance != VK_NULL_HANDLE) {
        vkDestroyInstance(s_resources.instance, nullptr);
        s_resources.instance = VK_NULL_HANDLE;
    }
    
    s_resources.physicalDevice = VK_NULL_HANDLE;
    s_resources.computeQueue = VK_NULL_HANDLE;
    s_resources.queueFamilyIndex = 0;
    
    s_initialized = false;
    LOGI("cleanup: Vulkan cleanup complete");
}

/**
 * 检查 Vulkan 是否可用
 */
bool VulkanBilateralFilter::isAvailable() {
    return s_initialized;
}

/**
 * 创建 Vulkan 实例
 */
bool VulkanBilateralFilter::createInstance() {
    LOGI("createInstance: Creating Vulkan instance");
    
    // 应用信息
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "FilmTracker";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName = "FilmTracker Engine";
    appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_0;
    
    // 实例创建信息
    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    
    // 不需要任何扩展（仅用于计算）
    createInfo.enabledExtensionCount = 0;
    createInfo.ppEnabledExtensionNames = nullptr;
    
    // 不启用验证层（生产环境）
    createInfo.enabledLayerCount = 0;
    createInfo.ppEnabledLayerNames = nullptr;
    
    // 创建实例
    VkResult result = vkCreateInstance(&createInfo, nullptr, &s_resources.instance);
    if (result != VK_SUCCESS) {
        LOGE("createInstance: vkCreateInstance failed with error %d", result);
        return false;
    }
    
    LOGI("createInstance: Vulkan instance created successfully");
    return true;
}

/**
 * 选择物理设备
 */
bool VulkanBilateralFilter::selectPhysicalDevice() {
    LOGI("selectPhysicalDevice: Selecting physical device");
    
    // 枚举物理设备
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(s_resources.instance, &deviceCount, nullptr);
    
    if (deviceCount == 0) {
        LOGE("selectPhysicalDevice: No Vulkan-capable devices found");
        return false;
    }
    
    LOGI("selectPhysicalDevice: Found %u physical device(s)", deviceCount);
    
    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(s_resources.instance, &deviceCount, devices.data());
    
    // 选择第一个支持计算队列的设备
    for (const auto& device : devices) {
        // 获取设备属性
        VkPhysicalDeviceProperties deviceProperties;
        vkGetPhysicalDeviceProperties(device, &deviceProperties);
        
        LOGI("selectPhysicalDevice: Checking device: %s", deviceProperties.deviceName);
        
        // 获取队列族属性
        uint32_t queueFamilyCount = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, nullptr);
        
        std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
        vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, queueFamilies.data());
        
        // 查找支持计算的队列族
        for (uint32_t i = 0; i < queueFamilyCount; i++) {
            if (queueFamilies[i].queueFlags & VK_QUEUE_COMPUTE_BIT) {
                s_resources.physicalDevice = device;
                s_resources.queueFamilyIndex = i;
                
                LOGI("selectPhysicalDevice: Selected device: %s (queue family %u)", 
                     deviceProperties.deviceName, i);
                return true;
            }
        }
    }
    
    LOGE("selectPhysicalDevice: No device with compute queue found");
    return false;
}

/**
 * 创建逻辑设备
 */
bool VulkanBilateralFilter::createDevice() {
    LOGI("createDevice: Creating logical device");
    
    // 队列创建信息
    float queuePriority = 1.0f;
    VkDeviceQueueCreateInfo queueCreateInfo{};
    queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    queueCreateInfo.queueFamilyIndex = s_resources.queueFamilyIndex;
    queueCreateInfo.queueCount = 1;
    queueCreateInfo.pQueuePriorities = &queuePriority;
    
    // 设备特性（不需要特殊特性）
    VkPhysicalDeviceFeatures deviceFeatures{};
    
    // 设备创建信息
    VkDeviceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    createInfo.queueCreateInfoCount = 1;
    createInfo.pQueueCreateInfos = &queueCreateInfo;
    createInfo.pEnabledFeatures = &deviceFeatures;
    createInfo.enabledExtensionCount = 0;
    createInfo.ppEnabledExtensionNames = nullptr;
    createInfo.enabledLayerCount = 0;
    createInfo.ppEnabledLayerNames = nullptr;
    
    // 创建逻辑设备
    VkResult result = vkCreateDevice(s_resources.physicalDevice, &createInfo, nullptr, &s_resources.device);
    if (result != VK_SUCCESS) {
        LOGE("createDevice: vkCreateDevice failed with error %d", result);
        return false;
    }
    
    // 获取计算队列
    vkGetDeviceQueue(s_resources.device, s_resources.queueFamilyIndex, 0, &s_resources.computeQueue);
    
    LOGI("createDevice: Logical device created successfully");
    return true;
}

/**
 * 创建命令池
 */
bool VulkanBilateralFilter::createCommandPool() {
    LOGI("createCommandPool: Creating command pool");
    
    VkCommandPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.queueFamilyIndex = s_resources.queueFamilyIndex;
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    
    VkResult result = vkCreateCommandPool(s_resources.device, &poolInfo, nullptr, &s_resources.commandPool);
    if (result != VK_SUCCESS) {
        LOGE("createCommandPool: vkCreateCommandPool failed with error %d", result);
        return false;
    }
    
    LOGI("createCommandPool: Command pool created successfully");
    return true;
}

/**
 * 创建描述符池
 */
bool VulkanBilateralFilter::createDescriptorPool() {
    LOGI("createDescriptorPool: Creating descriptor pool");
    
    // 描述符池大小（输入缓冲区 + 输出缓冲区）
    VkDescriptorPoolSize poolSize{};
    poolSize.type = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    poolSize.descriptorCount = 2;  // 输入和输出缓冲区
    
    VkDescriptorPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    poolInfo.poolSizeCount = 1;
    poolInfo.pPoolSizes = &poolSize;
    poolInfo.maxSets = 1;
    
    VkResult result = vkCreateDescriptorPool(s_resources.device, &poolInfo, nullptr, &s_resources.descriptorPool);
    if (result != VK_SUCCESS) {
        LOGE("createDescriptorPool: vkCreateDescriptorPool failed with error %d", result);
        return false;
    }
    
    LOGI("createDescriptorPool: Descriptor pool created successfully");
    return true;
}

/**
 * 编译 GLSL 到 SPIR-V
 * 
 * 注意：这个函数目前返回预编译的 SPIR-V 字节码
 * 实际的 GLSL 源码位于 app/src/main/cpp/shaders/bilateral_filter.comp
 * 
 * 要编译 shader：
 * 1. 安装 Vulkan SDK
 * 2. 运行: glslangValidator -V bilateral_filter.comp -o bilateral_filter.spv
 * 3. 将 SPIR-V 字节码嵌入到这个函数中
 * 
 * 详细说明请参考 app/src/main/cpp/shaders/README.md
 */
bool VulkanBilateralFilter::compileShader(
    const char* shaderSource,
    std::vector<uint32_t>& spirvCode) {
    
    LOGI("compileShader: Loading pre-compiled SPIR-V");
    
    // TODO: 嵌入预编译的 SPIR-V 字节码
    // 这里应该包含使用 glslangValidator 编译的 SPIR-V
    // 
    // 示例：
    // static const uint32_t BILATERAL_FILTER_SPV[] = {
    //     0x07230203, 0x00010000, 0x00080001, ...
    // };
    // spirvCode.assign(BILATERAL_FILTER_SPV, 
    //                  BILATERAL_FILTER_SPV + sizeof(BILATERAL_FILTER_SPV)/sizeof(uint32_t));
    
    // 临时占位符：最小的有效 SPIR-V（空 compute shader）
    // 这只是为了让代码编译通过，实际使用时必须替换为真实的 shader
    static const uint32_t PLACEHOLDER_SPV[] = {
        // SPIR-V 魔数和版本
        0x07230203, 0x00010000, 0x0008000a, 0x00000006,
        0x00000000, 0x00020011, 0x00000001, 0x0006000b,
        0x00000001, 0x4c534c47, 0x6474732e, 0x3035342e,
        0x00000000, 0x0003000e, 0x00000000, 0x00000001,
        0x0005000f, 0x00000005, 0x00000004, 0x6e69616d,
        0x00000000, 0x00060010, 0x00000004, 0x00000011,
        0x00000008, 0x00000008, 0x00000001, 0x00030003,
        0x00000002, 0x000001c2, 0x00040005, 0x00000004,
        0x6e69616d, 0x00000000, 0x00050048, 0x00000009,
        0x00000000, 0x00000023, 0x00000000, 0x00030047,
        0x00000009, 0x00000002, 0x00020013, 0x00000002,
        0x00030021, 0x00000003, 0x00000002, 0x00050036,
        0x00000002, 0x00000004, 0x00000000, 0x00000003,
        0x000200f8, 0x00000005, 0x000100fd, 0x00010038
    };
    
    spirvCode.assign(PLACEHOLDER_SPV, 
                     PLACEHOLDER_SPV + sizeof(PLACEHOLDER_SPV)/sizeof(uint32_t));
    
    LOGW("compileShader: Using placeholder SPIR-V - shader will not function correctly");
    LOGW("compileShader: Please compile the shader using instructions in shaders/README.md");
    
    return true;  // 返回 true 以允许继续初始化
}

/**
 * 创建 shader 模块
 */
bool VulkanBilateralFilter::createShaderModule(
    const std::vector<uint32_t>& spirvCode,
    VkShaderModule& shaderModule) {
    
    LOGI("createShaderModule: Creating shader module");
    
    VkShaderModuleCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    createInfo.codeSize = spirvCode.size() * sizeof(uint32_t);
    createInfo.pCode = spirvCode.data();
    
    VkResult result = vkCreateShaderModule(s_resources.device, &createInfo, nullptr, &shaderModule);
    if (result != VK_SUCCESS) {
        LOGE("createShaderModule: vkCreateShaderModule failed with error %d", result);
        return false;
    }
    
    LOGI("createShaderModule: Shader module created successfully");
    return true;
}

/**
 * 创建描述符集布局
 */
bool VulkanBilateralFilter::createDescriptorSetLayout() {
    LOGI("createDescriptorSetLayout: Creating descriptor set layout");
    
    // 描述符绑定：输入缓冲区（binding 0）和输出缓冲区（binding 1）
    VkDescriptorSetLayoutBinding bindings[2] = {};
    
    // 输入缓冲区（只读）
    bindings[0].binding = 0;
    bindings[0].descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    bindings[0].descriptorCount = 1;
    bindings[0].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
    bindings[0].pImmutableSamplers = nullptr;
    
    // 输出缓冲区（只写）
    bindings[1].binding = 1;
    bindings[1].descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    bindings[1].descriptorCount = 1;
    bindings[1].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
    bindings[1].pImmutableSamplers = nullptr;
    
    VkDescriptorSetLayoutCreateInfo layoutInfo{};
    layoutInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    layoutInfo.bindingCount = 2;
    layoutInfo.pBindings = bindings;
    
    VkResult result = vkCreateDescriptorSetLayout(
        s_resources.device,
        &layoutInfo,
        nullptr,
        &s_resources.descriptorSetLayout
    );
    
    if (result != VK_SUCCESS) {
        LOGE("createDescriptorSetLayout: vkCreateDescriptorSetLayout failed with error %d", result);
        return false;
    }
    
    LOGI("createDescriptorSetLayout: Descriptor set layout created successfully");
    return true;
}

/**
 * 创建管线布局
 */
bool VulkanBilateralFilter::createPipelineLayout() {
    LOGI("createPipelineLayout: Creating pipeline layout");
    
    // Push constants 范围（用于传递滤波参数）
    VkPushConstantRange pushConstantRange{};
    pushConstantRange.stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
    pushConstantRange.offset = 0;
    pushConstantRange.size = sizeof(PushConstants);  // width, height, spatialSigma, rangeSigma
    
    VkPipelineLayoutCreateInfo layoutInfo{};
    layoutInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    layoutInfo.setLayoutCount = 1;
    layoutInfo.pSetLayouts = &s_resources.descriptorSetLayout;
    layoutInfo.pushConstantRangeCount = 1;
    layoutInfo.pPushConstantRanges = &pushConstantRange;
    
    VkResult result = vkCreatePipelineLayout(
        s_resources.device,
        &layoutInfo,
        nullptr,
        &s_resources.pipelineLayout
    );
    
    if (result != VK_SUCCESS) {
        LOGE("createPipelineLayout: vkCreatePipelineLayout failed with error %d", result);
        return false;
    }
    
    LOGI("createPipelineLayout: Pipeline layout created successfully");
    return true;
}

/**
 * 创建计算管线
 */
bool VulkanBilateralFilter::createComputePipeline() {
    LOGI("createComputePipeline: Creating compute pipeline");
    
    // 1. 编译 GLSL compute shader 到 SPIR-V
    std::vector<uint32_t> spirvCode;
    if (!compileShader(BILATERAL_FILTER_SHADER, spirvCode)) {
        LOGE("createComputePipeline: Failed to compile shader");
        return false;
    }
    
    // 2. 创建 shader 模块
    if (!createShaderModule(spirvCode, s_resources.computeShader)) {
        LOGE("createComputePipeline: Failed to create shader module");
        return false;
    }
    
    // 3. 创建描述符集布局
    if (!createDescriptorSetLayout()) {
        LOGE("createComputePipeline: Failed to create descriptor set layout");
        vkDestroyShaderModule(s_resources.device, s_resources.computeShader, nullptr);
        s_resources.computeShader = VK_NULL_HANDLE;
        return false;
    }
    
    // 4. 创建管线布局（包含 push constants）
    if (!createPipelineLayout()) {
        LOGE("createComputePipeline: Failed to create pipeline layout");
        vkDestroyShaderModule(s_resources.device, s_resources.computeShader, nullptr);
        s_resources.computeShader = VK_NULL_HANDLE;
        return false;
    }
    
    // 5. 创建计算管线
    VkPipelineShaderStageCreateInfo shaderStageInfo{};
    shaderStageInfo.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    shaderStageInfo.stage = VK_SHADER_STAGE_COMPUTE_BIT;
    shaderStageInfo.module = s_resources.computeShader;
    shaderStageInfo.pName = "main";  // shader 入口点
    
    VkComputePipelineCreateInfo pipelineInfo{};
    pipelineInfo.sType = VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO;
    pipelineInfo.stage = shaderStageInfo;
    pipelineInfo.layout = s_resources.pipelineLayout;
    
    VkResult result = vkCreateComputePipelines(
        s_resources.device,
        VK_NULL_HANDLE,  // 不使用管线缓存
        1,
        &pipelineInfo,
        nullptr,
        &s_resources.pipeline
    );
    
    if (result != VK_SUCCESS) {
        LOGE("createComputePipeline: vkCreateComputePipelines failed with error %d", result);
        return false;
    }
    
    LOGI("createComputePipeline: Compute pipeline created successfully");
    LOGI("createComputePipeline: Note - using placeholder shader, replace with compiled SPIR-V for actual use");
    return true;
}

/**
 * 查找合适的内存类型
 */
uint32_t VulkanBilateralFilter::findMemoryType(
    uint32_t typeFilter,
    VkMemoryPropertyFlags properties) {
    
    VkPhysicalDeviceMemoryProperties memProperties;
    vkGetPhysicalDeviceMemoryProperties(s_resources.physicalDevice, &memProperties);
    
    for (uint32_t i = 0; i < memProperties.memoryTypeCount; i++) {
        if ((typeFilter & (1 << i)) && 
            (memProperties.memoryTypes[i].propertyFlags & properties) == properties) {
            return i;
        }
    }
    
    LOGE("findMemoryType: Failed to find suitable memory type");
    return UINT32_MAX;
}

/**
 * 创建缓冲区
 */
bool VulkanBilateralFilter::createBuffer(
    VkDeviceSize size,
    VkBufferUsageFlags usage,
    VkMemoryPropertyFlags properties,
    VkBuffer& buffer,
    VkDeviceMemory& memory) {
    
    // 创建缓冲区
    VkBufferCreateInfo bufferInfo{};
    bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size = size;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    
    VkResult result = vkCreateBuffer(s_resources.device, &bufferInfo, nullptr, &buffer);
    if (result != VK_SUCCESS) {
        LOGE("createBuffer: vkCreateBuffer failed with error %d", result);
        return false;
    }
    
    // 获取内存需求
    VkMemoryRequirements memRequirements;
    vkGetBufferMemoryRequirements(s_resources.device, buffer, &memRequirements);
    
    // 分配内存
    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties);
    
    if (allocInfo.memoryTypeIndex == UINT32_MAX) {
        LOGE("createBuffer: Failed to find suitable memory type");
        vkDestroyBuffer(s_resources.device, buffer, nullptr);
        return false;
    }
    
    result = vkAllocateMemory(s_resources.device, &allocInfo, nullptr, &memory);
    if (result != VK_SUCCESS) {
        LOGE("createBuffer: vkAllocateMemory failed with error %d", result);
        vkDestroyBuffer(s_resources.device, buffer, nullptr);
        return false;
    }
    
    // 绑定缓冲区和内存
    vkBindBufferMemory(s_resources.device, buffer, memory, 0);
    
    return true;
}

/**
 * 执行计算着色器
 */
bool VulkanBilateralFilter::executeComputeShader(
    const LinearImage& input,
    LinearImage& output,
    float spatialSigma,
    float rangeSigma) {
    
    LOGI("executeComputeShader: Starting GPU execution (width=%u, height=%u, spatialSigma=%.2f, rangeSigma=%.2f)",
         input.width, input.height, spatialSigma, rangeSigma);
    
    // 1. 计算缓冲区大小
    const VkDeviceSize bufferSize = input.width * input.height * 3 * sizeof(float);
    LOGI("executeComputeShader: Buffer size = %llu bytes", static_cast<unsigned long long>(bufferSize));
    
    // 2. 创建输入缓冲区（CPU 可写，GPU 可读）
    VkBuffer inputBuffer = VK_NULL_HANDLE;
    VkDeviceMemory inputMemory = VK_NULL_HANDLE;
    
    if (!createBuffer(
        bufferSize,
        VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
        inputBuffer,
        inputMemory)) {
        LOGE("executeComputeShader: Failed to create input buffer");
        return false;
    }
    
    // 3. 创建输出缓冲区（GPU 可写，CPU 可读）
    VkBuffer outputBuffer = VK_NULL_HANDLE;
    VkDeviceMemory outputMemory = VK_NULL_HANDLE;
    
    if (!createBuffer(
        bufferSize,
        VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
        outputBuffer,
        outputMemory)) {
        LOGE("executeComputeShader: Failed to create output buffer");
        vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
        vkFreeMemory(s_resources.device, inputMemory, nullptr);
        return false;
    }
    
    // 4. 将输入数据传输到 GPU
    void* data;
    VkResult result = vkMapMemory(s_resources.device, inputMemory, 0, bufferSize, 0, &data);
    if (result != VK_SUCCESS) {
        LOGE("executeComputeShader: Failed to map input memory, error %d", result);
        vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
        vkFreeMemory(s_resources.device, inputMemory, nullptr);
        vkDestroyBuffer(s_resources.device, outputBuffer, nullptr);
        vkFreeMemory(s_resources.device, outputMemory, nullptr);
        return false;
    }
    
    // 交错存储 RGB 数据（R, G, B, R, G, B, ...）
    float* inputData = static_cast<float*>(data);
    const uint32_t pixelCount = input.width * input.height;
    for (uint32_t i = 0; i < pixelCount; ++i) {
        inputData[i * 3 + 0] = input.r[i];
        inputData[i * 3 + 1] = input.g[i];
        inputData[i * 3 + 2] = input.b[i];
    }
    
    vkUnmapMemory(s_resources.device, inputMemory);
    LOGI("executeComputeShader: Input data transferred to GPU");
    
    // 5. 创建描述符集
    VkDescriptorSetAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    allocInfo.descriptorPool = s_resources.descriptorPool;
    allocInfo.descriptorSetCount = 1;
    allocInfo.pSetLayouts = &s_resources.descriptorSetLayout;
    
    VkDescriptorSet descriptorSet;
    result = vkAllocateDescriptorSets(s_resources.device, &allocInfo, &descriptorSet);
    if (result != VK_SUCCESS) {
        LOGE("executeComputeShader: Failed to allocate descriptor set, error %d", result);
        vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
        vkFreeMemory(s_resources.device, inputMemory, nullptr);
        vkDestroyBuffer(s_resources.device, outputBuffer, nullptr);
        vkFreeMemory(s_resources.device, outputMemory, nullptr);
        return false;
    }
    
    // 6. 更新描述符集（绑定输入和输出缓冲区）
    VkDescriptorBufferInfo inputBufferInfo{};
    inputBufferInfo.buffer = inputBuffer;
    inputBufferInfo.offset = 0;
    inputBufferInfo.range = bufferSize;
    
    VkDescriptorBufferInfo outputBufferInfo{};
    outputBufferInfo.buffer = outputBuffer;
    outputBufferInfo.offset = 0;
    outputBufferInfo.range = bufferSize;
    
    VkWriteDescriptorSet descriptorWrites[2] = {};
    
    // 输入缓冲区（binding 0）
    descriptorWrites[0].sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
    descriptorWrites[0].dstSet = descriptorSet;
    descriptorWrites[0].dstBinding = 0;
    descriptorWrites[0].dstArrayElement = 0;
    descriptorWrites[0].descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    descriptorWrites[0].descriptorCount = 1;
    descriptorWrites[0].pBufferInfo = &inputBufferInfo;
    
    // 输出缓冲区（binding 1）
    descriptorWrites[1].sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
    descriptorWrites[1].dstSet = descriptorSet;
    descriptorWrites[1].dstBinding = 1;
    descriptorWrites[1].dstArrayElement = 0;
    descriptorWrites[1].descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    descriptorWrites[1].descriptorCount = 1;
    descriptorWrites[1].pBufferInfo = &outputBufferInfo;
    
    vkUpdateDescriptorSets(s_resources.device, 2, descriptorWrites, 0, nullptr);
    LOGI("executeComputeShader: Descriptor set updated");
    
    // 7. 创建命令缓冲区
    VkCommandBufferAllocateInfo cmdAllocInfo{};
    cmdAllocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    cmdAllocInfo.commandPool = s_resources.commandPool;
    cmdAllocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    cmdAllocInfo.commandBufferCount = 1;
    
    VkCommandBuffer commandBuffer;
    result = vkAllocateCommandBuffers(s_resources.device, &cmdAllocInfo, &commandBuffer);
    if (result != VK_SUCCESS) {
        LOGE("executeComputeShader: Failed to allocate command buffer, error %d", result);
        vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
        vkFreeMemory(s_resources.device, inputMemory, nullptr);
        vkDestroyBuffer(s_resources.device, outputBuffer, nullptr);
        vkFreeMemory(s_resources.device, outputMemory, nullptr);
        return false;
    }
    
    // 8. 录制计算命令
    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    
    vkBeginCommandBuffer(commandBuffer, &beginInfo);
    
    // 绑定计算管线
    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, s_resources.pipeline);
    
    // 绑定描述符集
    vkCmdBindDescriptorSets(
        commandBuffer,
        VK_PIPELINE_BIND_POINT_COMPUTE,
        s_resources.pipelineLayout,
        0,
        1,
        &descriptorSet,
        0,
        nullptr
    );
    
    // 设置 push constants（传递参数）
    PushConstants pushConstants{};
    pushConstants.width = input.width;
    pushConstants.height = input.height;
    pushConstants.spatialSigma = spatialSigma;
    pushConstants.rangeSigma = rangeSigma;
    
    vkCmdPushConstants(
        commandBuffer,
        s_resources.pipelineLayout,
        VK_SHADER_STAGE_COMPUTE_BIT,
        0,
        sizeof(PushConstants),
        &pushConstants
    );
    
    // 分派计算任务（工作组大小 8x8）
    uint32_t groupCountX = (input.width + 7) / 8;
    uint32_t groupCountY = (input.height + 7) / 8;
    vkCmdDispatch(commandBuffer, groupCountX, groupCountY, 1);
    
    vkEndCommandBuffer(commandBuffer);
    LOGI("executeComputeShader: Command buffer recorded (dispatch %ux%u groups)", groupCountX, groupCountY);
    
    // 9. 创建 fence 用于同步
    VkFenceCreateInfo fenceInfo{};
    fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    
    VkFence fence;
    result = vkCreateFence(s_resources.device, &fenceInfo, nullptr, &fence);
    if (result != VK_SUCCESS) {
        LOGE("executeComputeShader: Failed to create fence, error %d", result);
        vkFreeCommandBuffers(s_resources.device, s_resources.commandPool, 1, &commandBuffer);
        vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
        vkFreeMemory(s_resources.device, inputMemory, nullptr);
        vkDestroyBuffer(s_resources.device, outputBuffer, nullptr);
        vkFreeMemory(s_resources.device, outputMemory, nullptr);
        return false;
    }
    
    // 10. 提交命令到 GPU
    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;
    
    result = vkQueueSubmit(s_resources.computeQueue, 1, &submitInfo, fence);
    if (result != VK_SUCCESS) {
        LOGE("executeComputeShader: Failed to submit command buffer, error %d", result);
        vkDestroyFence(s_resources.device, fence, nullptr);
        vkFreeCommandBuffers(s_resources.device, s_resources.commandPool, 1, &commandBuffer);
        vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
        vkFreeMemory(s_resources.device, inputMemory, nullptr);
        vkDestroyBuffer(s_resources.device, outputBuffer, nullptr);
        vkFreeMemory(s_resources.device, outputMemory, nullptr);
        return false;
    }
    
    LOGI("executeComputeShader: Command submitted to GPU, waiting for completion");
    
    // 11. 等待 GPU 执行完成
    result = vkWaitForFences(s_resources.device, 1, &fence, VK_TRUE, UINT64_MAX);
    if (result != VK_SUCCESS) {
        LOGE("executeComputeShader: Failed to wait for fence, error %d", result);
        vkDestroyFence(s_resources.device, fence, nullptr);
        vkFreeCommandBuffers(s_resources.device, s_resources.commandPool, 1, &commandBuffer);
        vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
        vkFreeMemory(s_resources.device, inputMemory, nullptr);
        vkDestroyBuffer(s_resources.device, outputBuffer, nullptr);
        vkFreeMemory(s_resources.device, outputMemory, nullptr);
        return false;
    }
    
    LOGI("executeComputeShader: GPU execution completed");
    
    // 12. 将输出数据传输回 CPU
    result = vkMapMemory(s_resources.device, outputMemory, 0, bufferSize, 0, &data);
    if (result != VK_SUCCESS) {
        LOGE("executeComputeShader: Failed to map output memory, error %d", result);
        vkDestroyFence(s_resources.device, fence, nullptr);
        vkFreeCommandBuffers(s_resources.device, s_resources.commandPool, 1, &commandBuffer);
        vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
        vkFreeMemory(s_resources.device, inputMemory, nullptr);
        vkDestroyBuffer(s_resources.device, outputBuffer, nullptr);
        vkFreeMemory(s_resources.device, outputMemory, nullptr);
        return false;
    }
    
    // 从交错格式转换回分离的 RGB 通道
    float* outputData = static_cast<float*>(data);
    for (uint32_t i = 0; i < pixelCount; ++i) {
        output.r[i] = outputData[i * 3 + 0];
        output.g[i] = outputData[i * 3 + 1];
        output.b[i] = outputData[i * 3 + 2];
    }
    
    vkUnmapMemory(s_resources.device, outputMemory);
    LOGI("executeComputeShader: Output data transferred from GPU");
    
    // 13. 清理资源
    vkDestroyFence(s_resources.device, fence, nullptr);
    vkFreeCommandBuffers(s_resources.device, s_resources.commandPool, 1, &commandBuffer);
    vkDestroyBuffer(s_resources.device, inputBuffer, nullptr);
    vkFreeMemory(s_resources.device, inputMemory, nullptr);
    vkDestroyBuffer(s_resources.device, outputBuffer, nullptr);
    vkFreeMemory(s_resources.device, outputMemory, nullptr);
    
    LOGI("executeComputeShader: GPU execution completed successfully");
    return true;
}

/**
 * 应用 GPU 加速双边滤波
 */
bool VulkanBilateralFilter::apply(
    const LinearImage& input,
    LinearImage& output,
    float spatialSigma,
    float rangeSigma) {
    
    // 检查 Vulkan 是否已初始化
    if (!s_initialized) {
        LOGW("apply: Vulkan not initialized, attempting initialization");
        if (!initialize()) {
            LOGE("apply: Vulkan initialization failed");
            return false;
        }
    }
    
    // 确保输出图像大小正确
    if (output.width != input.width || output.height != input.height) {
        output = LinearImage(input.width, input.height);
    }
    
    // 执行计算着色器
    bool success = executeComputeShader(input, output, spatialSigma, rangeSigma);
    
    if (!success) {
        LOGE("apply: GPU execution failed");
        return false;
    }
    
    LOGI("apply: GPU bilateral filter completed successfully");
    return true;
}

} // namespace filmtracker
