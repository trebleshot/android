package org.monora.uprotocol.client.android.model

import androidx.core.util.ObjectsCompat
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend

class DateSectionContentModel(val dateText: String, val time: Long) : ContentModel {
    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canSelect(): Boolean = false

    override fun canRemove(): Boolean = false

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun dateCreated(): Long = time

    override fun dateModified(): Long = time

    override fun dateSupported(): Boolean = true

    override fun filter(charSequence: CharSequence): Boolean = false

    override fun id(): Long = dateText.hashCode().toLong()

    override fun length(): Long {
        throw UnsupportedOperationException()
    }

    override fun lengthSupported(): Boolean = false

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(operationBackend: OperationBackend): Boolean {
        throw UnsupportedOperationException()
    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        throw UnsupportedOperationException()
    }

    override fun selected(): Boolean = false

    override fun select(selected: Boolean) {
        throw UnsupportedOperationException()
    }

    override fun name(): String = dateText
}