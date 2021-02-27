package com.journeyapps.barcodescanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.Reader
import com.google.zxing.common.HybridBinarizer

/**
 * Decoder that performs alternating scans in normal mode and inverted mode.
 */
class MixedDecoder(reader: Reader) : Decoder(reader) {
    private var isInverted = true

    /**
     * Given an image source, convert to a binary bitmap.
     *
     *
     * Override this to use a custom binarizer.
     *
     * @param source the image source
     * @return a BinaryBitmap
     */
    override fun toBitmap(source: LuminanceSource): BinaryBitmap {
        return if (isInverted) {
            isInverted = false
            BinaryBitmap(HybridBinarizer(source.invert()))
        } else {
            isInverted = true
            BinaryBitmap(HybridBinarizer(source))
        }
    }
}