package com.genonbeta.TrebleShot.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.FindConnectionDialog;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException;
import com.genonbeta.TrebleShot.task.FileTransferTask;
import com.genonbeta.TrebleShot.task.InitializeTransferTask;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;

import java.io.File;
import java.util.List;

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
public class Transfers
{
    public static final String TAG = Transfers.class.getSimpleName();

    private static void appendOutgoingData(IndexOfTransferGroup group, TransferItem object, TransferItem.Flag flag)
    {
        group.bytesOutgoing += object.size;
        group.numberOfOutgoing++;

        if (TransferItem.Flag.DONE.equals(flag)) {
            group.bytesOutgoingCompleted += object.size;
            group.numberOfOutgoingCompleted++;
        } else if (TransferItem.Flag.IN_PROGRESS.equals(flag))
            group.bytesOutgoingCompleted += flag.getBytesValue();
        else if (Transfers.isError(flag))
            group.hasIssues = true;
    }

    public static void createFolderStructure(List<TransferItem> list, long transferId, DocumentFile file,
                                             String directory, AsyncTask task)
            throws TaskStoppedException
    {
        DocumentFile[] files = file.listFiles();

        if (files == null || files.length <= 0)
            return;

        task.progress().addToTotal(files.length);

        for (DocumentFile thisFile : files) {
            task.throwIfStopped();
            task.setOngoingContent(thisFile.getName());
            task.progress().addToCurrent(1);

            if (thisFile.isDirectory()) {
                createFolderStructure(list, transferId, thisFile, (directory == null ? null
                        : directory + File.separator) + thisFile.getName(), task);
                continue;
            }

            list.add(TransferItem.from(thisFile, transferId, directory));
        }
    }

    @SuppressLint("DefaultLocale")
    public static long createUniqueTransferId(long transferId, String deviceId, TransferItem.Type type)
    {
        return String.format("%d_%s_%s", transferId, deviceId, type).hashCode();
    }

    public static SQLQuery.Select createIncomingSelection(long transferId)
    {
        return new SQLQuery.Select(Kuick.TABLE_TRANSFERITEM).setWhere(
                String.format("%s = ? AND %s = ?", Kuick.FIELD_TRANSFERITEM_TRANSFERID,
                        Kuick.FIELD_TRANSFERITEM_TYPE), String.valueOf(transferId),
                TransferItem.Type.INCOMING.toString());
    }

    public static SQLQuery.Select createIncomingSelection(long transferId, TransferItem.Flag flag, boolean equals)
    {
        return new SQLQuery.Select(Kuick.TABLE_TRANSFERITEM).setWhere(
                String.format("%s = ? AND %s = ? AND %s " + (equals ? "=" : "!=") + " ?",
                        Kuick.FIELD_TRANSFERITEM_TRANSFERID, Kuick.FIELD_TRANSFERITEM_TYPE,
                        Kuick.FIELD_TRANSFERITEM_FLAG), String.valueOf(transferId),
                TransferItem.Type.INCOMING.toString(), flag.toString());
    }

    public static SQLQuery.Select createAddressSelection(String deviceId)
    {
        return new SQLQuery.Select(Kuick.TABLE_DEVICEADDRESS)
                .setWhere(Kuick.FIELD_DEVICEADDRESS_DEVICEID + "=?", deviceId)
                .setOrderBy(Kuick.FIELD_DEVICEADDRESS_LASTCHECKEDDATE + " DESC");
    }

    public static double getPercentageByFlag(TransferItem.Flag flag, long size)
    {
        if (TransferItem.Flag.DONE.equals(flag))
            return 1;

        long bytesValue = flag.getBytesValue();
        return bytesValue == 0 || size == 0 ? 0 : (float) bytesValue / size;
    }

    public static LoadedMember fetchFirstMember(Kuick kuick, long transferId)
    {
        SQLQuery.Select select = new SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER)
                .setWhere(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=?", String.valueOf(transferId));

        List<LoadedMember> memberList = kuick.castQuery(select, LoadedMember.class, (db, item, object) -> {
            object.device = new Device(object.deviceId);

            try {
                db.reconstruct(object.device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return memberList.size() == 0 ? null : memberList.get(0);
    }

    public static LoadedMember fetchFirstMember(SnackbarPlacementProvider snackbar, Kuick kuick, long transferId)
    {
        LoadedMember member = fetchFirstMember(kuick, transferId);

        if (member == null) {
            snackbar.createSnackbar(R.string.mesg_noReceiverOrSender).show();
            return null;
        }

        return member;
    }

    public static TransferItem fetchFirstValidIncomingTransferItem(Context context, long transferId)
    {
        Kuick kuick = AppUtils.getKuick(context);
        ContentValues receiverInstance = kuick.getFirstFromTable(
                createIncomingSelection(transferId, TransferItem.Flag.PENDING, true)
                        .setOrderBy(String.format("`%s` ASC, `%s` ASC", Kuick.FIELD_TRANSFERITEM_DIRECTORY,
                                Kuick.FIELD_TRANSFERITEM_NAME)));

        if (receiverInstance == null)
            return null;

        TransferItem object = new TransferItem();
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

    public static boolean isError(TransferItem.Flag flag)
    {
        return TransferItem.Flag.INTERRUPTED.equals(flag) || TransferItem.Flag.REMOVED.equals(flag);
    }

    public static void loadMemberInfo(Context context, LoadedMember member)
    {
        loadMemberInfo(AppUtils.getKuick(context), member);
    }

    public static void loadMemberInfo(KuickDb kuick, LoadedMember member)
    {
        member.device = new Device(member.deviceId);

        try {
            kuick.reconstruct(member.device);
        } catch (Exception ignored) {
        }
    }

    public static List<LoadedMember> loadMemberList(Context context, long transferId,
                                                    @Nullable TransferItem.Type type)
    {
        SQLQuery.Select selection = new SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER);

        if (type == null)
            selection.setWhere(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=?", String.valueOf(transferId));
        else
            selection.setWhere(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=? AND "
                            + Kuick.FIELD_TRANSFERMEMBER_TYPE + "=?", String.valueOf(transferId),
                    type.toString());

        return AppUtils.getKuick(context).castQuery(selection, LoadedMember.class,
                (db, item, object) -> loadMemberInfo(db, object));
    }

    public static void loadGroupInfo(Context context, IndexOfTransferGroup group, @Nullable TransferMember member)
    {
        if (member == null)
            loadGroupInfo(context, group);
        else
            loadGroupInfo(context, group, member.deviceId, member.type);
    }

    public static void loadGroupInfo(Context context, IndexOfTransferGroup group)
    {
        loadGroupInfo(context, group, null, null);
    }

    public static void loadGroupInfo(Context context, IndexOfTransferGroup index, @Nullable String deviceId,
                                     @Nullable TransferItem.Type type)
    {
        Transfer transfer = index.transfer;

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

        SQLQuery.Select selection = new SQLQuery.Select(Kuick.TABLE_TRANSFERITEM).setWhere(
                Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=?", String.valueOf(transfer.id));

        if (type == null)
            selection.setWhere(Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=?", String.valueOf(transfer.id));
        else
            selection.setWhere(Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND " + Kuick.FIELD_TRANSFERITEM_TYPE + "=?",
                    String.valueOf(transfer.id), type.toString());

        List<LoadedMember> memberList = loadMemberList(context, transfer.id, type);
        List<TransferItem> objectList = AppUtils.getKuick(context).castQuery(selection, TransferItem.class);

        index.members = new LoadedMember[memberList.size()];

        memberList.toArray(index.members);

        for (TransferItem object : objectList) {
            if (TransferItem.Type.INCOMING.equals(object.type)) {
                index.bytesIncoming += object.size;
                index.numberOfIncoming++;

                TransferItem.Flag flag = object.getFlag();
                if (TransferItem.Flag.DONE.equals(flag)) {
                    index.bytesIncomingCompleted += object.size;
                    index.numberOfIncomingCompleted++;
                } else if (TransferItem.Flag.IN_PROGRESS.equals(flag))
                    index.bytesIncomingCompleted += flag.getBytesValue();
                else if (Transfers.isError(flag))
                    index.hasIssues = true;
            } else if (TransferItem.Type.OUTGOING.equals(object.type)) {
                if (deviceId != null)
                    appendOutgoingData(index, object, object.getFlag(deviceId));
                else if (memberList.size() < 1)
                    appendOutgoingData(index, object, TransferItem.Flag.PENDING);
                else {
                    for (LoadedMember member : memberList) {
                        if (!TransferItem.Type.OUTGOING.equals(member.type))
                            continue;

                        appendOutgoingData(index, object, object.getFlag(member.deviceId));
                    }
                }
            }
        }
    }

    public static void pauseTransfer(Activity activity, TransferMember member)
    {
        pauseTransfer(activity, member.transferId, member.deviceId, member.type);
    }

    public static void pauseTransfer(Activity activity, long transferId, @Nullable String deviceId,
                                     TransferItem.Type type)
    {
        App.interruptTasksBy(activity, FileTransferTask.identifyWith(transferId, deviceId, type), true);
    }

    @Deprecated
    public static void requestStartSending(Activity activity, TransferMember member, Device device,
                                           DeviceAddress address)
    {
        App.run(activity, new InitializeTransferTask(device, address, member));
    }

    public static void recoverIncomingInterruptions(Context context, long transferId)
    {
        Kuick kuick = AppUtils.getKuick(context);
        ContentValues contentValues = new ContentValues();
        contentValues.put(Kuick.FIELD_TRANSFERITEM_FLAG, TransferItem.Flag.PENDING.toString());

        kuick.update(new SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                .setWhere(Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND  " + Kuick.FIELD_TRANSFERITEM_FLAG + "=? AND "
                                + Kuick.FIELD_TRANSFERITEM_TYPE + "=?", String.valueOf(transferId),
                        TransferItem.Flag.INTERRUPTED.toString(), TransferItem.Type.INCOMING.toString()), contentValues);
        kuick.broadcast();
    }

    public static void startTransferWithTest(final Activity activity, final Transfer transfer,
                                             final TransferMember member)
    {
        final Context context = activity.getApplicationContext();

        if (activity.isFinishing())
            return;

        if (TransferItem.Type.INCOMING.equals(member.type)
                && fetchFirstValidIncomingTransferItem(activity, transfer.id) == null) {
            activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_noPendingTransferObjectExists)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_retryReceiving, (dialog, which) -> {
                        recoverIncomingInterruptions(activity, transfer.id);
                        startTransferWithTest(activity, transfer, member);
                    })
                    .show());
        } else if (TransferItem.Type.INCOMING.equals(member.type) && !FileUtils.getSavePath(activity, transfer)
                .getUri().toString().equals(transfer.savePath)) {
            activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                    .setMessage(context.getString(R.string.mesg_notSavingToChosenLocation,
                            FileUtils.getReadableUri(transfer.savePath)))
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_gotIt, (dialog, which) -> startTransfer(activity, member))
                    .show());
        } else
            startTransfer(activity, member);
    }

    public static void startTransfer(final Activity activity, final TransferMember member)
    {
        if (activity != null && !activity.isFinishing())
            activity.runOnUiThread(() -> {
                try {
                    final FileTransferTask task = FileTransferTask.createFrom(AppUtils.getKuick(activity),
                            member.transferId, member.deviceId, member.type);

                    FindConnectionDialog.show(activity, task.device, (device, address) -> {
                        try {
                            App.run(activity, task);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.mesg_somethingWentWrong)
                            .setNegativeButton(R.string.butn_cancel, null)
                            .setPositiveButton(R.string.butn_retry, (dialog, which) -> startTransfer(activity, member))
                            .show();
                }
            });
    }
}
