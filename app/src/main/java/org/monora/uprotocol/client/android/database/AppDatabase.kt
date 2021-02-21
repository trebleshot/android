package org.monora.uprotocol.client.android.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.monora.uprotocol.client.android.database.model.*

@Database(
    entities = [
        DefaultClient::class,
        DefaultClientAddress::class,
        DefaultTransferItem::class,
        SharedTextModel:: class,
        Transfer::class,
        TransferTarget::class
    ],
    version = 1
)
@TypeConverters(ClientTypeConverter::class, ClientAddressTypeConverter::class, TransferItemTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao

    abstract fun clientAddressDao(): ClientAddressDao

    abstract fun sharedTextDao(): SharedTextDao

    abstract fun transferDao(): TransferDao

    abstract fun transferItemDao(): TransferItemDao

    abstract fun transferTargetDao(): TransferTargetDao
}