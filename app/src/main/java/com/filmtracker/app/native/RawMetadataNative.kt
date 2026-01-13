package com.filmtracker.app.native

/**
 * RAW元数据Native封装
 */
class RawMetadataNative(val nativePtr: Long) {
    
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
