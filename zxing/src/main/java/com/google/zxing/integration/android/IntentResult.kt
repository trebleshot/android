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

/**
 *
 * Encapsulates the result of a barcode scan invoked through [IntentIntegrator].
 *
 * @author Sean Owen
 */
class IntentResult @JvmOverloads internal constructor(
    /**
     * @return raw content of barcode
     */
    val contents: String? = null,
    /**
     * @return name of format, like "QR_CODE", "UPC_A". See `BarcodeFormat` for more format names.
     */
    val formatName: String? = null,
    /**
     * @return raw bytes of the barcode content, if applicable, or null otherwise
     */
    val rawBytes: ByteArray? = null,
    /**
     * @return rotation of the image, in degrees, which resulted in a successful scan. May be null.
     */
    val orientation: Int? = null,
    /**
     * @return name of the error correction level used in the barcode, if applicable
     */
    val errorCorrectionLevel: String? = null,
    /**
     * @return path to a temporary file containing the barcode image, if applicable, or null otherwise
     */
    val barcodeImagePath: String? = null
) {
    override fun toString(): String {
        val rawBytesLength = rawBytes?.size ?: 0
        return """Format: $formatName
Contents: $contents
Raw bytes: ($rawBytesLength bytes)
Orientation: $orientation
EC level: $errorCorrectionLevel
Barcode image: $barcodeImagePath
"""
    }
}