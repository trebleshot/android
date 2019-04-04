package com.genonbeta.TrebleShot.task;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDevicesToTransferActivity;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.util.Interrupter;

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
    private NetworkDevice.Connection mConnection;

    public AddDeviceRunningTask(TransferGroup group, NetworkDevice device,
                                NetworkDevice.Connection connection)
    {
        mGroup = group;
        mDevice = device;
        mConnection = connection;
    }

    @Override
    public void onRun()
    {
        final Context context = getService().getApplicationContext();

        final DialogInterface.OnClickListener retryButtonListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (getAnchorListener() != null)
                    getAnchorListener().doCommunicate(mDevice, mConnection);
            }
        };

        CommunicationBridge.connect(AppUtils.getDatabase(getService()), true,
                new CommunicationBridge.Client.ConnectionHandler()
                {
                    @Override
                    public void onConnect(CommunicationBridge.Client client)
                    {
                        client.setDevice(mDevice);

                        try {
                            boolean doPublish = false;
                            final JSONObject jsonRequest = new JSONObject();
                            final TransferGroup.Assignee assignee = new TransferGroup.Assignee(mGroup, mDevice, mConnection);
                            final List<TransferObject> pendingRegistry = new ArrayList<>();

                            final List<TransferObject> existingRegistry =
                                    new ArrayList<>(AppUtils.getDatabase(context).castQuery(
                                            new SQLQuery.Select(AccessDatabase.DIVIS_TRANSFER)
                                                    .setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
                                                                    + AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
                                                            String.valueOf(mGroup.groupId),
                                                            TransferObject.Type.OUTGOING.toString()), TransferObject.class));
                            final SQLiteDatabase.ProgressUpdater progressUpdater = new SQLiteDatabase.ProgressUpdater()
                            {
                                @Override
                                public void onProgressChange(int total, int current)
                                {
                                    if (getAnchorListener() != null)
                                        getAnchorListener().updateProgress(total, current);
                                }

                                @Override
                                public boolean onProgressState()
                                {
                                    return !getInterrupter().interrupted();
                                }
                            };

                            try {
                                // Checks if the current assignee is already on the list, if so do publish not insert
                                AppUtils.getDatabase(context).reconstruct(
                                        new TransferGroup.Assignee(assignee.groupId,
                                                assignee.deviceId));

                                doPublish = true;
                            } catch (Exception e) {
                            }

                            if (mDevice instanceof NetworkDeviceListAdapter.HotspotNetwork
                                    && ((NetworkDeviceListAdapter.HotspotNetwork) mDevice).qrConnection)
                                jsonRequest.put(Keyword.FLAG_TRANSFER_QR_CONNECTION, true);

                            jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER);
                            jsonRequest.put(Keyword.TRANSFER_GROUP_ID, mGroup.groupId);

                            if (existingRegistry.size() == 0)
                                throw new Exception("Empty share holder id: " + mGroup.groupId);

                            JSONArray filesArray = new JSONArray();

                            for (TransferObject transferObject : existingRegistry) {
                                publishStatusText(transferObject.friendlyName);

                                TransferObject copyObject = new TransferObject(AccessDatabase.convertValues(transferObject.getValues()));

                                if (getInterrupter().interrupted())
                                    throw new InterruptedException("Interrupted by user");

                                copyObject.deviceId = assignee.deviceId; // We will clone the file index with new deviceId
                                copyObject.flag = TransferObject.Flag.PENDING;
                                copyObject.accessPort = 0;
                                copyObject.skippedBytes = 0;
                                JSONObject thisJson = new JSONObject();

                                try {
                                    thisJson.put(Keyword.INDEX_FILE_NAME, copyObject.friendlyName);
                                    thisJson.put(Keyword.INDEX_FILE_SIZE, copyObject.fileSize);
                                    thisJson.put(Keyword.TRANSFER_REQUEST_ID, copyObject.requestId);
                                    thisJson.put(Keyword.INDEX_FILE_MIME, copyObject.fileMimeType);

                                    if (copyObject.directory != null)
                                        thisJson.put(Keyword.INDEX_DIRECTORY, copyObject.directory);

                                    filesArray.put(thisJson);
                                    pendingRegistry.add(copyObject);
                                } catch (Exception e) {
                                    Log.e(AddDevicesToTransferActivity.TAG, "Sender error on fileUri: " + e.getClass().getName() + " : " + copyObject.friendlyName);
                                }
                            }

                            // so that if the user rejects, it won't be removed from the sender
                            jsonRequest.put(Keyword.FILES_INDEX, filesArray.toString());

                            getInterrupter().addCloser(new Interrupter.Closer()
                            {
                                @Override
                                public void onClose(boolean userAction)
                                {
                                    AppUtils.getDatabase(context).remove(assignee);
                                }
                            });

                            final CoolSocket.ActiveConnection activeConnection = client.communicate(mDevice, mConnection);

                            getInterrupter().addCloser(new Interrupter.Closer()
                            {
                                @Override
                                public void onClose(boolean userAction)
                                {
                                    try {
                                        activeConnection.getSocket().close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            activeConnection.reply(jsonRequest.toString());

                            CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                            activeConnection.getSocket().close();

                            JSONObject clientResponse = new JSONObject(response.response);

                            if (clientResponse.has(Keyword.RESULT) && clientResponse.getBoolean(Keyword.RESULT)) {
                                publishStatusText(context.getString(R.string.mesg_organizingFiles));

                                if (doPublish)
                                    AppUtils.getDatabase(context).publish(assignee);
                                else
                                    AppUtils.getDatabase(context).insert(assignee);

                                if (doPublish)
                                    AppUtils.getDatabase(context).publish(pendingRegistry, progressUpdater);
                                else
                                    AppUtils.getDatabase(context).insert(pendingRegistry, progressUpdater);

                                if (getAnchorListener() != null) {
                                    getAnchorListener().setResult(RESULT_OK, new Intent()
                                            .putExtra(AddDevicesToTransferActivity.EXTRA_DEVICE_ID, assignee.deviceId)
                                            .putExtra(AddDevicesToTransferActivity.EXTRA_GROUP_ID, assignee.groupId));

                                    getAnchorListener().finish();
                                }
                            } else if (getAnchorListener() != null) {
                                UIConnectionUtils.showConnectionRejectionInformation(
                                        getAnchorListener(), mDevice, clientResponse,
                                        retryButtonListener);
                            }
                        } catch (Exception e) {
                            if (!(e instanceof InterruptedException)) {
                                e.printStackTrace();

                                if (getAnchorListener() != null)
                                    getAnchorListener().runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            new AlertDialog.Builder(getAnchorListener())
                                                    .setMessage(context.getString(R.string.mesg_fileSendError,
                                                            context.getString(R.string.mesg_connectionProblem)))
                                                    .setNegativeButton(R.string.butn_close, null)
                                                    .setPositiveButton(R.string.butn_retry, retryButtonListener)
                                                    .show();
                                        }
                                    });
                            }
                        } finally {
                            if (getAnchorListener() != null)
                                getAnchorListener().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        getAnchorListener().resetStatusViews();
                                    }
                                });
                        }
                    }
                });
    }
}