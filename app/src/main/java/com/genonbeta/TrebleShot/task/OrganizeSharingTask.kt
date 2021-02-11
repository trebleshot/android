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
package com.genonbeta.TrebleShot.task

import android.content.*
import android.net.Uri
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.dataobject.TransferItem.from
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.util.AppUtils
import java.util.*

class OrganizeSharingTask(private val mUriList: List<Uri>) : AttachableAsyncTask<AttachedTaskListener?>() {
    @Throws(TaskStoppedException::class)
    public override fun onRun() {
        val db: SQLiteDatabase = kuick().writableDatabase
        val transfer: Transfer = Transfer(AppUtils.getUniqueNumber())
        val list: MutableList<TransferItem> = ArrayList()
        progress().addToTotal(mUriList.size)
        publishStatus()
        for (uri in mUriList) {
            throwIfStopped()
            progress().addToCurrent(1)
            try {
                val streamInfo: StreamInfo = StreamInfo.getStreamInfo(context, uri)
                ongoingContent = streamInfo.friendlyName
                publishStatus()
                list.add(from(streamInfo, transfer.id))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (list.size > 0) {
            kuick().insert(db, list, transfer, progressListener())
            kuick().insert(db, transfer, null, progressListener())
            addCloser(Stoppable.Closer { userAction: Boolean ->
                kuick().remove(
                    db, SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                        .setWhere(
                            String.format("%s = ?", Kuick.FIELD_TRANSFERITEM_TRANSFERID),
                            transfer.id.toString()
                        )
                )
            })
            kuick().broadcast()
            TransferMemberActivity.startInstance(context, transfer, true)
        }
    }

    override fun getName(context: Context?): String? {
        return getContext().getString(R.string.mesg_organizingFiles)
    }
}