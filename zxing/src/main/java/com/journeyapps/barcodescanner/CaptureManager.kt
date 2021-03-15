package com.journeyapps.barcodescanner

import android.Manifest.permission.CAMERA
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.ResultMetadataType
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.BeepManager
import com.google.zxing.client.android.InactivityTimer
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.CameraPreview.StateListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages barcode scanning for a CaptureActivity. This class may be used to have a custom Activity
 * (e.g. with a customized look and feel, or a different superclass), but not the barcode scanning
 * process itself.
 *
 *
 * This is intended for an Activity that is dedicated to capturing a single barcode and returning
 * it via setResult(). For other use cases, use DefaultBarcodeScannerView or BarcodeView directly.
 *
 *
 * The following is managed by this class:
 * - Orientation lock
 * - InactivityTimer
 * - BeepManager
 * - Initializing from an Intent (via IntentIntegrator)
 * - Setting the result and finishing the Activity when a barcode is scanned
 * - Displaying camera errors
 */
class CaptureManager(private val activity: Activity, private val decoratedBarcodeView: DecoratedBarcodeView) {
    private var orientationLock = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    private var returnBarcodeImagePath = false

    private var destroyed = false

    private val inactivityTimer: InactivityTimer

    private val beepManager = BeepManager(activity)

    private val handler = Handler()

    private var finishWhenClosed = false

    private val stateListener: StateListener = object : StateListener {
        override fun previewSized() {

        }

        override fun previewStarted() {

        }

        override fun previewStopped() {

        }

        override fun cameraError(error: Exception?) {
            displayFrameworkBugMessageAndExit()
        }

        override fun cameraClosed() {
            if (finishWhenClosed) {
                Log.d(TAG, "Camera closed; finishing activity")
                finish()
            }
        }
    }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            decoratedBarcodeView.pause()
            beepManager.playBeepSoundAndVibrate()
            handler.post { returnResult(result) }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    private var askedPermission = false

    /**
     * Perform initialization, according to preferences set in the intent.
     *
     * @param intent             the intent containing the scanning preferences
     * @param savedInstanceState saved state, containing orientation lock
     */
    fun initializeFromIntent(intent: Intent?, savedInstanceState: Bundle?) {
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (savedInstanceState != null) {
            // If the screen was locked and unlocked again, we may start in a different orientation
            // (even one not allowed by the manifest). In this case we restore the orientation we were
            // previously locked to.
            orientationLock =
                savedInstanceState.getInt(SAVED_ORIENTATION_LOCK, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        }
        if (intent != null) {
            // Only lock the orientation if it's not locked to something else yet
            val orientationLocked = intent.getBooleanExtra(Intents.Scan.ORIENTATION_LOCKED, true)
            if (orientationLocked) {
                lockOrientation()
            }
            if (Intents.Scan.ACTION == intent.action) {
                decoratedBarcodeView.initializeFromIntent(intent)
            }
            if (!intent.getBooleanExtra(Intents.Scan.BEEP_ENABLED, true)) {
                beepManager.isBeepEnabled = false
            }
            if (intent.hasExtra(Intents.Scan.TIMEOUT)) {
                val runnable = Runnable { returnResultTimeout() }
                handler.postDelayed(runnable, intent.getLongExtra(Intents.Scan.TIMEOUT, 0L))
            }
            if (intent.getBooleanExtra(Intents.Scan.BARCODE_IMAGE_ENABLED, false)) {
                returnBarcodeImagePath = true
            }
        }
    }

    /**
     * Lock display to current orientation.
     */
    protected fun lockOrientation() {
        // Only get the orientation if it's not locked to one yet.
        if (orientationLock == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            // Adapted from http://stackoverflow.com/a/14565436
            val display = activity.windowManager.defaultDisplay
            val rotation = display.rotation
            val baseOrientation = activity.resources.configuration.orientation
            var orientation = 0
            if (baseOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                orientation = if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }
            } else if (baseOrientation == Configuration.ORIENTATION_PORTRAIT) {
                orientation = if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                }
            }
            orientationLock = orientation
        }
        activity.requestedOrientation = orientationLock
    }

    /**
     * Start decoding.
     */
    fun decode() {
        decoratedBarcodeView.decodeSingle(callback)
    }

    /**
     * Call from Activity#onResume().
     */
    fun onResume() {
        if (Build.VERSION.SDK_INT >= 23) {
            openCameraWithPermission()
        } else {
            decoratedBarcodeView.resume()
        }
        inactivityTimer.start()
    }

    @TargetApi(23)
    private fun openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(activity, CAMERA) == PERMISSION_GRANTED) {
            decoratedBarcodeView.resume()
        } else if (!askedPermission) {
            ActivityCompat.requestPermissions(activity, arrayOf(CAMERA), REQUEST_PERMISSION_CAMERA)
            askedPermission = true
        } else {
            // Wait for permission result
        }
    }

    /**
     * Call from Activity#onRequestPermissionsResult
     *
     * @param requestCode  The request code passed in [ActivityCompat.requestPermissions].
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions
     * which is either [android.content.pm.PackageManager.PERMISSION_GRANTED]
     * or [android.content.pm.PackageManager.PERMISSION_DENIED]. Never null.
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>?, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                // permission was granted
                decoratedBarcodeView.resume()
            } else {
                // TODO: display better error message.
                displayFrameworkBugMessageAndExit()
            }
        }
    }

    /**
     * Call from Activity#onPause().
     */
    fun onPause() {
        inactivityTimer.cancel()
        decoratedBarcodeView.pauseAndWait()
    }

    /**
     * Call from Activity#onDestroy().
     */
    fun onDestroy() {
        destroyed = true
        inactivityTimer.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Call from Activity#onSaveInstanceState().
     *
     * @param outState bundle to preserve objects
     */
    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SAVED_ORIENTATION_LOCK, orientationLock)
    }

    /**
     * Save the barcode image to a temporary file stored in the application's cache, and return its path.
     * Only does so if returnBarcodeImagePath is enabled.
     *
     * @param rawResult the BarcodeResult, must not be null
     * @return the path or null
     */
    private fun getBarcodeImagePath(rawResult: BarcodeResult): String? {
        var barcodeImagePath: String? = null
        if (returnBarcodeImagePath) {
            val bmp = rawResult.bitmap
            try {
                if (bmp == null) throw NullPointerException("Bitmap should not be empty")

                val bitmapFile = File.createTempFile("barcodeimage", ".jpg", activity.cacheDir)
                FileOutputStream(bitmapFile).use {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
                barcodeImagePath = bitmapFile.absolutePath
            } catch (e: IOException) {
                Log.w(TAG, "Unable to create temporary file and store bitmap! $e")
            }
        }
        return barcodeImagePath
    }

    private fun finish() {
        activity.finish()
    }

    protected fun closeAndFinish() {
        if (decoratedBarcodeView.barcodeView.isCameraClosed) {
            finish()
        } else {
            finishWhenClosed = true
        }
        decoratedBarcodeView.pause()
        inactivityTimer.cancel()
    }

    protected fun returnResultTimeout() {
        val intent = Intent(Intents.Scan.ACTION)
        intent.putExtra(Intents.Scan.TIMEOUT, true)
        activity.setResult(Activity.RESULT_CANCELED, intent)
        closeAndFinish()
    }

    protected fun returnResult(rawResult: BarcodeResult) {
        val intent = resultIntent(rawResult, getBarcodeImagePath(rawResult))
        activity.setResult(Activity.RESULT_OK, intent)
        closeAndFinish()
    }

    protected fun displayFrameworkBugMessageAndExit() {
        if (activity.isFinishing || destroyed || finishWhenClosed) {
            return
        }
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(activity.getString(R.string.zxing_app_name))
        builder.setMessage(activity.getString(R.string.zxing_msg_camera_framework_bug))
        builder.setPositiveButton(R.string.zxing_button_ok) { dialog, which -> finish() }
        builder.setOnCancelListener { finish() }
        builder.show()
    }

    companion object {
        private val TAG = CaptureManager::class.simpleName

        private const val SAVED_ORIENTATION_LOCK = "SAVED_ORIENTATION_LOCK"

        const val REQUEST_PERMISSION_CAMERA = 250

        /**
         * Create a intent to return as the Activity result.
         *
         * @param rawResult        the BarcodeResult, must not be null.
         * @param barcodeImagePath a path to an exported file of the Barcode Image, can be null.
         * @return the Intent
         */
        fun resultIntent(rawResult: BarcodeResult, barcodeImagePath: String?): Intent {
            val intent = Intent(Intents.Scan.ACTION)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            intent.putExtra(Intents.Scan.RESULT, rawResult.toString())
            intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.barcodeFormat.toString())
            val rawBytes = rawResult.rawBytes
            if (rawBytes.isNotEmpty()) {
                intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes)
            }
            val metadata = rawResult.resultMetadata
            if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                intent.putExtra(
                    Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                    metadata[ResultMetadataType.UPC_EAN_EXTENSION].toString()
                )
            }
            val orientation = metadata[ResultMetadataType.ORIENTATION] as Number?
            if (orientation != null) {
                intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.toInt())
            }
            val ecLevel = metadata[ResultMetadataType.ERROR_CORRECTION_LEVEL] as String?
            if (ecLevel != null) {
                intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel)
            }
            val byteSegments = metadata[ResultMetadataType.BYTE_SEGMENTS] as Iterable<ByteArray>?
            if (byteSegments != null) {
                for ((i, byteSegment) in byteSegments.withIndex()) {
                    intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment)
                }
            }
            if (barcodeImagePath != null) {
                intent.putExtra(Intents.Scan.RESULT_BARCODE_IMAGE_PATH, barcodeImagePath)
            }
            return intent
        }
    }

    init {
        decoratedBarcodeView.barcodeView.addStateListener(stateListener)
        inactivityTimer = InactivityTimer(activity) {
            Log.d(TAG, "Finishing due to inactivity")
            finish()
        }
    }
}