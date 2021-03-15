package com.journeyapps.barcodescanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.Reader
import com.google.zxing.common.HybridBinarizer

class MixedDecoder(reader: Reader) : Decoder(reader) {
    private var inverted = true

    override fun toBitmap(source: LuminanceSource): BinaryBitmap {
        return if (inverted) {
            inverted = false
            BinaryBitmap(HybridBinarizer(source.invert()))
        } else {
            inverted = true
            BinaryBitmap(HybridBinarizer(source))
        }
    }
}