package com.filmtracker.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.filmtracker.app.data.mapper.AdjustmentParamsMapper
import com.filmtracker.app.data.repository.ImageRepositoryImpl
import com.filmtracker.app.data.repository.ProcessingRepositoryImpl
import com.filmtracker.app.data.source.local.FileImageSource
import com.filmtracker.app.data.source.native.NativeImageProcessor
import com.filmtracker.app.data.source.native.NativeRawProcessor
import com.filmtracker.app.domain.usecase.ApplyAdjustmentsUseCase
import com.filmtracker.app.domain.usecase.ExportImageUseCase
import com.filmtracker.app.domain.usecase.LoadImageUseCase

/**
 * ViewModel 工厂
 * 
 * 临时方案：在没有依赖注入的情况下创建 ViewModel
 * 未来将被 Hilt 或 Koin 替代
 * 
 * 使用示例：
 * ```kotlin
 * val viewModel: ProcessingViewModel = viewModel(
 *     factory = ViewModelFactory.getInstance(context)
 * )
 * ```
 */
class ViewModelFactory private constructor(
    private val context: Context
) : ViewModelProvider.Factory {
    
    // 懒加载依赖
    private val nativeImageProcessor by lazy { NativeImageProcessor() }
    private val nativeRawProcessor by lazy { NativeRawProcessor() }
    private val fileImageSource by lazy { FileImageSource(context) }
    private val adjustmentParamsMapper by lazy { AdjustmentParamsMapper() }
    
    // Repositories
    private val processingRepository by lazy {
        ProcessingRepositoryImpl(nativeImageProcessor, adjustmentParamsMapper)
    }
    
    private val imageRepository by lazy {
        ImageRepositoryImpl(fileImageSource, nativeRawProcessor)
    }
    
    // Use Cases
    private val applyAdjustmentsUseCase by lazy {
        ApplyAdjustmentsUseCase(processingRepository)
    }
    
    private val exportImageUseCase by lazy {
        ExportImageUseCase(processingRepository)
    }
    
    private val loadImageUseCase by lazy {
        LoadImageUseCase(imageRepository)
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ProcessingViewModel::class.java) -> {
                ProcessingViewModel(applyAdjustmentsUseCase, exportImageUseCase) as T
            }
            modelClass.isAssignableFrom(ImageSelectionViewModel::class.java) -> {
                ImageSelectionViewModel() as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ViewModelFactory? = null
        
        /**
         * 获取 ViewModelFactory 单例
         * 
         * @param context Application context
         * @return ViewModelFactory 实例
         */
        fun getInstance(context: Context): ViewModelFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ViewModelFactory(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
