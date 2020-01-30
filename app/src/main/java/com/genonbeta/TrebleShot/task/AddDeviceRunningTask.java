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
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.SQLQuery;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class AddDeviceRunningTask extends WorkerService.RunningTask<AddDevicesToTransferActivity>
{
    private TransferGroup mGroup;
    private NetworkDevice mDevice;
    private DeviceConnection mConnection;

    public AddDeviceRunningTask(TransferGroup group, NetworkDevice device,
                                DeviceConnection connection)
    {
        mGroup = group;
        mDevice = device;
        mConnection = connection;
    }

    @Override
    public void onRun()
    {
        final Context context = getService().getApplicationContext();
        final AccessDatabase database = AppUtils.getDatabase(context);
        final SQLiteDatabase instance = database.getWritableDatabase();

        final DialogInterface.OnClickListener retryButtonListener = (dialog, which) -> {
            if (getAnchorListener() != null)
                getAnchorListener().doCommunicate(mDevice, mConnection);
        };

        CommunicationBridge.connect(database, true,
                client -> {
                    try {
                        boolean doUpdate = false;
                        final JSONObject jsonRequest = new JSONObject();
                        final TransferAssignee assignee = new TransferAssignee(mGroup, mDevice,
                                TransferObject.Type.OUTGOING, mConnection);

                        final List<TransferObject> existingRegistry = database.castQuery(instance, new SQLQuery.Select(
                                AccessDatabase.TABLE_TRANSFER).setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID
                                        + "=? AND " + AccessDatabase.FIELD_TRANSFER_TYPE + "=?", String.valueOf(
                                                mGroup.id), TransferObject.Type.OUTGOING.toString()),
                                TransferObject.class, null);

                        try {
                            // Checks if the current assignee is already on the list, if so, update
                            database.reconstruct(instance, new TransferAssignee(assignee.groupId, assignee.deviceId,
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

                            if (getInterrupter().interrupted())
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

                        getInterrupter().addCloser(userAction -> database.remove(assignee));

                        final CoolSocket.ActiveConnection activeConnection = client.communicate(mDevice, mConnection);

                        getInterrupter().addCloser(userAction -> {
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
                                database.update(instance, assignee, mGroup);
                            else
                                database.insert(instance, assignee, mGroup);

                            database.update(instance, existingRegistry, null, mGroup);
                            database.broadcast();

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