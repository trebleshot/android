package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.transfer.TransferItem

@Parcelize
@Entity(tableName = "transferItem", primaryKeys = ["groupId", "id"])
data class UTransferItem(
    var id: Long,
    var groupId: Long,
    var name: String,
    var mimeType: String,
    var size: Long,
    var directory: String?,
    var location: String,
    var type: TransferItem.Type,
    var state: Int = PersistenceProvider.STATE_PENDING,
    var dateCreated: Long = System.currentTimeMillis(),
    var dateModified: Long = dateCreated,
) : TransferItem, Parcelable {
    override fun getItemDirectory(): String? = directory

    override fun getItemGroupId(): Long = groupId

    override fun getItemId(): Long = id

    override fun getItemLastChangeTime(): Long = dateModified

    override fun getItemMimeType(): String = mimeType

    override fun getItemName(): String = name

    override fun getItemSize(): Long = size

    override fun getItemType(): TransferItem.Type = type

    override fun setItemId(id: Long) {
        this.id = id
    }

    override fun setItemGroupId(groupId: Long) {
        this.groupId = groupId
    }

    override fun setItemName(name: String) {
        this.name = name
    }

    override fun setItemDirectory(directory: String?) {
        this.directory = directory
    }

    override fun setItemMimeType(mimeType: String) {
        this.mimeType = mimeType
    }

    override fun setItemSize(size: Long) {
        this.size = size
    }

    override fun setItemLastChangeTime(lastChangeTime: Long) {
        this.dateModified = lastChangeTime
    }

    override fun setItemType(type: TransferItem.Type) {
        this.type = type
    }
}