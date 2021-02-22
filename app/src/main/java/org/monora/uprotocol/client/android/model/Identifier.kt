/*
 * Copyright (C) 2020 Veli Tasalı
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
package org.monora.uprotocol.client.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Identifier(var key: String, var value: String = "", var isNull: Boolean = false) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (other is Identifier) {
            return key == other.key && isNull == other.isNull && (isNull || value == other.value)
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + isNull.hashCode()
        return result
    }

    companion object {
        fun from(key: Enum<*>, value: Any?): Identifier {
            return from(key.toString(), value)
        }


        fun from(key: String, value: Any?): Identifier {
            return Identifier(key, value?.toString() ?: "", value == null)
        }
    }
}