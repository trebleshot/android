package com.journeyapps.barcodescanner

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback
import com.journeyapps.barcodescanner.camera.CameraInstance
import com.journeyapps.barcodescanner.camera.CameraSettings
import com.journeyapps.barcodescanner.camera.CameraSurface
import java.util.*

class BarcodeScanner(
    private val barcodeView: BarcodeView,
    private val barcodeCallback: BarcodeCallback,
) {
    val cameraSettings = CameraSettings()

    private var decoderFactory: DecoderFactory = DefaultDecoderFactory()

    private var decoderSession: DecoderSession? = null

    private val resultCallback = Handler.Callback { message ->
        when (message.what) {
            R.id.zxing_decode_succeeded -> {
                barcodeCallback.barcodeResult(message.obj as BarcodeResult)
                return@Callback true
            }
            R.id.zxing_decode_failed -> return@Callback true
            R.id.zxing_possible_result_points -> {
                val resultPoints = message.obj as List<ResultPoint>

                for (point in resultPoints) {
                    barcodeView.addPossibleResultPoint(point)
                }

                barcodeCallback.possibleResultPoints(resultPoints)
                return@Callback true
            }
            else -> false
        }
    }

    private var resultHandler = Handler(Looper.getMainLooper(), resultCallback)

    private var resumed = false

    private val stateCallback = Handler.Callback { message ->
        when (message.what) {
            R.id.zxing_prewiew_size_ready -> {
                sizePreview(message.obj as Size)
                return@Callback true
            }
            R.id.zxing_camera_error -> {
                val error = message.obj as Exception

                if (isActive()) {
                    pause()
                }
            }
            R.id.zxing_camera_closed -> {

            }
        }
        false
    }

    private val stateHandler = Handler(stateCallback)


    private fun startPreviewIfReady() {
        val surfaceRect = barcodeView.surfaceRect
        val previewSize = barcodeView.previewSize
        val currentSurfaceSize = barcodeView.currentSurfaceSize

        if (currentSurfaceSize != null && previewSize != null && surfaceRect != null) {
            when (barcodeView.cameraView) {
                is SurfaceView -> if (currentSurfaceSize == Size(surfaceRect.width(), surfaceRect.height())) {
                    startCameraPreview(CameraSurface.create(barcodeView.cameraView.holder))
                }
                is TextureView -> if (barcodeView.cameraView.surfaceTexture != null) {
                    val transform = calculateTextureTransform(
                        Size(barcodeView.cameraView.width, barcodeView.cameraView.height), previewSize
                    )
                    barcodeView.cameraView.setTransform(transform)
                    startCameraPreview(CameraSurface.create(barcodeView.cameraView))
                }
            }
        }
    }

    // demo methods

    @Synchronized
    fun create() {
        val callback = object : ResultPointCallback {
            var actualCallback: ResultPointCallback? = null

            override fun foundPossibleResultPoint(point: ResultPoint) {
                actualCallback?.foundPossibleResultPoint(point)
            }
        }
        val hints = HashMap<DecodeHintType, Any>().also {
            it[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = callback
        }

        val decoder = decoderFactory.createDecoder(hints).also {
            callback.actualCallback = it
        }

        val cameraInstance = CameraInstance(barcodeView.context, resultHandler).also {
            it.open()
        }

        decoderSession = DecoderSession(cameraInstance, decoder, resultHandler)
    }

    @Synchronized
    fun destroy() {
        if (resumed) throw IllegalStateException("Cannot destroy unless pause() invoked")

        if (!sessionBegan()) {
            Log.d(TAG, "destroy: No session to destroy")
            return
        }

        destroy()
    }

    @Synchronized
    fun pause() {
        if (!resumed) {
            Log.d(TAG, "destroy: Already paused")
            return
        }

        try {
            decoderSession?.let {
                it.cameraInstance.close()

                val startTime = System.nanoTime()

                while (!it.cameraInstance.cameraClosed) {
                    // Don't wait for longer than 2 seconds
                    if (System.nanoTime() - startTime > 2e9) break

                    Thread.sleep(1)
                }
            }
        } catch (ignored: InterruptedException) {
        } finally {
            resumed = false
        }
    }

    @Synchronized
    fun resume() {
        if (!resumed) {
            Log.d(TAG, "destroy: Already resumed")
            return
        }

        resumed = true
    }

    private fun sessionBegan() = decoderSession != null

    companion object {
        const val TAG = "BarcodeScanner"
    }
}