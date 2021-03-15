package com.journeyapps.barcodescanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.Reader
import com.google.zxing.common.HybridBinarizer

class InvertedDecoder(reader: Reader) : Decoder(reader) {
    override fun toBitmap(source: LuminanceSource): BinaryBitmap {
        return BinaryBitmap(HybridBinarizer(source.invert()))
    }
}