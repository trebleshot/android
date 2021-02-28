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
package com.genonbeta.android.framework.util.date


import androidx.core.util.ObjectsCompat
import com.genonbeta.android.framework.util.listing.ComparableMerger
import java.util.*

/**
 * created by: Veli
 * date: 29.03.2018 01:23
 */
class DateMerger<T>(val time: Long) : ComparableMerger<T>() {
    private val year: Int
    private val month: Int
    private val day: Int
    private val dayOfYear: Int

    override operator fun compareTo(other: ComparableMerger<T>): Int {
        return if (other !is DateMerger<*> || year < other.year) {
            -1
        } else if (year > other.year) {
            1
        } else {
            dayOfYear.compareTo(other.dayOfYear)
        }
    }

    override fun equals(other: Any?): Boolean = other is DateMerger<*> && hashCode() == other.hashCode()

    override fun hashCode(): Int = ObjectsCompat.hash(year, dayOfYear)

    init {
        val calendar = GregorianCalendar.getInstance()
        calendar.time = Date(time)
        year = calendar[Calendar.YEAR]
        month = calendar[Calendar.MONTH]
        day = calendar[Calendar.DAY_OF_MONTH]
        dayOfYear = calendar[Calendar.DAY_OF_YEAR]
    }
}