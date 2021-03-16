package com.journeyapps.barcodescanner.camera

import android.graphics.Rect
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.camera.DisplayConfiguration

class DisplayConfiguration(val rotation: Int, val viewfinderSize: Size) {
    var previewScalingStrategy: PreviewScalingStrategy = FitCenterStrategy()

    fun getDesiredPreviewSize(rotate: Boolean) = if (rotate) viewfinderSize.rotate() else viewfinderSize

    fun getBestPreviewSize(sizes: List<Size>, isRotated: Boolean): Size {
        return previewScalingStrategy.getBestPreviewSize(sizes, getDesiredPreviewSize(isRotated))
    }

    fun scalePreview(previewSize: Size): Rect {
        return previewScalingStrategy.scalePreview(previewSize, viewfinderSize)
    }

    companion object {
        private val TAG = DisplayConfiguration::class.simpleName
    }
}