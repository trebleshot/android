package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.core.util.ObjectsCompat
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel
import retrofit2.http.Field

@Parcelize
@Entity(tableName = "shared_text")
data class SharedTextModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    var text: String,
    val created: Long = System.currentTimeMillis(),
    var modified: Long = created,
) : ContentModel, Parcelable {
    // TODO: 2/21/21 Should selection be serialized?
    @Ignore
    @IgnoredOnParcel
    private var selected = false

    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canSelect(): Boolean = true

    override fun canRemove(): Boolean = true

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun dateCreated(): Long = created

    override fun dateModified(): Long = modified

    override fun dateSupported(): Boolean = true

    override fun filter(charSequence: CharSequence): Boolean = text.contains(charSequence)

    override fun id(): Long = ObjectsCompat.hash(id, created).toLong()

    override fun length(): Long = throw UnsupportedOperationException()

    override fun lengthSupported(): Boolean = false

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun name(): String = this.text

    override fun remove(operationBackend: OperationBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun selected(): Boolean = selected

    override fun select(selected: Boolean) {
        this.selected = selected
    }
}