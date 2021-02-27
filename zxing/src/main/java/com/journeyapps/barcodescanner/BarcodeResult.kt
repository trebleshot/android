package com.journeyapps.barcodescanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.ResultPoint

/**
 * This contains the result of a barcode scan.
 *
 *
 * This class delegate all read-only fields of [com.google.zxing.Result],
 * and adds a bitmap with scanned barcode.
 */
class BarcodeResult(
    var result: Result,
    protected var sourceData: SourceData
) {
    /**
     * @return Bitmap preview scale factor
     */
    val bitmapScaleFactor = 2

    /**
     * @return [Bitmap] with barcode preview
     * @see getBitmapWithResultPoints
     */
    val bitmap: Bitmap?
        get() = sourceData.getBitmap(bitmapScaleFactor)

    /**
     * @param color Color of result points
     * @return [Bitmap] with result points on it, or plain bitmap, if no result points
     */
    fun getBitmapWithResultPoints(color: Int): Bitmap? {
        val bitmap = bitmap
        var barcode = bitmap
        val points = result.resultPoints
        if (points != null && points.isNotEmpty() && bitmap != null) {
            barcode = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(barcode)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            val paint = Paint()
            paint.color = color
            if (points.size == 2) {
                paint.strokeWidth = PREVIEW_LINE_WIDTH
                drawLine(canvas, paint, points[0], points[1], bitmapScaleFactor)
            } else if (points.size == 4 &&
                (result.barcodeFormat == BarcodeFormat.UPC_A ||
                        result.barcodeFormat == BarcodeFormat.EAN_13)
            ) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], bitmapScaleFactor)
                drawLine(canvas, paint, points[2], points[3], bitmapScaleFactor)
            } else {
                paint.strokeWidth = PREVIEW_DOT_WIDTH
                for (point in points) {
                    if (point != null) {
                        canvas.drawPoint(point.x / bitmapScaleFactor, point.y / bitmapScaleFactor, paint)
                    }
                }
            }
        }
        return barcode
    }

    /**
     * @return raw text encoded by the barcode
     * @see Result.getText
     */
    val text: String
        get() = result.text

    /**
     * @return raw bytes encoded by the barcode, if applicable, otherwise `null`
     * @see Result.getRawBytes
     */
    val rawBytes: ByteArray
        get() = result.rawBytes

    /**
     * @return points related to the barcode in the image. These are typically points
     * identifying finder patterns or the corners of the barcode. The exact meaning is
     * specific to the type of barcode that was decoded.
     * @see Result.getResultPoints
     */
    val resultPoints: Array<ResultPoint>
        get() = result.resultPoints

    /**
     * @return [BarcodeFormat] representing the format of the barcode that was decoded
     * @see Result.getBarcodeFormat
     */
    val barcodeFormat: BarcodeFormat
        get() = result.barcodeFormat

    /**
     * @return [Map] mapping [ResultMetadataType] keys to values. May be
     * `null`. This contains optional metadata about what was detected about the barcode,
     * like orientation.
     * @see Result.getResultMetadata
     */
    val resultMetadata: Map<ResultMetadataType, Any>
        get() = result.resultMetadata

    val timestamp: Long
        get() = result.timestamp

    override fun toString(): String {
        return result.text
    }

    companion object {
        private const val PREVIEW_LINE_WIDTH = 4.0f

        private const val PREVIEW_DOT_WIDTH = 10.0f

        private fun drawLine(canvas: Canvas, paint: Paint, a: ResultPoint?, b: ResultPoint?, scaleFactor: Int) {
            if (a != null && b != null) {
                canvas.drawLine(
                    a.x / scaleFactor,
                    a.y / scaleFactor,
                    b.x / scaleFactor,
                    b.y / scaleFactor,
                    paint
                )
            }
        }
    }
}