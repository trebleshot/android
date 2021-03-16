package com.journeyapps.barcodescanner.camera

import android.graphics.Rect
import android.util.Log
import com.journeyapps.barcodescanner.Size

class LegacyPreviewScalingStrategy : PreviewScalingStrategy() {
    override fun getBestPreviewSize(sizes: List<Size>, desired: Size?): Size {
        // Sample of supported preview sizes:
        // http://www.kirill.org/ar/ar.php
        if (desired == null) {
            return sizes[0]
        }

        val sortedList = sizes.toMutableList()

        sortedList.sortWith { a, b ->
            val aScaled = scale(a, desired)
            val aScale = aScaled.width - a.width
            val bScaled = scale(b, desired)
            val bScale = bScaled.width - b.width
            if (aScale == 0 && bScale == 0) {
                // Both no scaling, pick the smaller one
                a.compareTo(b)
            } else if (aScale == 0) {
                // No scaling for a; pick a
                -1
            } else if (bScale == 0) {
                // No scaling for b; pick b
                1
            } else if (aScale < 0 && bScale < 0) {
                // Both downscaled. Pick the smaller one (less downscaling).
                a.compareTo(b)
            } else if (aScale > 0 && bScale > 0) {
                // Both upscaled. Pick the larger one (less upscaling).
                -a.compareTo(b)
            } else if (aScale < 0) {
                // a downscaled, b upscaled. Pick a.
                -1
            } else {
                // a upscaled, b downscaled. Pick b.
                1
            }
        }
        Log.i(TAG, "Viewfinder size: $desired")
        Log.i(TAG, "Preview in order of preference: $sizes")
        return sizes[0]
    }

    override fun scalePreview(previewSize: Size, viewfinderSize: Size): Rect {
        // We avoid scaling if feasible.
        val scaledPreview = scale(previewSize, viewfinderSize)
        Log.i(TAG, "Preview: $previewSize; Scaled: $scaledPreview; Want: $viewfinderSize")
        val dx = (scaledPreview.width - viewfinderSize.width) / 2
        val dy = (scaledPreview.height - viewfinderSize.height) / 2
        return Rect(-dx, -dy, scaledPreview.width - dx, scaledPreview.height - dy)
    }

    companion object {
        private val TAG = LegacyPreviewScalingStrategy::class.simpleName

        fun scale(from: Size, to: Size): Size {
            var current = from
            if (!to.fitsIn(current)) {
                // Scale up
                while (true) {
                    val scaled150 = current.scale(3, 2)
                    val scaled200 = current.scale(2, 1)
                    when {
                        to.fitsIn(scaled150) -> return scaled150 // Scale by 3/2
                        to.fitsIn(scaled200) -> return scaled200 // Scale by 2/1
                        else -> current = scaled200 // Scale by 2/1 and continue
                    }
                }
            } else {
                // Scale down
                while (true) {
                    val scaled66 = current.scale(2, 3)
                    val scaled50 = current.scale(1, 2)
                    if (!to.fitsIn(scaled50)) {
                        return if (to.fitsIn(scaled66)) {
                            scaled66 // Scale by 2/3
                        } else {
                            current // No more downscaling
                        }
                    } else {
                        current = scaled50 // Scale by 1/2
                    }
                }
            }
        }
    }
}