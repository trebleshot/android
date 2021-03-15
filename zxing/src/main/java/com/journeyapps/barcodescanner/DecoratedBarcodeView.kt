package com.journeyapps.barcodescanner

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.FrameLayout
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.camera.CameraParametersCallback

class DecoratedBarcodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    var barcodeView: BarcodeView

    var viewFinder: ViewfinderView
        private set

    var torchStateListener: TorchStateListener? = null

    fun pause() {
        barcodeView.pause()
    }

    fun pauseAndWait() {
        barcodeView.pauseAndWait()
    }

    fun resume() {
        barcodeView.resume()
    }

    fun decodeSingle(callback: BarcodeCallback) {
        barcodeView.decodeSingle(WrappedCallback(callback))
    }

    fun decodeContinuous(callback: BarcodeCallback) {
        barcodeView.decodeContinuous(WrappedCallback(callback))
    }

    fun setTorchOn() {
        barcodeView.setTorch(true)
        torchStateListener?.onTorchStateChanged(true)
    }

    fun setTorchOff() {
        barcodeView.setTorch(false)
        torchStateListener?.onTorchStateChanged(false)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_FOCUS, KeyEvent.KEYCODE_CAMERA -> return true // Handle these events so they don't launch the Camera app
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                setTorchOff()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                setTorchOn()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun interface TorchStateListener {
        fun onTorchStateChanged(enabled: Boolean)
    }

    private inner class WrappedCallback(private val delegate: BarcodeCallback) : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            delegate.barcodeResult(result)
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
            for (point in resultPoints) {
                viewFinder.addPossibleResultPoint(point)
            }
            delegate.possibleResultPoints(resultPoints)
        }
    }

    init {
        // Get attributes set on view
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.zxing_view)
        val scannerLayout = attributes.getResourceId(
            R.styleable.zxing_view_zxing_scanner_layout, R.layout.zxing_barcode_scanner
        )
        attributes.recycle()
        inflate(context, scannerLayout, this)
        barcodeView = findViewById(R.id.zxing_barcode_surface) ?: throw NullPointerException()
        barcodeView.initializeAttributes(attrs)

        viewFinder = findViewById(R.id.zxing_viewfinder_view) ?: throw NullPointerException()
        viewFinder.cameraPreview = barcodeView
    }
}