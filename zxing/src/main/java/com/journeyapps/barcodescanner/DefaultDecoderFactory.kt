package com.journeyapps.barcodescanner

import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import java.util.*

/**
 * DecoderFactory that creates a MultiFormatReader with specified hints.
 */
class DefaultDecoderFactory(
    private val decodeFormats: Collection<BarcodeFormat?>? = null,
    private val hints: Map<DecodeHintType, Any?>? = null,
    private val characterSet: String? = null,
    private val scanType: Int = 0,
) : DecoderFactory {
    override fun createDecoder(baseHints: Map<DecodeHintType, Any?>?): Decoder {
        val hints: MutableMap<DecodeHintType?, Any?> = EnumMap(DecodeHintType::class.java)

        if (baseHints != null) {
            hints.putAll(baseHints)
        }

        if (this.hints != null) {
            hints.putAll(this.hints)
        }

        if (decodeFormats != null) {
            hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
        }

        if (characterSet != null) {
            hints[DecodeHintType.CHARACTER_SET] = characterSet
        }

        val reader = MultiFormatReader().also { it.setHints(hints) }

        return when (scanType) {
            0 -> Decoder(reader)
            1 -> InvertedDecoder(reader)
            2 -> MixedDecoder(reader)
            else -> Decoder(reader)
        }
    }
}