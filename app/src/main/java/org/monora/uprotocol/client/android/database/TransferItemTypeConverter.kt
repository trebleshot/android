package org.monora.uprotocol.client.android.database

import androidx.room.TypeConverter
import org.monora.uprotocol.core.transfer.TransferItem

class TransferItemTypeConverter {
    @TypeConverter
    fun fromType(value: TransferItem.Type): String = value.protocolValue

    @TypeConverter
    fun toType(value: String): TransferItem.Type = TransferItem.Type.from(value)
}