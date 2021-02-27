/*
 * Copyright (C) 2010 ZXing authors
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

import android.content.Intent
import com.google.zxing.BarcodeFormat
import java.util.*
import java.util.regex.Pattern

object DecodeFormatManager {
    private val PRODUCT_FORMATS: Set<BarcodeFormat> = EnumSet.of(
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.EAN_13,
        BarcodeFormat.EAN_8,
        BarcodeFormat.RSS_14,
        BarcodeFormat.RSS_EXPANDED
    )

    private val INDUSTRIAL_FORMATS: Set<BarcodeFormat> = EnumSet.of(
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.CODE_128,
        BarcodeFormat.ITF,
        BarcodeFormat.CODABAR
    )

    private val QR_CODE_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.QR_CODE)

    private val DATA_MATRIX_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.DATA_MATRIX)

    private val AZTEC_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.AZTEC)

    private val PDF417_FORMATS: Set<BarcodeFormat> = EnumSet.of(BarcodeFormat.PDF_417)

    private val COMMA_PATTERN = Pattern.compile(",")

    private val ONE_D_FORMATS: MutableSet<BarcodeFormat> = EnumSet.copyOf(PRODUCT_FORMATS)

    private val FORMATS_FOR_MODE = HashMap<String, Set<BarcodeFormat>>().also {
        it[Intents.Scan.ONE_D_MODE] = ONE_D_FORMATS
        it[Intents.Scan.PRODUCT_MODE] = PRODUCT_FORMATS
        it[Intents.Scan.QR_CODE_MODE] = QR_CODE_FORMATS
        it[Intents.Scan.DATA_MATRIX_MODE] = DATA_MATRIX_FORMATS
        it[Intents.Scan.AZTEC_MODE] = AZTEC_FORMATS
        it[Intents.Scan.PDF417_MODE] = PDF417_FORMATS
    }

    fun parseDecodeFormats(intent: Intent): Set<BarcodeFormat>? {
        var scanFormats: Iterable<String>? = null
        val scanFormatsString: CharSequence? = intent.getStringExtra(Intents.Scan.FORMATS)
        if (scanFormatsString != null) {
            scanFormats = Arrays.asList(*COMMA_PATTERN.split(scanFormatsString))
        }
        return parseDecodeFormats(scanFormats, intent.getStringExtra(Intents.Scan.MODE))
    }

    private fun parseDecodeFormats(scanFormats: Iterable<String>?, decodeMode: String?): Set<BarcodeFormat>? {
        if (scanFormats != null) {
            val formats: MutableSet<BarcodeFormat> = EnumSet.noneOf(BarcodeFormat::class.java)
            try {
                for (format in scanFormats) {
                    formats.add(BarcodeFormat.valueOf(format))
                }
                return formats
            } catch (iae: IllegalArgumentException) {
                // ignore it then
            }
        }
        return if (decodeMode != null) {
            FORMATS_FOR_MODE[decodeMode]
        } else null
    }
}