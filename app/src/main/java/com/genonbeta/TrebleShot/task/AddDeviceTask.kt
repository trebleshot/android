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

import android.app.Activity
import android.content.*
import android.util.Log
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.protocol.communication.ContentException
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.CommunicationBridge
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AddDeviceTask(private val mTransfer: Transfer, private val mDevice: Device, address: DeviceAddress) :
    AttachableAsyncTask<TransferMemberActivity?>() {
    private val mAddress: DeviceAddress
    @Throws(TaskStoppedException::class)
    public override fun onRun() {
        val context = context
        val kuick = AppUtils.getKuick(context)
        val db: SQLiteDatabase = kuick.writableDatabase
        try {
            val member = TransferMember(mTransfer, mDevice, TransferItem.Type.OUTGOING)
            val objectList = kuick.castQuery(
                db, SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM)
                    .setWhere(
                        Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID + "=? AND " + Kuick.Companion.FIELD_TRANSFERITEM_TYPE
                                + "=?", mTransfer.id.toString(), TransferItem.Type.OUTGOING.toString()
                    ),
                TransferItem::class.java, null
            )
            if (objectList.size == 0) throw ContentException(ContentException.Error.NotFound)
            val filesArray = JSONArray()
            progress().addToTotal(objectList.size)
            for (transferItem in objectList) {
                throwIfStopped()
                progress().addToCurrent(1)
                ongoingContent = transferItem.name
                publishStatus()
                try {
                    val json = JSONObject()
                        .put(Keyword.INDEX_FILE_NAME, transferItem.name)
                        .put(Keyword.INDEX_FILE_SIZE, transferItem.comparableSize)
                        .put(Keyword.TRANSFER_REQUEST_ID, transferItem.id)
                        .put(Keyword.INDEX_FILE_MIME, transferItem.mimeType)
                    if (transferItem.directory != null) json.put(Keyword.INDEX_DIRECTORY, transferItem.directory)
                    filesArray.put(json)
                } catch (e: Exception) {
                    Log.e(TransferMemberActivity.Companion.TAG, "Sender error on fileUri on " + transferItem.name, e)
                }
            }
            if (filesArray.length() < 1) throw IOException("There is no file in the JSON array.")
            var successful: Boolean
            CommunicationBridge.Companion.connect(kuick, mAddress, mDevice, 0).use { bridge ->
                bridge.requestFileTransfer(mTransfer.id, filesArray)
                successful = bridge.receiveResult()
            }
            if (successful) {
                kuick.publish<Transfer, TransferMember>(member)
                kuick.broadcast()
                val anchor: TransferMemberActivity? = anchor
                if (anchor != null) {
                    anchor.setResult(
                        Activity.RESULT_OK, Intent()
                            .putExtra(TransferMemberActivity.Companion.EXTRA_DEVICE, mDevice)
                            .putExtra(TransferMemberActivity.Companion.EXTRA_TRANSFER, mTransfer)
                    )
                    if (anchor.isAddingFirstDevice()) {
                        TransferDetailActivity.Companion.startInstance(anchor, mTransfer)
                        anchor.finish()
                    }
                }
            } else throw Exception("The remote returned false.")
        } catch (e: Exception) {
            e.printStackTrace()
            post(CommonErrorHelper.messageOf(getContext(), e))
        }
    }

    override fun getName(context: Context?): String? {
        return context!!.getString(R.string.text_addDevices)
    }

    init {
        mAddress = address
    }
}