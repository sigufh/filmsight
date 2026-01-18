package com.filmtracker.app.util

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Utility for computing file hashes to verify file integrity.
 * Used primarily for verifying original image immutability in the non-destructive editing system.
 */
object FileHashUtil {
    
    /**
     * Computes the SHA-256 hash of a file.
     * 
     * This function reads the file in chunks to handle large image files efficiently
     * without loading the entire file into memory.
     * 
     * @param file The file to hash
     * @return The SHA-256 hash as a hexadecimal string, or null if the file cannot be read
     * @throws IOException if there's an error reading the file
     */
    fun computeSHA256(file: File): String? {
        if (!file.exists() || !file.isFile) {
            return null
        }
        
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192) // 8KB buffer for efficient reading
            
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            // Convert byte array to hex string
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Log error but return null to indicate failure
            null
        }
    }
    
    /**
     * Computes the SHA-256 hash of a file specified by path.
     * 
     * @param filePath The path to the file to hash
     * @return The SHA-256 hash as a hexadecimal string, or null if the file cannot be read
     */
    fun computeSHA256(filePath: String): String? {
        return computeSHA256(File(filePath))
    }
    
    /**
     * Verifies that a file's current hash matches an expected hash.
     * 
     * This is used to verify that original images have not been modified.
     * 
     * @param file The file to verify
     * @param expectedHash The expected SHA-256 hash (hexadecimal string)
     * @return true if the file's hash matches the expected hash, false otherwise
     */
    fun verifyFileHash(file: File, expectedHash: String): Boolean {
        val currentHash = computeSHA256(file) ?: return false
        return currentHash.equals(expectedHash, ignoreCase = true)
    }
    
    /**
     * Verifies that a file's current hash matches an expected hash.
     * 
     * @param filePath The path to the file to verify
     * @param expectedHash The expected SHA-256 hash (hexadecimal string)
     * @return true if the file's hash matches the expected hash, false otherwise
     */
    fun verifyFileHash(filePath: String, expectedHash: String): Boolean {
        return verifyFileHash(File(filePath), expectedHash)
    }
}
