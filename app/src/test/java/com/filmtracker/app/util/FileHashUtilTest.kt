package com.filmtracker.app.util

import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Unit tests for FileHashUtil.
 * 
 * Tests the SHA-256 hash computation and verification functionality
 * used for verifying original image immutability.
 */
class FileHashUtilTest {
    
    private fun createTempFile(name: String, content: String = ""): File {
        return File.createTempFile(name, ".tmp").apply {
            writeText(content)
            deleteOnExit()
        }
    }
    
    private fun createTempDir(name: String): File {
        return File.createTempFile(name, ".tmp").apply {
            delete()
            mkdir()
            deleteOnExit()
        }
    }
    
    @Test
    fun `computeSHA256 returns null for non-existent file`() {
        val nonExistentFile = File("does_not_exist_${System.currentTimeMillis()}.txt")
        val hash = FileHashUtil.computeSHA256(nonExistentFile)
        assertNull("Hash should be null for non-existent file", hash)
    }
    
    @Test
    fun `computeSHA256 returns null for directory`() {
        val directory = createTempDir("test_dir")
        val hash = FileHashUtil.computeSHA256(directory)
        assertNull("Hash should be null for directory", hash)
    }
    
    @Test
    fun `computeSHA256 returns consistent hash for same content`() {
        val file = createTempFile("test", "Hello, World!")
        
        val hash1 = FileHashUtil.computeSHA256(file)
        val hash2 = FileHashUtil.computeSHA256(file)
        
        assertNotNull("Hash should not be null", hash1)
        assertNotNull("Hash should not be null", hash2)
        assertEquals("Hash should be consistent for same content", hash1, hash2)
    }
    
    @Test
    fun `computeSHA256 returns different hash for different content`() {
        val file1 = createTempFile("test1", "Content A")
        val file2 = createTempFile("test2", "Content B")
        
        val hash1 = FileHashUtil.computeSHA256(file1)
        val hash2 = FileHashUtil.computeSHA256(file2)
        
        assertNotNull("Hash should not be null", hash1)
        assertNotNull("Hash should not be null", hash2)
        assertNotEquals("Hash should be different for different content", hash1, hash2)
    }
    
    @Test
    fun `computeSHA256 returns correct hash for known content`() {
        val file = createTempFile("test", "Hello, World!")
        
        val hash = FileHashUtil.computeSHA256(file)
        
        // Known SHA-256 hash for "Hello, World!"
        val expectedHash = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        
        assertNotNull("Hash should not be null", hash)
        assertEquals("Hash should match known SHA-256 value", expectedHash, hash)
    }
    
    @Test
    fun `computeSHA256 handles empty file`() {
        val file = createTempFile("empty", "")
        
        val hash = FileHashUtil.computeSHA256(file)
        
        // Known SHA-256 hash for empty file
        val expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        
        assertNotNull("Hash should not be null for empty file", hash)
        assertEquals("Hash should match known SHA-256 value for empty file", expectedHash, hash)
    }
    
    @Test
    fun `computeSHA256 handles large file efficiently`() {
        val file = createTempFile("large", "")
        
        // Create a 1MB file
        val largeContent = ByteArray(1024 * 1024) { it.toByte() }
        file.writeBytes(largeContent)
        
        val hash = FileHashUtil.computeSHA256(file)
        
        assertNotNull("Hash should not be null for large file", hash)
        assertEquals("Hash should be 64 characters (256 bits in hex)", 64, hash?.length)
    }
    
    @Test
    fun `computeSHA256 with path string works correctly`() {
        val file = createTempFile("test", "Test content")
        
        val hashFromFile = FileHashUtil.computeSHA256(file)
        val hashFromPath = FileHashUtil.computeSHA256(file.absolutePath)
        
        assertNotNull("Hash from file should not be null", hashFromFile)
        assertNotNull("Hash from path should not be null", hashFromPath)
        assertEquals("Hash from file and path should match", hashFromFile, hashFromPath)
    }
    
    @Test
    fun `verifyFileHash returns true for matching hash`() {
        val file = createTempFile("test", "Hello, World!")
        
        val hash = FileHashUtil.computeSHA256(file)
        assertNotNull("Hash should not be null", hash)
        
        val isValid = FileHashUtil.verifyFileHash(file, hash!!)
        assertTrue("Verification should succeed for matching hash", isValid)
    }
    
    @Test
    fun `verifyFileHash returns false for non-matching hash`() {
        val file = createTempFile("test", "Hello, World!")
        
        val wrongHash = "0000000000000000000000000000000000000000000000000000000000000000"
        
        val isValid = FileHashUtil.verifyFileHash(file, wrongHash)
        assertFalse("Verification should fail for non-matching hash", isValid)
    }
    
    @Test
    fun `verifyFileHash returns false for non-existent file`() {
        val nonExistentFile = File("does_not_exist_${System.currentTimeMillis()}.txt")
        val someHash = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        
        val isValid = FileHashUtil.verifyFileHash(nonExistentFile, someHash)
        assertFalse("Verification should fail for non-existent file", isValid)
    }
    
    @Test
    fun `verifyFileHash is case-insensitive`() {
        val file = createTempFile("test", "Hello, World!")
        
        val hash = FileHashUtil.computeSHA256(file)
        assertNotNull("Hash should not be null", hash)
        
        val upperCaseHash = hash!!.uppercase()
        val lowerCaseHash = hash.lowercase()
        
        assertTrue("Verification should succeed with uppercase hash", 
            FileHashUtil.verifyFileHash(file, upperCaseHash))
        assertTrue("Verification should succeed with lowercase hash", 
            FileHashUtil.verifyFileHash(file, lowerCaseHash))
    }
    
    @Test
    fun `verifyFileHash with path string works correctly`() {
        val file = createTempFile("test", "Test content")
        
        val hash = FileHashUtil.computeSHA256(file)
        assertNotNull("Hash should not be null", hash)
        
        val isValidFromFile = FileHashUtil.verifyFileHash(file, hash!!)
        val isValidFromPath = FileHashUtil.verifyFileHash(file.absolutePath, hash)
        
        assertTrue("Verification from file should succeed", isValidFromFile)
        assertTrue("Verification from path should succeed", isValidFromPath)
    }
    
    @Test
    fun `hash detects file modification`() {
        val file = createTempFile("test", "Original content")
        
        val originalHash = FileHashUtil.computeSHA256(file)
        assertNotNull("Original hash should not be null", originalHash)
        
        // Modify the file
        file.writeText("Modified content")
        
        val modifiedHash = FileHashUtil.computeSHA256(file)
        assertNotNull("Modified hash should not be null", modifiedHash)
        
        assertNotEquals("Hash should change after file modification", originalHash, modifiedHash)
        assertFalse("Verification should fail with original hash after modification",
            FileHashUtil.verifyFileHash(file, originalHash!!))
    }
}

