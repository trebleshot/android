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

package com.genonbeta.TrebleShot.task;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommonErrorHelper;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.SQLQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class AddDeviceTask extends AttachableAsyncTask<AddDevicesToTransferActivity>
{
    private final Transfer mTransfer;
    private final Device mDevice;
    private final DeviceAddress mAddress;

    public AddDeviceTask(Transfer transfer, Device device, DeviceAddress address)
    {
        mTransfer = transfer;
        mDevice = device;
        mAddress = address;
    }

    @Override
    public void onRun() throws TaskStoppedException
    {
        Context context = getContext();
        Kuick kuick = AppUtils.getKuick(context);
        SQLiteDatabase db = kuick.getWritableDatabase();

        boolean update = false;

        try {
            TransferMember member = new TransferMember(mTransfer, mDevice, TransferItem.Type.OUTGOING);
            List<TransferItem> objectList = kuick.castQuery(db, new SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                            .setWhere(Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND " + Kuick.FIELD_TRANSFERITEM_TYPE
                                    + "=?", String.valueOf(mTransfer.id), TransferItem.Type.OUTGOING.toString()),
                    TransferItem.class, null);

            try {
                // Checks if the current member is already on the list, if so, update
                kuick.reconstruct(db, member);
                update = true;
            } catch (Exception ignored) {
            }

            if (objectList.size() == 0)
                throw new Exception("Empty share holder id: " + mTransfer.id);

            JSONArray filesArray = new JSONArray();

            for (TransferItem transferItem : objectList) {
                setOngoingContent(transferItem.name);
                transferItem.putFlag(member.deviceId, TransferItem.Flag.PENDING);
                throwIfStopped();

                try {
                    JSONObject json = new JSONObject()
                            .put(Keyword.INDEX_FILE_NAME, transferItem.name)
                            .put(Keyword.INDEX_FILE_SIZE, transferItem.size)
                            .put(Keyword.TRANSFER_REQUEST_ID, transferItem.id)
                            .put(Keyword.INDEX_FILE_MIME, transferItem.mimeType);

                    if (transferItem.directory != null)
                        json.put(Keyword.INDEX_DIRECTORY, transferItem.directory);

                    filesArray.put(json);
                } catch (Exception e) {
                    Log.e(AddDevicesToTransferActivity.TAG, "Sender error on fileUri: "
                            + e.getClass().getName() + " : " + transferItem.name);
                }
            }

            if (filesArray.length() < 1)
                throw new IOException("There is no file in the JSON array.");

            boolean successful;

            try (CommunicationBridge bridge = CommunicationBridge.connect(kuick, mAddress, mDevice, 0)) {
                bridge.requestFileTransfer(mTransfer.id, filesArray);
                successful = bridge.receiveResult();
            }

            if (successful) {
                if (update)
                    kuick.update(db, member, mTransfer, progressListener());
                else
                    kuick.insert(db, member, mTransfer, progressListener());

                addCloser(userAction -> kuick.remove(member));
                kuick.update(db, objectList, mTransfer, progressListener());
                kuick.broadcast();

                AddDevicesToTransferActivity anchor = getAnchor();

                if (anchor != null) {
                    anchor.setResult(RESULT_OK, new Intent()
                            .putExtra(AddDevicesToTransferActivity.EXTRA_DEVICE, mDevice)
                            .putExtra(AddDevicesToTransferActivity.EXTRA_TRANSFER, mTransfer));

                    anchor.finish();
                }
            } else
                throw new Exception("The remote returned false.");
        } catch (Exception e) {
            e.printStackTrace();
            post(CommonErrorHelper.messageOf(getContext(), e));
        }
    }
    
    @Override
    public String getName()
    {
        return null;
    }
}