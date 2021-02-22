package org.monora.uprotocol.client.android.model

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel

@Parcelize
data class AudioMediaModel(
    val id: Long,
    val artist: String,
    val song: String,
    val folder: String,
    val album: String,
) : ContentModel, Parcelable {
    @IgnoredOnParcel
    @Ignore
    private var selected = false

    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canRemove(): Boolean = false

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        TODO("Not yet implemented")
    }

    override fun dateCreated(): Long {
        TODO("Not yet implemented")
    }

    override fun dateModified(): Long {
        TODO("Not yet implemented")
    }

    override fun dateSupported(): Boolean = false

    override fun filter(charSequence: CharSequence): Boolean = false

    override fun id(): Long = id

    override fun length(): Long {
        TODO("Not yet implemented")
    }

    override fun lengthSupported(): Boolean {
        TODO("Not yet implemented")
    }

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(operationBackend: OperationBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun canSelect(): Boolean {
        TODO("Not yet implemented")
    }

    override fun selected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun select(selected: Boolean) {
        TODO("Not yet implemented")
    }

    override fun name(): String {
        TODO("Not yet implemented")
    }
}