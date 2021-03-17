package com.journeyapps.barcodescanner.camera

import android.content.Context
import android.os.Handler
import android.util.Log
import com.journeyapps.barcodescanner.R
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.Util

class CameraInstance(context: Context, private val readyHandler: Handler) {
    private val cameraManager: CameraManager = CameraManager(context)

    private val cameraThreadManager: CameraThreadManager = CameraThreadManager.INSTANCE

    private val previewSize: Size?
        get() = cameraManager.getPreviewSize()

    var cameraClosed = true
        private set

    var displayConfiguration: DisplayConfiguration? = null
        set(configuration) {
            field = configuration
            cameraManager.displayConfiguration = configuration
        }

    private var mainHandler = Handler()

    var open = false
        private set

    var surface: CameraSurface? = null

    fun close() {
        Util.validateMainThread()

        if (open) cameraThreadManager.enqueue {
            try {
                Log.d(TAG, "Closing camera")
                cameraManager.stopPreview()
                cameraManager.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close camera", e)
            }

            cameraClosed = true

            readyHandler.sendEmptyMessage(R.id.zxing_camera_closed)
            cameraThreadManager.decrementInstances()
        } else {
            cameraClosed = true
        }

        open = false
    }

    fun configureCamera() {
        Util.validateMainThread()
        validateOpen()

        cameraThreadManager.enqueue {
            try {
                Log.d(TAG, "Configuring camera")
                cameraManager.configure()
                readyHandler.obtainMessage(R.id.zxing_prewiew_size_ready, previewSize).sendToTarget()
            } catch (e: Exception) {
                notifyError(e)
                Log.e(TAG, "Failed to configure camera", e)
            }
        }
    }

    private fun notifyError(error: Exception) {
        readyHandler.obtainMessage(R.id.zxing_camera_error, error).sendToTarget()
    }

    fun open() {
        Util.validateMainThread()

        open = true
        cameraClosed = false

        cameraThreadManager.incrementAndEnqueue {
            try {
                Log.d(TAG, "Opening camera")
                cameraManager.open()
            } catch (e: Exception) {
                notifyError(e)
                Log.e(TAG, "Failed to open camera", e)
            }
        }
    }

    fun requestPreview(callback: PreviewCallback?) {
        mainHandler.post {
            if (!open) {
                Log.d(TAG, "Camera is closed, not requesting preview")
            } else {
                cameraThreadManager.enqueue { cameraManager.requestPreviewFrame(callback) }
            }
        }
    }

    fun setCameraSettings(cameraSettings: CameraSettings) {
        if (!open) {
            cameraManager.cameraSettings = cameraSettings
        }
    }

    fun setTorch(on: Boolean) {
        Util.validateMainThread()

        if (open) {
            cameraThreadManager.enqueue { cameraManager.setTorch(on) }
        }
    }

    fun startPreview() {
        Util.validateMainThread()
        validateOpen()

        cameraThreadManager.enqueue {
            try {
                Log.d(TAG, "Starting preview")
                cameraManager.setPreviewDisplay(surface)
                cameraManager.startPreview()
            } catch (e: Exception) {
                notifyError(e)
                Log.e(TAG, "Failed to start preview", e)
            }
        }
    }

    private fun validateOpen() {
        check(open) { "CameraInstance is not open" }
    }

    companion object {
        private val TAG = CameraInstance::class.simpleName
    }

    init {
        Util.validateMainThread()
    }
}