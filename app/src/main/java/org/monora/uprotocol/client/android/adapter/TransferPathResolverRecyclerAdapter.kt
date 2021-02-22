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
package org.monora.uprotocol.client.android.adapter

import android.content.*
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.model.LoadedMember
import java.io.File

/**
 * created by: veli
 * date: 3/11/19 7:39 PM
 */
class TransferPathResolverRecyclerAdapter(context: Context) : PathResolverRecyclerAdapter<String?>(context) {
    private var member: LoadedMember? = null

    private val homeName: String = context.getString(R.string.text_home)

    override fun onFirstItem(): Index<String?> {
        return Index(
            member?.device?.username ?: homeName,
            null,
            if (member == null) R.drawable.ic_home_white_24dp else R.drawable.ic_device_hub_white_24dp
        )
    }

    fun goTo(member: LoadedMember?, paths: Array<String>?) {
        this.member = member
        val mergedPath = StringBuilder()
        initAdapter()
        synchronized(list) {
            if (paths != null) {
                for (path in paths) {
                    if (path.isEmpty()) continue
                    if (mergedPath.isNotEmpty()) mergedPath.append(File.separator)

                    mergedPath.append(path)
                    list.add(Index(path, mergedPath.toString()))
                }
            }
        }
    }

}