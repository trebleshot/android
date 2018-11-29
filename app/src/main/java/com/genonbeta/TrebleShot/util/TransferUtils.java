package com.genonbeta.TrebleShot.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
public class TransferUtils
{
    public static final String TAG = TransferUtils.class.getSimpleName();

    public static final int TASK_START_TRANSFER_WITH_OVERVIEW = 1;

    public static void changeConnection(FragmentActivity activity, final AccessDatabase database, final TransferGroup group, final NetworkDevice device, final ConnectionUpdatedListener listener)
    {
        new ConnectionChooserDialog(activity, device, new OnDeviceSelectedListener()
        {
            @Override
            public void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> connectionList)
            {
                TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, device, connection);

                database.publish(assignee);

                if (listener != null)
                    listener.onConnectionUpdated(connection, assignee);
            }
        }).show();
    }

    @SuppressLint("DefaultLocale")
    public static long createUniqueTransferId(long groupId, String deviceId, TransferObject.Type type)
    {
        return String.format("%d_%s_%s", groupId, deviceId, type).hashCode();
    }

    public static SQLQuery.Select createTransferSelection(long groupId, String deviceId)
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(String.format("%s = ? AND %s = ?",
                        AccessDatabase.FIELD_TRANSFER_GROUPID,
                        AccessDatabase.FIELD_TRANSFER_DEVICEID),
                        String.valueOf(groupId), deviceId);
    }

    public static SQLQuery.Select createTransferSelection(long groupId, String deviceId, TransferObject.Flag flag, boolean equals)
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(String.format("%s = ? AND %s = ? AND %s " + (equals ? "=" : "!=") + " ?",
                        AccessDatabase.FIELD_TRANSFER_GROUPID,
                        AccessDatabase.FIELD_TRANSFER_DEVICEID,
                        AccessDatabase.FIELD_TRANSFER_FLAG),
                        String.valueOf(groupId), deviceId, flag.toString());
    }

    public static TransferObject fetchValidIncomingTransfer(Context context, long groupId, String deviceId)
    {
        CursorItem receiverInstance = AppUtils.getDatabase(context).getFirstFromTable(new SQLQuery
                .Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_DEVICEID + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_FLAG + "=?",
                        TransferObject.Type.INCOMING.toString(),
                        String.valueOf(groupId),
                        deviceId,
                        TransferObject.Flag.PENDING.toString()));

        return receiverInstance == null
                ? null
                : new TransferObject(receiverInstance);
    }

    public static void pauseTransfer(Context context, TransferGroup group, @Nullable TransferGroup.Assignee assignee)
    {
        pauseTransfer(context, group.groupId, assignee == null ? null : assignee.deviceId);
    }

    public static void pauseTransfer(Context context, long groupId, @Nullable String deviceId)
    {
        Intent intent = new Intent(context, CommunicationService.class)
                .setAction(CommunicationService.ACTION_CANCEL_JOB)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, groupId)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, deviceId);

        AppUtils.startForegroundService(context, intent);
    }

    public static void startTransferWithTest(final Activity activity, final TransferGroup group, final TransferGroup.Assignee assignee)
    {
        final Context context = activity.getApplicationContext();

        WorkerService.run(activity, new WorkerService.RunningTask(TAG, TASK_START_TRANSFER_WITH_OVERVIEW)
        {
            @Override
            protected void onRun()
            {
                if (fetchValidIncomingTransfer(activity, group.groupId, assignee.deviceId) == null) {
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                            builder.setMessage(R.string.mesg_noPendingTransferObjectExists);
                            builder.setNegativeButton(R.string.butn_close, null);

                            builder.show();
                        }
                    });
                } else {
                    final String savingPath = FileUtils.getSavePath(activity, AppUtils.getDefaultPreferences(activity), group)
                            .getUri()
                            .toString();

                    if (!savingPath.equals(group.savePath)) {
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                                builder.setMessage(context.getString(R.string.mesg_notSavingToChosenLocation, FileUtils.getReadableUri(group.savePath)));
                                builder.setNegativeButton(R.string.butn_close, null);

                                builder.setPositiveButton(R.string.butn_saveAnyway, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        startTransfer(activity, group, assignee);
                                    }
                                });

                                builder.show();
                            }
                        });
                    } else
                        startTransfer(activity, group, assignee);
                }
            }
        });
    }

    public static void startTransfer(final Activity activity, final TransferGroup group, final TransferGroup.Assignee assignee)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    NetworkDevice networkDevice = new NetworkDevice(assignee.deviceId);

                    AppUtils.getDatabase(activity)
                            .reconstruct(networkDevice);

                    new EstablishConnectionDialog(activity, networkDevice, new OnDeviceSelectedListener()
                    {
                        @Override
                        public void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces)
                        {
                            AppUtils.startForegroundService(activity, new Intent(activity, CommunicationService.class)
                                    .setAction(CommunicationService.ACTION_SEAMLESS_RECEIVE)
                                    .putExtra(CommunicationService.EXTRA_GROUP_ID, group.groupId)
                                    .putExtra(CommunicationService.EXTRA_DEVICE_ID, assignee.deviceId));
                        }
                    }).show();
                } catch (Exception e) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.mesg_somethingWentWrong)
                            .setNegativeButton(R.string.butn_cancel, null)
                            .setPositiveButton(R.string.butn_retry, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    startTransfer(activity, group, assignee);
                                }
                            })
                            .show();
                }
            }
        });
    }

    public interface ConnectionUpdatedListener
    {
        void onConnectionUpdated(NetworkDevice.Connection connection, TransferGroup.Assignee assignee);
    }
}
