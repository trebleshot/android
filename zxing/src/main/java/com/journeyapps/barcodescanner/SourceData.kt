package com.journeyapps.barcodescanner

import android.graphics.*
import com.google.zxing.PlanarYUVLuminanceSource
import java.io.ByteArrayOutputStream

/**
 * Raw preview data from a camera.
 */
class SourceData(
    /**
     * Raw YUV data
     */
    val data: ByteArray,
    /**
     * Source data width
     */
    val dataWidth: Int,
    /**
     * Source data height
     */
    val dataHeight: Int,
    /**
     * The format of the image data. ImageFormat.NV21 and ImageFormat.YUY2 are supported.
     */
    val imageFormat: Int,
    /**
     * Rotation in degrees (0, 90, 180 or 270). This is camera rotation relative to display rotation.
     */
    private val rotation: Int
) {
    /**
     * Set the crop rectangle.
     * Crop rectangle, in display orientation.
     */
    var cropRect: Rect? = null

    /**
     * @return true if the preview image is rotated orthogonal to the display
     */
    val isRotated: Boolean
        get() = rotation % 180 != 0

    fun createSource(): PlanarYUVLuminanceSource {
        val rotated = rotateCameraPreview(rotation, data, dataWidth, dataHeight)
        val cropRect = cropRect ?: throw NullPointerException("cropRect cannot be null")
        // TODO: handle mirrored (front) camera. Probably only the ResultPoints should be mirrored,
        // not the preview for decoding.
        return if (isRotated) {
            PlanarYUVLuminanceSource(
                rotated,
                dataHeight,
                dataWidth,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height(),
                false
            )
        } else {
            PlanarYUVLuminanceSource(
                rotated,
                dataWidth,
                dataHeight,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height(),
                false
            )
        }
    }

    /**
     * Return the source bitmap (cropped; in display orientation).
     *
     * @return the bitmap
     */
    val bitmap: Bitmap
        get() = getBitmap(1)

    /**
     * Return the source bitmap (cropped; in display orientation).
     *
     * @param scaleFactor factor to scale down by. Must be a power of 2.
     * @return the bitmap
     */
    fun getBitmap(scaleFactor: Int): Bitmap {
        return getBitmap(cropRect, scaleFactor)
    }

    private fun getBitmap(cropRect: Rect?, scaleFactor: Int): Bitmap {
        var localRect = cropRect ?: throw NullPointerException("cropRect cannot be null")

        if (isRotated) {
            localRect = Rect(localRect.top, localRect.left, localRect.bottom, localRect.right)
        }

        // TODO: there should be a way to do this without JPEG compression / decompression cycle.
        val img = YuvImage(data, imageFormat, dataWidth, dataHeight, null)
        val buffer = ByteArrayOutputStream()
        img.compressToJpeg(localRect, 90, buffer)
        val jpegData = buffer.toByteArray()
        val options = BitmapFactory.Options()
        options.inSampleSize = scaleFactor
        var bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size, options)

        // Rotate if required
        if (rotation != 0) {
            val imageMatrix = Matrix()
            imageMatrix.postRotate(rotation.toFloat())
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, imageMatrix, false)
        }
        return bitmap
    }

    companion object {
        fun rotateCameraPreview(cameraRotation: Int, data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
            return when (cameraRotation) {
                0 -> data
                90 -> rotateCW(data, imageWidth, imageHeight)
                180 -> rotate180(data, imageWidth, imageHeight)
                270 -> rotateCCW(data, imageWidth, imageHeight)
                else ->                 // Should not happen
                    data
            }
        }

        /**
         * Rotate an image by 90 degrees CW.
         *
         * @param data        the image data, in with the first width * height bytes being the luminance data.
         * @param imageWidth  the width of the image
         * @param imageHeight the height of the image
         * @return the rotated bytes
         */
        fun rotateCW(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
            // Adapted from http://stackoverflow.com/a/15775173
            // data may contain more than just y (u and v), but we are only interested in the y section.
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

        /**
         * Rotate an image by 180 degrees.
         *
         * @param data        the image data, in with the first width * height bytes being the luminance data.
         * @param imageWidth  the width of the image
         * @param imageHeight the height of the image
         * @return the rotated bytes
         */
        fun rotate180(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
            val n = imageWidth * imageHeight
            val yuv = ByteArray(n)
            var i = n - 1
            for (j in 0 until n) {
                yuv[i] = data[j]
                i--
            }
            return yuv
        }

        /**
         * Rotate an image by 90 degrees CCW.
         *
         * @param data        the image data, in with the first width * height bytes being the luminance data.
         * @param imageWidth  the width of the image
         * @param imageHeight the height of the image
         * @return the rotated bytes
         */
        fun rotateCCW(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
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
    }

    init {
        require(dataWidth * dataHeight <= data.size) {
            "Image data does not match the resolution. " + dataWidth + "x" + dataHeight + " > " + data.size
        }
    }
}