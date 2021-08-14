/*
 * Copyright (C) 2021 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.monora.uprotocol.client.android.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.util.picturePath
import org.monora.uprotocol.core.protocol.ClientType
import org.monora.uprotocol.core.spec.v1.Config
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.asn1.x500.X500NameBuilder
import org.spongycastle.asn1.x500.style.BCStyle
import org.spongycastle.asn1.x500.style.IETFUtils
import org.spongycastle.cert.X509CertificateHolder
import org.spongycastle.cert.X509v3CertificateBuilder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log

@Singleton
class UserDataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bouncyCastleProvider: BouncyCastleProvider = BouncyCastleProvider()

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_CREDENTIALS_STORE, Context.MODE_PRIVATE)
    }

    val certificate: X509Certificate by lazy {
        val certificateIndex = preferences.getString(KEY_CERTIFICATE, null)

        if (certificateIndex != null) {
            val certificateHolder = X509CertificateHolder(Base64.decode(certificateIndex, Base64.DEFAULT))
            val cert: X509Certificate = JcaX509CertificateConverter().setProvider(bouncyCastleProvider)
                .getCertificate(certificateHolder)

            try {
                val principal = cert.subjectX500Principal
                val x500name = X500Name(principal.name)
                val rdn = x500name.getRDNs(BCStyle.CN)[0]
                val commonName = IETFUtils.valueToString(rdn.first.value)

                if (clientUid != commonName) {
                    throw IllegalArgumentException("The client uid changed. The certificate should be regenerated.")
                }

                return@lazy cert
            } catch (e: Exception) {

            }
        }

        Log.d(TAG, "certificate: Generating new certificate")

        // Avoid crash in Persian or similar locales: https://issuetracker.google.com/issues/37095309
        val defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)

        val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)

        nameBuilder.addRDN(BCStyle.CN, clientUid)
        nameBuilder.addRDN(BCStyle.OU, "uprotocol")
        nameBuilder.addRDN(BCStyle.O, "monora")
        val localDate = LocalDate.now().minusYears(1)
        val notBefore = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val notAfter = localDate.plusYears(10).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val certificateBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            nameBuilder.build(), BigInteger.ONE, Date.from(notBefore), Date.from(notAfter),
            nameBuilder.build(), keyPair.public
        )
        val contentSigner = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider(bouncyCastleProvider).build(keyPair.private)
        val cert = JcaX509CertificateConverter().setProvider(bouncyCastleProvider)
            .getCertificate(certificateBuilder.build(contentSigner))

        preferences.edit {
            putString(KEY_CERTIFICATE, Base64.encodeToString(cert.encoded, Base64.DEFAULT))
        }

        Locale.setDefault(defaultLocale)

        return@lazy cert
    }

    val clientStatic
        get() = UClient(
            clientUid,
            clientNickname,
            Build.MANUFACTURER,
            Build.MODEL,
            ClientType.Portable,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            Config.VERSION_UPROTOCOL,
            Config.VERSION_UPROTOCOL_MIN,
            clientRevisionOfPicture,
            System.currentTimeMillis(),
            blocked = false,
            local = true,
            trusted = true,
            certificate,
        )

    val client by lazy {
        MutableLiveData(clientStatic)
    }

    var clientNickname: String
        get() = preferences.getString(KEY_NICKNAME, null) ?: Build.MODEL.uppercase(Locale.getDefault())
        set(value) {
            preferences.edit {
                putString(KEY_NICKNAME, value)
            }
            client.postValue(clientStatic)
        }

    private var clientRevisionOfPicture: Long
        get() = preferences.getLong(KEY_REVISION_OF_PICTURE, 0)
        set(value) {
            preferences.edit {
                putLong(KEY_REVISION_OF_PICTURE, value)
            }
            client.postValue(clientStatic)
        }

    val clientUid: String
        get() = preferences.getString(KEY_UUID, null) ?: UUID.randomUUID().toString().also {
            preferences.edit {
                putString(KEY_UUID, it)
            }
        }

    private val _hasPicture by lazy {
        MutableLiveData(pictureFile.exists())
    }

    val hasPicture by lazy {
        liveData {
            emitSource(_hasPicture)
        }
    }

    val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")

    val keyPair: KeyPair by lazy {
        val publicKey = preferences.getString(KEY_PUBLIC_KEY, null)
        val privateKey = preferences.getString(KEY_PRIVATE_KEY, null)

        if (publicKey == null || privateKey == null) {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.genKeyPair()

            preferences.edit {
                putString(KEY_PUBLIC_KEY, Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT))
                putString(KEY_PRIVATE_KEY, Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT))
            }

            return@lazy keyPair
        }

        return@lazy KeyPair(
            keyFactory.generatePublic(X509EncodedKeySpec(Base64.decode(publicKey, Base64.DEFAULT))),
            keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.DEFAULT))),
        )
    }

    private val pictureFile by lazy {
        context.getFileStreamPath(clientStatic.picturePath)
    }

    fun deletePicture() {
        context.deleteFile(clientStatic.picturePath)
        notifyPictureChanges()
    }

    fun notifyPictureChanges() {
        clientRevisionOfPicture = System.currentTimeMillis()
        _hasPicture.postValue(pictureFile.exists())
    }

    companion object {
        private const val TAG = "UserDataRepository"

        private const val PREFERENCES_CREDENTIALS_STORE = "credentials_store"

        private const val KEY_CERTIFICATE = "certificate"

        private const val KEY_NICKNAME = "client_nickname"

        private const val KEY_REVISION_OF_PICTURE = "revision_of_picture"

        private const val KEY_PRIVATE_KEY = "private_key"

        private const val KEY_PUBLIC_KEY = "public_key"

        private const val KEY_UUID = "uuid"
    }
}
