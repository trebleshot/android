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

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import org.monora.uprotocol.client.android.util.Files
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.transfer.TransferItem.Type.Incoming
import java.util.*

class IndexTransferTask(
    private val connectionFactory: ConnectionFactory,
    private val persistenceProvider: PersistenceProvider,
    private val appDatabase: AppDatabase,
    private val transferId: Long,
    private val jsonIndex: String,
    private val client: UClient,
    private val noPrompt: Boolean
) : AsyncTask() {
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        // Do not let it add the same transfer id again.
        appDatabase.transferDao().get(transferId)?.run { return }

        val saveLocation = Files.getApplicationDirectory(context).toString()
        val transfer = Transfer(transferId, client.clientUid, Incoming, saveLocation)
        val jsonArray: JSONArray = try {
            JSONArray(jsonIndex)
        } catch (e: Exception) {
            return
        }

        progress.increaseTotalBy(jsonArray.length())
        appDatabase.transferDao().insertAll(transfer)

        val itemList: MutableList<UTransferItem> = ArrayList()

        for (i in 0 until jsonArray.length()) {
            throwIfStopped()
            progress.increaseBy(1)
            try {
                val index = jsonArray.getJSONObject(i)
                val uniqueName = "." + UUID.randomUUID().toString() + AppConfig.EXT_FILE_PART
                val directory = index.optString(Keyword.INDEX_DIRECTORY).takeIf { it.isNotEmpty() }
                val transferItem = UTransferItem(
                    index.getLong(Keyword.TRANSFER_REQUEST_ID),
                    transferId,
                    index.getString(Keyword.INDEX_FILE_NAME),
                    index.getString(Keyword.INDEX_FILE_MIME),
                    index.getLong(Keyword.INDEX_FILE_SIZE),
                    directory,
                    uniqueName,
                    Incoming,
                )
                ongoingContent = transferItem.name
                if (index.has(Keyword.INDEX_DIRECTORY)) transferItem.directory =
                    index.getString(Keyword.INDEX_DIRECTORY)
                itemList.add(transferItem)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (itemList.size > 0) CoroutineScope(Dispatchers.Main).launch {
            appDatabase.transferItemDao().insertAll(itemList)

            context.sendBroadcast(
                Intent(BackgroundService.ACTION_INCOMING_TRANSFER_READY)
                    .putExtra(BackgroundService.EXTRA_TRANSFER, transfer)
                    .putExtra(BackgroundService.EXTRA_DEVICE, client)
            )
            if (noPrompt) {
                try {
                    backend.run(
                        FileTransferStarterTask.createFrom(
                            connectionFactory, persistenceProvider, appDatabase, transfer, client, Incoming
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                backend.notifyFileRequest(client, transfer, itemList)
            }
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_preparingFiles)
    }
}