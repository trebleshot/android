package org.monora.uprotocol.client.android.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.monora.uprotocol.core.transfer.TransferItem

@Entity(primaryKeys = ["groupId", "id"])
data class DefaultTransferItem(
    var directory: String?,
    var groupId: Long,
    var id: Long,
    var lastChangeTime: Long,
    var mimeType: String?,
    var name: String?,
    var size: Long,
    var type: TransferItem.Type?,
    var state: Int
) : TransferItem {

    override fun getItemDirectory(): String? = directory

    override fun getItemGroupId(): Long = groupId

    override fun getItemId(): Long = id

    override fun getItemLastChangeTime(): Long = lastChangeTime

    override fun getItemMimeType(): String? = mimeType

    override fun getItemName(): String? = name

    override fun getItemSize(): Long = size

    override fun getItemType(): TransferItem.Type? = type

    override fun setItemId(id: Long) {
        this.id = id
    }

    override fun setItemGroupId(groupId: Long) {
        this.groupId = groupId
    }

    override fun setItemName(name: String?) {
        this.name = name
    }

    override fun setItemDirectory(directory: String?) {
        this.directory = directory
    }

    override fun setItemMimeType(mimeType: String?) {
        this.mimeType = mimeType
    }

    override fun setItemSize(size: Long) {
        this.size = size
    }

    override fun setItemLastChangeTime(lastChangeTime: Long) {
        this.lastChangeTime = lastChangeTime
    }

    override fun setItemType(type: TransferItem.Type) {
        this.type = type
    }
}