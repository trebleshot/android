package com.journeyapps.barcodescanner

import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.google.zxing.LuminanceSource
import com.google.zxing.Result
import com.journeyapps.barcodescanner.camera.CameraInstance
import com.journeyapps.barcodescanner.camera.PreviewCallback

/**
 *
 */
class DecoderThread(
    private val cameraInstance: CameraInstance,
    var decoder: Decoder,
    private val resultHandler: Handler,
    var cropRect: Rect,
) {
    private val lock = Any()

    private var thread: HandlerThread? = null

    private var handler: Handler? = null

    private var running = false

    private val previewCallback: PreviewCallback = object : PreviewCallback {
        override fun onPreview(sourceData: SourceData?) {
            synchronized(lock) {
                if (running) {
                    // Post to our thread.
                    handler?.obtainMessage(R.id.zxing_decode, sourceData)?.sendToTarget()
                }
            }
        }

        override fun onPreviewError(e: Exception?) {
            synchronized(lock) {
                if (running) {
                    // Post to our thread.
                    handler?.obtainMessage(R.id.zxing_preview_failed)?.sendToTarget()
                }
            }
        }
    }

    private val callback = Handler.Callback { message ->
        if (message.what == R.id.zxing_decode) {
            decode(message.obj as SourceData)
        } else if (message.what == R.id.zxing_preview_failed) {
            // Error already logged. Try again.
            requestNextPreview()
        }
        true
    }

    fun start() {
        Util.validateMainThread()
        thread = HandlerThread(TAG).also {
            it.start()
            handler = Handler(it.looper, callback)
        }

        running = true
        requestNextPreview()
    }

    fun stop() {
        Util.validateMainThread()
        synchronized(lock) {
            running = false
            handler?.removeCallbacksAndMessages(null)
            thread?.quit()
        }
    }

    private fun requestNextPreview() {
        cameraInstance.requestPreview(previewCallback)
    }

    protected fun createSource(sourceData: SourceData): LuminanceSource? {
        return if (cropRect == null) {
            null
        } else {
            sourceData.createSource()
        }
    }

    private fun decode(sourceData: SourceData) {
        val start = System.currentTimeMillis()
        var rawResult: Result? = null
        val decoder = decoder
        sourceData.cropRect = cropRect
        val source = createSource(sourceData)
        if (source != null) {
            rawResult = decoder.decode(source)
        }
        if (rawResult != null) {
            // Don't log the barcode contents for security.
            val end = System.currentTimeMillis()
            Log.d(TAG, "Found barcode in " + (end - start) + " ms")
            if (resultHandler != null) {
                val barcodeResult = BarcodeResult(rawResult, sourceData)
                val message = Message.obtain(resultHandler, R.id.zxing_decode_succeeded, barcodeResult)
                val bundle = Bundle()
                message.data = bundle
                message.sendToTarget()
            }
        } else {
            if (resultHandler != null) {
                val message = Message.obtain(resultHandler, R.id.zxing_decode_failed)
                message.sendToTarget()
            }
        }
        if (resultHandler != null) {
            val resultPoints = decoder.possibleResultPoints
            val message = Message.obtain(resultHandler, R.id.zxing_possible_result_points, resultPoints)
            message.sendToTarget()
        }
        requestNextPreview()
    }

    companion object {
        private val TAG = DecoderThread::class.simpleName
    }

    init {
        Util.validateMainThread()
    }
}