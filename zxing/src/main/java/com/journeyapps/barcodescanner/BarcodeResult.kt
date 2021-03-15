package com.journeyapps.barcodescanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.BarcodeFormat.EAN_13
import com.google.zxing.BarcodeFormat.UPC_A
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.ResultPoint

class BarcodeResult(
    var result: Result,
    sourceData: SourceData,
) {
    val bitmap = sourceData.getBitmap(SCALE_FACTOR)

    fun getBitmapWithResultPoints(color: Int): Bitmap {
        val points = result.resultPoints

        if (points != null && points.isNotEmpty()) {
            val barcode = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(barcode).apply {
                drawBitmap(bitmap, 0f, 0f, null)
            }
            val paint = Paint().also {
                it.color = color
            }

            if (points.size == 2) {
                paint.strokeWidth = PREVIEW_LINE_WIDTH

                drawLine(canvas, paint, points[0], points[1])
            } else if (points.size == 4 && (result.barcodeFormat == UPC_A || result.barcodeFormat == EAN_13)) {
                drawLine(canvas, paint, points[0], points[1])
                drawLine(canvas, paint, points[2], points[3])
            } else {
                paint.strokeWidth = PREVIEW_DOT_WIDTH

                for (point in points) {
                    if (point != null) {
                        canvas.drawPoint(point.x / SCALE_FACTOR, point.y / SCALE_FACTOR, paint)
                    }
                }
            }

            return barcode
        }

        return bitmap
    }

    val barcodeFormat: BarcodeFormat
        get() = result.barcodeFormat

    val rawBytes: ByteArray
        get() = result.rawBytes

    val resultMetadata: Map<ResultMetadataType, Any>
        get() = result.resultMetadata

    val resultPoints: Array<ResultPoint>
        get() = result.resultPoints

    val text: String
        get() = result.text

    val timestamp: Long
        get() = result.timestamp

    override fun toString(): String {
        return result.text
    }

    companion object {
        private const val PREVIEW_LINE_WIDTH = 4.0f

        private const val PREVIEW_DOT_WIDTH = 10.0f

        private const val SCALE_FACTOR = 2

        private fun drawLine(canvas: Canvas, paint: Paint, a: ResultPoint?, b: ResultPoint?) {
            if (a != null && b != null) {
                canvas.drawLine(
                    a.x / SCALE_FACTOR,
                    a.y / SCALE_FACTOR,
                    b.x / SCALE_FACTOR,
                    b.y / SCALE_FACTOR,
                    paint
                )
            }
        }
    }
}