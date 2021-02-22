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

/**
 * This class has two modes. One is the AND values and the other is the OR values. The AND values are the first
 * to be tried. If they don't match, which is when they don't have any values or the same value size, or one of the
 * values not matching, the OR values are tried and if any of them matches with something in the list then the match
 * is considered to be true. When using this, try to avoid using default values like 0, false, or "". The values
 * you put should be unique, so that it doesn't match with a random identifier. This class can be transferred with
 * intents.
 */

@Parcelize
class Identity(private val orList: MutableList<Identifier>, private val andList: MutableList<Identifier>) : Parcelable {
    constructor() : this(mutableListOf<Identifier>(), mutableListOf<Identifier>())

    override fun equals(other: Any?): Boolean {
        return if (other is Identity) isANDsTrue(other) || isORsTrue(other) else super.equals(other)
    }

    // AND
    @Synchronized
    private fun isANDsTrue(identity: Identity): Boolean {
        if (andList.size <= 0 || identity.andList.size <= 0)
            return false
        for (identifier in andList) if (!identity.andList.contains(identifier)) return false
        return true
    }

    @Synchronized
    private fun isORsTrue(identity: Identity): Boolean {
        for (identifier in orList) if (identity.orList.contains(identifier)) return true
        return false
    }

    fun putAND(identifier: Identifier) {
        synchronized(andList) { andList.add(identifier) }
    }

    fun putOR(identifier: Identifier) {
        synchronized(orList) { orList.add(identifier) }
    }

    companion object {
        fun withANDs(vararg ands: Identifier): Identity {
            val identity = Identity()
            for (and in ands) identity.putAND(and)
            return identity
        }

        fun withORs(vararg ors: Identifier): Identity {
            val identity = Identity()
            for (or in ors) identity.putOR(or)
            return identity
        }
    }
}