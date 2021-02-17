package org.monora.uprotocol.client.android.database

import android.util.Base64
import androidx.room.TypeConverter
import org.monora.uprotocol.core.protocol.ClientType
import org.spongycastle.cert.X509CertificateHolder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.cert.X509Certificate

class ClientTypeConverter {
    private val bouncyCastleProvider = BouncyCastleProvider()

    @TypeConverter
    fun fromCertificate(value: X509Certificate?): String? {
        if (value == null)
            return null
        return Base64.encodeToString(value.encoded, Base64.DEFAULT)
    }

    @TypeConverter
    fun toCertificate(value: String?): X509Certificate? {
        if (value == null)
            return null

        return JcaX509CertificateConverter().setProvider(bouncyCastleProvider)
            .getCertificate(X509CertificateHolder(Base64.decode(value, Base64.DEFAULT)))
    }

    @TypeConverter
    fun fromType(value: ClientType): String = value.protocolValue

    @TypeConverter
    fun toType(value: String): ClientType = ClientType.from(value)
}