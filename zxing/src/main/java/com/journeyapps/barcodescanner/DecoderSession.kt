package com.journeyapps.barcodescanner

import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.google.zxing.LuminanceSource
import com.journeyapps.barcodescanner.camera.CameraInstance
import com.journeyapps.barcodescanner.camera.PreviewCallback

class DecoderSession(
    val cameraInstance: CameraInstance,
    private val decoder: Decoder,
    private val resultHandler: Handler,
    private var cropRect: Rect,
) {
    private val lock = Any()

    private val callback = Handler.Callback { message ->
        if (message.what == R.id.zxing_decode) {
            decode(message.obj as SourceData)
        } else if (message.what == R.id.zxing_preview_failed) {
            requestNextPreview()
        }
        true
    }

    private val handlerThread by lazy {
        HandlerThread(TAG).apply {
            start()
        }
    }

    private val handler by lazy {
        Handler(handlerThread.looper, callback)
    }

    private val previewCallback = object : PreviewCallback {
        override fun onPreview(sourceData: SourceData?) {
            synchronized(lock) {
                if (handlerThread.isAlive) {
                    handler.obtainMessage(R.id.zxing_decode, sourceData).sendToTarget()
                }
            }
        }

        override fun onPreviewError(e: Exception?) {
            synchronized(lock) {
                if (handlerThread.isAlive) {
                    handler.obtainMessage(R.id.zxing_preview_failed).sendToTarget()
                }
            }
        }
    }

    private fun createSource(sourceData: SourceData): LuminanceSource {
        return sourceData.createSource()
    }

    private fun decode(sourceData: SourceData) {
        sourceData.cropRect = cropRect

        val start = System.currentTimeMillis()
        val source = createSource(sourceData)
        val rawResult = decoder.decode(source)

        if (rawResult != null) {
            val end = System.currentTimeMillis()
            Log.d(TAG, "Found barcode in " + (end - start) + " ms")
            val barcodeResult = BarcodeResult(rawResult, sourceData)

            Message.obtain(resultHandler, R.id.zxing_decode_succeeded, barcodeResult).sendToTarget()
        } else {
            Message.obtain(resultHandler, R.id.zxing_decode_failed).sendToTarget()
        }

        Message.obtain(resultHandler, R.id.zxing_possible_result_points, decoder.possibleResultPoints).sendToTarget()

        requestNextPreview()
    }

    private fun destroy() {
        synchronized(lock) {
            handler.removeCallbacksAndMessages(null)
            handlerThread.quit()
        }
    }

    private fun requestNextPreview() {
        cameraInstance.requestPreview(previewCallback)
    }

    fun start() {
        requestNextPreview()
    }

    companion object {
        private val TAG = DecoderSession::class.simpleName
    }

    init {
        Util.validateMainThread()
    }
}