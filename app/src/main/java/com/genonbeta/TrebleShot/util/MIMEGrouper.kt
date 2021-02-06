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
package com.genonbeta.TrebleShot.util

import java.io.File

class MIMEGrouper {
    private var mMajor: String? = null
    private var mMinor: String? = null
    var isLocked = false
        private set
    val major: String
        get() = if (mMajor == null) TYPE_GENERIC else mMajor!!
    val minor: String
        get() = if (mMinor == null) TYPE_GENERIC else mMinor!!

    fun process(mimeType: String?) {
        if (mimeType == null || mimeType.length < 3 || !mimeType.contains(File.separator)) return
        val splitMIME = mimeType.split(File.separator.toRegex()).toTypedArray()
        process(splitMIME[0], splitMIME[1])
    }

    fun process(major: String, minor: String) {
        if (mMajor == null || mMinor == null) {
            mMajor = major
            mMinor = minor
        } else if (major == TYPE_GENERIC) isLocked = true else if (major != major) {
            mMajor = TYPE_GENERIC
            mMinor = TYPE_GENERIC
            isLocked = true
        } else if (minor != minor) {
            mMinor = TYPE_GENERIC
        }
    }

    override fun toString(): String {
        return major + File.separator + minor
    }

    companion object {
        const val TYPE_GENERIC = "*"
    }
}