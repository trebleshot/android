package com.journeyapps.barcodescanner

import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

open class Decoder(protected val reader: Reader) : ResultPointCallback {
    val possibleResultPoints: MutableList<ResultPoint> = ArrayList()

    fun decode(source: LuminanceSource): Result? {
        return decode(toBitmap(source))
    }

    protected fun decode(bitmap: BinaryBitmap?): Result? {
        possibleResultPoints.clear()

        return try {
            if (reader is MultiFormatReader) {
                reader.decodeWithState(bitmap)
            } else {
                reader.decode(bitmap)
            }
        } catch (e: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    override fun foundPossibleResultPoint(point: ResultPoint) {
        possibleResultPoints.add(point)
    }

    protected open fun toBitmap(source: LuminanceSource): BinaryBitmap {
        return BinaryBitmap(HybridBinarizer(source))
    }
}