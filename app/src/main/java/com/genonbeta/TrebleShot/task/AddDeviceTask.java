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
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableBgTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommonErrorHelper;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.android.database.SQLQuery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class AddDeviceTask extends AttachableBgTask<AddDevicesToTransferActivity>
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
        Context context = getService().getApplicationContext();
        Kuick kuick = AppUtils.getKuick(context);
        SQLiteDatabase db = kuick.getWritableDatabase();

        ConnectionUtils utils = new ConnectionUtils(getService());
        boolean update = false;

        try (CommunicationBridge bridge = CommunicationBridge.connect(kuick, mAddress, mDevice, 0)) {
            TransferMember member = new TransferMember(mTransfer, mDevice, TransferItem.Type.OUTGOING);
            List<TransferItem> objectList = kuick.castQuery(db, new SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                            .setWhere(Kuick.FIELD_TRANSFER_TRANSFERID + "=? AND " + Kuick.FIELD_TRANSFER_TYPE
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

            // so that if the user rejects, it won't be removed from the sender
            JSONObject jsonObject = new JSONObject()
                    .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                    .put(Keyword.TRANSFER_ID, mTransfer.id)
                    .put(Keyword.INDEX, filesArray.toString());

            final ActiveConnection activeConnection = bridge.getActiveConnection();

            addCloser(userAction -> {
                try {
                    activeConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            activeConnection.reply(jsonObject.toString());

            if (bridge.receiveResult()) {
                setOngoingContent(context.getString(R.string.mesg_organizingFiles));

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
                            .putExtra(AddDevicesToTransferActivity.EXTRA_GROUP, mTransfer));

                    anchor.finish();
                }
            }
        } catch (Exception e) {
            post(CommonErrorHelper.messageOf(getService(), e));
        }
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return null;
    }

    private enum TaskId
    {
        Finalize
    }
}