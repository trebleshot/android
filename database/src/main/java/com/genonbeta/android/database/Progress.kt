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

package com.genonbeta.android.database

class Progress(var total: Int = 0, var progress: Int = 0) {
    interface Context {
        var progress: Progress?
        
        fun onProgressChange(progress: Progress): Boolean

        fun getProgress(): Int

        fun getTotal(): Int

        fun increaseBy(increase: Int): Boolean

        fun increaseTotalBy(increase: Int): Boolean
    }

    abstract class SimpleContext : Context {
        override var progress: Progress? = null

        override fun getProgress(): Int = dissect(this).progress

        override fun getTotal(): Int = dissect(this).total

        override fun increaseBy(increase: Int): Boolean {
            val progress = dissect(this)
            progress.progress += increase
            return onProgressChange(progress)
        }

        override fun increaseTotalBy(increase: Int): Boolean {
            val progress = dissect(this)
            progress.total += increase
            return onProgressChange(progress)
        }
    }

    companion object {
        fun dissect(listener: Context): Progress = listener.progress ?: run {
            Progress().also { listener.progress = it }
        }
    }
}