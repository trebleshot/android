package com.genonbeta.TrebleShot.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.exception.AssigneeNotFoundException;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.TransferGroupNotFoundException;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.task.FileTransferTask;
import com.genonbeta.TrebleShot.task.InitializeTransferTask;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.Progress;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.genonbeta.android.framework.util.Stoppable;

import java.io.File;
import java.util.List;

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
public class Transfers
{
    public static final String TAG = Transfers.class.getSimpleName();

    private static void appendOutgoingData(IndexOfTransferGroup group, TransferObject object, TransferObject.Flag flag)
    {
        group.bytesOutgoing += object.size;
        group.numberOfOutgoing++;

        if (TransferObject.Flag.DONE.equals(flag)) {
            group.bytesOutgoingCompleted += object.size;
            group.numberOfOutgoingCompleted++;
        } else if (TransferObject.Flag.IN_PROGRESS.equals(flag))
            group.bytesOutgoingCompleted += flag.getBytesValue();
        else if (Transfers.isError(flag))
            group.hasIssues = true;
    }

    public static void createFolderStructure(List<TransferObject> list, long groupId, DocumentFile file,
                                             String directory, Stoppable stoppable, Progress.Listener progress)
            throws InterruptedException
    {
        DocumentFile[] files = file.listFiles();

        if (files == null || files.length <= 0)
            return;

        Progress.addToTotal(progress, files.length);

        for (DocumentFile thisFile : files) {
            Progress.addToCurrent(progress, 1);

            if (stoppable.isInterrupted())
                throw new InterruptedException();

            if (thisFile.isDirectory()) {
                createFolderStructure(list, groupId, thisFile, (directory == null ? null
                        : directory + File.separator) + thisFile.getName(), stoppable, progress);
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

    public static SQLQuery.Select createAddressSelection(String deviceId)
    {
        return new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=?", deviceId)
                .setOrderBy(Kuick.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC");
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
            object.device = new Device(object.deviceId);

            try {
                db.reconstruct(object.device);
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

    public static List<DeviceAddress> getAddressListFor(KuickDb kuick, String deviceId)
            throws ConnectionNotFoundException
    {
        List<DeviceAddress> addressList = kuick.castQuery(createAddressSelection(deviceId), DeviceAddress.class);
        if (addressList.size() <= 0)
            throw new ConnectionNotFoundException(deviceId);
        return addressList;
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
        assignee.device = new Device(assignee.deviceId);

        try {
            kuick.reconstruct(assignee.device);
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

    public static void loadGroupInfo(Context context, IndexOfTransferGroup group, @Nullable TransferAssignee assignee)
    {
        if (assignee == null)
            loadGroupInfo(context, group);
        else
            loadGroupInfo(context, group, assignee.deviceId, assignee.type);
    }

    public static void loadGroupInfo(Context context, IndexOfTransferGroup group)
    {
        loadGroupInfo(context, group, null, null);
    }

    public static void loadGroupInfo(Context context, IndexOfTransferGroup index, @Nullable String deviceId,
                                     @Nullable TransferObject.Type type)
    {
        TransferGroup group = index.group;

        index.numberOfOutgoing = 0;
        index.numberOfIncoming = 0;
        index.numberOfOutgoingCompleted = 0;
        index.numberOfIncomingCompleted = 0;
        index.bytesOutgoing = 0;
        index.bytesIncoming = 0;
        index.bytesOutgoingCompleted = 0;
        index.bytesIncomingCompleted = 0;
        index.isRunning = false;
        index.hasIssues = false;

        SQLQuery.Select selection = new SQLQuery.Select(Kuick.TABLE_TRANSFER).setWhere(
                Kuick.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(group.id));

        if (type == null)
            selection.setWhere(Kuick.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(group.id));
        else
            selection.setWhere(Kuick.FIELD_TRANSFER_GROUPID + "=? AND " + Kuick.FIELD_TRANSFER_TYPE + "=?",
                    String.valueOf(group.id), type.toString());

        List<ShowingAssignee> assigneeList = loadAssigneeList(context, group.id, type);
        List<TransferObject> objectList = AppUtils.getKuick(context).castQuery(selection, TransferObject.class);

        index.assignees = new ShowingAssignee[assigneeList.size()];

        assigneeList.toArray(index.assignees);

        for (TransferObject object : objectList) {
            if (TransferObject.Type.INCOMING.equals(object.type)) {
                index.bytesIncoming += object.size;
                index.numberOfIncoming++;

                TransferObject.Flag flag = object.getFlag();
                if (TransferObject.Flag.DONE.equals(flag)) {
                    index.bytesIncomingCompleted += object.size;
                    index.numberOfIncomingCompleted++;
                } else if (TransferObject.Flag.IN_PROGRESS.equals(flag))
                    index.bytesIncomingCompleted += flag.getBytesValue();
                else if (Transfers.isError(flag))
                    index.hasIssues = true;
            } else if (TransferObject.Type.OUTGOING.equals(object.type)) {
                if (deviceId != null)
                    appendOutgoingData(index, object, object.getFlag(deviceId));
                else if (assigneeList.size() < 1)
                    appendOutgoingData(index, object, TransferObject.Flag.PENDING);
                else {
                    for (ShowingAssignee assignee : assigneeList) {
                        if (!TransferObject.Type.OUTGOING.equals(assignee.type))
                            continue;

                        appendOutgoingData(index, object, object.getFlag(assignee.deviceId));
                    }
                }
            }
        }
    }

    public static void pauseTransfer(Activity activity, TransferAssignee assignee)
    {
        pauseTransfer(activity, assignee.groupId, assignee.deviceId, assignee.type);
    }

    public static void pauseTransfer(Activity activity, long groupId, @Nullable String deviceId,
                                     TransferObject.Type type)
    {
        AppUtils.interruptTasksBy(activity, FileTransferTask.identifyWith(groupId, deviceId, type), true);
    }

    @Deprecated
    public static void requestStartSending(final Activity activity, final TransferAssignee assignee,
                                           final Device device, final DeviceAddress connection)
    {
        BackgroundService.run(activity, new InitializeTransferTask(device, connection, assignee));
    }

    public static void recoverIncomingInterruptions(Context context, long groupId)
    {
        Kuick kuick = AppUtils.getKuick(context);
        ContentValues contentValues = new ContentValues();
        contentValues.put(Kuick.FIELD_TRANSFER_FLAG, TransferObject.Flag.PENDING.toString());

        kuick.update(new SQLQuery.Select(Kuick.TABLE_TRANSFER)
                .setWhere(Kuick.FIELD_TRANSFER_GROUPID + "=? AND  " + Kuick.FIELD_TRANSFER_FLAG + "=? AND "
                                + Kuick.FIELD_TRANSFER_TYPE + "=?", String.valueOf(groupId),
                        TransferObject.Flag.INTERRUPTED.toString(), TransferObject.Type.INCOMING.toString()), contentValues);
        kuick.broadcast();
    }

    public static void startTransferWithTest(final Activity activity, final TransferGroup group,
                                             final TransferAssignee assignee)
    {
        final Context context = activity.getApplicationContext();

        if (activity.isFinishing())
            return;

        if (TransferObject.Type.INCOMING.equals(assignee.type)
                && fetchFirstValidIncomingTransfer(activity, group.id) == null) {
            activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_noPendingTransferObjectExists)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_retryReceiving, (dialog, which) -> {
                        recoverIncomingInterruptions(activity, group.id);
                        startTransferWithTest(activity, group, assignee);
                    })
                    .show());
        } else if (TransferObject.Type.INCOMING.equals(assignee.type) && !FileUtils.getSavePath(activity, group)
                .getUri().toString().equals(group.savePath)) {
            activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                    .setMessage(context.getString(R.string.mesg_notSavingToChosenLocation,
                            FileUtils.getReadableUri(group.savePath)))
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_gotIt, (dialog, which) -> startTransfer(activity, assignee))
                    .show());
        } else
            startTransfer(activity, assignee);
    }

    public static void startTransfer(final Activity activity, final TransferAssignee assignee)
    {
        if (activity != null && !activity.isFinishing())
            activity.runOnUiThread(() -> {
                try {
                    Device device = new Device(assignee.deviceId);
                    Kuick kuick = AppUtils.getKuick(activity);

                    kuick.reconstruct(device);

                    EstablishConnectionDialog.show(activity, device, connection -> {
                        try {
                            FileTransferTask task = FileTransferTask.createFrom(kuick, assignee.groupId,
                                    assignee.deviceId, assignee.type);

                            BackgroundService.run(activity, task);
                        } catch (TransferGroupNotFoundException e) {
                            e.printStackTrace();
                        } catch (DeviceNotFoundException e) {
                            e.printStackTrace();
                        } catch (ConnectionNotFoundException e) {
                            e.printStackTrace();
                        } catch (AssigneeNotFoundException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.mesg_somethingWentWrong)
                            .setNegativeButton(R.string.butn_cancel, null)
                            .setPositiveButton(R.string.butn_retry, (dialog, which) -> startTransfer(activity, assignee))
                            .show();
                }
            });
    }

    public interface ConnectionUpdatedListener
    {
        void onConnectionUpdated(DeviceAddress connection, TransferAssignee assignee);
    }
}
