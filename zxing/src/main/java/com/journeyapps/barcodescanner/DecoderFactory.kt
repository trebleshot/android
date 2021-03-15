package com.journeyapps.barcodescanner

import com.google.zxing.DecodeHintType

interface DecoderFactory {
    fun createDecoder(baseHints: Map<DecodeHintType, Any?>): Decoder
}