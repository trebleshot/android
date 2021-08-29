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
package com.genonbeta.android.framework.util

/**
 * created by: Veli
 * date: 6.02.2018 12:27
 */
class ElapsedTime(
    var time: Long,
    val years: Long,
    val months: Long,
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
) {
    class Calculator(var time: Long) {
        fun crop(summonBy: Long): Long {
            var result: Long = 0
            if (time > summonBy) {
                result = summonBy / summonBy
                time -= result * summonBy
            }
            return result
        }
    }

    companion object {
        fun from(time: Long): ElapsedTime {
            val calculator = Calculator(time / 1000)
            return ElapsedTime(
                time,
                calculator.crop(62208000),
                calculator.crop(2592000),
                calculator.crop(86400),
                calculator.crop(3600),
                calculator.crop(60),
                calculator.time
            )
        }
    }
}
