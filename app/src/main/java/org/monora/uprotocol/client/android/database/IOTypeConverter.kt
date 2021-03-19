/*
 * Copyright (C) 2021 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

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