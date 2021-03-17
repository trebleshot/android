package com.journeyapps.barcodescanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import com.google.zxing.PlanarYUVLuminanceSource
import java.io.ByteArrayOutputStream

class SourceData(
    val data: ByteArray,
    private val dataWidth: Int,
    private val dataHeight: Int,
    private val imageFormat: Int,
    private val rotation: Int,
) {
    var cropRect: Rect? = null

    fun createSource(): PlanarYUVLuminanceSource {
        val rotated = rotateCameraPreview(rotation, data, dataWidth, dataHeight)
        val cropRect = cropRect ?: throw NullPointerException("cropRect cannot be null")

        return PlanarYUVLuminanceSource(
            rotated,
            if (isRotated()) dataHeight else dataWidth,
            if (isRotated()) dataWidth else dataHeight,
            if (isRotated()) cropRect.left else cropRect.top,
            if (isRotated()) cropRect.top else cropRect.left,
            cropRect.width(),
            cropRect.height(),
            false
        )
    }

    fun getBitmap(scaleFactor: Int = 1, cropRect: Rect = Rect(this.cropRect!!)): Bitmap {
        if (isRotated()) {
            cropRect.set(cropRect.top, cropRect.left, cropRect.bottom, cropRect.right)
        }

        val img = YuvImage(data, imageFormat, dataWidth, dataHeight, null)
        val buffer = ByteArrayOutputStream()

        img.compressToJpeg(cropRect, 90, buffer)

        val jpegData = buffer.toByteArray()
        val options = BitmapFactory.Options().apply {
            inSampleSize = scaleFactor
        }
        var bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size, options)

        if (rotation != 0) {
            val imageMatrix = Matrix()
            imageMatrix.postRotate(rotation.toFloat())
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, imageMatrix, false)
        }
        return bitmap
    }

    private fun isRotated(): Boolean = rotation % 180 != 0

    companion object {
        fun rotateCameraPreview(cameraRotation: Int, data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
            return when (cameraRotation) {
                0 -> data
                90 -> rotateClockwise(data, imageWidth, imageHeight)
                180 -> rotateInverse(data, imageWidth, imageHeight)
                270 -> rotateCounterClockwise(data, imageWidth, imageHeight)
                else -> data
            }
        }

        private fun rotateClockwise(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
            val yuv = ByteArray(imageWidth * imageHeight)
            var i = 0
            for (x in 0 until imageWidth) {
                for (y in imageHeight - 1 downTo 0) {
                    yuv[i] = data[y * imageWidth + x]
                    i++
                }
            }
            return yuv
        }

        private fun rotateCounterClockwise(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
            val n = imageWidth * imageHeight
            val yuv = ByteArray(n)
            var i = n - 1
            for (x in 0 until imageWidth) {
                for (y in imageHeight - 1 downTo 0) {
                    yuv[i] = data[y * imageWidth + x]
                    i--
                }
            }
            return yuv
        }

        private fun rotateInverse(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
            val n = imageWidth * imageHeight
            val yuv = ByteArray(n)
            var i = n - 1
            for (j in 0 until n) {
                yuv[i] = data[j]
                i--
            }
            return yuv
        }
    }

    init {
        require(dataWidth * dataHeight <= data.size) {
            "Image data does not match the resolution. " + dataWidth + "x" + dataHeight + " > " + data.size
        }
    }
}