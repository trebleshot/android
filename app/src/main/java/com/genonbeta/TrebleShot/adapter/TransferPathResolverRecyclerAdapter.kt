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
package com.genonbeta.TrebleShot.adapter

import android.content.*
import com.genonbeta.TrebleShot.R
import java.io.File

/**
 * created by: veli
 * date: 3/11/19 7:39 PM
 */
class TransferPathResolverRecyclerAdapter(context: Context) : PathResolverRecyclerAdapter<String?>(context) {
    private var mMember: LoadedMember? = null
    private val mHomeName: String
    override fun onFirstItem(): Index<String?> {
        return if (mMember != null) Index(
            mMember.device.username,
            R.drawable.ic_device_hub_white_24dp,
            null
        ) else Index(mHomeName, R.drawable.ic_home_white_24dp, null)
    }

    fun goTo(member: LoadedMember?, paths: Array<String>?) {
        mMember = member
        val mergedPath = StringBuilder()
        initAdapter()
        synchronized(list) {
            if (paths != null) for (path in paths) {
                if (path.length == 0) continue
                if (mergedPath.length > 0) mergedPath.append(File.separator)
                mergedPath.append(path)
                list.add(Index(path, mergedPath.toString()))
            }
        }
    }

    init {
        mHomeName = context.getString(R.string.text_home)
    }
}