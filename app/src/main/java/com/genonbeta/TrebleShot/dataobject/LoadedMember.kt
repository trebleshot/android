/*
 * Copyright (C) 2019 Veli Tasalı
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
package com.genonbeta.TrebleShot.dataobject

import java.util.*

class LoadedMember : TransferMember, Editable {
    lateinit var device: Device

    override var id: Long
        get() = String.format(Locale.getDefault(), "%s_%d", deviceId, transferId).hashCode().toLong()
        set(id) {}

    constructor()

    constructor(transferId: Long, deviceId: String, type: TransferItem.Type) : super(transferId, deviceId, type)

    override fun applyFilter(filteringKeywords: Array<String>): Boolean {
        return false
    }

    override fun comparisonSupported(): Boolean {
        return false
    }

    override fun getComparableName(): String = device.username

    override fun getComparableDate(): Long = device.lastUsageTime

    override fun getComparableSize(): Long = 0

    override fun getSelectableTitle(): String = getComparableName()

    override fun isSelectableSelected(): Boolean = false

    override fun setSelectableSelected(selected: Boolean): Boolean {
        return false
    }
}