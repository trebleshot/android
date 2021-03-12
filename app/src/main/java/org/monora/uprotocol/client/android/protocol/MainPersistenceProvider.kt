package org.monora.uprotocol.client.android.protocol

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.data.UserDataRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.io.DocumentFileStreamDescriptor
import org.monora.uprotocol.client.android.io.FileStreamDescriptor
import org.monora.uprotocol.client.android.util.Graphics
import org.monora.uprotocol.core.io.StreamDescriptor
import org.monora.uprotocol.core.persistence.PersistenceException
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientAddress
import org.monora.uprotocol.core.protocol.ClientType
import org.monora.uprotocol.core.transfer.TransferItem
import java.io.*
import java.net.InetAddress
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class MainPersistenceProvider @Inject constructor(
    @ApplicationContext val context: Context,
    private val clientRepository: ClientRepository,
    private val userDataRepository: UserDataRepository,
    private val transferRepository: TransferRepository,
) : PersistenceProvider {
    override fun approveInvalidationOfCredentials(client: Client): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsTransfer(groupId: Long): Boolean = transferRepository.containsTransfer(groupId)

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

    override fun getCertificate(): X509Certificate = userDataRepository.certificate

    override fun getClient(): UClient = userDataRepository.clientStatic()

    override fun getClientFor(uid: String): UClient? {
        return clientRepository.get(uid)
    }

    override fun getClientNickname(): String = userDataRepository.clientNicknameStatic()

    override fun getClientUid(): String = userDataRepository.clientUid()

    override fun getDescriptorFor(transferItem: TransferItem): StreamDescriptor = FileStreamDescriptor(
        File(
            context.filesDir,
            transferItem.itemGroupId.toString() + File.separator + transferItem.itemDirectory
                    + File.separator + transferItem.itemName
        )
    )

    override fun getFirstReceivableItem(groupId: Long) = transferRepository.getReceivable(groupId)

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

    override fun getPrivateKey(): PrivateKey = userDataRepository.keyFactory.generatePrivate(
        PKCS8EncodedKeySpec(userDataRepository.keyPair.private.encoded)
    )

    override fun getPublicKey(): PublicKey = userDataRepository.keyFactory.generatePublic(
        X509EncodedKeySpec(userDataRepository.keyPair.public.encoded)
    )

    override fun hasRequestForInvalidationOfCredentials(clientUid: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun loadTransferItem(
        clientUid: String,
        groupId: Long,
        id: Long,
        type: TransferItem.Type,
    ): TransferItem = transferRepository.getTransferItem(
        groupId, id, type
    ) ?: throw PersistenceException("Item does not exist")

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

    override fun persist(client: Client, updating: Boolean) {
        if (client is UClient) {
            if (updating) {
                clientRepository.update(client)
            } else {
                clientRepository.insert(client)
            }
        } else {
            throw UnsupportedOperationException()
        }
    }

    override fun persist(clientAddress: ClientAddress) {
        if (clientAddress is UClientAddress) {
            clientRepository.insert(clientAddress)
        } else {
            throw UnsupportedOperationException()
        }
    }

    override fun persist(clientUid: String, item: TransferItem) {
        if (item is UTransferItem) {
            transferRepository.update(item)
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

        transferRepository.insert(usableItemList)
    }

    override fun persistClientPicture(client: Client, data: ByteArray?, checksum: Int) {
        Graphics.saveClientPicture(context, clientRepository, client, data, checksum)
    }

    override fun revokeNetworkPin() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt("pin", 0)
            .apply()
    }

    override fun saveRequestForInvalidationOfCredentials(clientUid: String) {
        TODO("Not yet implemented")
    }

    override fun setState(clientUid: String, item: TransferItem, state: Int, e: Exception?) {
        if (item is UTransferItem) {
            item.state = state
        } else {
            throw UnsupportedOperationException()
        }
    }

    companion object {
        private val TAG = MainPersistenceProvider::class.simpleName
    }
}

fun uniqueFileName() = "." + UUID.randomUUID().toString() + AppConfig.EXT_FILE_PART