package com.journeyapps.barcodescanner.camera

import android.graphics.Rect
import android.util.Log
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.camera.PreviewScalingStrategy
import java.util.*

abstract class PreviewScalingStrategy {
    fun getBestPreviewOrder(sizes: List<Size>, desired: Size?): List<Size> {
        if (desired == null) {
            return sizes
        }

        val sortedList = sizes.toMutableList()

        sortedList.sortWith { a, b ->
            val aScore = getScore(a, desired)
            val bScore = getScore(b, desired)
            // Bigger score first
            bScore.compareTo(aScore)
        }
        return sortedList
    }

    open fun getBestPreviewSize(sizes: List<Size>, desired: Size?): Size {
        // Sample of supported preview sizes:
        // http://www.kirill.org/ar/ar.php
        val ordered = getBestPreviewOrder(sizes, desired)
        Log.i(TAG, "Viewfinder size: $desired")
        Log.i(TAG, "Preview in order of preference: $ordered")
        return ordered[0]
    }

    protected open fun getScore(size: Size, desired: Size): Float {
        return 0.5f
    }

    abstract fun scalePreview(previewSize: Size, viewfinderSize: Size): Rect

    companion object {
        private val TAG = PreviewScalingStrategy::class.simpleName
    }
}