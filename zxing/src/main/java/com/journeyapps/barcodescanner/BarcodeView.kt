package com.journeyapps.barcodescanner

import android.content.*
import android.os.*
import android.util.AttributeSet
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPoint
import java.util.*

/**
 * A view for scanning barcodes.
 *
 *
 * Two methods MUST be called to manage the state:
 * 1. resume() - initialize the camera and start the preview. Call from the Activity's onResume().
 * 2. pause() - stop the preview and release any resources. Call from the Activity's onPause().
 *
 *
 * Start decoding with decodeSingle() or decodeContinuous(). Stop decoding with stopDecoding().
 *
 * @see CameraPreview for more details on the preview lifecycle.
 */
open class BarcodeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CameraPreview(context, attrs, defStyleAttr) {
    private var decodeMode = DecodeMode.NONE

    private var callback: BarcodeCallback? = null

    private var decoderThread: DecoderThread? = null

    private val resultCallback = Handler.Callback { message ->
        when (message.what) {
            R.id.zxing_decode_succeeded -> {
                val result = message.obj as BarcodeResult
                if (decodeMode != DecodeMode.NONE) {
                    callback?.barcodeResult(result)
                    if (decodeMode == DecodeMode.SINGLE) {
                        stopDecoding()
                    }
                }
                return@Callback true
            }
            R.id.zxing_decode_failed -> {
                // Failed. Next preview is automatically tried.
                return@Callback true
            }
            R.id.zxing_possible_result_points -> {
                val resultPoints = message.obj as List<ResultPoint>
                if (decodeMode != DecodeMode.NONE) {
                    callback?.possibleResultPoints(resultPoints)
                }
                return@Callback true
            }
            else -> false
        }
    }

    var decoderFactory: DecoderFactory = createDefaultDecoderFactory()
        set(value) {
            Util.validateMainThread()
            field = value
            decoderThread?.decoder = createDecoder()
        }

    private var resultHandler = Handler(resultCallback)

    private fun createDecoder(): Decoder {
        val callback = DecoderResultPointCallback()
        val hints = HashMap<DecodeHintType, Any>().also {
            it[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = callback
        }
        val decoder = decoderFactory.createDecoder(hints)
        callback.decoder = decoder
        return decoder
    }

    /**
     * Decode a single barcode, then stop decoding.
     *
     *
     * The callback will only be called on the UI thread.
     *
     * @param callback called with the barcode result, as well as possible ResultPoints
     */
    fun decodeSingle(callback: BarcodeCallback?) {
        decodeMode = DecodeMode.SINGLE
        this.callback = callback
        startDecoderThread()
    }

    /**
     * Continuously decode barcodes. The same barcode may be returned multiple times per second.
     *
     *
     * The callback will only be called on the UI thread.
     *
     * @param callback called with the barcode result, as well as possible ResultPoints
     */
    fun decodeContinuous(callback: BarcodeCallback?) {
        decodeMode = DecodeMode.CONTINUOUS
        this.callback = callback
        startDecoderThread()
    }

    /**
     * Stop decoding, but do not stop the preview.
     */
    fun stopDecoding() {
        decodeMode = DecodeMode.NONE
        callback = null
        stopDecoderThread()
    }

    protected fun createDefaultDecoderFactory(): DecoderFactory {
        return DefaultDecoderFactory()
    }

    private fun startDecoderThread() {
        stopDecoderThread() // To be safe
        if (decodeMode != DecodeMode.NONE && isPreviewActive) {
            // We only start the thread if both:
            // 1. decoding was requested
            // 2. the preview is active
            val cameraInstance = cameraInstance ?: throw NullPointerException("Camera instance should not be null")

            decoderThread = DecoderThread(cameraInstance, createDecoder(), resultHandler).also {
                it.cropRect = previewFramingRect
                it.start()
            }
        }
    }

    override fun previewStarted() {
        super.previewStarted()
        startDecoderThread()
    }

    private fun stopDecoderThread() {
        decoderThread?.let {
            it.stop()
            decoderThread = null
        }
    }

    /**
     * Stops the live preview and decoding.
     *
     * Call from the Activity's onPause() method.
     */
    override fun pause() {
        stopDecoderThread()
        super.pause()
    }

    private enum class DecodeMode {
        NONE, SINGLE, CONTINUOUS
    }
}