package com.journeyapps.barcodescanner

import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback

class DecoderResultPointCallback(var decoder: Decoder? = null) : ResultPointCallback {
    override fun foundPossibleResultPoint(point: ResultPoint) {
        decoder?.foundPossibleResultPoint(point)
    }
}