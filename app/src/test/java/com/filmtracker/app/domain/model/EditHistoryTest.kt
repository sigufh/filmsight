package com.filmtracker.app.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * EditHistory 单元测试
 */
class EditHistoryTest {
    
    @Test
    fun `test empty history cannot undo or redo`() {
        val history = EditHistory()
        
        assertFalse(history.canUndo())
        assertFalse(history.canRedo())
        assertEquals(0, history.undoCount())
        assertEquals(0, history.redoCount())
    }
    
    @Test
    fun `test record change adds to undo stack`() {
        val history = EditHistory()
        val params1 = AdjustmentParams(exposure = 0.5f)
        
        val newHistory = history.recordChange(params1, "调整曝光")
        
        assertTrue(newHistory.canUndo())
        assertFalse(newHistory.canRedo())
        assertEquals(1, newHistory.undoCount())
        assertEquals(0, newHistory.redoCount())
    }
    
    @Test
    fun `test multiple changes build undo stack`() {
        var history = EditHistory()
        val params1 = AdjustmentParams(exposure = 0.5f)
        val params2 = AdjustmentParams(exposure = 0.5f, contrast = 1.2f)
        val params3 = AdjustmentParams(exposure = 0.5f, contrast = 1.2f, saturation = 1.1f)
        
        history = history.recordChange(params1, "调整1")
        history = history.recordChange(params2, "调整2")
        history = history.recordChange(params3, "调整3")
        
        assertEquals(3, history.undoCount())
        assertEquals(0, history.redoCount())
    }
    
    @Test
    fun `test undo restores previous state`() {
        var history = EditHistory()
        val params1 = AdjustmentParams(exposure = 0.5f)
        val params2 = AdjustmentParams(exposure = 0.8f)
        val currentParams = AdjustmentParams(exposure = 1.0f)
        
        history = history.recordChange(params1, "调整1")
        history = history.recordChange(params2, "调整2")
        
        val (restoredParams, newHistory) = history.undoWithCurrent(currentParams)
        
        assertNotNull(restoredParams)
        assertEquals(0.8f, restoredParams!!.exposure, 0.0001f)
        assertEquals(1, newHistory.undoCount())
        assertEquals(1, newHistory.redoCount())
    }
    
    @Test
    fun `test undo on empty history returns null`() {
        val history = EditHistory()
        val currentParams = AdjustmentParams(exposure = 1.0f)
        
        val (restoredParams, newHistory) = history.undoWithCurrent(currentParams)
        
        assertNull(restoredParams)
        assertEquals(history, newHistory)
    }
    
    @Test
    fun `test redo after undo restores state`() {
        var history = EditHistory()
        val params1 = AdjustmentParams(exposure = 0.5f)
        val params2 = AdjustmentParams(exposure = 0.8f)
        val currentParams = AdjustmentParams(exposure = 1.0f)
        
        history = history.recordChange(params1, "调整1")
        history = history.recordChange(params2, "调整2")
        
        // 撤销
        val (undoParams, historyAfterUndo) = history.undoWithCurrent(currentParams)
        assertNotNull(undoParams)
        
        // 重做
        val (redoParams, historyAfterRedo) = historyAfterUndo.redoWithCurrent(undoParams!!)
        assertNotNull(redoParams)
        assertEquals(1.0f, redoParams!!.exposure, 0.0001f)
    }
    
    @Test
    fun `test new change after undo clears redo stack`() {
        var history = EditHistory()
        val params1 = AdjustmentParams(exposure = 0.5f)
        val params2 = AdjustmentParams(exposure = 0.8f)
        val currentParams = AdjustmentParams(exposure = 1.0f)
        
        history = history.recordChange(params1, "调整1")
        history = history.recordChange(params2, "调整2")
        
        // 撤销
        val (undoParams, historyAfterUndo) = history.undoWithCurrent(currentParams)
        assertTrue(historyAfterUndo.canRedo())
        
        // 新的变更
        val params3 = AdjustmentParams(exposure = 0.6f)
        val historyAfterNewChange = historyAfterUndo.recordChange(params3, "新调整")
        
        // 重做栈应该被清空
        assertFalse(historyAfterNewChange.canRedo())
        assertEquals(0, historyAfterNewChange.redoCount())
    }
    
    @Test
    fun `test history size limit`() {
        var history = EditHistory(maxHistorySize = 5)
        
        // 添加 10 个变更
        for (i in 1..10) {
            val params = AdjustmentParams(exposure = i * 0.1f)
            history = history.recordChange(params, "调整$i")
        }
        
        // 应该只保留最后 5 个
        assertEquals(5, history.undoCount())
    }
    
    @Test
    fun `test clear history`() {
        var history = EditHistory()
        val params1 = AdjustmentParams(exposure = 0.5f)
        val params2 = AdjustmentParams(exposure = 0.8f)
        
        history = history.recordChange(params1, "调整1")
        history = history.recordChange(params2, "调整2")
        
        assertTrue(history.canUndo())
        
        val clearedHistory = history.clear()
        
        assertFalse(clearedHistory.canUndo())
        assertFalse(clearedHistory.canRedo())
        assertEquals(0, clearedHistory.undoCount())
        assertEquals(0, clearedHistory.redoCount())
    }
    
    @Test
    fun `test serialization round-trip`() {
        var history = EditHistory()
        val params1 = AdjustmentParams(exposure = 0.5f)
        val params2 = AdjustmentParams(exposure = 0.8f)
        
        history = history.recordChange(params1, "调整1")
        history = history.recordChange(params2, "调整2")
        
        // 序列化
        val serializable = history.toSerializable()
        
        // 反序列化
        val deserialized = serializable.toEditHistory()
        
        // 验证
        assertEquals(history.undoCount(), deserialized.undoCount())
        assertEquals(history.redoCount(), deserialized.redoCount())
        assertEquals(history.canUndo(), deserialized.canUndo())
        assertEquals(history.canRedo(), deserialized.canRedo())
    }
}
