package com.genonbeta.TrebleShot.util

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.model.*
import com.genonbeta.TrebleShot.dialog.FindConnectionDialog
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException
import com.genonbeta.TrebleShot.task.FileTransferStarterTask
import com.genonbeta.TrebleShot.task.FileTransferTask
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import java.io.File

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
object Transfers {
    val TAG = Transfers::class.java.simpleName

    private fun appendOutgoingData(group: TransferIndex, item: TransferItem, flag: TransferItem.Flag) {
        group.bytesOutgoing += item.getComparableSize()
        group.numberOfOutgoing++
        if (TransferItem.Flag.DONE == flag) {
            group.bytesOutgoingCompleted += item.getComparableSize()
            group.numberOfOutgoingCompleted++
        } else if (TransferItem.Flag.IN_PROGRESS == flag) {
            group.bytesOutgoingCompleted += flag.bytesValue
        } else if (isError(flag)) group.hasIssues = true
    }

    @Throws(TaskStoppedException::class)
    fun createFolderStructure(
        list: MutableList<TransferItem>, transferId: Long, file: DocumentFile,
        directory: String?, task: AsyncTask,
    ) {
        val files = file.listFiles()
        if (files.isEmpty()) return
        task.progress.increaseTotalBy(files.size)
        for (thisFile in files) {
            task.throwIfStopped()
            task.ongoingContent = thisFile.getName()
            task.progress.increaseBy(1)
            if (thisFile.isDirectory()) {
                createFolderStructure(
                    list,
                    transferId,
                    thisFile,
                    (if (directory == null) null else directory + File.separator) + thisFile.getName(),
                    task
                )
                continue
            }
            list.add(TransferItem.from(thisFile, transferId, directory))
        }
    }

    @SuppressLint("DefaultLocale")
    fun createUniqueTransferId(transferId: Long, deviceId: String, type: TransferItem.Type): Long {
        return String.format("%d_%s_%s", transferId, deviceId, type).hashCode().toLong()
    }

    fun createIncomingSelection(transferId: Long): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_TRANSFERITEM).setWhere(
            String.format(
                "%s = ? AND %s = ?", Kuick.FIELD_TRANSFERITEM_TRANSFERID,
                Kuick.FIELD_TRANSFERITEM_TYPE
            ), transferId.toString(),
            TransferItem.Type.INCOMING.toString()
        )
    }

    fun createIncomingSelection(transferId: Long, flag: TransferItem.Flag, equals: Boolean): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_TRANSFERITEM).setWhere(
            String.format(
                "%s = ? AND %s = ? AND %s " + (if (equals) "=" else "!=") + " ?",
                Kuick.FIELD_TRANSFERITEM_TRANSFERID, Kuick.FIELD_TRANSFERITEM_TYPE,
                Kuick.FIELD_TRANSFERITEM_FLAG
            ), transferId.toString(),
            TransferItem.Type.INCOMING.toString(), flag.toString()
        )
    }

    fun createAddressSelection(deviceId: String): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_DEVICEADDRESS)
            .setWhere(Kuick.FIELD_DEVICEADDRESS_DEVICEID + "=?", deviceId)
            .setOrderBy(Kuick.FIELD_DEVICEADDRESS_LASTCHECKEDDATE + " DESC")
    }

    fun getPercentageByFlag(flag: TransferItem.Flag, size: Long): Double {
        if (TransferItem.Flag.DONE == flag) return 1.0
        val bytesValue = flag.bytesValue
        return if (bytesValue == 0L || size == 0L) 0.0 else (bytesValue.toFloat() / size).toDouble()
    }

    fun fetchFirstMember(kuick: Kuick, transferId: Long): LoadedMember? {
        val select: SQLQuery.Select = SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER)
            .setWhere(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=?", transferId.toString())
        val memberList: List<LoadedMember> = kuick.castQuery<Transfer, LoadedMember>(
            select,
            LoadedMember::class.java,
            object : KuickDb.CastQueryListener<LoadedMember> {
                override fun onObjectReconstructed(manager: KuickDb, values: ContentValues, item: LoadedMember) {
                    item.device = Device(item.deviceId)
                    try {
                        manager.reconstruct(item.device)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
        return if (memberList.isEmpty()) null else memberList[0]
    }

    fun fetchFirstMember(snackbar: SnackbarPlacementProvider, kuick: Kuick, transferId: Long): LoadedMember? {
        return fetchFirstMember(kuick, transferId) ?: run {
            snackbar.createSnackbar(R.string.mesg_noReceiverOrSender)?.show()
            null
        }
    }

    fun fetchFirstValidIncomingTransferItem(context: Context, transferId: Long): TransferItem? {
        val kuick = AppUtils.getKuick(context)
        val receiverInstance = kuick.getFirstFromTable(
            createIncomingSelection(
                transferId,
                TransferItem.Flag.PENDING,
                true
            )
                .setOrderBy(
                    String.format(
                        "`%s` ASC, `%s` ASC", Kuick.FIELD_TRANSFERITEM_DIRECTORY,
                        Kuick.FIELD_TRANSFERITEM_NAME
                    )
                )
        ) ?: return null
        val item = TransferItem()
        item.reconstruct(kuick.writableDatabase, kuick, receiverInstance)
        return item
    }

    @Throws(ConnectionNotFoundException::class)
    fun getAddressListFor(kuick: KuickDb, deviceId: String): List<DeviceAddress> {
        val addressList = kuick.castQuery(createAddressSelection(deviceId), DeviceAddress::class.java)
        if (addressList.isEmpty()) throw ConnectionNotFoundException(deviceId)
        return addressList
    }

    fun isError(flag: TransferItem.Flag): Boolean {
        return TransferItem.Flag.INTERRUPTED == flag || TransferItem.Flag.REMOVED == flag
    }

    fun loadMemberInfo(context: Context, member: LoadedMember) {
        loadMemberInfo(AppUtils.getKuick(context), member)
    }

    fun loadMemberInfo(kuick: KuickDb, member: LoadedMember) {
        member.device = Device(member.deviceId)
        try {
            kuick.reconstruct(member.device)
        } catch (ignored: Exception) {
        }
    }

    fun loadMemberList(context: Context, transferId: Long, type: TransferItem.Type?): List<LoadedMember> {
        val selection: SQLQuery.Select = SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER)
        if (type == null) selection.setWhere(
            Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=?",
            transferId.toString()
        ) else selection.setWhere(
            Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=? AND "
                    + Kuick.FIELD_TRANSFERMEMBER_TYPE + "=?", transferId.toString(),
            type.toString()
        )
        return AppUtils.getKuick(context).castQuery(selection, LoadedMember::class.java,
            object : KuickDb.CastQueryListener<LoadedMember> {
                override fun onObjectReconstructed(manager: KuickDb, values: ContentValues, item: LoadedMember) {
                    loadMemberInfo(manager, item)
                }
            }
        )
    }

    fun loadTransferInfo(context: Context, transfer: TransferIndex, member: TransferMember) {
        loadTransferInfo(context, transfer, member.deviceId, member.type)
    }

    @JvmOverloads
    fun loadTransferInfo(
        context: Context, index: TransferIndex, deviceId: String? = null, type: TransferItem.Type? = null,
    ) {
        val transfer: Transfer = index.transfer
        index.numberOfOutgoing = 0
        index.numberOfIncoming = 0
        index.numberOfOutgoingCompleted = 0
        index.numberOfIncomingCompleted = 0
        index.bytesOutgoing = 0
        index.bytesIncoming = 0
        index.bytesOutgoingCompleted = 0
        index.bytesIncomingCompleted = 0
        index.isRunning = false
        index.hasIssues = false
        val selection: SQLQuery.Select = SQLQuery.Select(Kuick.TABLE_TRANSFERITEM).setWhere(
            Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=?", transfer.id.toString()
        )
        if (type == null) selection.setWhere(
            Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=?",
            transfer.id.toString()
        ) else selection.setWhere(
            Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND " + Kuick.FIELD_TRANSFERITEM_TYPE + "=?",
            transfer.id.toString(),
            type.toString()
        )
        val memberList: List<LoadedMember> = loadMemberList(context, transfer.id, type)
        val objectList = AppUtils.getKuick(context).castQuery(selection, TransferItem::class.java)
        index.members = memberList.toTypedArray()
        for (item in objectList) {
            if (TransferItem.Type.INCOMING == item.type) {
                index.bytesIncoming += item.getComparableSize()
                index.numberOfIncoming++
                val flag = item.flag
                if (TransferItem.Flag.DONE == flag) {
                    index.bytesIncomingCompleted += item.getComparableSize()
                    index.numberOfIncomingCompleted++
                } else if (TransferItem.Flag.IN_PROGRESS == flag) {
                    index.bytesIncomingCompleted += flag.bytesValue
                } else if (isError(flag)
                ) index.hasIssues = true
            } else if (TransferItem.Type.OUTGOING == item.type) {
                if (deviceId != null) {
                    appendOutgoingData(index, item, item.getFlag(deviceId))
                } else if (memberList.isEmpty()) {
                    appendOutgoingData(index, item, TransferItem.Flag.PENDING)
                } else {
                    for (member in memberList) {
                        if (TransferItem.Type.OUTGOING != member.type) continue
                        appendOutgoingData(index, item, item.getFlag(member.deviceId))
                    }
                }
            }
        }
    }

    fun pauseTransfer(activity: Activity, member: TransferMember) {
        pauseTransfer(activity, member.transferId, member.deviceId, member.type)
    }

    fun pauseTransfer(activity: Activity, transferId: Long, deviceId: String, type: TransferItem.Type) {
        App.interruptTasksBy(
            activity,
            FileTransferTask.identifyWith(transferId, deviceId, type),
            true
        )
    }

    fun recoverIncomingInterruptions(context: Context, transferId: Long) {
        val kuick = AppUtils.getKuick(context)
        val contentValues = ContentValues()
        contentValues.put(Kuick.FIELD_TRANSFERITEM_FLAG, TransferItem.Flag.PENDING.toString())
        kuick.update(
            SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                .setWhere(
                    Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND  " + Kuick.FIELD_TRANSFERITEM_FLAG + "=? AND "
                            + Kuick.FIELD_TRANSFERITEM_TYPE + "=?", transferId.toString(),
                    TransferItem.Flag.INTERRUPTED.toString(), TransferItem.Type.INCOMING.toString()
                ), contentValues
        )
        kuick.broadcast()
    }

    fun startTransferWithTest(activity: Activity, transfer: Transfer, member: TransferMember) {
        if (activity.isFinishing) return

        val context = activity.applicationContext
        val savePath = transfer.savePath
        if (TransferItem.Type.INCOMING == member.type
            && fetchFirstValidIncomingTransferItem(activity, transfer.id) == null
        ) {
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_noPendingTransferObjectExists)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_retryReceiving) { dialog: DialogInterface?, which: Int ->
                        recoverIncomingInterruptions(activity, transfer.id)
                        startTransferWithTest(activity, transfer, member)
                    }
                    .show()
            }
        } else if (TransferItem.Type.INCOMING == member.type
            && Files.getSavePath(activity, transfer).getUri().toString() != savePath
        ) {
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setMessage(
                        context.getString(
                            R.string.mesg_notSavingToChosenLocation,
                            if (savePath == null) "null" else Files.getReadableUri(savePath)
                        )
                    )
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_gotIt) { dialog: DialogInterface?, which: Int ->
                        startTransfer(
                            activity,
                            member
                        )
                    }
                    .show()
            }
        } else startTransfer(activity, member)
    }

    fun startTransfer(activity: Activity, member: TransferMember) {
        if (!activity.isFinishing) activity.runOnUiThread {
            try {
                val task = FileTransferStarterTask.createFrom(
                    AppUtils.getKuick(activity),
                    member.transferId, member.deviceId, member.type
                )
                FindConnectionDialog.show(
                    activity,
                    task.device,
                    object : DeviceLoader.OnDeviceResolvedListener {
                        override fun onDeviceResolved(device: Device, address: DeviceAddress) {
                            try {
                                App.run(activity, task)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_somethingWentWrong)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_retry) { dialog: DialogInterface?, which: Int ->
                        startTransfer(
                            activity,
                            member
                        )
                    }
                    .show()
            }
        }
    }
}