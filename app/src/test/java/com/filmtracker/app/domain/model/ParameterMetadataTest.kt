package com.filmtracker.app.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * ParameterMetadata 单元测试
 */
class ParameterMetadataTest {
    
    @Test
    fun `test serialization round-trip with default parameters`() {
        // 创建测试数据
        val params = AdjustmentParams.default()
        val metadata = ParameterMetadata(
            version = 1,
            imageUri = "content://media/external/images/media/1234",
            imagePath = "/storage/emulated/0/DCIM/Camera/IMG_1234.jpg",
            imageHash = "abc123",
            parameters = SerializableAdjustmentParams.fromAdjustmentParams(params),
            createdAt = 1705449600000L,
            modifiedAt = 1705536000000L,
            appVersion = "1.0.0"
        )
        
        // 序列化
        val json = metadata.toJson()
        assertNotNull(json)
        assertTrue(json.isNotEmpty())
        
        // 反序列化
        val deserialized = ParameterMetadata.fromJson(json)
        
        // 验证
        assertEquals(metadata.version, deserialized.version)
        assertEquals(metadata.imageUri, deserialized.imageUri)
        assertEquals(metadata.imagePath, deserialized.imagePath)
        assertEquals(metadata.imageHash, deserialized.imageHash)
        assertEquals(metadata.createdAt, deserialized.createdAt)
        assertEquals(metadata.modifiedAt, deserialized.modifiedAt)
        assertEquals(metadata.appVersion, deserialized.appVersion)
        
        // 验证参数
        val originalParams = metadata.parameters.toAdjustmentParams()
        val deserializedParams = deserialized.parameters.toAdjustmentParams()
        assertEquals(originalParams, deserializedParams)
    }
    
    @Test
    fun `test serialization round-trip with modified parameters`() {
        // 创建修改过的参数
        val params = AdjustmentParams(
            exposure = 0.5f,
            contrast = 1.2f,
            saturation = 1.1f,
            temperature = 10f,
            tint = -5f,
            highlights = -15f,
            shadows = 20f
        )
        
        val metadata = ParameterMetadata(
            version = 1,
            imageUri = "content://media/external/images/media/5678",
            imagePath = "/storage/emulated/0/DCIM/Camera/IMG_5678.dng",
            imageHash = null,
            parameters = SerializableAdjustmentParams.fromAdjustmentParams(params),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appVersion = "1.0.0"
        )
        
        // 序列化和反序列化
        val json = metadata.toJson()
        val deserialized = ParameterMetadata.fromJson(json)
        
        // 验证参数值
        val deserializedParams = deserialized.parameters.toAdjustmentParams()
        assertEquals(0.5f, deserializedParams.exposure, 0.0001f)
        assertEquals(1.2f, deserializedParams.contrast, 0.0001f)
        assertEquals(1.1f, deserializedParams.saturation, 0.0001f)
        assertEquals(10f, deserializedParams.temperature, 0.0001f)
        assertEquals(-5f, deserializedParams.tint, 0.0001f)
        assertEquals(-15f, deserializedParams.highlights, 0.0001f)
        assertEquals(20f, deserializedParams.shadows, 0.0001f)
    }
    
    @Test
    fun `test metadata file name generation`() {
        val imagePath1 = "/storage/emulated/0/DCIM/Camera/IMG_1234.jpg"
        val fileName1 = ParameterMetadata.getMetadataFileName(imagePath1)
        assertEquals("IMG_1234.jpg.filmtracker.json", fileName1)
        
        val imagePath2 = "/path/to/photo.dng"
        val fileName2 = ParameterMetadata.getMetadataFileName(imagePath2)
        assertEquals("photo.dng.filmtracker.json", fileName2)
    }
    
    @Test
    fun `test version migration - same version`() {
        val params = AdjustmentParams.default()
        val metadata = ParameterMetadata(
            version = 1,
            imageUri = "content://test",
            imagePath = "/test/path.jpg",
            parameters = SerializableAdjustmentParams.fromAdjustmentParams(params),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appVersion = "1.0.0"
        )
        
        val migrated = metadata.migrate(1)
        assertEquals(metadata, migrated)
    }
    
    @Test
    fun `test version migration - upward migration preserves parameters`() {
        // 模拟版本 0 的元数据 (假设未来有版本 0)
        val params = AdjustmentParams(
            exposure = 0.5f,
            contrast = 1.2f,
            saturation = 1.1f
        )
        
        val metadata = ParameterMetadata(
            version = 0,
            imageUri = "content://test",
            imagePath = "/test/path.jpg",
            parameters = SerializableAdjustmentParams.fromAdjustmentParams(params),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appVersion = "0.9.0"
        )
        
        // 迁移到版本 1
        val migrated = metadata.migrate(1)
        
        // 验证版本已更新
        assertEquals(1, migrated.version)
        
        // 验证参数保持不变
        val migratedParams = migrated.parameters.toAdjustmentParams()
        assertEquals(0.5f, migratedParams.exposure, 0.0001f)
        assertEquals(1.2f, migratedParams.contrast, 0.0001f)
        assertEquals(1.1f, migratedParams.saturation, 0.0001f)
    }
    
    @Test
    fun `test version migration - newer version best effort loading`() {
        val params = AdjustmentParams.default()
        val metadata = ParameterMetadata(
            version = 2,  // 未来版本
            imageUri = "content://test",
            imagePath = "/test/path.jpg",
            parameters = SerializableAdjustmentParams.fromAdjustmentParams(params),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appVersion = "2.0.0"
        )
        
        // 尝试迁移到版本 1 (向下迁移不支持)
        val migrated = metadata.migrate(1)
        
        // 验证保持原版本号
        assertEquals(2, migrated.version)
        
        // 验证参数未改变
        assertEquals(metadata.parameters, migrated.parameters)
    }
    
    @Test
    fun `test fromJson with version 1 metadata`() {
        // 创建版本 1 的 JSON
        val json = """
            {
                "version": 1,
                "imageUri": "content://test",
                "imagePath": "/test/path.jpg",
                "imageHash": "hash123",
                "parameters": {
                    "exposure": 0.5,
                    "contrast": 1.2,
                    "saturation": 1.0
                },
                "createdAt": 1705449600000,
                "modifiedAt": 1705536000000,
                "appVersion": "1.0.0"
            }
        """.trimIndent()
        
        val metadata = ParameterMetadata.fromJson(json)
        
        // 验证版本正确
        assertEquals(1, metadata.version)
        
        // 验证参数正确加载
        val params = metadata.parameters.toAdjustmentParams()
        assertEquals(0.5f, params.exposure, 0.0001f)
        assertEquals(1.2f, params.contrast, 0.0001f)
    }
    
    @Test
    fun `test fromJson with older version triggers migration`() {
        // 创建版本 0 的 JSON (模拟旧版本)
        val json = """
            {
                "version": 0,
                "imageUri": "content://test",
                "imagePath": "/test/path.jpg",
                "parameters": {
                    "exposure": 0.3,
                    "contrast": 1.1
                },
                "createdAt": 1705449600000,
                "modifiedAt": 1705536000000,
                "appVersion": "0.9.0"
            }
        """.trimIndent()
        
        val metadata = ParameterMetadata.fromJson(json)
        
        // 验证已迁移到当前版本
        assertEquals(ParameterMetadata.CURRENT_VERSION, metadata.version)
        
        // 验证参数保留
        val params = metadata.parameters.toAdjustmentParams()
        assertEquals(0.3f, params.exposure, 0.0001f)
        assertEquals(1.1f, params.contrast, 0.0001f)
    }
    
    @Test
    fun `test fromJson with newer version preserves version`() {
        // 创建版本 2 的 JSON (未来版本)
        val json = """
            {
                "version": 2,
                "imageUri": "content://test",
                "imagePath": "/test/path.jpg",
                "parameters": {
                    "exposure": 0.7,
                    "contrast": 1.3
                },
                "createdAt": 1705449600000,
                "modifiedAt": 1705536000000,
                "appVersion": "2.0.0"
            }
        """.trimIndent()
        
        val metadata = ParameterMetadata.fromJson(json)
        
        // 验证保持原版本号 (最佳努力加载)
        assertEquals(2, metadata.version)
        
        // 验证参数正确加载
        val params = metadata.parameters.toAdjustmentParams()
        assertEquals(0.7f, params.exposure, 0.0001f)
        assertEquals(1.3f, params.contrast, 0.0001f)
    }
    
    @Test
    fun `test curve points serialization`() {
        val curvePoints = listOf(
            Pair(0f, 0f),
            Pair(0.25f, 0.3f),
            Pair(0.5f, 0.55f),
            Pair(0.75f, 0.7f),
            Pair(1f, 1f)
        )
        
        val params = AdjustmentParams(
            enableRgbCurve = true,
            rgbCurvePoints = curvePoints
        )
        
        val metadata = ParameterMetadata(
            version = 1,
            imageUri = "content://test",
            imagePath = "/test/path.jpg",
            parameters = SerializableAdjustmentParams.fromAdjustmentParams(params),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appVersion = "1.0.0"
        )
        
        // 序列化和反序列化
        val json = metadata.toJson()
        val deserialized = ParameterMetadata.fromJson(json)
        val deserializedParams = deserialized.parameters.toAdjustmentParams()
        
        // 验证曲线点
        assertTrue(deserializedParams.enableRgbCurve)
        assertEquals(curvePoints.size, deserializedParams.rgbCurvePoints.size)
        
        curvePoints.forEachIndexed { index, point ->
            val deserializedPoint = deserializedParams.rgbCurvePoints[index]
            assertEquals(point.first, deserializedPoint.first, 0.0001f)
            assertEquals(point.second, deserializedPoint.second, 0.0001f)
        }
    }
    
    @Test
    fun `test HSL arrays serialization`() {
        val hueShift = FloatArray(8) { it * 5f }
        val saturation = FloatArray(8) { it * 10f }
        val luminance = FloatArray(8) { it * -5f }
        
        val params = AdjustmentParams(
            enableHSL = true,
            hslHueShift = hueShift,
            hslSaturation = saturation,
            hslLuminance = luminance
        )
        
        val metadata = ParameterMetadata(
            version = 1,
            imageUri = "content://test",
            imagePath = "/test/path.jpg",
            parameters = SerializableAdjustmentParams.fromAdjustmentParams(params),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            appVersion = "1.0.0"
        )
        
        // 序列化和反序列化
        val json = metadata.toJson()
        val deserialized = ParameterMetadata.fromJson(json)
        val deserializedParams = deserialized.parameters.toAdjustmentParams()
        
        // 验证 HSL 数组
        assertTrue(deserializedParams.enableHSL)
        assertArrayEquals(hueShift, deserializedParams.hslHueShift, 0.0001f)
        assertArrayEquals(saturation, deserializedParams.hslSaturation, 0.0001f)
        assertArrayEquals(luminance, deserializedParams.hslLuminance, 0.0001f)
    }
}
