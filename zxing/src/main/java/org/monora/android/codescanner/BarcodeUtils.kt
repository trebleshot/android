/*
 * MIT License
 *
 * Copyright (c) 2017 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 * Copyright (c) 2021 Veli Tasalı [me@velitasali.com]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY t.zxing]:org.monora.android.codescanner.*IND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.monora.android.codescanner

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.IntDef
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.util.*

/**
 * Utils for decoding and encoding bar codes.
 */
object BarcodeUtils {
    const val ROTATION_0 = 0

    const val ROTATION_90 = 90

    const val ROTATION_180 = 180

    const val ROTATION_270 = 270

    /**
     * Decode barcode from bitmap.
     *
     * @param bitmap Bitmap.
     * @param hints  Decoder hints.
     * @return Decode result, if barcode was decoded successfully, `null` otherwise.
     * @see DecodeHintType
     */
    @JvmOverloads
    fun decodeBitmap(
        bitmap: Bitmap,
        hints: Map<DecodeHintType?, *>? = null,
    ): Result? {
        Objects.requireNonNull(bitmap)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return decodeRgb(pixels, width, height, hints)
    }

    /**
     * Decode barcode from RGB pixels array.
     *
     * @param pixels Colors in standard Android ARGB format.
     * @param width  Image width.
     * @param height Image height.
     * @param hints  Decoder hints.
     * @return Decode result, if barcode was decoded successfully, `null` otherwise.
     * @see DecodeHintType
     *
     * @see Color
     */
    @JvmOverloads
    fun decodeRgb(
        pixels: IntArray,
        width: Int,
        height: Int,
        hints: Map<DecodeHintType?, *>? = null,
    ): Result? {
        Objects.requireNonNull(pixels)
        val reader = createReader(hints)
        return try {
            Utils.decodeLuminanceSource(reader, RGBLuminanceSource(width, height, pixels))
        } catch (e: ReaderException) {
            null
        }
    }

    /**
     * Decode barcode from YUV pixels array.
     *
     * @param pixels            YUV image data.
     * @param width             Image width.
     * @param height            Image height.
     * @param rotation          Degrees to rotate image before decoding (only 0, 90, 180 or 270 are allowed).
     * @param reverseHorizontal Reverse image horizontally before decoding.
     * @param hints             Decoder hints.
     * @return Decode result, if barcode was decoded successfully, `null` otherwise.
     * @see DecodeHintType
     */
    @JvmOverloads
    fun decodeYuv(
        pixels: ByteArray,
        width: Int,
        height: Int,
        @Rotation rotation: Int = ROTATION_0,
        reverseHorizontal: Boolean = false,
        hints: Map<DecodeHintType?, *>? = null,
    ): Result? {
        Objects.requireNonNull(pixels)
        val rotatedPixels: ByteArray = Utils.rotateYuv(pixels, width, height, rotation)
        val rotatedWidth: Int
        val rotatedHeight: Int
        if (rotation == ROTATION_90 || rotation == ROTATION_270) {
            rotatedWidth = height
            rotatedHeight = width
        } else {
            rotatedWidth = width
            rotatedHeight = height
        }
        val reader = createReader(hints)
        return try {
            Utils.decodeLuminanceSource(reader,
                PlanarYUVLuminanceSource(rotatedPixels, rotatedWidth, rotatedHeight, 0, 0,
                    rotatedWidth, rotatedHeight, reverseHorizontal))
        } catch (e: ReaderException) {
            null
        }
    }

    /**
     * Encode text content.
     *
     * @param content Text to be encoded.
     * @param format  Result barcode format.
     * @param width   Result image width.
     * @param height  Result image height.
     * @param hints   Encoder hints.
     * @return Barcode bit matrix, if it was encoded successfully, `null` otherwise.
     * @see EncodeHintType
     * @see BitMatrix
     */
    @JvmOverloads
    fun encodeBitMatrix(
        content: String,
        format: BarcodeFormat,
        width: Int,
        height: Int,
        hints: Map<EncodeHintType?, *>? = null,
    ): BitMatrix? {
        Objects.requireNonNull(content)
        Objects.requireNonNull(format)
        val writer = MultiFormatWriter()
        return try {
            if (hints != null) {
                writer.encode(content, format, width, height, hints)
            } else {
                writer.encode(content, format, width, height)
            }
        } catch (e: WriterException) {
            null
        }
    }

    /**
     * Encode text content.
     *
     * @param content Text to be encoded.
     * @param format  Result barcode format.
     * @param width   Result image width.
     * @param height  Result image height.
     * @param hints   Encoder hints.
     * @return Barcode bitmap, if it was encoded successfully, `null` otherwise.
     * @see EncodeHintType
     */
    @JvmOverloads
    fun encodeBitmap(
        content: String,
        format: BarcodeFormat,
        width: Int,
        height: Int,
        hints: Map<EncodeHintType?, *>? = null,
    ): Bitmap? {
        val matrix = encodeBitMatrix(content, format, width, height, hints)
        return if (matrix != null) {
            createBitmap(matrix)
        } else {
            null
        }
    }

    /**
     * Create bitmap from bit matrix.
     *
     * @param matrix Bit matrix.
     * @return Bitmap.
     * @see BitMatrix
     * @see Bitmap
     */
    fun createBitmap(matrix: BitMatrix): Bitmap {
        Objects.requireNonNull(matrix)
        val width = matrix.width
        val height = matrix.height
        val length = width * height
        val pixels = IntArray(length)
        for (i in 0 until length) {
            pixels[i] = if (matrix[i % width, i / height]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun createReader(hints: Map<DecodeHintType?, *>?): MultiFormatReader {
        val reader = MultiFormatReader()
        if (hints != null) {
            reader.setHints(hints)
        } else {
            reader.setHints(Collections
                .singletonMap(DecodeHintType.POSSIBLE_FORMATS, CodeScanner.ALL_FORMATS))
        }
        return reader
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270)
    annotation class Rotation
}