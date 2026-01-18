package com.filmtracker.app.processing

import android.graphics.Bitmap
import android.graphics.Matrix
import com.filmtracker.app.data.BasicAdjustmentParams

class GeometryProcessor : BaseStageProcessor(ProcessingStage.GEOMETRY) {

    private val eps = 0.001f

    override fun areParamsDefault(params: BasicAdjustmentParams): Boolean {
        val rotZero = kotlin.math.abs(params.rotation) < eps
        val cropDefault = !params.cropEnabled || (
            kotlin.math.abs(params.cropLeft - 0f) < eps &&
            kotlin.math.abs(params.cropTop - 0f) < eps &&
            kotlin.math.abs(params.cropRight - 1f) < eps &&
            kotlin.math.abs(params.cropBottom - 1f) < eps
        )
        return rotZero && cropDefault
    }

    override fun process(input: Bitmap, params: BasicAdjustmentParams): Bitmap? {
        var working = input
        var owns = false
        
        // 1. 旋转
        val r = normalizeRotation(params.rotation)
        if (kotlin.math.abs(r) > eps) {
            val matrix = Matrix()
            matrix.postRotate(r)
            val rotated = Bitmap.createBitmap(working, 0, 0, working.width, working.height, matrix, true)
            if (owns) working.recycle()
            working = rotated
            owns = true
        }
        
        // 2. 裁剪
        if (params.cropEnabled) {
            val l = params.cropLeft.coerceIn(0f, 1f)
            val t = params.cropTop.coerceIn(0f, 1f)
            val rgt = params.cropRight.coerceIn(0f, 1f)
            val btm = params.cropBottom.coerceIn(0f, 1f)
            
            val leftPx = (l * working.width).toInt().coerceIn(0, working.width - 1)
            val topPx = (t * working.height).toInt().coerceIn(0, working.height - 1)
            val rightPx = (rgt * working.width).toInt().coerceIn(leftPx + 1, working.width)
            val bottomPx = (btm * working.height).toInt().coerceIn(topPx + 1, working.height)
            
            val w = (rightPx - leftPx).coerceAtLeast(1)
            val h = (bottomPx - topPx).coerceAtLeast(1)
            
            val cropped = Bitmap.createBitmap(working, leftPx, topPx, w, h)
            if (owns) working.recycle()
            working = cropped
            owns = true
        }
        
        // 如果没有任何变换，返回副本
        if (!owns) {
            return input.copy(input.config, true)
        }
        
        return working
    }

    private fun normalizeRotation(deg: Float): Float {
        var r = deg % 360f
        if (r > 180f) r -= 360f
        if (r < -180f) r += 360f
        return r
    }
}
