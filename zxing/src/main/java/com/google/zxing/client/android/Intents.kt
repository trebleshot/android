/*
 * Copyright (C) 2008 ZXing authors
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
package com.google.zxing.client.android

/**
 * This class provides the constants to use when sending an Intent to Barcode Scanner.
 * These strings are effectively API and cannot be changed.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
class Intents private constructor() {
    object Scan {
        /**
         * Send this intent to open the Barcodes app in scanning mode, find a barcode, and return
         * the results.
         */
        const val ACTION = "com.google.zxing.client.android.SCAN"

        /**
         * By default, sending this will decode all barcodes that we understand. However it
         * may be useful to limit scanning to certain formats. Use
         * [android.content.Intent.putExtra] with one of the values below.
         *
         *
         * Setting this is effectively shorthand for setting explicit formats with [.FORMATS].
         * It is overridden by that setting.
         */
        const val MODE = "SCAN_MODE"

        /**
         * Decode only UPC and EAN barcodes. This is the right choice for shopping apps which get
         * prices, reviews, etc. for products.
         */
        const val PRODUCT_MODE = "PRODUCT_MODE"

        /**
         * Decode only 1D barcodes.
         */
        const val ONE_D_MODE = "ONE_D_MODE"

        /**
         * Decode only QR codes.
         */
        const val QR_CODE_MODE = "QR_CODE_MODE"

        /**
         * Decode only Data Matrix codes.
         */
        const val DATA_MATRIX_MODE = "DATA_MATRIX_MODE"

        /**
         * Decode only Aztec.
         */
        const val AZTEC_MODE = "AZTEC_MODE"

        /**
         * Decode only PDF417.
         */
        const val PDF417_MODE = "PDF417_MODE"

        /**
         * Comma-separated list of formats to scan for. The values must match the names of
         * [com.google.zxing.BarcodeFormat]s, e.g. [com.google.zxing.BarcodeFormat.EAN_13].
         * Example: "EAN_13,EAN_8,QR_CODE". This overrides [.MODE].
         */
        const val FORMATS = "SCAN_FORMATS"

        /**
         * Optional parameter to specify the id of the camera from which to recognize barcodes.
         * Overrides the default camera that would otherwise would have been selected.
         * If provided, should be an int.
         */
        const val CAMERA_ID = "SCAN_CAMERA_ID"

        /**
         * @see com.google.zxing.DecodeHintType.CHARACTER_SET
         */
        const val CHARACTER_SET = "CHARACTER_SET"

        /**
         * Set to false to disable beep. Defaults to true.
         */
        const val BEEP_ENABLED = "BEEP_ENABLED"

        /**
         * Set to true to return a path to the barcode's image as it was captured. Defaults to false.
         */
        const val BARCODE_IMAGE_ENABLED = "BARCODE_IMAGE_ENABLED"

        /**
         * Set the time to finish the scan screen.
         */
        const val TIMEOUT = "TIMEOUT"

        /**
         * Whether or not the orientation should be locked when the activity is first started.
         * Defaults to true.
         */
        const val ORIENTATION_LOCKED = "SCAN_ORIENTATION_LOCKED"

        /**
         * Prompt to show on-screen when scanning by intent. Specified as a [String].
         */
        const val PROMPT_MESSAGE = "PROMPT_MESSAGE"

        /**
         * If a barcode is found, Barcodes returns [android.app.Activity.RESULT_OK] to
         * [android.app.Activity.onActivityResult]
         * of the app which requested the scan via
         * [android.app.Activity.startActivityForResult]
         * The barcodes contents can be retrieved with
         * [android.content.Intent.getStringExtra].
         * If the user presses Back, the result code will be [android.app.Activity.RESULT_CANCELED].
         */
        const val RESULT = "SCAN_RESULT"

        /**
         * Call [android.content.Intent.getStringExtra] with [.RESULT_FORMAT]
         * to determine which barcode format was found.
         * See [com.google.zxing.BarcodeFormat] for possible values.
         */
        const val RESULT_FORMAT = "SCAN_RESULT_FORMAT"

        /**
         * Call [android.content.Intent.getStringExtra] with [.RESULT_UPC_EAN_EXTENSION]
         * to return the content of any UPC extension barcode that was also found. Only applicable
         * to [com.google.zxing.BarcodeFormat.UPC_A] and [com.google.zxing.BarcodeFormat.EAN_13]
         * formats.
         */
        const val RESULT_UPC_EAN_EXTENSION = "SCAN_RESULT_UPC_EAN_EXTENSION"

        /**
         * Call [android.content.Intent.getByteArrayExtra] with [.RESULT_BYTES]
         * to get a `byte[]` of raw bytes in the barcode, if available.
         */
        const val RESULT_BYTES = "SCAN_RESULT_BYTES"

        /**
         * Key for the value of [com.google.zxing.ResultMetadataType.ORIENTATION], if available.
         * Call [android.content.Intent.getIntArrayExtra] with [.RESULT_ORIENTATION].
         */
        const val RESULT_ORIENTATION = "SCAN_RESULT_ORIENTATION"

        /**
         * Key for the value of [com.google.zxing.ResultMetadataType.ERROR_CORRECTION_LEVEL], if available.
         * Call [android.content.Intent.getStringExtra] with [.RESULT_ERROR_CORRECTION_LEVEL].
         */
        const val RESULT_ERROR_CORRECTION_LEVEL = "SCAN_RESULT_ERROR_CORRECTION_LEVEL"

        /**
         * Prefix for keys that map to the values of [com.google.zxing.ResultMetadataType.BYTE_SEGMENTS],
         * if available. The actual values will be set under a series of keys formed by adding 0, 1, 2, ...
         * to this prefix. So the first byte segment is under key "SCAN_RESULT_BYTE_SEGMENTS_0" for example.
         * Call [android.content.Intent.getByteArrayExtra] with these keys.
         */
        const val RESULT_BYTE_SEGMENTS_PREFIX = "SCAN_RESULT_BYTE_SEGMENTS_"

        /**
         * Call [android.content.Intent.getStringExtra] with [.RESULT_BARCODE_IMAGE_PATH]
         * to get a `String` path to a cropped and compressed png file of the barcode's image
         * as it was displayed. Only available if
         * [com.google.zxing.integration.android.IntentIntegrator.setBarcodeImageEnabled]
         * is called with true.
         */
        const val RESULT_BARCODE_IMAGE_PATH = "SCAN_RESULT_IMAGE_PATH"

        /**
         * Define the scan type.
         */
        const val SCAN_TYPE = "SCAN_TYPE"

        /**
         * Scan normal barcodes white on black
         */
        const val NORMAL_SCAN = 0

        /**
         * The scan should be inverted. White becomes black, black becomes white.
         */
        const val INVERTED_SCAN = 1

        /**
         * Scan alternating inverted and normal barcodes.
         */
        const val MIXED_SCAN = 2
    }
}