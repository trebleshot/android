/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.zxing.integration.android

import android.annotation.TargetApi
import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.google.zxing.client.android.Intents
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.CaptureActivity
import java.util.*

/**
 * @author Sean Owen
 * @author Fred Lin
 * @author Isaac Potoczny-Jones
 * @author Brad Drehmer
 * @author gcstang
 */
class IntentIntegrator(private val activity: Activity?) {
    private val moreExtras: MutableMap<String?, Any> = HashMap(3)

    private var fragment: Fragment? = null

    private var supportFragment: androidx.fragment.app.Fragment? = null

    private var desiredBarcodeFormats: Collection<String>? = null

    private var captureActivity: Class<*>? = null

    private var requestCode = REQUEST_CODE

    protected val defaultCaptureActivity: Class<*>
        get() = CaptureActivity::class.java

    fun getCaptureActivity(): Class<*>? {
        if (captureActivity == null) {
            captureActivity = defaultCaptureActivity
        }
        return captureActivity
    }

    /**
     * Set the Activity class to use. It can be any activity, but should handle the intent extras
     * as used here.
     *
     * @param captureActivity the class
     * @return this object
     */
    fun setCaptureActivity(captureActivity: Class<*>?): IntentIntegrator {
        this.captureActivity = captureActivity
        return this
    }

    /**
     * Change the request code that is used for the Intent. If it is changed, it is the caller's
     * responsibility to check the request code from the result intent.
     *
     * @param requestCode the new request code
     * @return this
     */
    fun setRequestCode(requestCode: Int): IntentIntegrator {
        require(!(requestCode <= 0 || requestCode > 0x0000ffff)) { "requestCode out of range" }
        this.requestCode = requestCode
        return this
    }

    fun getMoreExtras(): Map<String?, *> {
        return moreExtras
    }

    fun addExtra(key: String?, value: Any): IntentIntegrator {
        moreExtras[key] = value
        return this
    }

    /**
     * Set a prompt to display on the capture screen, instead of using the default.
     *
     * @param prompt the prompt to display
     * @return this object
     */
    fun setPrompt(prompt: String?): IntentIntegrator {
        if (prompt != null) {
            addExtra(Intents.Scan.PROMPT_MESSAGE, prompt)
        }
        return this
    }

    /**
     * By default, the orientation is locked. Set to false to not lock.
     *
     * @param locked true to lock orientation
     * @return this object
     */
    fun setOrientationLocked(locked: Boolean): IntentIntegrator {
        addExtra(Intents.Scan.ORIENTATION_LOCKED, locked)
        return this
    }

    /**
     * Use the specified camera ID.
     *
     * @param cameraId camera ID of the camera to use. A negative value means "no preference".
     * @return this
     */
    fun setCameraId(cameraId: Int): IntentIntegrator {
        if (cameraId >= 0) {
            addExtra(Intents.Scan.CAMERA_ID, cameraId)
        }
        return this
    }

    /**
     * Set to false to disable beep on scan.
     *
     * @param enabled false to disable beep
     * @return this
     */
    fun setBeepEnabled(enabled: Boolean): IntentIntegrator {
        addExtra(Intents.Scan.BEEP_ENABLED, enabled)
        return this
    }

    /**
     * Set to true to enable saving the barcode image and sending its path in the result Intent.
     *
     * @param enabled true to enable barcode image
     * @return this
     */
    fun setBarcodeImageEnabled(enabled: Boolean): IntentIntegrator {
        addExtra(Intents.Scan.BARCODE_IMAGE_ENABLED, enabled)
        return this
    }

    /**
     * Set the desired barcode formats to scan.
     *
     * @param desiredBarcodeFormats names of `BarcodeFormat`s to scan for
     * @return this
     */
    fun setDesiredBarcodeFormats(desiredBarcodeFormats: Collection<String>?): IntentIntegrator {
        this.desiredBarcodeFormats = desiredBarcodeFormats
        return this
    }

    /**
     * Set the desired barcode formats to scan.
     *
     * @param desiredBarcodeFormats names of `BarcodeFormat`s to scan for
     * @return this
     */
    fun setDesiredBarcodeFormats(vararg desiredBarcodeFormats: String): IntentIntegrator {
        this.desiredBarcodeFormats = Arrays.asList(*desiredBarcodeFormats)
        return this
    }

    /**
     * Initiates a scan for all known barcode types with the default camera.
     */
    fun initiateScan() {
        startActivityForResult(createScanIntent(), requestCode)
    }

    /**
     * Initiates a scan for all known barcode types with the default camera.
     * And starts a timer to finish on timeout
     *
     * @param timeout in milliseconds to quit
     * @return Activity.RESULT_CANCELED and true on parameter TIMEOUT.
     */
    fun setTimeout(timeout: Long): IntentIntegrator {
        addExtra(Intents.Scan.TIMEOUT, timeout)
        return this
    }

    /**
     * Create an scan intent with the specified options.
     *
     * @return the intent
     */
    fun createScanIntent(): Intent {
        val intentScan = Intent(activity, getCaptureActivity()).also { it.action = Intents.Scan.ACTION }
        val desiredBarcodeFormats = desiredBarcodeFormats

        // check which types of codes to scan for
        if (desiredBarcodeFormats != null) {
            // set the desired barcode types
            val joinedByComma = StringBuilder()
            for (format in desiredBarcodeFormats) {
                if (joinedByComma.isNotEmpty()) {
                    joinedByComma.append(',')
                }
                joinedByComma.append(format)
            }
            intentScan.putExtra(Intents.Scan.FORMATS, joinedByComma.toString())
        }
        intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intentScan.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        attachMoreExtras(intentScan)
        return intentScan
    }

    /**
     * Initiates a scan, only for a certain set of barcode types, given as strings corresponding
     * to their names in ZXing's `BarcodeFormat` class like "UPC_A". You can supply constants
     * like [.PRODUCT_CODE_TYPES] for example.
     *
     * @param desiredBarcodeFormats names of `BarcodeFormat`s to scan for
     */
    fun initiateScan(desiredBarcodeFormats: Collection<String>?) {
        setDesiredBarcodeFormats(desiredBarcodeFormats)
        initiateScan()
    }

    /**
     * Start an activity. This method is defined to allow different methods of activity starting for
     * newer versions of Android and for compatibility library.
     *
     * @param intent Intent to start.
     * @param code   Request code for the activity
     * @see android.app.Activity.startActivityForResult
     * @see android.app.Fragment.startActivityForResult
     */
    protected fun startActivityForResult(intent: Intent?, code: Int) {
        val fragment = fragment
        val supportFragment = supportFragment
        when {
            fragment != null -> {
                fragment.startActivityForResult(intent, code)
            }
            supportFragment != null -> {
                supportFragment.startActivityForResult(intent, code)
            }
            else -> {
                activity?.startActivityForResult(intent, code)
            }
        }
    }

    protected fun startActivity(intent: Intent?) {
        val fragment = fragment
        val supportFragment = supportFragment
        when {
            fragment != null -> fragment.startActivity(intent)
            supportFragment != null -> supportFragment.startActivity(intent)
            else -> activity?.startActivity(intent)
        }
    }

    private fun attachMoreExtras(intent: Intent) {
        for ((key, value) in moreExtras) {
            // Kind of hacky
            if (value is Int) {
                intent.putExtra(key, value)
            } else if (value is Long) {
                intent.putExtra(key, value)
            } else if (value is Boolean) {
                intent.putExtra(key, value)
            } else if (value is Double) {
                intent.putExtra(key, value)
            } else if (value is Float) {
                intent.putExtra(key, value)
            } else if (value is Bundle) {
                intent.putExtra(key, value)
            } else {
                intent.putExtra(key, value.toString())
            }
        }
    }

    companion object {
        const val REQUEST_CODE = 0x0000c0de // Only use bottom 16 bits

        // Product Codes
        const val UPC_A = "UPC_A"

        // supported barcode formats
        const val UPC_E = "UPC_E"
        const val EAN_8 = "EAN_8"
        const val EAN_13 = "EAN_13"
        const val RSS_14 = "RSS_14"

        // Other 1D
        const val CODE_39 = "CODE_39"
        const val CODE_93 = "CODE_93"
        const val CODE_128 = "CODE_128"
        const val ITF = "ITF"
        const val RSS_EXPANDED = "RSS_EXPANDED"

        // 2D
        const val QR_CODE = "QR_CODE"
        const val DATA_MATRIX = "DATA_MATRIX"
        const val PDF_417 = "PDF_417"
        val PRODUCT_CODE_TYPES: Collection<String> = list(UPC_A, UPC_E, EAN_8, EAN_13, RSS_14)
        val ONE_D_CODE_TYPES: Collection<String> = list(
            UPC_A, UPC_E, EAN_8, EAN_13, RSS_14, CODE_39, CODE_93, CODE_128,
            ITF, RSS_14, RSS_EXPANDED
        )
        val ALL_CODE_TYPES: Collection<String>? = null

        private val TAG = IntentIntegrator::class.simpleName

        /**
         * @param fragment [Fragment] invoking the integration.
         * [.startActivityForResult] will be called on the [Fragment] instead
         * of an [Activity]
         * @return generated object
         */
        fun forSupportFragment(fragment: androidx.fragment.app.Fragment): IntentIntegrator {
            val integrator = IntentIntegrator(fragment.activity)
            integrator.supportFragment = fragment
            return integrator
        }

        /**
         * @param fragment [Fragment] invoking the integration.
         * [.startActivityForResult] will be called on the [Fragment] instead
         * of an [Activity]
         * @return generated object
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        fun forFragment(fragment: Fragment): IntentIntegrator {
            val integrator = IntentIntegrator(fragment.activity)
            integrator.fragment = fragment
            return integrator
        }

        /**
         *
         * Call this from your [Activity]'s
         * [Activity.onActivityResult] method.
         *
         *
         * This checks that the requestCode is equal to the default REQUEST_CODE.
         *
         * @param requestCode request code from `onActivityResult()`
         * @param resultCode  result code from `onActivityResult()`
         * @param intent      [Intent] from `onActivityResult()`
         * @return null if the event handled here was not related to this class, or
         * else an [IntentResult] containing the result of the scan. If the user cancelled scanning,
         * the fields will be null.
         */
        fun parseActivityResult(requestCode: Int, resultCode: Int, intent: Intent): IntentResult? {
            return if (requestCode == REQUEST_CODE) {
                parseActivityResult(resultCode, intent)
            } else null
        }

        /**
         * Parse activity result, without checking the request code.
         *
         * @param resultCode result code from `onActivityResult()`
         * @param intent     [Intent] from `onActivityResult()`
         * @return an [IntentResult] containing the result of the scan. If the user cancelled scanning,
         * the fields will be null.
         */
        fun parseActivityResult(resultCode: Int, intent: Intent): IntentResult {
            if (resultCode == Activity.RESULT_OK) {
                val contents = intent.getStringExtra(Intents.Scan.RESULT)
                val formatName = intent.getStringExtra(Intents.Scan.RESULT_FORMAT)
                val rawBytes = intent.getByteArrayExtra(Intents.Scan.RESULT_BYTES)
                val intentOrientation = intent.getIntExtra(Intents.Scan.RESULT_ORIENTATION, Int.MIN_VALUE)
                val orientation = if (intentOrientation == Int.MIN_VALUE) null else intentOrientation
                val errorCorrectionLevel = intent.getStringExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL)
                val barcodeImagePath = intent.getStringExtra(Intents.Scan.RESULT_BARCODE_IMAGE_PATH)
                return IntentResult(
                    contents,
                    formatName,
                    rawBytes,
                    orientation,
                    errorCorrectionLevel,
                    barcodeImagePath
                )
            }
            return IntentResult()
        }

        private fun list(vararg values: String): List<String> {
            return Collections.unmodifiableList(Arrays.asList(*values))
        }
    }
}