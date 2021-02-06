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
package com.genonbeta.TrebleShot.util

import android.content.*
import com.genonbeta.TrebleShot.R
import java.util.*

/**
 * created by: Veli
 * date: 12.11.2017 10:53
 */
object TimeUtils {
    fun formatDateTime(context: Context?, millis: Long): CharSequence {
        return DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE)
    }

    fun getDuration(milliseconds: Long): String {
        return getDuration(milliseconds, true)
    }

    fun getDuration(time: Long, divideMilliseconds: Boolean): String {
        val string = StringBuilder()
        val calculator = ElapsedTimeCalculator(
            if (divideMilliseconds) time / 1000 else time
        )
        val hours: Long = calculator.crop(3600)
        val minutes: Long = calculator.crop(60)
        val seconds: Long = calculator.getLeftTime()
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

    fun getFriendlyElapsedTime(context: Context?, estimatedTime: Long): String {
        val elapsedTime = ElapsedTime(estimatedTime)
        val appendList: MutableList<String> = ArrayList()
        if (elapsedTime.getYears() > 0) appendList.add(
            context!!.getString(
                R.string.text_yearCountShort,
                elapsedTime.getYears()
            )
        )
        if (elapsedTime.getMonths() > 0) appendList.add(
            context!!.getString(
                R.string.text_monthCountShort,
                elapsedTime.getMonths()
            )
        )
        if (elapsedTime.getYears() == 0L) {
            if (elapsedTime.getDays() > 0) appendList.add(
                context!!.getString(
                    R.string.text_dayCountShort,
                    elapsedTime.getDays()
                )
            )
            if (elapsedTime.getMonths() == 0L) {
                if (elapsedTime.getHours() > 0) appendList.add(
                    context!!.getString(
                        R.string.text_hourCountShort,
                        elapsedTime.getHours()
                    )
                )
                if (elapsedTime.getDays() == 0L) {
                    if (elapsedTime.getMinutes() > 0) appendList.add(
                        context!!.getString(
                            R.string.text_minuteCountShort,
                            elapsedTime.getMinutes()
                        )
                    )
                    if (elapsedTime.getHours() == 0L) // always applied
                        appendList.add(context!!.getString(R.string.text_secondCountShort, elapsedTime.getSeconds()))
                }
            }
        }
        val stringBuilder = StringBuilder()
        for (appendItem in appendList) {
            if (stringBuilder.length > 0) stringBuilder.append(" ")
            stringBuilder.append(appendItem)
        }
        return stringBuilder.toString()
    }

    fun getTimeAgo(context: Context, time: Long): String {
        val differ = ((System.currentTimeMillis() - time) / 1000).toInt()
        if (differ == 0) return context.getString(R.string.text_timeJustNow) else if (differ < 60) return context.resources.getQuantityString(
            R.plurals.text_secondsAgo,
            differ,
            differ
        ) else if (differ < 3600) return context.resources.getQuantityString(
            R.plurals.text_minutesAgo,
            differ / 60,
            differ / 60
        )
        return context.getString(R.string.text_longAgo)
    }
}