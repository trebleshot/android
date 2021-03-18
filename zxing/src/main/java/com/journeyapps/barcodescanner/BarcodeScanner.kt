package com.journeyapps.barcodescanner

import android.graphics.Rect
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
            R.id.zxing_preview_proportions -> {
                //sizePreview(message.obj as Size)
                return@Callback true
            }
            R.id.zxing_camera_error -> {
                val error = message.obj as Exception
            }
            R.id.zxing_camera_closed -> {

            }
        }
        false
    }

    private val stateHandler = Handler(stateCallback)

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

        // TODO: 3/18/21 Use an actual crop rect
        val cropRect = Rect()
        val surfaceRect = barcodeView.surfaceRect
        val previewSize = barcodeView.previewSize
        val currentSurfaceSize = barcodeView.currentSurfaceSize

        if (currentSurfaceSize == null || previewSize == null || surfaceRect == null) {
            throw java.lang.IllegalStateException("Not ready yet")
        }

        val cameraSurface = when (barcodeView.cameraView) {
            is SurfaceView -> CameraSurface.create(barcodeView.cameraView.holder)
            is TextureView -> {
                val texture = barcodeView.cameraView.surfaceTexture ?: throw IllegalStateException(
                    "Texture is not ready yet"
                )
                val transform = barcodeView.calculateTextureTransform(
                    Size(barcodeView.cameraView.width, barcodeView.cameraView.height), previewSize
                )
                barcodeView.cameraView.setTransform(transform)
                CameraSurface.create(texture)
            }
            else -> throw IllegalStateException("An undefined surface was requested")
        }

        val cameraInstance = CameraInstance(barcodeView.context, resultHandler, cameraSurface).also {
            it.open()
        }

        decoderSession = DecoderSession(cameraInstance, decoder, resultHandler, cropRect)
    }

    @Synchronized
    fun destroy() {
        if (resumed) throw IllegalStateException("Cannot destroy unless pause() invoked")

        if (!sessionBegan()) {
            Log.d(TAG, "destroy: No session to destroy")
            return
        }

        decoderSession?.let {
            it.destroy()
        }
    }

    @Synchronized
    fun pause() {
        check(sessionBegan()) {"Trying to pause after destroy()"}

        if (!resumed) {
            Log.d(TAG, "pause: Already paused")
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
        check(sessionBegan()) {"Trying to resume before create()"}

        if (!resumed) {
            Log.d(TAG, "resume: Already resumed")
            return
        }

        decoderSession?.let {
            it.cameraInstance.startPreview()
        }

        resumed = true
    }

    private fun sessionBegan() = decoderSession != null

    companion object {
        const val TAG = "BarcodeScanner"
    }
}