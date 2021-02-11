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
package com.genonbeta.android.framework.util.listing.merger

import com.genonbeta.android.framework.util.listing.ComparableMerger

/**
 * created by: Veli
 * date: 29.03.2018 01:44
 */
class StringMerger<T>(val text: String) : ComparableMerger<T>() {
    override operator fun compareTo(other: ComparableMerger<T>): Int {
        return if (other is StringMerger<*>) text.compareTo(other.text, ignoreCase = true) else -1
    }

    override fun equals(other: Any?): Boolean {
        return other == text
    }

    override fun hashCode(): Int = text.hashCode()
}