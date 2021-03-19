/*
 * MIT License
 *
 * Copyright (c) 2017 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 * Copyright (c) 2021 Veli Tasalı [me@velitasali.com]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.monora.android.codescanner

import android.content.Context
import android.graphics.Rect
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX
import android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX
import android.view.Surface
import android.view.WindowManager
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Utils {
    private const val MIN_DISTORTION = 0.3f

    private const val MAX_DISTORTION = 3f

    private const val DISTORTION_STEP = 0.1f

    private const val MIN_PREVIEW_PIXELS = 589824

    private const val MIN_FPS = 10000

    private const val MAX_FPS = 30000

    fun findSuitableImageSize(
        parameters: Camera.Parameters,
        frameWidth: Int, frameHeight: Int,
    ): CartesianCoordinate {
        val sizes = parameters.supportedPreviewSizes
        if (sizes != null && sizes.isNotEmpty()) {
            Collections.sort(sizes, CameraSizeComparator())
            val frameRatio = frameWidth.toFloat() / frameHeight.toFloat()
            var distortion = MIN_DISTORTION
            while (distortion <= MAX_DISTORTION) {
                for (size in sizes) {
                    val width = size.width
                    val height = size.height
                    if (width * height >= MIN_PREVIEW_PIXELS &&
                        Math.abs(frameRatio - width.toFloat() / height.toFloat()) <= distortion
                    ) {
                        return CartesianCoordinate(width, height)
                    }
                }
                distortion += DISTORTION_STEP
            }
        }
        val defaultSize =
            parameters.previewSize ?: throw CodeScannerException("Unable to configure camera preview size")
        return CartesianCoordinate(defaultSize.width, defaultSize.height)
    }

    fun configureFpsRange(parameters: Camera.Parameters) {
        val supportedFpsRanges = parameters.supportedPreviewFpsRange
        if (supportedFpsRanges == null || supportedFpsRanges.isEmpty()) {
            return
        }
        Collections.sort(supportedFpsRanges, FpsRangeComparator())
        for (fpsRange in supportedFpsRanges) {
            if (fpsRange[PREVIEW_FPS_MIN_INDEX] >= MIN_FPS &&
                fpsRange[PREVIEW_FPS_MAX_INDEX] <= MAX_FPS
            ) {
                parameters.setPreviewFpsRange(fpsRange[PREVIEW_FPS_MIN_INDEX],
                    fpsRange[PREVIEW_FPS_MAX_INDEX])
                return
            }
        }
    }

    fun configureSceneMode(parameters: Camera.Parameters) {
        if (Camera.Parameters.SCENE_MODE_BARCODE != parameters.sceneMode) {
            val supportedSceneModes = parameters.supportedSceneModes
            if (supportedSceneModes != null &&
                supportedSceneModes.contains(Camera.Parameters.SCENE_MODE_BARCODE)
            ) {
                parameters.sceneMode = Camera.Parameters.SCENE_MODE_BARCODE
            }
        }
    }

    fun configureFocusArea(
        parameters: Camera.Parameters,
        area: Rect, width: Int, height: Int, orientation: Int,
    ) {
        val areas: MutableList<Camera.Area> = ArrayList(1)
        val rotatedArea = area.rotate(
            -orientation.toFloat(),
            width / 2f,
            height / 2f
        ).bound(0, 0, width, height)

        areas.add(Camera.Area(Rect(mapCoordinate(rotatedArea.left, width),
            mapCoordinate(rotatedArea.top, height),
            mapCoordinate(rotatedArea.right, width),
            mapCoordinate(rotatedArea.bottom, height)), 1000))
        if (parameters.maxNumFocusAreas > 0) {
            parameters.focusAreas = areas
        }
        if (parameters.maxNumMeteringAreas > 0) {
            parameters.meteringAreas = areas
        }
    }

    fun configureDefaultFocusArea(
        parameters: Camera.Parameters,
        frameRect: Rect,
        previewSize: CartesianCoordinate,
        viewSize: CartesianCoordinate,
        width: Int,
        height: Int,
        orientation: Int,
    ) {
        val portrait = isPortrait(orientation)
        val rotatedWidth = if (portrait) height else width
        val rotatedHeight = if (portrait) width else height

        configureFocusArea(
            parameters,
            getImageFrameRect(rotatedWidth, rotatedHeight, frameRect, previewSize, viewSize),
            rotatedWidth,
            rotatedHeight,
            orientation
        )
    }

    fun configureDefaultFocusArea(
        parameters: Camera.Parameters,
        decoderWrapper: DecoderWrapper, frameRect: Rect,
    ) {
        val imageSize = decoderWrapper.imageSize

        configureDefaultFocusArea(
            parameters,
            frameRect,
            decoderWrapper.previewSize,
            decoderWrapper.viewSize,
            imageSize.x,
            imageSize.y,
            decoderWrapper.displayOrientation
        )
    }

    fun configureFocusModeForTouch(parameters: Camera.Parameters) {
        if (Camera.Parameters.FOCUS_MODE_AUTO == parameters.focusMode) {
            return
        }
        val focusModes = parameters.supportedFocusModes
        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }
    }

    fun disableAutoFocus(parameters: Camera.Parameters) {
        val focusModes = parameters.supportedFocusModes
        if (focusModes == null || focusModes.isEmpty()) {
            return
        }
        val focusMode = parameters.focusMode
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            if (Camera.Parameters.FOCUS_MODE_FIXED == focusMode) {
                return
            } else {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_FIXED
                return
            }
        }
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            if (Camera.Parameters.FOCUS_MODE_AUTO != focusMode) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            }
        }
    }

    fun setAutoFocusMode(
        parameters: Camera.Parameters,
        autoFocusMode: AutoFocusMode,
    ) {
        val focusModes = parameters.supportedFocusModes
        if (focusModes == null || focusModes.isEmpty()) {
            return
        }
        if (autoFocusMode === AutoFocusMode.CONTINUOUS) {
            if (Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE == parameters.focusMode) {
                return
            }
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                return
            }
        }
        if (Camera.Parameters.FOCUS_MODE_AUTO == parameters.focusMode) {
            return
        }
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }
    }

    fun setFlashMode(
        parameters: Camera.Parameters,
        flashMode: String,
    ) {
        if (flashMode == parameters.flashMode) {
            return
        }
        val flashModes = parameters.supportedFlashModes
        if (flashModes != null && flashModes.contains(flashMode)) {
            parameters.flashMode = flashMode
        }
    }

    fun setZoom(parameters: Camera.Parameters, zoom: Int) {
        if (parameters.isZoomSupported) {
            if (parameters.zoom != zoom) {
                val maxZoom = parameters.maxZoom
                if (zoom <= maxZoom) {
                    parameters.zoom = zoom
                } else {
                    parameters.zoom = maxZoom
                }
            }
        }
    }

    fun getDisplayOrientation(
        context: Context,
        cameraInfo: CameraInfo,
    ): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            ?: throw CodeScannerException("Unable to access window manager")
        val degrees: Int
        val rotation = windowManager.defaultDisplay.rotation
        degrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> if (rotation % 90 == 0) {
                (360 + rotation) % 360
            } else {
                throw CodeScannerException("Invalid display rotation")
            }
        }
        return ((if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) 180 else 360) +
                cameraInfo.orientation - degrees) % 360
    }

    fun isPortrait(orientation: Int): Boolean {
        return orientation == 90 || orientation == 270
    }

    fun getPreviewSize(
        imageWidth: Int, imageHeight: Int,
        frameWidth: Int, frameHeight: Int,
    ): CartesianCoordinate {
        if (imageWidth == frameWidth && imageHeight == frameHeight) {
            return CartesianCoordinate(frameWidth, frameHeight)
        }
        val resultWidth = imageWidth * frameHeight / imageHeight
        return if (resultWidth < frameWidth) {
            CartesianCoordinate(frameWidth, imageHeight * frameWidth / imageWidth)
        } else {
            CartesianCoordinate(resultWidth, frameHeight)
        }
    }

    fun getImageFrameRect(
        imageWidth: Int, imageHeight: Int,
        viewFrameRect: Rect, previewSize: CartesianCoordinate,
        viewSize: CartesianCoordinate,
    ): Rect {
        val previewWidth: Int = previewSize.x
        val previewHeight: Int = previewSize.y
        val viewWidth: Int = viewSize.x
        val viewHeight: Int = viewSize.y
        val wD = (previewWidth - viewWidth) / 2
        val hD = (previewHeight - viewHeight) / 2
        val wR = imageWidth.toFloat() / previewWidth.toFloat()
        val hR = imageHeight.toFloat() / previewHeight.toFloat()

        return Rect(
            max(((viewFrameRect.left + wD) * wR).roundToInt(), 0),
            max(((viewFrameRect.top + hD) * hR).roundToInt(), 0),
            min(((viewFrameRect.right + wD) * wR).roundToInt(), imageWidth),
            min(((viewFrameRect.bottom + hD) * hR).roundToInt(), imageHeight)
        )
    }

    fun rotateYuv(
        source: ByteArray, width: Int, height: Int,
        rotation: Int,
    ): ByteArray {
        if (rotation == 0 || rotation == 360) return source

        require(!(rotation % 90 != 0 || rotation < 0 || rotation > 270)) {
            "Invalid rotation (valid: 0, 90, 180, 270)"
        }

        val output = ByteArray(source.size)
        val frameSize = width * height
        val swap = rotation % 180 != 0
        val flipX = rotation % 270 != 0
        val flipY = rotation >= 180
        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIn = j * width + i
                val uIn = frameSize + (j shr 1) * width + (i and 1.inv())
                val vIn = uIn + 1
                val wOut = if (swap) height else width
                val hOut = if (swap) width else height
                val iSwapped = if (swap) j else i
                val jSwapped = if (swap) i else j
                val iOut = if (flipX) wOut - iSwapped - 1 else iSwapped
                val jOut = if (flipY) hOut - jSwapped - 1 else jSwapped
                val yOut = jOut * wOut + iOut
                val uOut = frameSize + (jOut shr 1) * wOut + (iOut and 1.inv())
                val vOut = uOut + 1
                output[yOut] = (0xff and source[yIn].toInt()).toByte()
                output[uOut] = (0xff and source[uIn].toInt()).toByte()
                output[vOut] = (0xff and source[vIn].toInt()).toByte()
            }
        }
        return output
    }

    @Throws(ReaderException::class)
    fun decodeLuminanceSource(
        reader: MultiFormatReader,
        luminanceSource: LuminanceSource,
    ): Result? {
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(luminanceSource)))
        } catch (e: NotFoundException) {
            reader.decodeWithState(
                BinaryBitmap(HybridBinarizer(luminanceSource.invert())))
        } finally {
            reader.reset()
        }
    }

    private fun mapCoordinate(value: Int, size: Int): Int {
        return 2000 * value / size - 1000
    }

    class SuppressErrorCallback : ErrorCallback {
        override fun onError(error: Exception) {
            // Do nothing
        }
    }

    private class CameraSizeComparator : Comparator<Camera.Size> {
        override fun compare(a: Camera.Size, b: Camera.Size): Int {
            return (b.height * b.width).compareTo(a.height * a.width)
        }
    }

    private class FpsRangeComparator : Comparator<IntArray> {
        override fun compare(a: IntArray, b: IntArray): Int {
            var comparison = b[PREVIEW_FPS_MAX_INDEX].compareTo(a[PREVIEW_FPS_MAX_INDEX])
            if (comparison == 0) {
                comparison = b[PREVIEW_FPS_MIN_INDEX].compareTo(a[PREVIEW_FPS_MIN_INDEX])
            }
            return comparison
        }
    }
}