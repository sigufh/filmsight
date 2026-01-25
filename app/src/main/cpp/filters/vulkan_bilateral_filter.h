#ifndef FILMTRACKER_VULKAN_BILATERAL_FILTER_H
#define FILMTRACKER_VULKAN_BILATERAL_FILTER_H

#include "raw_types.h"
#include <cstdint>
#include <vector>

// Include Vulkan headers
#ifdef __ANDROID__
#define VK_USE_PLATFORM_ANDROID_KHR
#endif
#include <vulkan/vulkan.h>

namespace filmtracker {

/**
 * Vulkan GPU 加速双边滤波器
 * 
 * 使用 Vulkan compute shader 实现并行双边滤波
 * 提供 3-5 倍性能提升（相比 CPU 多线程实现）
 */
class VulkanBilateralFilter {
public:
    /**
     * 初始化 Vulkan
     * 
     * @return true 如果初始化成功，false 如果失败
     */
    static bool initialize();
    
    /**
     * 清理 Vulkan 资源
     */
    static void cleanup();
    
    /**
     * 检查 Vulkan 是否可用
     * 
     * @return true 如果 Vulkan 已初始化且可用
     */
    static bool isAvailable();
    
    /**
     * 应用 GPU 加速双边滤波
     * 
     * @param input 输入图像
     * @param output 输出图像（必须预先分配）
     * @param spatialSigma 空间域标准差
     * @param rangeSigma 强度域标准差
     * @return true 如果成功，false 如果失败（应回退到 CPU）
     */
    static bool apply(
        const LinearImage& input,
        LinearImage& output,
        float spatialSigma,
        float rangeSigma
    );
    
private:
    /**
     * Push constants 结构（用于传递参数到 shader）
     */
    struct PushConstants {
        uint32_t width;
        uint32_t height;
        float spatialSigma;
        float rangeSigma;
    };
    
    /**
     * Vulkan 资源结构
     */
    struct VulkanResources {
        VkInstance instance = VK_NULL_HANDLE;
        VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
        VkDevice device = VK_NULL_HANDLE;
        VkQueue computeQueue = VK_NULL_HANDLE;
        uint32_t queueFamilyIndex = 0;
        VkCommandPool commandPool = VK_NULL_HANDLE;
        VkDescriptorPool descriptorPool = VK_NULL_HANDLE;
        VkPipeline pipeline = VK_NULL_HANDLE;
        VkPipelineLayout pipelineLayout = VK_NULL_HANDLE;
        VkDescriptorSetLayout descriptorSetLayout = VK_NULL_HANDLE;
        VkShaderModule computeShader = VK_NULL_HANDLE;
    };
    
    static VulkanResources s_resources;
    static bool s_initialized;
    static bool s_initializationAttempted;
    
    /**
     * 创建 Vulkan 实例
     */
    static bool createInstance();
    
    /**
     * 选择物理设备（GPU）
     */
    static bool selectPhysicalDevice();
    
    /**
     * 创建逻辑设备
     */
    static bool createDevice();
    
    /**
     * 创建命令池
     */
    static bool createCommandPool();
    
    /**
     * 创建描述符池
     */
    static bool createDescriptorPool();
    
    /**
     * 创建计算管线
     */
    static bool createComputePipeline();
    
    /**
     * 编译 GLSL shader 到 SPIR-V
     * 
     * @param shaderSource GLSL 源码
     * @param spirvCode 输出：SPIR-V 字节码
     * @return true 如果成功
     */
    static bool compileShader(
        const char* shaderSource,
        std::vector<uint32_t>& spirvCode
    );
    
    /**
     * 创建 shader 模块
     * 
     * @param spirvCode SPIR-V 字节码
     * @param shaderModule 输出：创建的 shader 模块
     * @return true 如果成功
     */
    static bool createShaderModule(
        const std::vector<uint32_t>& spirvCode,
        VkShaderModule& shaderModule
    );
    
    /**
     * 创建描述符集布局
     * 
     * @return true 如果成功
     */
    static bool createDescriptorSetLayout();
    
    /**
     * 创建管线布局
     * 
     * @return true 如果成功
     */
    static bool createPipelineLayout();
    
    /**
     * 创建缓冲区
     * 
     * @param size 缓冲区大小（字节）
     * @param usage 缓冲区用途标志
     * @param properties 内存属性标志
     * @param buffer 输出：创建的缓冲区
     * @param memory 输出：分配的内存
     * @return true 如果成功
     */
    static bool createBuffer(
        VkDeviceSize size,
        VkBufferUsageFlags usage,
        VkMemoryPropertyFlags properties,
        VkBuffer& buffer,
        VkDeviceMemory& memory
    );
    
    /**
     * 查找合适的内存类型
     * 
     * @param typeFilter 内存类型过滤器
     * @param properties 所需的内存属性
     * @return 内存类型索引，如果未找到则返回 UINT32_MAX
     */
    static uint32_t findMemoryType(
        uint32_t typeFilter,
        VkMemoryPropertyFlags properties
    );
    
    /**
     * 执行计算着色器
     * 
     * @param input 输入图像
     * @param output 输出图像
     * @param spatialSigma 空间域标准差
     * @param rangeSigma 强度域标准差
     * @return true 如果成功
     */
    static bool executeComputeShader(
        const LinearImage& input,
        LinearImage& output,
        float spatialSigma,
        float rangeSigma
    );
};

} // namespace filmtracker

#endif // FILMTRACKER_VULKAN_BILATERAL_FILTER_H
