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
package com.genonbeta.android.framework.util

/**
 * created by: Veli
 * date: 9.12.2017 00:36
 */
object MathUtils {
    fun calculatePercentage(max: Long, current: Long): Int {
        return (100.toFloat() / max * current).toInt()
    }

    fun compare(x: Long, y: Long): Int {
        return if (x < y) -1 else if (x == y) 0 else 1
    }

    fun compare(x: Int, y: Int): Int {
        return if (x < y) -1 else if (x == y) 0 else 1
    }
}