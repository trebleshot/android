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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.SQLQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class AddDeviceRunningTask extends BackgroundService.AttachableRunningTask<AddDevicesToTransferActivity>
{
    private TransferGroup mGroup;
    private NetworkDevice mDevice;
    private DeviceConnection mConnection;

    public AddDeviceRunningTask(TransferGroup group, NetworkDevice device, DeviceConnection connection)
    {
        mGroup = group;
        mDevice = device;
        mConnection = connection;
    }

    @Override
    public void onRun()
    {
        final Context context = getService().getApplicationContext();
        final Kuick kuick = AppUtils.getKuick(context);
        final SQLiteDatabase db = kuick.getWritableDatabase();

        final DialogInterface.OnClickListener retryButtonListener = (dialog, which) -> {
            if (getAnchorListener() != null)
                getAnchorListener().doCommunicate(mDevice, mConnection);
        };

        CommunicationBridge.connect(kuick, true, client -> {
            try {
                boolean doUpdate = false;
                final JSONObject jsonRequest = new JSONObject();
                final TransferAssignee assignee = new TransferAssignee(mGroup, mDevice,
                        TransferObject.Type.OUTGOING, mConnection);

                final List<TransferObject> existingRegistry = kuick.castQuery(db, new SQLQuery.Select(
                                Kuick.TABLE_TRANSFER).setWhere(Kuick.FIELD_TRANSFER_GROUPID
                                + "=? AND " + Kuick.FIELD_TRANSFER_TYPE + "=?", String.valueOf(
                        mGroup.id), TransferObject.Type.OUTGOING.toString()),
                        TransferObject.class, null);

                try {
                    // Checks if the current assignee is already on the list, if so, update
                    kuick.reconstruct(db, new TransferAssignee(assignee.groupId, assignee.deviceId,
                            TransferObject.Type.OUTGOING));

                    doUpdate = true;
                } catch (Exception ignored) {
                }

                jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                        .put(Keyword.TRANSFER_GROUP_ID, mGroup.id);

                if (existingRegistry.size() == 0)
                    throw new Exception("Empty share holder id: " + mGroup.id);

                JSONArray filesArray = new JSONArray();

                for (TransferObject transferObject : existingRegistry) {
                    publishStatusText(transferObject.name);
                    transferObject.putFlag(assignee.deviceId, TransferObject.Flag.PENDING);

                    if (isInterrupted())
                        throw new InterruptedException("Interrupted by user");

                    JSONObject thisJson = new JSONObject();

                    try {
                        thisJson.put(Keyword.INDEX_FILE_NAME, transferObject.name)
                                .put(Keyword.INDEX_FILE_SIZE, transferObject.size)
                                .put(Keyword.TRANSFER_REQUEST_ID, transferObject.id)
                                .put(Keyword.INDEX_FILE_MIME, transferObject.mimeType);

                        if (transferObject.directory != null)
                            thisJson.put(Keyword.INDEX_DIRECTORY, transferObject.directory);

                        filesArray.put(thisJson);
                    } catch (Exception e) {
                        Log.e(AddDevicesToTransferActivity.TAG, "Sender error on fileUri: "
                                + e.getClass().getName() + " : " + transferObject.name);
                    }
                }

                // so that if the user rejects, it won't be removed from the sender
                jsonRequest.put(Keyword.FILES_INDEX, filesArray.toString());

                addCloser(userAction -> kuick.remove(assignee));

                final CoolSocket.ActiveConnection activeConnection = client.communicate(mDevice, mConnection);

                addCloser(userAction -> {
                    try {
                        activeConnection.getSocket().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                activeConnection.reply(jsonRequest.toString());

                CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                activeConnection.getSocket().close();

                JSONObject clientResponse = new JSONObject(response.response);

                if (clientResponse.has(Keyword.RESULT) && clientResponse.getBoolean(Keyword.RESULT)) {
                    publishStatusText(context.getString(R.string.mesg_organizingFiles));

                    if (doUpdate)
                        kuick.update(db, assignee, mGroup, null);
                    else
                        kuick.insert(db, assignee, mGroup, null);

                    // TODO: 28.02.2020 Add listener to update the task
                    kuick.update(db, existingRegistry, mGroup, null);
                    kuick.broadcast();

                    if (getAnchorListener() != null) {
                        getAnchorListener().setResult(RESULT_OK, new Intent()
                                .putExtra(AddDevicesToTransferActivity.EXTRA_DEVICE_ID, assignee.deviceId)
                                .putExtra(AddDevicesToTransferActivity.EXTRA_GROUP_ID, assignee.groupId));

                        getAnchorListener().finish();
                    }
                } else if (getAnchorListener() != null) {
                    UIConnectionUtils.showConnectionRejectionInformation(getAnchorListener(), mDevice,
                            clientResponse, retryButtonListener);
                }
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    e.printStackTrace();

                    if (getAnchorListener() != null)
                        getAnchorListener().runOnUiThread(() -> new AlertDialog.Builder(getAnchorListener())
                                .setMessage(context.getString(R.string.mesg_fileSendError,
                                        context.getString(R.string.mesg_connectionProblem)))
                                .setNegativeButton(R.string.butn_close, null)
                                .setPositiveButton(R.string.butn_retry, retryButtonListener)
                                .show());
                }
            } finally {
                if (getAnchorListener() != null)
                    getAnchorListener().runOnUiThread(() -> getAnchorListener().resetStatusViews());
            }
        });
    }
}