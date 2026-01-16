package com.filmtracker.app.native

/**
 * 并行图像处理器 Native 接口
 * 使用多线程 + ARM NEON SIMD 优化
 */
class ParallelProcessorNative {
    private var nativeHandle: Long = 0
    
    init {
        nativeHandle = nativeCreate()
    }
    
    /**
     * 并行处理图像
     * @param inputImageHandle 输入图像句柄
     * @param outputImageHandle 输出图像句柄
     * @param paramsHandle 参数句柄
     */
    fun process(
        inputImageHandle: Long,
        outputImageHandle: Long,
        paramsHandle: Long
    ) {
        if (nativeHandle == 0L) {
            throw IllegalStateException("ParallelProcessor not initialized")
        }
        nativeProcess(nativeHandle, inputImageHandle, outputImageHandle, paramsHandle)
    }
    
    /**
     * 获取线程数
     */
    fun getNumThreads(): Int {
        if (nativeHandle == 0L) {
            return 0
        }
        return nativeGetNumThreads(nativeHandle)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }
    
    protected fun finalize() {
        release()
    }
    
    // Native 方法
    private external fun nativeCreate(): Long
    private external fun nativeProcess(
        handle: Long,
        inputImageHandle: Long,
        outputImageHandle: Long,
        paramsHandle: Long
    )
    private external fun nativeGetNumThreads(handle: Long): Int
    private external fun nativeDestroy(handle: Long)
    
    companion object {
        init {
            System.loadLibrary("filmtracker")
        }
    }
}
