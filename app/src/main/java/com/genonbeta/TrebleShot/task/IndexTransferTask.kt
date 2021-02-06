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
package com.genonbeta.TrebleShot.task

import android.content.*
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.service.BackgroundService
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.service.backgroundserviceimport.TaskStoppedException
import org.json.JSONArray
import org.json.JSONException
import java.util.*

class IndexTransferTask(
    private val mTransferId: Long,
    private val mJsonIndex: String,
    private val mDevice: Device,
    private val mNoPrompt: Boolean
) : AsyncTask() {
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        val db: SQLiteDatabase = kuick().writableDatabase
        val jsonArray: JSONArray
        val transfer = Transfer(mTransferId)
        val member = TransferMember(transfer, mDevice, TransferItem.Type.INCOMING)
        jsonArray = try {
            JSONArray(mJsonIndex)
        } catch (e: Exception) {
            return
        }
        progress().addToTotal(jsonArray.length())
        try {
            kuick().reconstruct(transfer)
            return
        } catch (ignored: Exception) {
        }
        kuick().publish(transfer)
        kuick().publish<Transfer, TransferMember>(member)
        var uniqueId = System.currentTimeMillis() // The uniqueIds
        val itemList: MutableList<TransferItem> = ArrayList()
        for (i in 0 until jsonArray.length()) {
            throwIfStopped()
            progress().addToCurrent(1)
            try {
                val index = jsonArray.getJSONObject(i)
                val transferItem = TransferItem(
                    index.getLong(Keyword.TRANSFER_REQUEST_ID), mTransferId,
                    index.getString(Keyword.INDEX_FILE_NAME), "." + uniqueId++ + "." + AppConfig.EXT_FILE_PART,
                    index.getString(Keyword.INDEX_FILE_MIME), index.getLong(Keyword.INDEX_FILE_SIZE),
                    TransferItem.Type.INCOMING
                )
                ongoingContent = transferItem.name
                if (index.has(Keyword.INDEX_DIRECTORY)) transferItem.directory =
                    index.getString(Keyword.INDEX_DIRECTORY)
                itemList.add(transferItem)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (itemList.size > 0) {
            kuick().insert(db, itemList, transfer, progressListener())
            context.sendBroadcast(
                Intent(BackgroundService.ACTION_INCOMING_TRANSFER_READY)
                    .putExtra(BackgroundService.EXTRA_TRANSFER, transfer)
                    .putExtra(BackgroundService.EXTRA_DEVICE, mDevice)
            )
            if (mNoPrompt) try {
                app.run(FileTransferTask.Companion.createFrom(kuick(), transfer, mDevice, TransferItem.Type.INCOMING))
            } catch (e: Exception) {
                e.printStackTrace()
            } else app.notifyFileRequest(mDevice, transfer, itemList)
        }
        kuick().broadcast()
    }

    override fun getName(context: Context?): String? {
        return context!!.getString(R.string.text_preparingFiles)
    }
}