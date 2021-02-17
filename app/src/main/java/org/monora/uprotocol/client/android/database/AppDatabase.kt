package org.monora.uprotocol.client.android.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.monora.uprotocol.client.android.model.DefaultClient
import org.monora.uprotocol.client.android.model.DefaultClientAddress
import org.monora.uprotocol.client.android.model.DefaultTransferItem

@Database(entities = [DefaultClient::class, DefaultClientAddress::class, DefaultTransferItem::class], version = 1)
@TypeConverters(ClientTypeConverter::class, ClientAddressTypeConverter::class, TransferItemTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao

    abstract fun clientAddressDao(): ClientAddressDao

    abstract fun transferItemDao(): TransferItemDao
}