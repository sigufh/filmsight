#include "jni_common.h"
#include "../core/parallel_processor.h"
#include "../color/basic_adjustment_params.h"

extern "C" {

/**
 * 创建并行处理器
 */
JNIEXPORT jlong JNICALL
Java_com_filmtracker_app_native_ParallelProcessorNative_nativeCreate(
    JNIEnv *env, jobject thiz) {
    
    ParallelProcessor* processor = new ParallelProcessor();
    LOGI("ParallelProcessor created with %d threads", processor->getNumThreads());
    return reinterpret_cast<jlong>(processor);
}

/**
 * 并行处理图像
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ParallelProcessorNative_nativeProcess(
    JNIEnv *env, jobject thiz, jlong processorPtr, jlong inputImagePtr, 
    jlong outputImagePtr, jlong paramsPtr) {
    
    ParallelProcessor* processor = reinterpret_cast<ParallelProcessor*>(processorPtr);
    filmtracker::LinearImage* inputImage = reinterpret_cast<filmtracker::LinearImage*>(inputImagePtr);
    filmtracker::LinearImage* outputImage = reinterpret_cast<filmtracker::LinearImage*>(outputImagePtr);
    filmtracker::BasicAdjustmentParams* params = reinterpret_cast<filmtracker::BasicAdjustmentParams*>(paramsPtr);
    
    if (!processor || !inputImage || !outputImage || !params) {
        LOGE("Invalid pointers in nativeProcess");
        return;
    }
    
    processor->process(*inputImage, *outputImage, *params);
}

/**
 * 获取线程数
 */
JNIEXPORT jint JNICALL
Java_com_filmtracker_app_native_ParallelProcessorNative_nativeGetNumThreads(
    JNIEnv *env, jobject thiz, jlong processorPtr) {
    
    ParallelProcessor* processor = reinterpret_cast<ParallelProcessor*>(processorPtr);
    if (!processor) {
        LOGE("Invalid processor pointer in nativeGetNumThreads");
        return 0;
    }
    
    return processor->getNumThreads();
}

/**
 * 释放并行处理器
 */
JNIEXPORT void JNICALL
Java_com_filmtracker_app_native_ParallelProcessorNative_nativeDestroy(
    JNIEnv *env, jobject thiz, jlong processorPtr) {
    
    ParallelProcessor* processor = reinterpret_cast<ParallelProcessor*>(processorPtr);
    if (processor) {
        LOGI("Destroying ParallelProcessor");
        delete processor;
    }
}

} // extern "C"
