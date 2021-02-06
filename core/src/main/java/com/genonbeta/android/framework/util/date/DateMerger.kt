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

import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.util.*

/**
 * created by: Veli
 * date: 29.03.2018 01:23
 */
class DateMerger<T>(time: Long) : ComparableMerger<T?>() {
    private val mTime: Long
    private val mYear: Int
    private val mMonth: Int
    private val mDay: Int
    private val mDayOfYear: Int
    override operator fun compareTo(merger: ComparableMerger<T?>): Int {
        if (merger !is DateMerger<*>) return -1
        val o = merger as DateMerger<*>
        if (getYear() < o.getYear()) return -1 else if (getYear() > o.getYear()) return 1 else if (getDayOfYear() == o.getDayOfYear()) return 0
        return if (getDayOfYear() < o.getDayOfYear()) -1 else 1
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is DateMerger<*>) return false
        val dateMerger = obj as DateMerger<*>?
        return getYear() == dateMerger.getYear() && getMonth() == dateMerger.getMonth() && getDay() == dateMerger.getDay()
    }

    fun getDay(): Int {
        return mDay
    }

    fun getDayOfYear(): Int {
        return mDayOfYear
    }

    fun getMonth(): Int {
        return mMonth
    }

    fun getTime(): Long {
        return mTime
    }

    fun getYear(): Int {
        return mYear
    }

    init {
        val calendar = GregorianCalendar.getInstance()
        calendar.time = Date(time)
        mTime = time
        mYear = calendar[Calendar.YEAR]
        mMonth = calendar[Calendar.MONTH]
        mDay = calendar[Calendar.DAY_OF_MONTH]
        mDayOfYear = calendar[Calendar.DAY_OF_YEAR]
    }
}