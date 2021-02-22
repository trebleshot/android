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
package org.monora.uprotocol.client.android.task

import android.content.*
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import org.json.JSONArray
import org.json.JSONException
import org.monora.uprotocol.client.android.model.Device
import org.monora.uprotocol.client.android.model.Transfer
import org.monora.uprotocol.client.android.model.TransferItem
import org.monora.uprotocol.client.android.model.TransferMember
import java.util.*

class IndexTransferTask(
    private val transferId: Long,
    private val jsonIndex: String,
    private val device: Device,
    private val noPrompt: Boolean
) : AsyncTask() {
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        val db: SQLiteDatabase = kuick.writableDatabase
        val transfer = Transfer(transferId)
        val member = TransferMember(transfer, device, TransferItem.Type.INCOMING)
        val jsonArray: JSONArray = try {
            JSONArray(jsonIndex)
        } catch (e: Exception) {
            return
        }

        progress.increaseTotalBy(jsonArray.length())

        try {
            kuick.reconstruct(transfer)
            return
        } catch (ignored: Exception) {
        }

        kuick.publish(transfer)
        kuick.publish(member)

        var uniqueId = System.currentTimeMillis() // The uniqueIds
        val itemList: MutableList<TransferItem> = ArrayList()

        for (i in 0 until jsonArray.length()) {
            throwIfStopped()
            progress.increaseBy(1)
            try {
                val index = jsonArray.getJSONObject(i)
                val transferItem = TransferItem(
                    index.getLong(Keyword.TRANSFER_REQUEST_ID), transferId,
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
            kuick.insert(db, itemList, transfer, progress)
            context.sendBroadcast(
                Intent(BackgroundService.ACTION_INCOMING_TRANSFER_READY)
                    .putExtra(BackgroundService.EXTRA_TRANSFER, transfer)
                    .putExtra(BackgroundService.EXTRA_DEVICE, device)
            )
            if (noPrompt) try {
                app.run(FileTransferStarterTask.createFrom(kuick, transfer, device, TransferItem.Type.INCOMING))
            } catch (e: Exception) {
                e.printStackTrace()
            } else app.notifyFileRequest(device, transfer, itemList)
        }
        kuick.broadcast()
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_preparingFiles)
    }
}