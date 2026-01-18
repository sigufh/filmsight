package com.filmtracker.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 编辑历史
 * 使用不可变的栈结构管理撤销/重做历史
 */
data class EditHistory(
    private val undoStack: List<ParameterSnapshot> = emptyList(),
    private val redoStack: List<ParameterSnapshot> = emptyList(),
    private val maxHistorySize: Int = 100
) {
    /**
     * 记录参数变更
     * 将当前参数添加到撤销栈,清空重做栈
     */
    fun recordChange(params: AdjustmentParams, description: String = "调整"): EditHistory {
        val snapshot = ParameterSnapshot(
            params = params,
            timestamp = System.currentTimeMillis(),
            description = description
        )
        
        // 添加到撤销栈,限制大小
        val newUndoStack = (undoStack + snapshot).takeLast(maxHistorySize)
        
        // 清空重做栈(新的变更会使重做历史失效)
        return copy(
            undoStack = newUndoStack,
            redoStack = emptyList()
        )
    }
    
    /**
     * 撤销操作
     * 返回上一个参数状态和新的历史对象
     */
    fun undo(): Pair<AdjustmentParams?, EditHistory> {
        if (undoStack.isEmpty()) {
            return Pair(null, this)
        }
        
        // 从撤销栈弹出最后一个
        val lastSnapshot = undoStack.last()
        val newUndoStack = undoStack.dropLast(1)
        
        // 当前状态不需要添加到重做栈,因为它还在外部保存
        // 只有当我们有多个撤销时才需要管理重做栈
        
        return Pair(
            lastSnapshot.toAdjustmentParams(),
            copy(undoStack = newUndoStack)
        )
    }
    
    /**
     * 撤销操作(带当前状态)
     * 将当前状态保存到重做栈
     */
    fun undoWithCurrent(currentParams: AdjustmentParams): Pair<AdjustmentParams?, EditHistory> {
        if (undoStack.isEmpty()) {
            return Pair(null, this)
        }
        
        // 从撤销栈弹出最后一个
        val lastSnapshot = undoStack.last()
        val newUndoStack = undoStack.dropLast(1)
        
        // 将当前状态添加到重做栈
        val currentSnapshot = ParameterSnapshot(
            params = currentParams,
            timestamp = System.currentTimeMillis(),
            description = "当前状态"
        )
        val newRedoStack = redoStack + currentSnapshot
        
        return Pair(
            lastSnapshot.toAdjustmentParams(),
            copy(
                undoStack = newUndoStack,
                redoStack = newRedoStack
            )
        )
    }
    
    /**
     * 重做操作
     * 返回下一个参数状态和新的历史对象
     */
    fun redo(): Pair<AdjustmentParams?, EditHistory> {
        if (redoStack.isEmpty()) {
            return Pair(null, this)
        }
        
        // 从重做栈弹出最后一个
        val nextSnapshot = redoStack.last()
        val newRedoStack = redoStack.dropLast(1)
        
        return Pair(
            nextSnapshot.toAdjustmentParams(),
            copy(redoStack = newRedoStack)
        )
    }
    
    /**
     * 重做操作(带当前状态)
     * 将当前状态保存到撤销栈
     */
    fun redoWithCurrent(currentParams: AdjustmentParams): Pair<AdjustmentParams?, EditHistory> {
        if (redoStack.isEmpty()) {
            return Pair(null, this)
        }
        
        // 从重做栈弹出最后一个
        val nextSnapshot = redoStack.last()
        val newRedoStack = redoStack.dropLast(1)
        
        // 将当前状态添加到撤销栈
        val currentSnapshot = ParameterSnapshot(
            params = currentParams,
            timestamp = System.currentTimeMillis(),
            description = "当前状态"
        )
        val newUndoStack = (undoStack + currentSnapshot).takeLast(maxHistorySize)
        
        return Pair(
            nextSnapshot.toAdjustmentParams(),
            copy(
                undoStack = newUndoStack,
                redoStack = newRedoStack
            )
        )
    }
    
    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * 清空历史
     */
    fun clear(): EditHistory = EditHistory(maxHistorySize = maxHistorySize)
    
    /**
     * 获取撤销栈大小
     */
    fun undoCount(): Int = undoStack.size
    
    /**
     * 获取重做栈大小
     */
    fun redoCount(): Int = redoStack.size
    
    /**
     * 转换为可序列化格式
     */
    fun toSerializable(): SerializableEditHistory {
        return SerializableEditHistory(
            undoStack = undoStack,
            redoStack = redoStack,
            maxHistorySize = maxHistorySize
        )
    }
}

/**
 * 参数快照
 * 用于历史记录中保存参数状态
 */
@Serializable
data class ParameterSnapshot(
    val params: SerializableAdjustmentParams,
    val timestamp: Long,
    val description: String
) {
    constructor(
        params: AdjustmentParams,
        timestamp: Long,
        description: String
    ) : this(
        params = SerializableAdjustmentParams.fromAdjustmentParams(params),
        timestamp = timestamp,
        description = description
    )
    
    /**
     * 转换为领域模型
     */
    fun toAdjustmentParams(): AdjustmentParams = params.toAdjustmentParams()
}

/**
 * 可序列化的编辑历史
 * 用于会话持久化
 */
@Serializable
data class SerializableEditHistory(
    val undoStack: List<ParameterSnapshot> = emptyList(),
    val redoStack: List<ParameterSnapshot> = emptyList(),
    val maxHistorySize: Int = 100
) {
    fun toEditHistory(): EditHistory {
        return EditHistory(
            undoStack = undoStack,
            redoStack = redoStack,
            maxHistorySize = maxHistorySize
        )
    }
    
    companion object {
        fun fromEditHistory(history: EditHistory): SerializableEditHistory {
            return history.toSerializable()
        }
    }
}
