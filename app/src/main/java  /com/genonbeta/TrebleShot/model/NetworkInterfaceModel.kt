package com.genonbeta.TrebleShot.model

import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel
import java.net.NetworkInterface

class NetworkInterfaceModel(
    private val networkInterface: NetworkInterface,
    private val name: String,
) : ContentModel {
    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canSelect(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canRemove(): Boolean = false

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun dateCreated(): Long = throw UnsupportedOperationException()

    override fun dateModified(): Long = throw UnsupportedOperationException()

    override fun dateSupported(): Boolean = false

    override fun filter(charSequence: CharSequence): Boolean = networkInterface.displayName.contains(charSequence)
            && name.contains(charSequence)

    override fun id(): Long = networkInterface.hashCode().toLong()

    override fun length(): Long = throw UnsupportedOperationException()

    override fun lengthSupported(): Boolean = false

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun name(): String = name

    override fun remove(operationBackend: OperationBackend): Boolean {
        throw UnsupportedOperationException()
    }

    override fun selected(): Boolean = false

    override fun select(selected: Boolean) {

    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        throw UnsupportedOperationException()
    }
}