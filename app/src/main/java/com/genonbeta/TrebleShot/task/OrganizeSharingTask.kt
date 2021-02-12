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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.TransferMemberActivity
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.util.Stoppable
import java.util.*

class OrganizeSharingTask(private val uriList: List<Uri>) : AttachableAsyncTask<AttachedTaskListener>() {
    @Throws(TaskStoppedException::class)
    public override fun onRun() {
        val db: SQLiteDatabase = kuick.writableDatabase
        val transfer = Transfer(AppUtils.uniqueNumber.toLong())
        val list: MutableList<TransferItem> = ArrayList()
        progress.increaseTotalBy(uriList.size)

        for (uri in uriList) {
            throwIfStopped()

            try {
                val streamInfo: StreamInfo = StreamInfo.from(context, uri)
                ongoingContent = streamInfo.friendlyName
                list.add(from(streamInfo, transfer.id))
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                progress.increaseBy(1)
            }
        }
        if (list.size > 0) {
            kuick.insert(db, list, transfer, progress)
            kuick.insert(db, transfer, null, progress)
            addCloser(object : Stoppable.Closer {
                override fun onClose(userAction: Boolean) {
                    kuick.remove(
                        db, SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                            .setWhere(
                                String.format("%s = ?", Kuick.FIELD_TRANSFERITEM_TRANSFERID),
                                transfer.id.toString()
                            )
                    )
                }
            })
            kuick.broadcast()
            TransferMemberActivity.startInstance(context, transfer, true)
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.mesg_organizingFiles)
    }
}