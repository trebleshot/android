package com.journeyapps.barcodescanner

import android.content.*
import android.os.*
import android.util.AttributeSet
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPoint
import java.util.*

open class BarcodeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CameraPreview(context, attrs, defStyleAttr) {
    private var barcodeCallback: BarcodeCallback? = null

    var decoderFactory: DecoderFactory = createDefaultDecoderFactory()
        set(value) {
            Util.validateMainThread()
            field = value
            decoderThreadManager?.decoder = createDecoder()
        }

    private var decodeMode = DecodeMode.NONE

    private var decoderThreadManager: DecoderThreadManager? = null

    private val resultCallback = Handler.Callback { message ->
        when (message.what) {
            R.id.zxing_decode_succeeded -> {
                val result = message.obj as BarcodeResult
                if (decodeMode != DecodeMode.NONE) {
                    barcodeCallback?.barcodeResult(result)
                    if (decodeMode == DecodeMode.SINGLE) {
                        stopDecoding()
                    }
                }
                return@Callback true
            }
            R.id.zxing_decode_failed -> {
                return@Callback true
            }
            R.id.zxing_possible_result_points -> {
                val resultPoints = message.obj as List<ResultPoint>
                if (decodeMode != DecodeMode.NONE) {
                    barcodeCallback?.possibleResultPoints(resultPoints)
                }
                return@Callback true
            }
            else -> false
        }
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

    protected fun createDefaultDecoderFactory(): DecoderFactory {
        return DefaultDecoderFactory()
    }

    fun decodeSingle(callback: BarcodeCallback?) {
        decodeMode = DecodeMode.SINGLE
        barcodeCallback = callback

        startDecoderThread()
    }

    fun decodeContinuous(callback: BarcodeCallback?) {
        decodeMode = DecodeMode.CONTINUOUS
        barcodeCallback = callback

        startDecoderThread()
    }

    override fun pause() {
        stopDecoderThread()
        super.pause()
    }

    override fun previewStarted() {
        super.previewStarted()
        startDecoderThread()
    }

    private fun startDecoderThread() {
        stopDecoderThread()

        if (decodeMode != DecodeMode.NONE && previewActive) {
            val cameraInstance = cameraInstance ?: throw NullPointerException("Camera instance should not be null")

            decoderThreadManager = DecoderThreadManager(
                cameraInstance,
                createDecoder(),
                resultHandler,
                previewFramingRect!!
            ).also {
                it.start()
            }
        }
    }

    fun stopDecoding() {
        decodeMode = DecodeMode.NONE
        barcodeCallback = null

        stopDecoderThread()
    }

    private fun stopDecoderThread() {
        decoderThreadManager?.let {
            it.stop()
            decoderThreadManager = null
        }
    }

    private enum class DecodeMode {
        NONE, SINGLE, CONTINUOUS
    }
}