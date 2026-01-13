package com.filmtracker.app.native

/**
 * RAW元数据Native封装
 */
class RawMetadataNative(val nativePtr: Long) {
    
    val width: Int
        get() = getWidth()
    val height: Int
        get() = getHeight()
    val bitsPerSample: Int
        get() = getBitsPerSample()
    val iso: Float
        get() = getIso()
    val exposureTime: Float
        get() = getExposureTime()
    val aperture: Float
        get() = getAperture()
    val focalLength: Float
        get() = getFocalLength()
    val whiteBalanceTemperature: Float
        get() = getWhiteBalanceTemperature()
    val whiteBalanceTint: Float
        get() = getWhiteBalanceTint()
    val cameraModel: String?
        get() = getCameraModel()
    val colorSpace: String?
        get() = getColorSpace()
    val blackLevel: Float
        get() = getBlackLevel()
    val whiteLevel: Float
        get() = getWhiteLevel()
    
    external fun getWidth(): Int
    external fun getHeight(): Int
    external fun getBitsPerSample(): Int
    external fun getIso(): Float
    external fun getExposureTime(): Float
    external fun getAperture(): Float
    external fun getFocalLength(): Float
    external fun getWhiteBalanceTemperature(): Float
    external fun getWhiteBalanceTint(): Float
    external fun getCameraModel(): String?
    external fun getColorSpace(): String?
    external fun getBlackLevel(): Float
    external fun getWhiteLevel(): Float
    
    companion object {
        init {
            System.loadLibrary("filmtracker")
        }
    }
}
