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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.database.model.UClient
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

@Singleton
class UserDataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bouncyCastleProvider: BouncyCastleProvider = BouncyCastleProvider()

    private val client by lazy {
        MutableLiveData(clientStatic())
    }

    private val clientNickname by lazy {
        MutableLiveData(clientNicknameStatic())
    }

    private val preferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_NICKNAME || key == KEY_PICTURE_CHECKSUM) {
            client.postValue(clientStatic())
            clientNickname.postValue(clientNicknameStatic())
        }
    }

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context).also {
        it.registerOnSharedPreferenceChangeListener(preferencesListener)
    }

    val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")

    val keyPair: KeyPair by lazy {
        val publicKey = preferences.getString(KEY_PUBLIC_KEY, null)
        val privateKey = preferences.getString(KEY_PRIVATE_KEY, null)

        if (publicKey == null || privateKey == null) {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.genKeyPair()

            preferences.edit()
                .putString(KEY_PUBLIC_KEY, Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT))
                .putString(KEY_PRIVATE_KEY, Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT))
                .apply()

            return@lazy keyPair
        }

        return@lazy KeyPair(
            keyFactory.generatePublic(X509EncodedKeySpec(Base64.decode(publicKey, Base64.DEFAULT))),
            keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.DEFAULT))),
        )
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

                if (clientUid() != commonName) {
                    throw IllegalArgumentException("The client uid changed. The certificate should be regenerated.")
                }

                return@lazy cert
            } catch (e: Exception) {

            }
        }

        // TODO: 1/22/21 Don't forget to change the locale to English in production environments when it is set to
        //  Persian to fix the issue: https://issuetracker.google.com/issues/37095309
        val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)

        nameBuilder.addRDN(BCStyle.CN, clientUid())
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

        preferences.edit()
            .putString(KEY_CERTIFICATE, Base64.encodeToString(cert.encoded, Base64.DEFAULT))
            .apply()

        return@lazy cert
    }

    fun client(): LiveData<UClient> = client

    fun clientStatic() = UClient(
        clientUid(),
        clientNicknameStatic(),
        Build.MANUFACTURER,
        Build.PRODUCT,
        ClientType.Portable,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE,
        Config.VERSION_UPROTOCOL,
        Config.VERSION_UPROTOCOL_MIN,
        System.currentTimeMillis(),
        blocked = false,
        local = true,
        trusted = true,
        certificate,
        clientPictureFile(),
        clientPictureChecksum()
    )

    fun clientNickname(): LiveData<String> = clientNickname

    fun clientNicknameStatic() = preferences.getString(
        KEY_NICKNAME, null
    ) ?: Build.MODEL.toUpperCase(Locale.getDefault())

    fun clientUid() = preferences.getString(KEY_UUID, null) ?: UUID.randomUUID().toString().also {
        preferences.edit()
            .putString(KEY_UUID, it)
            .apply()
    }

    private fun clientPictureFile() = with(context.getFileStreamPath(FILE_CLIENT_PICTURE)) {
        if (isFile) this else null
    }

    private fun clientPictureChecksum() = preferences.getInt(KEY_PICTURE_CHECKSUM, 0)

    companion object {
        private const val KEY_CERTIFICATE = "certificate"

        const val KEY_NICKNAME = "client_nickname"

        const val KEY_PICTURE_CHECKSUM = "picture_checksum"

        private const val KEY_PRIVATE_KEY = "private_key"

        private const val KEY_PUBLIC_KEY = "public_key"

        private const val KEY_UUID = "uuid"

        const val FILE_CLIENT_PICTURE = "localPicture"
    }
}