package org.monora.uprotocol.client.android.database

import androidx.room.TypeConverter
import java.net.InetAddress

class ClientAddressTypeConverter {
    @TypeConverter
    fun fromInetAddress(value: InetAddress): String = value.hostAddress

    @TypeConverter
    fun toInetAddress(value: String): InetAddress = InetAddress.getByName(value)
}