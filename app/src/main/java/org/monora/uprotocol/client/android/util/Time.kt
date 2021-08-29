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
package org.monora.uprotocol.client.android.util

import android.content.Context
import android.text.format.DateUtils
import com.genonbeta.android.framework.util.ElapsedTime
import org.monora.uprotocol.client.android.R
import java.util.*

/**
 * created by: Veli
 * date: 12.11.2017 10:53
 */
object Time {
    fun formatDateTime(context: Context, millis: Long): CharSequence {
        return DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
        )
    }

    fun formatDuration(time: Long, divideMilliseconds: Boolean = true): String {
        val string = StringBuilder()
        val calculator = ElapsedTime.Calculator(if (divideMilliseconds) time / 1000 else time)
        val hours: Long = calculator.crop(3600)
        val minutes: Long = calculator.crop(60)
        val seconds: Long = calculator.time

        if (hours > 0) {
            if (hours < 10) string.append("0")
            string.append(hours)
            string.append(":")
        }

        if (minutes < 10) string.append("0")
        string.append(minutes)
        string.append(":")
        if (seconds < 10) string.append("0")
        string.append(seconds)
        return string.toString()
    }

    fun formatRelativeTime(context: Context, time: Long): String {
        val differ = ((System.currentTimeMillis() - time) / 1000).toInt()
        return when {
            differ == 0 -> context.getString(R.string.just_now)
            differ < 60 -> context.resources.getQuantityString(R.plurals.seconds_ago, differ, differ)
            differ < 3600 -> {
                val minutes = differ / 60
                return context.resources.getQuantityString(R.plurals.minutes_ago, minutes, minutes)
            }
            else -> context.getString(R.string.long_ago)
        }
    }

    fun formatElapsedTime(context: Context, estimatedTime: Long): String {
        val elapsedTime = ElapsedTime.from(estimatedTime)
        val list: MutableList<String> = ArrayList()
        if (elapsedTime.years > 0) list.add(context.getString(R.string.count_years_short, elapsedTime.years))
        if (elapsedTime.months > 0) list.add(context.getString(R.string.count_months_short, elapsedTime.months))
        if (elapsedTime.years == 0L) {
            if (elapsedTime.days > 0) list.add(context.getString(R.string.count_days_short, elapsedTime.days))
            if (elapsedTime.months == 0L) {
                if (elapsedTime.hours > 0) list.add(context.getString(R.string.count_hours_short, elapsedTime.hours))
                if (elapsedTime.days == 0L) {
                    if (elapsedTime.minutes > 0) {
                        list.add(context.getString(R.string.count_minutes_short, elapsedTime.minutes))
                    }

                    if (elapsedTime.hours == 0L) {
                        // always applied
                        list.add(context.getString(R.string.count_seconds_short, elapsedTime.seconds))
                    }

                }
            }
        }
        val stringBuilder = StringBuilder()
        for (appendItem in list) {
            if (stringBuilder.isNotEmpty()) stringBuilder.append(" ")
            stringBuilder.append(appendItem)
        }
        return stringBuilder.toString()
    }
}
