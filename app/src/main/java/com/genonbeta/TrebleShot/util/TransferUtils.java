package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.exception.AssigneeNotFoundException;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

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
        new ConnectionChooserDialog(activity, device, new ConnectionChooserDialog.OnDeviceSelectedListener()
        {
            @Override
            public void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> connectionList)
            {
                TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, device, connection);

                database.publish(assignee);

                if (listener != null)
                    listener.onConnectionUpdated(connection, assignee);
            }
        }, false).show();
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

    public static TransferGroup.Assignee getDefaultAssignee(Context context, long groupId) throws AssigneeNotFoundException
    {
        ArrayList<TransferGroup.Assignee> assignees = AppUtils.getDatabase(context)
                .castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                        .setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID),
                                String.valueOf(groupId)), TransferGroup.Assignee.class);

        for (TransferGroup.Assignee assignee : assignees)
            if (!assignee.isClone)
                return assignee;

        if (assignees.size() > 0)
            return assignees.get(0);

        throw new AssigneeNotFoundException();
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
        Intent intent = new Intent(context, CommunicationService.class)
                .setAction(CommunicationService.ACTION_CANCEL_JOB)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, group.groupId);

        if (assignee != null)
            intent.putExtra(CommunicationService.EXTRA_DEVICE_ID, assignee.deviceId);

        AppUtils.startForegroundService(context, intent);
    }

    public static void resumeTransfer(final Activity activity, final TransferGroup group, final TransferGroup.Assignee assignee)
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
                                        resumeTransfer(activity.getApplicationContext(), group, assignee);
                                    }
                                });

                                builder.show();
                            }
                        });
                    } else
                        resumeTransfer(activity.getApplicationContext(), group, assignee);
                }
            }
        });
    }

    public static void resumeTransfer(Context context, TransferGroup group, TransferGroup.Assignee assignee)
    {
        AppUtils.startForegroundService(context, new Intent(context, CommunicationService.class)
                .setAction(CommunicationService.ACTION_SEAMLESS_RECEIVE)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, group.groupId)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, assignee.deviceId));
    }

    public interface ConnectionUpdatedListener
    {
        void onConnectionUpdated(NetworkDevice.Connection connection, TransferGroup.Assignee assignee);
    }
}
