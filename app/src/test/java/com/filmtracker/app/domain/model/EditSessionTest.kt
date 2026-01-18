package com.filmtracker.app.domain.model

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test

/**
 * EditSession 单元测试
 */
class EditSessionTest {
    
    @Test
    fun `test create new session`() {
        val uri = Uri.parse("content://media/external/images/media/1234")
        val path = "/storage/emulated/0/DCIM/Camera/IMG_1234.jpg"
        
        val session = EditSession.create(uri, path)
        
        assertEquals(uri, session.imageUri)
        assertEquals(path, session.imagePath)
        assertEquals(AdjustmentParams.default(), session.currentParams)
        assertFalse(session.isModified)
        assertFalse(session.canUndo())
        assertFalse(session.canRedo())
    }
    
    @Test
    fun `test apply parameter change`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        val session = EditSession.create(uri, path)
        
        val change = ParameterChange.ExposureChange(0.5f)
        val newSession = session.applyParameterChange(change)
        
        assertEquals(0.5f, newSession.currentParams.exposure, 0.0001f)
        assertTrue(newSession.isModified)
        assertTrue(newSession.canUndo())
        assertFalse(newSession.canRedo())
    }
    
    @Test
    fun `test multiple parameter changes`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        var session = EditSession.create(uri, path)
        
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.5f))
        session = session.applyParameterChange(ParameterChange.ContrastChange(1.2f))
        session = session.applyParameterChange(ParameterChange.SaturationChange(1.1f))
        
        assertEquals(0.5f, session.currentParams.exposure, 0.0001f)
        assertEquals(1.2f, session.currentParams.contrast, 0.0001f)
        assertEquals(1.1f, session.currentParams.saturation, 0.0001f)
        assertTrue(session.canUndo())
    }
    
    @Test
    fun `test undo restores previous state`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        var session = EditSession.create(uri, path)
        
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.5f))
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.8f))
        
        assertEquals(0.8f, session.currentParams.exposure, 0.0001f)
        
        val undoneSession = session.undo()
        assertNotNull(undoneSession)
        assertEquals(0.5f, undoneSession!!.currentParams.exposure, 0.0001f)
        assertTrue(undoneSession.canRedo())
    }
    
    @Test
    fun `test undo on new session returns null`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        val session = EditSession.create(uri, path)
        
        val undoneSession = session.undo()
        assertNull(undoneSession)
    }
    
    @Test
    fun `test redo after undo`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        var session = EditSession.create(uri, path)
        
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.5f))
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.8f))
        
        val undoneSession = session.undo()
        assertNotNull(undoneSession)
        
        val redoneSession = undoneSession!!.redo()
        assertNotNull(redoneSession)
        assertEquals(0.8f, redoneSession!!.currentParams.exposure, 0.0001f)
    }
    
    @Test
    fun `test reset to defaults`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        var session = EditSession.create(uri, path)
        
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.5f))
        session = session.applyParameterChange(ParameterChange.ContrastChange(1.5f))
        
        val resetSession = session.resetToDefaults()
        
        assertEquals(AdjustmentParams.default(), resetSession.currentParams)
        assertTrue(resetSession.isModified)
        assertTrue(resetSession.canUndo())
    }
    
    @Test
    fun `test canUndo and canRedo`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        var session = EditSession.create(uri, path)
        
        // 初始状态
        assertFalse(session.canUndo())
        assertFalse(session.canRedo())
        
        // 应用变更
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.5f))
        assertTrue(session.canUndo())
        assertFalse(session.canRedo())
        
        // 撤销
        session = session.undo()!!
        assertTrue(session.canUndo())
        assertTrue(session.canRedo())
        
        // 重做
        session = session.redo()!!
        assertTrue(session.canUndo())
        assertFalse(session.canRedo())
    }
    
    @Test
    fun `test isModified flag`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        var session = EditSession.create(uri, path)
        
        // 初始状态未修改
        assertFalse(session.isModified)
        
        // 应用变更后已修改
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.5f))
        assertTrue(session.isModified)
        
        // 撤销后仍然是已修改状态
        session = session.undo()!!
        assertTrue(session.isModified)
    }
    
    @Test
    fun `test lastModified timestamp updates`() {
        val uri = Uri.parse("content://test")
        val path = "/test/path.jpg"
        var session = EditSession.create(uri, path)
        
        val initialTimestamp = session.lastModified
        
        // 等待一小段时间
        Thread.sleep(10)
        
        session = session.applyParameterChange(ParameterChange.ExposureChange(0.5f))
        
        assertTrue(session.lastModified > initialTimestamp)
    }
}
