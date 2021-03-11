package org.monora.uprotocol.client.android.database

import androidx.room.TypeConverter
import java.io.File
import java.net.InetAddress

class IOTypeConverter {
    @TypeConverter
    fun fromFile(value: File?) = value?.absolutePath

    @TypeConverter
    fun toFile(value: String?): File? = value?.let { File(value) }

    @TypeConverter
    fun fromInetAddress(value: InetAddress): String = value.hostAddress

    @TypeConverter
    fun toInetAddress(value: String): InetAddress = InetAddress.getByName(value)
}