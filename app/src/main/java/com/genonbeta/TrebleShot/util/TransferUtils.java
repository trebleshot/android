package com.genonbeta.TrebleShot.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.dialog.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.util.Interrupter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
public class TransferUtils
{
    public static final String TAG = TransferUtils.class.getSimpleName();

    public static void changeConnection(FragmentActivity activity, final AccessDatabase database, final TransferGroup group, final NetworkDevice device, final ConnectionUpdatedListener listener)
    {
        new ConnectionChooserDialog(activity, device, new OnDeviceSelectedListener()
        {
            @Override
            public void onDeviceSelected(NetworkDevice.Connection connection, List<NetworkDevice.Connection> connectionList)
            {
                // FIXME: 7/18/19 Changing the connection for a transfer doesn't work anymore because assignee needs Type.
                /*
                TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, device, connection);

                database.publish(assignee);

                if (listener != null)
                    listener.onConnectionUpdated(connection, assignee);*/
            }
        }).show();
    }

    @SuppressLint("DefaultLocale")
    public static long createUniqueTransferId(long groupId, String deviceId, TransferObject.Type type)
    {
        return String.format("%d_%s_%s", groupId, deviceId, type).hashCode();
    }

    public static SQLQuery.Select createIncomingSelection(long groupId)
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER).setWhere(
                String.format("%s = ? AND %s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID,
                        AccessDatabase.FIELD_TRANSFER_TYPE), String.valueOf(groupId),
                TransferObject.Type.INCOMING.toString());
    }

    public static SQLQuery.Select createIncomingSelection(long groupId, TransferObject.Flag flag, boolean equals)
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER).setWhere(
                String.format("%s = ? AND %s = ? AND %s " + (equals ? "=" : "!=") + " ?",
                        AccessDatabase.FIELD_TRANSFER_GROUPID, AccessDatabase.FIELD_TRANSFER_TYPE,
                        AccessDatabase.FIELD_TRANSFER_FLAG), String.valueOf(groupId),
                TransferObject.Type.INCOMING.toString(), flag.toString());
    }

    public static ShowingAssignee fetchFirstAssignee(AccessDatabase database, long groupId)
    {
        SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId));

        List<ShowingAssignee> assignees = database
                .castQuery(select, ShowingAssignee.class, new SQLiteDatabase.CastQueryListener<ShowingAssignee>()
                {
                    @Override
                    public void onObjectReconstructed(SQLiteDatabase db, CursorItem item, ShowingAssignee object)
                    {
                        object.device = new NetworkDevice(object.deviceId);
                        object.connection = new NetworkDevice.Connection(object);

                        try {
                            db.reconstruct(object.device);
                            db.reconstruct(object.connection);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

        return assignees.size() == 0 ? null : assignees.get(0);
    }

    public static ShowingAssignee fetchFirstAssignee(SnackbarSupport snackbar, AccessDatabase database, long groupId)
    {
        ShowingAssignee assignee = fetchFirstAssignee(database, groupId);

        if (assignee == null) {
            snackbar.createSnackbar(R.string.mesg_noReceiverOrSender)
                    .show();

            return null;
        }

        return assignee;
    }

    public static TransferObject fetchFirstValidIncomingTransfer(Context context, long groupId)
    {
        CursorItem receiverInstance = AppUtils.getDatabase(context).getFirstFromTable(
                createIncomingSelection(groupId, TransferObject.Flag.PENDING, true)
                        .setOrderBy(String.format("`%s` ASC, `%s` ASC", AccessDatabase.FIELD_TRANSFER_DIRECTORY,
                                AccessDatabase.FIELD_TRANSFER_NAME)));

        return receiverInstance == null
                ? null
                : new TransferObject(receiverInstance);
    }

    public static List<ShowingAssignee> loadAssigneeList(SQLiteDatabase database, long groupId)
    {
        SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId));

        return database.castQuery(select, ShowingAssignee.class, new SQLiteDatabase.CastQueryListener<ShowingAssignee>()
        {
            @Override
            public void onObjectReconstructed(SQLiteDatabase db, CursorItem item, ShowingAssignee object)
            {
                object.device = new NetworkDevice(object.deviceId);
                object.connection = new NetworkDevice.Connection(object);

                try {
                    db.reconstruct(object.device);
                } catch (Exception e) {
                    // Nope
                }

                try {
                    db.reconstruct(object.connection);
                } catch (Exception e) {
                    // Nope
                }
            }
        });
    }

    public static void pauseTransfer(Context context, TransferGroup group, TransferObject.Type type)
    {
        pauseTransfer(context, group.id, null, type);
    }

    public static void pauseTransfer(Context context, TransferGroup.Assignee assignee)
    {
        pauseTransfer(context, assignee.groupId, assignee.deviceId, assignee.type);
    }

    public static void pauseTransfer(Context context, long groupId, @Nullable String deviceId,
                                     TransferObject.Type type)
    {
        Intent intent = new Intent(context, CommunicationService.class)
                .setAction(CommunicationService.ACTION_STOP_TRANSFER)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, groupId)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, deviceId)
                .putExtra(CommunicationService.EXTRA_TRANSFER_TYPE, type.toString());

        AppUtils.startForegroundService(context, intent);
    }

    @Deprecated
    public static void requestStartSending(final Activity activity, final TransferGroup.Assignee assignee,
                                           final NetworkDevice device,
                                           final NetworkDevice.Connection connection)
    {
        final Context context = activity.getApplicationContext();

        WorkerService.RunningTask task = new WorkerService.RunningTask()
        {
            @Override
            protected void onRun()
            {
                CommunicationBridge.Client client = new CommunicationBridge.Client(
                        AppUtils.getDatabase(activity));

                try {
                    final CoolSocket.ActiveConnection activeConnection = client.communicate(device,
                            connection);

                    Interrupter.Closer connectionCloser = new Interrupter.Closer()
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
                    };

                    getInterrupter().addCloser(connectionCloser);

                    JSONObject jsonRequest = new JSONObject();

                    jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB);
                    jsonRequest.put(Keyword.TRANSFER_GROUP_ID, assignee.groupId);

                    activeConnection.reply(jsonRequest.toString());

                    final CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                    activeConnection.getSocket().close();
                    getInterrupter().removeCloser(connectionCloser);

                    final JSONObject responseJSON = new JSONObject(response.response);

                    if (!responseJSON.getBoolean(Keyword.RESULT) && !activity.isFinishing())
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                @StringRes int msg = R.string.mesg_somethingWentWrong;
                                String errorMsg = Keyword.ERROR_UNKNOWN;

                                try {
                                    errorMsg = responseJSON.getString(Keyword.ERROR);
                                } catch (JSONException e) {
                                    // do nothing
                                }

                                switch (errorMsg) {
                                    case Keyword.ERROR_NOT_FOUND:
                                        msg = R.string.mesg_notValidTransfer;
                                        break;
                                    case Keyword.ERROR_REQUIRE_TRUSTZONE:
                                        msg = R.string.mesg_errorNotTrustZoneDevice;
                                        break;
                                    case Keyword.ERROR_NOT_ALLOWED:
                                        msg = R.string.mesg_notAllowed;
                                        break;
                                }

                                builder.setMessage(context.getString(msg));
                                builder.setNegativeButton(R.string.butn_close, null);
                                builder.setPositiveButton(R.string.butn_retry, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        requestStartSending(activity, assignee, device,
                                                connection);
                                    }
                                });

                                builder.show();
                            }
                        });
                } catch (Exception e) {
                    if (!activity.isFinishing())
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                                builder.setMessage(context.getString(R.string.mesg_connectionFailure));
                                builder.setNegativeButton(R.string.butn_close, null);

                                builder.setPositiveButton(R.string.butn_retry, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        requestStartSending(activity, assignee, device, connection);
                                    }
                                });

                                builder.show();
                            }
                        });
                }
            }
        };

        task.setTitle(activity.getString(R.string.mesg_communicating))
                .run(activity);
    }

    public static void recoverIncomingInterruptions(Context context, long groupId)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransferObject.Flag.PENDING.toString());

        AppUtils.getDatabase(context).update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_FLAG + "=? AND "
                                + AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
                        String.valueOf(groupId),
                        TransferObject.Flag.INTERRUPTED.toString(),
                        TransferObject.Type.INCOMING.toString()), contentValues);
    }

    public static void startTransferWithTest(final Activity activity, final TransferGroup group,
                                             final TransferGroup.Assignee assignee)
    {
        final Context context = activity.getApplicationContext();

        new WorkerService.RunningTask()
        {
            @Override
            protected void onRun()
            {
                if (activity.isFinishing())
                    return;

                if (fetchFirstValidIncomingTransfer(activity, group.id) == null
                        && TransferObject.Type.INCOMING.equals(assignee.type)) {
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                            builder.setMessage(R.string.mesg_noPendingTransferObjectExists);
                            builder.setNegativeButton(R.string.butn_close, null);

                            builder.setPositiveButton(R.string.butn_retryReceiving, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    recoverIncomingInterruptions(activity, group.id);
                                    startTransferWithTest(activity, group, assignee);
                                }
                            });

                            builder.show();
                        }
                    });
                } else if (TransferObject.Type.INCOMING.equals(assignee.type) && !FileUtils.getSavePath(
                        activity, group).getUri().toString().equals(group.savePath)) {
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
                                    startTransfer(activity, assignee);
                                }
                            });

                            builder.show();
                        }
                    });
                } else
                    startTransfer(activity, assignee);
            }
        }.setTitle(activity.getString(R.string.mesg_completing)).run(activity);
    }

    public static void startTransfer(final Activity activity, final TransferGroup.Assignee assignee)
    {
        if (activity != null && !activity.isFinishing())
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    try {
                        final NetworkDevice networkDevice = new NetworkDevice(assignee.deviceId);

                        AppUtils.getDatabase(activity).reconstruct(networkDevice);

                        new EstablishConnectionDialog(activity, networkDevice, new OnDeviceSelectedListener()
                        {
                            @Override
                            public void onDeviceSelected(NetworkDevice.Connection connection, List<NetworkDevice.Connection> availableInterfaces)
                            {
                                if (!assignee.connectionAdapter.equals(connection.adapterName)) {
                                    assignee.connectionAdapter = connection.adapterName;

                                    AppUtils.getDatabase(activity)
                                            .publish(assignee);
                                }

                                AppUtils.startForegroundService(activity, new Intent(activity,
                                        CommunicationService.class)
                                        .setAction(CommunicationService.ACTION_START_TRANSFER)
                                        .putExtra(CommunicationService.EXTRA_GROUP_ID, assignee.groupId)
                                        .putExtra(CommunicationService.EXTRA_DEVICE_ID, assignee.deviceId)
                                        .putExtra(CommunicationService.EXTRA_TRANSFER_TYPE, assignee.type.toString()));
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
                                        startTransfer(activity, assignee);
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
