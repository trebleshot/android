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
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.Camera
import android.hardware.Camera.AutoFocusCallback
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.SurfaceHolder
import androidx.annotation.MainThread
import com.google.zxing.BarcodeFormat
import org.monora.android.codescanner.Utils.configureDefaultFocusArea
import org.monora.android.codescanner.Utils.configureFocusArea
import org.monora.android.codescanner.Utils.configureFocusModeForTouch
import org.monora.android.codescanner.Utils.configureFpsRange
import org.monora.android.codescanner.Utils.configureSceneMode
import org.monora.android.codescanner.Utils.disableAutoFocus
import org.monora.android.codescanner.Utils.findSuitableImageSize
import org.monora.android.codescanner.Utils.getDisplayOrientation
import org.monora.android.codescanner.Utils.getImageFrameRect
import org.monora.android.codescanner.Utils.getPreviewSize
import org.monora.android.codescanner.Utils.isPortrait
import org.monora.android.codescanner.Utils.setAutoFocusMode
import org.monora.android.codescanner.Utils.setFlashMode
import org.monora.android.codescanner.Utils.setZoom
import java.util.*

/**
 * Code scanner.
 *
 * Supports portrait and landscape screen orientations, back and front facing cameras,
 * auto focus and flash light control, touch focus, viewfinder customization.
 *
 * @see CodeScannerView
 * @see BarcodeFormat
 */
class CodeScanner @MainThread constructor(
    private val context: Context,
    private val scannerView: CodeScannerView,
    decodeCallback: DecodeCallback? = null,
    requestedCameraId: Int = CAMERA_BACK,
) {
    private val initializeLock = Any()

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private val surfaceHolder = scannerView.previewView.holder

    private val surfaceCallback: SurfaceHolder.Callback = SurfaceCallback()

    private val previewCallback: Camera.PreviewCallback = PreviewCallback()

    private val touchFocusCallback: AutoFocusCallback = TouchFocusCallback()

    private val safeAutoFocusCallback: AutoFocusCallback = SafeAutoFocusCallback()

    private val safeAutoFocusTask: Runnable = SafeAutoFocusTask()

    private val stopPreviewTask: Runnable = StopPreviewTask()

    private val decoderStateListener = DecoderStateListener()

    /**
     * Whether the auto-focus is enabled.
     */
    @Volatile
    @set:MainThread
    var autoFocusEnabled = DEFAULT_AUTO_FOCUS_ENABLED
        set(value) {
            synchronized(initializeLock) {
                val changed = field != value
                field = value
                scannerView.setAutoFocusEnabled(value)
                val decoderWrapper = decoderWrapper
                if (initialized && isPreviewActive && changed
                    && decoderWrapper != null
                    && decoderWrapper.autoFocusSupported
                ) {
                    setAutoFocusEnabledInternal(value)
                }
            }
        }

    /**
     * Auto-focus mode.
     */
    @Volatile
    @set:MainThread
    var autoFocusMode = DEFAULT_AUTO_FOCUS_MODE
        set(value) {
            synchronized(initializeLock) {
                field = value
                if (initialized && autoFocusEnabled) {
                    setAutoFocusEnabledInternal(true)
                }
            }
        }

    /**
     * The ID of the camera that is in use.
     */
    @Volatile
    @set:MainThread
    private var cameraId = requestedCameraId
        set(value) {
            synchronized(initializeLock) {
                if (field != value) {
                    field = value
                    if (initialized) {
                        val previewActive = isPreviewActive
                        releaseResources()
                        if (previewActive) {
                            initialize()
                        }
                    }
                }
            }
        }

    /**
     * Decode callback.
     */
    @Volatile
    @set:MainThread
    var decodeCallback: DecodeCallback? = decodeCallback
        set(value) {
            synchronized(initializeLock) {
                field = value
                if (initialized) {
                    decoderWrapper?.decoder?.decodeCallback = field
                }
            }
        }

    @Volatile
    private var decoderWrapper: DecoderWrapper? = null

    /**
     * Camera initialization error callback.
     *
     * If not set, an exception will be thrown when error will occur.
     *
     * @see ErrorCallback.SUPPRESS
     */
    @Volatile
    var errorCallback: ErrorCallback? = null

    /**
     * Whether flash-light is enabled.
     */
    @Volatile
    @set:MainThread
    var flashEnabled = DEFAULT_FLASH_ENABLED
        set(value) {
            synchronized(initializeLock) {
                val changed = field != value
                field = value
                scannerView.setFlashEnabled(value)
                val decoderWrapper = decoderWrapper
                if (initialized && isPreviewActive && changed && decoderWrapper != null
                    && decoderWrapper.flashSupported
                ) {
                    setFlashEnabledInternal(value)
                }
            }
        }

    /**
     * Formats that decoder should react to, which is ([ALL_FORMATS] by default).
     *
     * @see BarcodeFormat
     * @see ALL_FORMATS
     * @see ONE_DIMENSIONAL_FORMATS
     * @see TWO_DIMENSIONAL_FORMATS
     */
    @Volatile
    @set:MainThread
    private var formats = DEFAULT_FORMATS
        set(value) {
            synchronized(initializeLock) {
                field = Objects.requireNonNull(value)
                if (initialized) {
                    decoderWrapper?.decoder?.setFormats(value)
                }
            }
        }

    @Volatile
    private var initialization = false

    private var initializationRequested = false

    @Volatile
    private var initialized = false

    val isAutoFocusSupportedOrUnknown: Boolean
        get() {
            val wrapper = decoderWrapper
            return wrapper == null || wrapper.autoFocusSupported
        }

    val isFlashSupportedOrUnknown: Boolean
        get() {
            val wrapper = decoderWrapper
            return wrapper == null || wrapper.flashSupported
        }

    /**
     * Preview is active or not.
     */
    var isPreviewActive = false
        private set

    /**
     * Whether the touch focus is enable at the moment or not.
     */
    var isTouchFocusEnabled = DEFAULT_TOUCH_FOCUS_ENABLED

    private var safeAutoFocusing = false

    /**
     * Auto focus interval in milliseconds for [AutoFocusMode.SAFE] mode which is
     * [DEFAULT_SAFE_AUTO_FOCUS_INTERVAL] by default.
     */
    @Volatile
    var safeAutoFocusInterval = DEFAULT_SAFE_AUTO_FOCUS_INTERVAL

    /**
     * Scan mode.
     */
    @Volatile
    var scanMode = DEFAULT_SCAN_MODE

    private var safeAutoFocusAttemptsCount = 0

    private var safeAutoFocusTaskScheduled = false

    @Volatile
    private var stoppingPreview = false

    private var touchFocusing = false

    private var viewWidth = 0

    private var viewHeight = 0

    /**
     * Camera zoom value.
     */
    @Volatile
    private var zoom = 0
        set(value) {
            require(value >= 0) { "Zoom value must be greater than or equal to zero" }
            synchronized(initializeLock) {
                if (value != field) {
                    field = value
                    if (initialized) {
                        val decoderWrapper = decoderWrapper
                        if (decoderWrapper != null) {
                            val camera = decoderWrapper.camera
                            val parameters = camera.parameters
                            setZoom(parameters, value)
                            camera.parameters = parameters
                        }
                    }
                }
            }
            field = value
        }

    //@RequiresPermission(Manifest.permission.CAMERA)
    @MainThread
    fun startPreview() {
        synchronized(initializeLock) {
            if (!initialized && !initialization) {
                initialize()
                return
            }
        }

        if (!isPreviewActive) {
            surfaceHolder.addCallback(surfaceCallback)
            startPreviewInternal(false)
        }
    }

    /**
     * Stop camera preview.
     */
    @MainThread
    fun stopPreview() {
        if (initialized && isPreviewActive) {
            surfaceHolder.removeCallback(surfaceCallback)
            stopPreviewInternal(false)
        }
    }

    /**
     * Release resources, and stop preview if needed; call this method in [Activity.onPause].
     */
    @MainThread
    fun releaseResources() {
        if (initialized) {
            if (isPreviewActive) {
                stopPreview()
            }
            releaseResourcesInternal()
        }
    }

    fun performTouchFocus(viewFocusArea: Rect) {
        synchronized(initializeLock) {
            if (initialized && isPreviewActive && !touchFocusing) {
                try {
                    autoFocusEnabled = false
                    val decoderWrapper = decoderWrapper
                    if (isPreviewActive && decoderWrapper != null && decoderWrapper.autoFocusSupported) {
                        val imageSize = decoderWrapper.imageSize
                        var imageWidth: Int = imageSize.x
                        var imageHeight: Int = imageSize.y
                        val orientation = decoderWrapper.displayOrientation
                        if (orientation == 90 || orientation == 270) {
                            val width = imageWidth
                            imageWidth = imageHeight
                            imageHeight = width
                        }
                        val imageArea = getImageFrameRect(
                            imageWidth,
                            imageHeight,
                            viewFocusArea,
                            decoderWrapper.previewSize,
                            decoderWrapper.viewSize
                        )
                        val camera = decoderWrapper.camera
                        camera.cancelAutoFocus()
                        val parameters = camera.parameters
                        configureFocusArea(parameters, imageArea, imageWidth, imageHeight,
                            orientation)
                        configureFocusModeForTouch(parameters)
                        camera.parameters = parameters
                        camera.autoFocus(touchFocusCallback)
                        touchFocusing = true
                    }
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun initialize(width: Int = scannerView.width, height: Int = scannerView.height) {
        viewWidth = width
        viewHeight = height
        if (width > 0 && height > 0) {
            initialization = true
            initializationRequested = false
            InitializationThread(width, height).start()
        } else {
            initializationRequested = true
        }
    }

    private fun startPreviewInternal(internal: Boolean) {
        val decoderWrapper = decoderWrapper ?: return
        try {
            val camera = decoderWrapper.camera
            camera.setPreviewCallback(previewCallback)
            camera.setPreviewDisplay(surfaceHolder)
            if (!internal && decoderWrapper.flashSupported && flashEnabled) {
                setFlashEnabledInternal(true)
            }
            camera.startPreview()
            stoppingPreview = false
            isPreviewActive = true
            safeAutoFocusing = false
            safeAutoFocusAttemptsCount = 0
            if (decoderWrapper.autoFocusSupported && autoFocusEnabled) {
                val frameRect: Rect? = scannerView.frameRect
                if (frameRect != null) {
                    val parameters = camera.parameters
                    configureDefaultFocusArea(parameters, decoderWrapper, frameRect)
                    camera.parameters = parameters
                }
                if (autoFocusMode === AutoFocusMode.SAFE) {
                    scheduleSafeAutoFocusTask()
                }
            }
        } catch (ignored: Exception) {
        }
    }

    private fun startPreviewInternalSafe() {
        if (initialized && !isPreviewActive) {
            startPreviewInternal(true)
        }
    }

    private fun stopPreviewInternal(internal: Boolean) {
        val decoderWrapper = decoderWrapper ?: return
        try {
            val camera = decoderWrapper.camera
            camera.cancelAutoFocus()
            val parameters = camera.parameters
            if (!internal && decoderWrapper.flashSupported && flashEnabled) {
                setFlashMode(parameters, Camera.Parameters.FLASH_MODE_OFF)
            }
            camera.parameters = parameters
            camera.setPreviewCallback(null)
            camera.stopPreview()
        } catch (ignored: Exception) {
        }
        stoppingPreview = false
        isPreviewActive = false
        safeAutoFocusing = false
        safeAutoFocusAttemptsCount = 0
    }

    private fun stopPreviewInternalSafe() {
        if (initialized && isPreviewActive) {
            stopPreviewInternal(true)
        }
    }

    private fun releaseResourcesInternal() {
        initialized = false
        initialization = false
        stoppingPreview = false
        isPreviewActive = false
        safeAutoFocusing = false

        decoderWrapper?.let {
            it.release()
            decoderWrapper = null
        }
    }

    private fun setFlashEnabledInternal(flashEnabled: Boolean) {
        try {
            val decoderWrapper = decoderWrapper
            if (decoderWrapper != null) {
                val camera = decoderWrapper.camera
                val parameters = camera.parameters ?: return
                if (flashEnabled) {
                    setFlashMode(parameters, Camera.Parameters.FLASH_MODE_TORCH)
                } else {
                    setFlashMode(parameters, Camera.Parameters.FLASH_MODE_OFF)
                }
                camera.parameters = parameters
            }
        } catch (ignored: Exception) {
        }
    }

    private fun setAutoFocusEnabledInternal(autoFocusEnabled: Boolean) {
        try {
            val decoderWrapper = decoderWrapper
            if (decoderWrapper != null) {
                val camera = decoderWrapper.camera
                camera.cancelAutoFocus()
                touchFocusing = false
                val parameters = camera.parameters
                val autoFocusMode = autoFocusMode
                if (autoFocusEnabled) {
                    setAutoFocusMode(parameters, autoFocusMode)
                } else {
                    disableAutoFocus(parameters)
                }
                if (autoFocusEnabled) {
                    val frameRect: Rect? = scannerView.frameRect
                    if (frameRect != null) {
                        configureDefaultFocusArea(parameters, decoderWrapper, frameRect)
                    }
                }
                camera.parameters = parameters
                if (autoFocusEnabled) {
                    safeAutoFocusAttemptsCount = 0
                    safeAutoFocusing = false
                    if (autoFocusMode === AutoFocusMode.SAFE) {
                        scheduleSafeAutoFocusTask()
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    private fun safeAutoFocusCamera() {
        if (!initialized || !isPreviewActive) {
            return
        }
        val decoderWrapper = decoderWrapper
        if (decoderWrapper == null || !decoderWrapper.autoFocusSupported || !autoFocusEnabled
        ) {
            return
        }
        if (safeAutoFocusing && safeAutoFocusAttemptsCount < SAFE_AUTO_FOCUS_ATTEMPTS_THRESHOLD) {
            safeAutoFocusAttemptsCount++
        } else {
            try {
                val camera = decoderWrapper.camera
                camera.cancelAutoFocus()
                camera.autoFocus(safeAutoFocusCallback)
                safeAutoFocusAttemptsCount = 0
                safeAutoFocusing = true
            } catch (e: Exception) {
                safeAutoFocusing = false
            }
        }
        scheduleSafeAutoFocusTask()
    }

    private fun scheduleSafeAutoFocusTask() {
        if (safeAutoFocusTaskScheduled) {
            return
        }
        safeAutoFocusTaskScheduled = true
        mainThreadHandler.postDelayed(safeAutoFocusTask, safeAutoFocusInterval)
    }

    private inner class ScannerSizeListener : CodeScannerView.SizeListener {
        override fun onSizeChanged(width: Int, height: Int) {
            synchronized(initializeLock) {
                if (width != viewWidth || height != viewHeight) {
                    val previewActive: Boolean = isPreviewActive
                    if (initialized) {
                        releaseResources()
                    }
                    if (previewActive || initializationRequested) {
                        initialize(width, height)
                    }
                }
            }
        }
    }

    private inner class PreviewCallback : Camera.PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            if (!initialized || stoppingPreview || scanMode == ScanMode.PREVIEW) {
                return
            }
            val decoderWrapper = decoderWrapper ?: return
            val decoder: Decoder = decoderWrapper.decoder
            if (decoder.decoderState !== Decoder.State.IDLE) {
                return
            }
            val frameRect: Rect? = scannerView.frameRect
            if (frameRect == null || frameRect.width() < 1 || frameRect.height() < 1) {
                return
            }
            decoder.decode(
                DecodeTask(
                    data,
                    decoderWrapper.imageSize,
                    decoderWrapper.previewSize,
                    decoderWrapper.viewSize,
                    frameRect,
                    decoderWrapper.displayOrientation,
                    decoderWrapper.reverseHorizontal
                )
            )
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            startPreviewInternalSafe()
        }

        override fun surfaceChanged(
            holder: SurfaceHolder, format: Int, width: Int, height: Int,
        ) {
            if (holder.surface == null) {
                isPreviewActive = false
                return
            }
            stopPreviewInternalSafe()
            startPreviewInternalSafe()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            stopPreviewInternalSafe()
        }
    }

    private inner class DecoderStateListener : Decoder.StateListener {
        override fun onStateChanged(state: Decoder.State): Boolean {
            if (state === Decoder.State.DECODED) {
                val scanMode = scanMode

                if (scanMode == ScanMode.PREVIEW) {
                    return false
                } else if (scanMode == ScanMode.SINGLE) {
                    stoppingPreview = true
                    mainThreadHandler.post(stopPreviewTask)
                }
            }
            return true
        }
    }

    private inner class InitializationThread(
        private val width: Int,
        private val height: Int,
    ) : Thread("cs-init") {
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            try {
                initialize()
            } catch (e: Exception) {
                releaseResourcesInternal()
                errorCallback?.onError(e) ?: throw e
            }
        }

        private fun initialize() {
            var camera: Camera? = null
            val cameraInfo = CameraInfo()
            val cameraId = cameraId
            if (cameraId == CAMERA_BACK || cameraId == CAMERA_FRONT) {
                val numberOfCameras = Camera.getNumberOfCameras()
                val facing =
                    if (cameraId == CAMERA_BACK) CAMERA_FACING_BACK else CameraInfo.CAMERA_FACING_FRONT
                for (i in 0 until numberOfCameras) {
                    Camera.getCameraInfo(i, cameraInfo)
                    if (cameraInfo.facing == facing) {
                        camera = Camera.open(i)
                        this@CodeScanner.cameraId = i
                        break
                    }
                }
            } else {
                camera = Camera.open(cameraId)
                Camera.getCameraInfo(cameraId, cameraInfo)
            }
            if (camera == null) {
                throw CodeScannerException("Unable to access camera")
            }
            val parameters = camera.parameters ?: throw CodeScannerException("Unable to configure camera")
            val orientation = getDisplayOrientation(context, cameraInfo)
            val portrait = isPortrait(orientation)
            val imageSize = findSuitableImageSize(
                parameters,
                if (portrait) height else width,
                if (portrait) width else height
            )
            val imageWidth: Int = imageSize.x
            val imageHeight: Int = imageSize.y
            parameters.setPreviewSize(imageWidth, imageHeight)
            parameters.previewFormat = ImageFormat.NV21
            val previewSize = getPreviewSize(
                if (portrait) imageHeight else imageWidth,
                if (portrait) imageWidth else imageHeight,
                width,
                height
            )
            val focusModes = parameters.supportedFocusModes
            val autoFocusSupported = focusModes != null
                    && (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)
                    || focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            if (!autoFocusSupported) {
                autoFocusEnabled = false
            }
            val viewSize = CartesianCoordinate(width, height)
            if (autoFocusSupported && autoFocusEnabled) {
                setAutoFocusMode(parameters, autoFocusMode)
                val frameRect: Rect? = scannerView.frameRect
                if (frameRect != null) {
                    configureDefaultFocusArea(
                        parameters,
                        frameRect,
                        previewSize,
                        viewSize,
                        imageWidth,
                        imageHeight,
                        orientation
                    )
                }
            }
            val flashModes = parameters.supportedFlashModes
            val flashSupported = flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)
            if (!flashSupported) {
                flashEnabled = false
            }
            val zoom = zoom
            if (zoom != 0) {
                setZoom(parameters, zoom)
            }
            configureFpsRange(parameters)
            configureSceneMode(parameters)
            camera.parameters = parameters
            camera.setDisplayOrientation(orientation)
            synchronized(initializeLock) {
                val decoder = Decoder(decoderStateListener, formats, decodeCallback)
                decoderWrapper = DecoderWrapper(
                    camera,
                    cameraInfo,
                    decoder,
                    imageSize,
                    previewSize,
                    viewSize,
                    orientation,
                    autoFocusSupported,
                    flashSupported
                )
                decoder.start()
                initialization = false
                initialized = true
            }
            mainThreadHandler.post(FinishInitializationTask(previewSize))
        }
    }

    private inner class TouchFocusCallback : AutoFocusCallback {
        override fun onAutoFocus(success: Boolean, camera: Camera) {
            touchFocusing = false
        }
    }

    private inner class SafeAutoFocusCallback : AutoFocusCallback {
        override fun onAutoFocus(success: Boolean, camera: Camera) {
            safeAutoFocusing = false
        }
    }

    private inner class SafeAutoFocusTask : Runnable {
        override fun run() {
            safeAutoFocusTaskScheduled = false
            if (autoFocusMode === AutoFocusMode.SAFE) {
                safeAutoFocusCamera()
            }
        }
    }

    private inner class StopPreviewTask : Runnable {
        override fun run() {
            stopPreview()
        }
    }

    private inner class FinishInitializationTask constructor(
        private val previewSize: CartesianCoordinate,
    ) : Runnable {
        override fun run() {
            if (!initialized) {
                return
            }
            scannerView.previewSize = previewSize
            scannerView.setAutoFocusEnabled(autoFocusEnabled)
            scannerView.setFlashEnabled(flashEnabled)
            startPreview()
        }
    }

    companion object {
        /**
         * All supported barcode formats.
         */
        val ALL_FORMATS = listOf(*BarcodeFormat.values())

        /**
         * One dimensional barcode formats.
         */
        val ONE_DIMENSIONAL_FORMATS = listOf(
            BarcodeFormat.CODABAR,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_93,
            BarcodeFormat.CODE_128,
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.ITF,
            BarcodeFormat.RSS_14,
            BarcodeFormat.RSS_EXPANDED,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.UPC_EAN_EXTENSION
        )

        /**
         * Two dimensional barcode formats.
         */
        val TWO_DIMENSIONAL_FORMATS = listOf(
            BarcodeFormat.AZTEC,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.MAXICODE,
            BarcodeFormat.PDF_417,
            BarcodeFormat.QR_CODE
        )

        /**
         * First back-facing camera.
         */
        const val CAMERA_BACK = -1

        /**
         * First front-facing camera.
         */
        const val CAMERA_FRONT = -2

        private val DEFAULT_FORMATS = ALL_FORMATS

        private val DEFAULT_SCAN_MODE = ScanMode.SINGLE

        private val DEFAULT_AUTO_FOCUS_MODE = AutoFocusMode.SAFE

        private const val DEFAULT_AUTO_FOCUS_ENABLED = true

        private const val DEFAULT_TOUCH_FOCUS_ENABLED = true

        private const val DEFAULT_FLASH_ENABLED = false

        private const val DEFAULT_SAFE_AUTO_FOCUS_INTERVAL = 2000L

        private const val SAFE_AUTO_FOCUS_ATTEMPTS_THRESHOLD = 2
    }

    init {
        scannerView.codeScanner = this
        scannerView.sizeListener = ScannerSizeListener()
    }
}