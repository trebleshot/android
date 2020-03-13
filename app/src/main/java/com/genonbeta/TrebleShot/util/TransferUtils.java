package com.genonbeta.TrebleShot.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.dialog.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.genonbeta.android.framework.util.Interrupter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.genonbeta.TrebleShot.service.WorkerService.AttachedTaskListener;
import static com.genonbeta.TrebleShot.service.WorkerService.RunningTask;

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
public class TransferUtils
{
    public static final String TAG = TransferUtils.class.getSimpleName();

    private static void appendOutgoingData(PreloadedGroup group, TransferObject object, TransferObject.Flag flag)
    {
        group.bytesOutgoing += object.size;
        group.numberOfOutgoing++;

        if (TransferObject.Flag.DONE.equals(flag)) {
            group.bytesOutgoingCompleted += object.size;
            group.numberOfOutgoingCompleted++;
        } else if (TransferObject.Flag.IN_PROGRESS.equals(flag))
            group.bytesOutgoingCompleted += flag.getBytesValue();
        else if (TransferUtils.isError(flag))
            group.hasIssues = true;
    }

    public static void changeConnection(final FragmentActivity activity, final NetworkDevice device,
                                        final TransferAssignee assignee, final ConnectionUpdatedListener listener)
    {
        new ConnectionChooserDialog(activity, device, (connection, connectionList) -> {
            try {
                AppUtils.getKuick(activity).reconstruct(assignee);
                AppUtils.getKuick(activity).publish(assignee);
                AppUtils.getKuick(activity).broadcast();

                if (listener != null)
                    listener.onConnectionUpdated(connection, assignee);
            } catch (ReconstructionFailedException e) {
                e.printStackTrace();
            }
        }).show();
    }

    public static void createFolderStructure(List<TransferObject> list, long groupId, DocumentFile file,
                                             String directory, Interrupter interrupter, AttachedTaskListener listener)
            throws InterruptedException
    {
        DocumentFile[] files = file.listFiles();

        if (files == null || files.length <= 0)
            return;

        if (listener != null)
            listener.updateTaskPosition(0, files.length);

        for (DocumentFile thisFile : files) {
            if (listener != null)
                listener.updateTaskPosition(1, 0);

            if (interrupter.interrupted())
                throw new InterruptedException();

            if (thisFile.isDirectory()) {
                createFolderStructure(list, groupId, thisFile, (directory == null ? null
                        : directory + File.separator) + thisFile.getName(), interrupter, listener);
                continue;
            }

            try {
                list.add(TransferObject.from(thisFile, groupId, directory));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("DefaultLocale")
    public static long createUniqueTransferId(long groupId, String deviceId, TransferObject.Type type)
    {
        return String.format("%d_%s_%s", groupId, deviceId, type).hashCode();
    }

    public static SQLQuery.Select createIncomingSelection(long groupId)
    {
        return new SQLQuery.Select(Kuick.TABLE_TRANSFER).setWhere(
                String.format("%s = ? AND %s = ?", Kuick.FIELD_TRANSFER_GROUPID,
                        Kuick.FIELD_TRANSFER_TYPE), String.valueOf(groupId),
                TransferObject.Type.INCOMING.toString());
    }

    public static SQLQuery.Select createIncomingSelection(long groupId, TransferObject.Flag flag, boolean equals)
    {
        return new SQLQuery.Select(Kuick.TABLE_TRANSFER).setWhere(
                String.format("%s = ? AND %s = ? AND %s " + (equals ? "=" : "!=") + " ?",
                        Kuick.FIELD_TRANSFER_GROUPID, Kuick.FIELD_TRANSFER_TYPE,
                        Kuick.FIELD_TRANSFER_FLAG), String.valueOf(groupId),
                TransferObject.Type.INCOMING.toString(), flag.toString());
    }

    public static double getPercentageByFlag(TransferObject.Flag flag, long size)
    {
        if (TransferObject.Flag.DONE.equals(flag))
            return 1;

        long bytesValue = flag.getBytesValue();
        return bytesValue == 0 || size == 0 ? 0 : (float) bytesValue / size;
    }

    public static ShowingAssignee fetchFirstAssignee(Kuick kuick, long groupId)
    {
        SQLQuery.Select select = new SQLQuery.Select(Kuick.TABLE_TRANSFERASSIGNEE)
                .setWhere(Kuick.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId));

        List<ShowingAssignee> assignees = kuick.castQuery(select, ShowingAssignee.class, (db, item, object) -> {
            object.device = new NetworkDevice(object.deviceId);
            object.connection = new DeviceConnection(object);

            try {
                db.reconstruct(object.device);
                db.reconstruct(object.connection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return assignees.size() == 0 ? null : assignees.get(0);
    }

    public static ShowingAssignee fetchFirstAssignee(SnackbarPlacementProvider snackbar, Kuick kuick, long groupId)
    {
        ShowingAssignee assignee = fetchFirstAssignee(kuick, groupId);

        if (assignee == null) {
            snackbar.createSnackbar(R.string.mesg_noReceiverOrSender).show();
            return null;
        }

        return assignee;
    }

    public static TransferObject fetchFirstValidIncomingTransfer(Context context, long groupId)
    {
        Kuick kuick = AppUtils.getKuick(context);
        ContentValues receiverInstance = kuick.getFirstFromTable(
                createIncomingSelection(groupId, TransferObject.Flag.PENDING, true)
                        .setOrderBy(String.format("`%s` ASC, `%s` ASC", Kuick.FIELD_TRANSFER_DIRECTORY,
                                Kuick.FIELD_TRANSFER_NAME)));

        if (receiverInstance == null)
            return null;

        TransferObject object = new TransferObject();
        object.reconstruct(kuick.getWritableDatabase(), kuick, receiverInstance);
        return object;
    }

    public static boolean isError(TransferObject.Flag flag)
    {
        return TransferObject.Flag.INTERRUPTED.equals(flag) || TransferObject.Flag.REMOVED.equals(flag);
    }

    public static void loadAssigneeInfo(Context context, ShowingAssignee assignee)
    {
        loadAssigneeInfo(AppUtils.getKuick(context), assignee);
    }

    public static void loadAssigneeInfo(KuickDb kuick, ShowingAssignee assignee)
    {
        assignee.device = new NetworkDevice(assignee.deviceId);
        assignee.connection = new DeviceConnection(assignee);

        try {
            kuick.reconstruct(assignee.device);
        } catch (Exception ignored) {
        }

        try {
            kuick.reconstruct(assignee.connection);
        } catch (Exception ignored) {
        }
    }

    public static List<ShowingAssignee> loadAssigneeList(Context context, long groupId,
                                                         @Nullable TransferObject.Type type)
    {
        SQLQuery.Select selection = new SQLQuery.Select(Kuick.TABLE_TRANSFERASSIGNEE);

        if (type == null)
            selection.setWhere(Kuick.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId));
        else
            selection.setWhere(Kuick.FIELD_TRANSFERASSIGNEE_GROUPID + "=? AND "
                            + Kuick.FIELD_TRANSFERASSIGNEE_TYPE + "=?", String.valueOf(groupId),
                    type.toString());

        return AppUtils.getKuick(context).castQuery(selection, ShowingAssignee.class,
                (db, item, object) -> loadAssigneeInfo(db, object));
    }

    public static void loadGroupInfo(Context context, PreloadedGroup group,
                                     @Nullable TransferAssignee assignee)
    {
        if (assignee == null)
            loadGroupInfo(context, group);
        else
            loadGroupInfo(context, group, assignee.deviceId, assignee.type);
    }

    public static void loadGroupInfo(Context context, PreloadedGroup group)
    {
        loadGroupInfo(context, group, null, null);
    }

    public static void loadGroupInfo(Context context, PreloadedGroup group, @Nullable String deviceId,
                                     @Nullable TransferObject.Type type)
    {
        group.numberOfOutgoing = 0;
        group.numberOfIncoming = 0;
        group.numberOfOutgoingCompleted = 0;
        group.numberOfIncomingCompleted = 0;
        group.bytesOutgoing = 0;
        group.bytesIncoming = 0;
        group.bytesOutgoingCompleted = 0;
        group.bytesIncomingCompleted = 0;
        group.isRunning = false;
        group.hasIssues = false;

        SQLQuery.Select selection = new SQLQuery.Select(Kuick.TABLE_TRANSFER).setWhere(
                Kuick.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(group.id));

        if (type == null)
            selection.setWhere(Kuick.FIELD_TRANSFER_GROUPID + "=?",
                    String.valueOf(group.id));
        else
            selection.setWhere(Kuick.FIELD_TRANSFER_GROUPID + "=? AND "
                            + Kuick.FIELD_TRANSFER_TYPE + "=?", String.valueOf(group.id),
                    type.toString());

        List<ShowingAssignee> assigneeList = TransferUtils.loadAssigneeList(context, group.id, type);
        List<TransferObject> objectList = AppUtils.getKuick(context).castQuery(selection, TransferObject.class);

        group.assignees = new ShowingAssignee[assigneeList.size()];

        assigneeList.toArray(group.assignees);

        for (TransferObject object : objectList) {
            if (TransferObject.Type.INCOMING.equals(object.type)) {
                group.bytesIncoming += object.size;
                group.numberOfIncoming++;

                TransferObject.Flag flag = object.getFlag();
                if (TransferObject.Flag.DONE.equals(flag)) {
                    group.bytesIncomingCompleted += object.size;
                    group.numberOfIncomingCompleted++;
                } else if (TransferObject.Flag.IN_PROGRESS.equals(flag))
                    group.bytesIncomingCompleted += flag.getBytesValue();
                else if (TransferUtils.isError(flag))
                    group.hasIssues = true;
            } else if (TransferObject.Type.OUTGOING.equals(object.type)) {
                if (deviceId != null)
                    appendOutgoingData(group, object, object.getFlag(deviceId));
                else if (assigneeList.size() < 1)
                    appendOutgoingData(group, object, TransferObject.Flag.PENDING);
                else {
                    for (ShowingAssignee assignee : assigneeList) {
                        if (!TransferObject.Type.OUTGOING.equals(assignee.type))
                            continue;

                        appendOutgoingData(group, object, object.getFlag(assignee.deviceId));
                    }
                }
            }
        }
    }

    public static void pauseTransfer(Context context, TransferGroup group, TransferObject.Type type)
    {
        pauseTransfer(context, group.id, null, type);
    }

    public static void pauseTransfer(Context context, TransferAssignee assignee)
    {
        pauseTransfer(context, assignee.groupId, assignee.deviceId, assignee.type);
    }

    public static void pauseTransfer(Context context, long groupId, @Nullable String deviceId,
                                     TransferObject.Type type)
    {
        Intent intent = new Intent(context, BackgroundService.class)
                .setAction(BackgroundService.ACTION_STOP_TRANSFER)
                .putExtra(BackgroundService.EXTRA_GROUP_ID, groupId)
                .putExtra(BackgroundService.EXTRA_DEVICE_ID, deviceId)
                .putExtra(BackgroundService.EXTRA_TRANSFER_TYPE, type.toString());

        AppUtils.startForegroundService(context, intent);
    }

    @Deprecated
    public static void requestStartSending(final Activity activity, final TransferAssignee assignee,
                                           final NetworkDevice device, final DeviceConnection connection)
    {
        final Context context = activity.getApplicationContext();

        RunningTask task = new RunningTask()
        {
            @Override
            protected void onRun()
            {
                CommunicationBridge.Client client = new CommunicationBridge.Client(
                        AppUtils.getKuick(activity));

                try {
                    final CoolSocket.ActiveConnection activeConnection = client.communicate(device, connection);

                    Interrupter.Closer connectionCloser = userAction -> {
                        try {
                            activeConnection.getSocket().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };

                    getInterrupter().addCloser(connectionCloser);

                    JSONObject jsonRequest = new JSONObject()
                            .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                            .put(Keyword.TRANSFER_GROUP_ID, assignee.groupId);

                    activeConnection.reply(jsonRequest.toString());

                    final JSONObject responseJSON = new JSONObject(activeConnection.receive().response);
                    activeConnection.getSocket().close();
                    getInterrupter().removeCloser(connectionCloser);

                    if (!responseJSON.getBoolean(Keyword.RESULT) && !activity.isFinishing())
                        activity.runOnUiThread(() -> {
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
                                case Keyword.ERROR_REQUIRE_TRUST:
                                    msg = R.string.mesg_errorNotTrusted;
                                    break;
                                case Keyword.ERROR_NOT_ALLOWED:
                                    msg = R.string.mesg_notAllowed;
                                    break;
                            }

                            builder.setMessage(context.getString(msg));
                            builder.setNegativeButton(R.string.butn_close, null);
                            builder.setPositiveButton(R.string.butn_retry,
                                    (dialog, which) -> requestStartSending(activity, assignee, device,
                                            connection));

                            builder.show();
                        });
                } catch (Exception e) {
                    if (!activity.isFinishing())
                        activity.runOnUiThread(() -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                            builder.setMessage(context.getString(R.string.mesg_connectionFailure));
                            builder.setNegativeButton(R.string.butn_close, null);

                            builder.setPositiveButton(R.string.butn_retry,
                                    (dialog, which) -> requestStartSending(activity, assignee,
                                            device, connection));

                            builder.show();
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
        contentValues.put(Kuick.FIELD_TRANSFER_FLAG, TransferObject.Flag.PENDING.toString());

        AppUtils.getKuick(context).update(new SQLQuery.Select(Kuick.TABLE_TRANSFER)
                .setWhere(Kuick.FIELD_TRANSFER_GROUPID + "=? AND "
                                + Kuick.FIELD_TRANSFER_FLAG + "=? AND "
                                + Kuick.FIELD_TRANSFER_TYPE + "=?",
                        String.valueOf(groupId),
                        TransferObject.Flag.INTERRUPTED.toString(),
                        TransferObject.Type.INCOMING.toString()), contentValues);
        AppUtils.getKuick(context).broadcast();
    }

    public static void startTransferWithTest(final Activity activity, final TransferGroup group,
                                             final TransferAssignee assignee)
    {
        final Context context = activity.getApplicationContext();

        new RunningTask()
        {
            @Override
            protected void onRun()
            {
                if (activity.isFinishing())
                    return;

                if (TransferObject.Type.INCOMING.equals(assignee.type)
                        && fetchFirstValidIncomingTransfer(activity, group.id) == null) {
                    activity.runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                        builder.setMessage(R.string.mesg_noPendingTransferObjectExists);
                        builder.setNegativeButton(R.string.butn_close, null);

                        builder.setPositiveButton(R.string.butn_retryReceiving, (dialog, which) -> {
                            recoverIncomingInterruptions(activity, group.id);
                            startTransferWithTest(activity, group, assignee);
                        });

                        builder.show();
                    });
                } else if (TransferObject.Type.INCOMING.equals(assignee.type) && !FileUtils.getSavePath(
                        activity, group).getUri().toString().equals(group.savePath)) {
                    activity.runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                        builder.setMessage(context.getString(R.string.mesg_notSavingToChosenLocation,
                                FileUtils.getReadableUri(group.savePath)));
                        builder.setNegativeButton(R.string.butn_close, null);

                        builder.setPositiveButton(R.string.butn_gotIt,
                                (dialog, which) -> startTransfer(activity, assignee));

                        builder.show();
                    });
                } else
                    startTransfer(activity, assignee);
            }
        }.setTitle(activity.getString(R.string.mesg_completing)).run(activity);
    }

    public static void startTransfer(final Activity activity, final TransferAssignee assignee)
    {
        if (activity != null && !activity.isFinishing())
            activity.runOnUiThread(() -> {
                try {
                    final NetworkDevice networkDevice = new NetworkDevice(assignee.deviceId);

                    AppUtils.getKuick(activity).reconstruct(networkDevice);

                    new EstablishConnectionDialog(activity, networkDevice, (connection, availableInterfaces) -> {
                        if (!assignee.connectionAdapter.equals(connection.adapterName)) {
                            assignee.connectionAdapter = connection.adapterName;

                            AppUtils.getKuick(activity).publish(assignee);
                            AppUtils.getKuick(activity).broadcast();
                        }

                        AppUtils.startForegroundService(activity, new Intent(activity,
                                BackgroundService.class)
                                .setAction(BackgroundService.ACTION_START_TRANSFER)
                                .putExtra(BackgroundService.EXTRA_GROUP_ID, assignee.groupId)
                                .putExtra(BackgroundService.EXTRA_DEVICE_ID, assignee.deviceId)
                                .putExtra(BackgroundService.EXTRA_TRANSFER_TYPE, assignee.type.toString()));
                    }).show();
                } catch (Exception e) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.mesg_somethingWentWrong)
                            .setNegativeButton(R.string.butn_cancel, null)
                            .setPositiveButton(R.string.butn_retry,
                                    (dialog, which) -> startTransfer(activity, assignee))
                            .show();
                }
            });
    }

    public interface ConnectionUpdatedListener
    {
        void onConnectionUpdated(DeviceConnection connection, TransferAssignee assignee);
    }
}
