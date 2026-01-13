package com.filmtracker.app.native

/**
 * RAW元数据Native封装
 */
class RawMetadataNative(val nativePtr: Long) {
    
    val width: Int get() = getWidth()
    val height: Int get() = getHeight()
    val bitsPerSample: Int get() = getBitsPerSample()
    val iso: Float get() = getIso()
    val exposureTime: Float get() = getExposureTime()
    val aperture: Float get() = getAperture()
    val focalLength: Float get() = getFocalLength()
    val whiteBalanceTemperature: Float get() = getWhiteBalanceTemperature()
    val whiteBalanceTint: Float get() = getWhiteBalanceTint()
    val cameraModel: String? get() = getCameraModel()
    val colorSpace: String? get() = getColorSpace()
    val blackLevel: Float get() = getBlackLevel()
    val whiteLevel: Float get() = getWhiteLevel()
    
    private external fun getWidth(): Int
    private external fun getHeight(): Int
    private external fun getBitsPerSample(): Int
    private external fun getIso(): Float
    private external fun getExposureTime(): Float
    private external fun getAperture(): Float
    private external fun getFocalLength(): Float
    private external fun getWhiteBalanceTemperature(): Float
    private external fun getWhiteBalanceTint(): Float
    private external fun getCameraModel(): String?
    private external fun getColorSpace(): String?
    private external fun getBlackLevel(): Float
    private external fun getWhiteLevel(): Float
    
    companion object {
        init {
            System.loadLibrary("filmtracker")
        }
    }
}
