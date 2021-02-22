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
package org.monora.uprotocol.client.android.task

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.activity.TransferDetailActivity
import org.monora.uprotocol.client.android.activity.TransferMemberActivity
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.database.Kuick
import org.monora.uprotocol.client.android.protocol.communication.ContentException
import org.monora.uprotocol.client.android.service.backgroundservice.AttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import org.monora.uprotocol.client.android.util.CommonErrorHelper
import org.monora.uprotocol.client.android.util.CommunicationBridge
import org.monora.uprotocol.client.android.util.CommunicationBridge.Companion.receiveResult
import com.genonbeta.android.database.SQLQuery
import org.json.JSONArray
import org.json.JSONObject
import org.monora.uprotocol.client.android.model.*
import java.io.IOException

class AddDeviceTask(
    private val transfer: Transfer, val device: Device, val address: DeviceAddress,
) : AttachableAsyncTask<TransferMemberActivity>() {
    @Throws(TaskStoppedException::class)
    public override fun onRun() {
        val db: SQLiteDatabase = kuick.writableDatabase
        try {
            val member = TransferMember(transfer, device, TransferItem.Type.OUTGOING)
            val objectList = kuick.castQuery(
                db, SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                    .setWhere(
                        Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND " + Kuick.FIELD_TRANSFERITEM_TYPE
                                + "=?", transfer.id.toString(), TransferItem.Type.OUTGOING.toString()
                    ),
                TransferItem::class.java, null
            )
            if (objectList.isEmpty()) throw ContentException(ContentException.Error.NotFound)
            val filesArray = JSONArray()

            progress.increaseTotalBy(objectList.size)

            for (transferItem in objectList) {
                throwIfStopped()
                ongoingContent = transferItem.name

                try {
                    val json = JSONObject()
                        .put(Keyword.INDEX_FILE_NAME, transferItem.name)
                        .put(Keyword.INDEX_FILE_SIZE, transferItem.length)
                        .put(Keyword.TRANSFER_REQUEST_ID, transferItem.id)
                        .put(Keyword.INDEX_FILE_MIME, transferItem.mimeType)
                    if (transferItem.directory != null) json.put(Keyword.INDEX_DIRECTORY, transferItem.directory)
                    filesArray.put(json)
                } catch (e: Exception) {
                    Log.e(TransferMemberActivity.TAG, "Sender error on fileUri on " + transferItem.name, e)
                } finally {
                    progress.increaseBy(1)
                }
            }

            if (filesArray.length() < 1) {
                throw IOException("There is no file in the JSON array.")
            }
            var successful: Boolean
            CommunicationBridge.connect(kuick, address, device, 0).use { bridge ->
                bridge.requestFileTransfer(transfer.id, filesArray)
                successful = bridge.receiveResult()
            }
            if (successful) {
                kuick.publish(member)
                kuick.broadcast()
                anchor?.let {
                    it.setResult(
                        Activity.RESULT_OK, Intent()
                            .putExtra(TransferMemberActivity.EXTRA_DEVICE, device)
                            .putExtra(TransferMemberActivity.EXTRA_TRANSFER, transfer)
                    )
                    if (it.addingInitialDevice) {
                        TransferDetailActivity.startInstance(it, transfer)
                        it.finish()
                    }
                }
            } else throw Exception("The remote returned false.")
        } catch (e: Exception) {
            e.printStackTrace()
            post(CommonErrorHelper.messageOf(context, e))
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_addDevices)
    }
}