package org.monora.uprotocol.client.android.protocol

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.io.DocumentFileStreamDescriptor
import org.monora.uprotocol.client.android.io.FileStreamDescriptor
import org.monora.uprotocol.core.io.StreamDescriptor
import org.monora.uprotocol.core.persistence.PersistenceException
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientAddress
import org.monora.uprotocol.core.protocol.ClientType
import org.monora.uprotocol.core.spec.v1.Config
import org.monora.uprotocol.core.transfer.TransferItem
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
import java.io.*
import java.math.BigInteger
import java.net.InetAddress
import java.security.*
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class MainPersistenceProvider @Inject constructor(
    @ApplicationContext var context: Context,
    var db: AppDatabase,
) : PersistenceProvider {
    private val bouncyCastleProvider: BouncyCastleProvider = BouncyCastleProvider()

    private val keyFactory = KeyFactory.getInstance("RSA")

    private val keyPair: KeyPair
        get() {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val publicKey = sharedPreferences.getString("publicKey", null)
            val privateKey = sharedPreferences.getString("privateKey", null)

            if (publicKey == null || privateKey == null) {
                val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
                keyPairGenerator.initialize(2048)
                val keyPair = keyPairGenerator.genKeyPair()

                sharedPreferences.edit()
                    .putString("publicKey", Base64.encodeToString(keyPair.public.encoded, Base64.DEFAULT))
                    .putString("privateKey", Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT))
                    .apply()

                return keyPair
            }

            val keyFactory = KeyFactory.getInstance("RSA")

            return KeyPair(
                keyFactory.generatePublic(X509EncodedKeySpec(Base64.decode(publicKey, Base64.DEFAULT))),
                keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.DEFAULT))),
            )
        }

    private val _certificate: X509Certificate
        get() {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val certificateIndex = sharedPreferences.getString("certificate", null)

            if (certificateIndex != null) {
                val certificateHolder = X509CertificateHolder(Base64.decode(certificateIndex, Base64.DEFAULT))
                val cert: X509Certificate = JcaX509CertificateConverter().setProvider(bouncyCastleProvider)
                    .getCertificate(certificateHolder)

                try {
                    val principal = cert.subjectX500Principal
                    val x500name = X500Name(principal.name)
                    val rdn = x500name.getRDNs(BCStyle.CN)[0]
                    val commonName = IETFUtils.valueToString(rdn.first.value)

                    if (clientUid != commonName)
                        throw IllegalArgumentException("The client uid changed. The certificate must be regenerated.")

                    return cert
                } catch (e: Exception) {

                }
            }

            // TODO: 1/22/21 Don't forget to change the locale to English in production environments when it is set to
            //  Persian to fix the issue: https://issuetracker.google.com/issues/37095309
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

            sharedPreferences.edit()
                .putString("certificate", Base64.encodeToString(cert.encoded, Base64.DEFAULT))
                .apply()

            return cert
        }

    override fun approveInvalidationOfCredentials(client: Client): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsTransfer(groupId: Long): Boolean = db.transferDao().contains(groupId)

    override fun createClientAddressFor(address: InetAddress, clientUid: String) = UClientAddress(
        address, clientUid, System.currentTimeMillis()
    )

    override fun createClientFor(
        uid: String,
        nickname: String,
        manufacturer: String,
        product: String,
        type: ClientType,
        versionName: String,
        versionCode: Int,
        protocolVersion: Int,
        protocolVersionMin: Int,
    ) = UClient(
        uid, nickname, manufacturer, product, type, versionName, versionCode, protocolVersion, protocolVersionMin,
        System.currentTimeMillis(), blocked = false, local = false, trusted = false, certificate = null
    )

    override fun createTransferItemFor(
        groupId: Long,
        id: Long,
        name: String,
        mimeType: String,
        size: Long,
        directory: String?,
        type: TransferItem.Type,
    ): TransferItem = UTransferItem(groupId, id, name, mimeType, size, directory, uniqueFileName(), type)

    override fun getCertificate(): X509Certificate = _certificate

    override fun getClient() = UClient(
        clientUid, clientNickname, Build.MANUFACTURER, Build.PRODUCT, ClientType.Portable, BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE, Config.VERSION_UPROTOCOL, Config.VERSION_UPROTOCOL_MIN, System.currentTimeMillis(),
        blocked = false, local = true, trusted = true, certificate
    )

    override fun getClientFor(uid: String): UClient? {
        return db.clientDao().get(client.uid)
    }

    // TODO: 2/21/21 Can we get the device name programmatically, on Android TV for instance?
    override fun getClientNickname(): String = PreferenceManager.getDefaultSharedPreferences(context)
        .getString("nickname", null) ?: Build.MODEL.toUpperCase(Locale.getDefault())

    override fun getClientPicture(): ByteArray = getClientPictureFor(client)

    override fun getClientPictureFor(client: Client): ByteArray {
        try {
            return context.openFileInput("photo-" + client?.clientUid.hashCode()).use {
                return@use it.readBytes()
            }
        } catch (e: Exception) {
            Log.d(TAG, "getClientPictureFor: Could not open the picture - ${e.message}")
        }

        return ByteArray(0)
    }

    override fun getClientUid(): String {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var uuid = sharedPreferences.getString("uuid", null)

        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            sharedPreferences.edit()
                .putString("uuid", uuid)
                .apply()
        }

        return uuid
    }

    override fun getDescriptorFor(transferItem: TransferItem): StreamDescriptor = FileStreamDescriptor(
        File(
            context.filesDir, transferItem.itemGroupId.toString() + File.separator + transferItem.itemDirectory
                    + File.separator + transferItem.itemName
        )
    )

    override fun getFirstReceivableItem(groupId: Long): TransferItem? = db.transferItemDao().getReceivable(groupId)

    override fun getNetworkPin(): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var pin = sharedPreferences.getInt("pin", 0)

        if (pin == 0) {
            pin = SecureRandom().nextInt()
            sharedPreferences.edit()
                .putInt("pin", pin)
                .apply()
        }

        return pin
    }

    override fun getPrivateKey(): PrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyPair.private.encoded))

    override fun getPublicKey(): PublicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyPair.public.encoded))

    override fun hasRequestForInvalidationOfCredentials(clientUid: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun loadTransferItem(
        clientUid: String,
        groupId: Long,
        id: Long,
        type: TransferItem.Type,
    ): TransferItem = db.transferItemDao().get(groupId, id, type) ?: throw PersistenceException("Item does not exist")

    override fun openInputStream(descriptor: StreamDescriptor): InputStream {
        if (descriptor is FileStreamDescriptor) {
            return FileInputStream(descriptor.file)
        } else if (descriptor is DocumentFileStreamDescriptor) {
            return context.contentResolver.openInputStream(descriptor.documentFile.getUri()) ?: kotlin.run {
                throw IOException("Supported resource did not open")
            }
        }

        throw RuntimeException("Unsupported descriptor.")
    }

    override fun openOutputStream(descriptor: StreamDescriptor): OutputStream {
        if (descriptor is FileStreamDescriptor) {
            return FileOutputStream(descriptor.file)
        } else if (descriptor is DocumentFileStreamDescriptor) {
            return context.contentResolver.openOutputStream(descriptor.documentFile.getUri()) ?: kotlin.run {
                throw IOException("Supported resource did not open")
            }
        }

        throw RuntimeException("Unsupported descriptor.")
    }

    override fun revokeNetworkPin() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt("pin", 0)
            .apply()
    }

    override fun persist(client: Client, updating: Boolean) {
        if (client is UClient) {
            db.clientDao().insertAll(client)
        } else {
            throw UnsupportedOperationException()
        }
    }

    override fun persist(clientAddress: ClientAddress) {
        if (clientAddress is UClientAddress) {
            db.clientAddressDao().insertAll(clientAddress)
        } else {
            throw UnsupportedOperationException()
        }
    }

    override fun persist(clientUid: String, item: TransferItem) {
        if (item is UTransferItem) {
            db.transferItemDao().update(item)
        } else {
            throw UnsupportedOperationException()
        }
    }

    override fun persist(clientUid: String, itemList: MutableList<out TransferItem>) {
        val usableItemList = ArrayList<UTransferItem>()

        itemList.forEach {
            if (it is UTransferItem) {
                usableItemList.add(it)
            }
        }

        db.transferItemDao().insertAll(usableItemList)
    }

    override fun persistClientPicture(clientUid: String, bitmap: ByteArray?) {
        val fileName = "photo-" + clientUid.hashCode()

        if (bitmap == null) {
            context.deleteFile(fileName)
        } else context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(bitmap)
        }
    }

    override fun saveRequestForInvalidationOfCredentials(clientUid: String) {
        TODO("Not yet implemented")
    }

    override fun setState(clientUid: String, item: TransferItem, state: Int, e: Exception?) {
        if (item is UTransferItem) {
            item.state = state
            db.transferItemDao().insertAll(item)
        } else {
            throw UnsupportedOperationException()
        }
    }

    companion object {
        val TAG = MainPersistenceProvider::class.simpleName
    }
}

fun uniqueFileName() = "." + UUID.randomUUID().toString() + AppConfig.EXT_FILE_PART