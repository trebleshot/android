package com.genonbeta.TrebleShot.util

import android.content.*
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.dataobject.TransferItem.from
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.android.framework.io.DocumentFile
import java.io.File

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
object Transfers {
    val TAG = Transfers::class.java.simpleName
    private fun appendOutgoingData(group: TransferIndex?, item: TransferItem, flag: TransferItem.Flag) {
        group.bytesOutgoing += item.comparableSize
        group.numberOfOutgoing++
        if (TransferItem.Flag.DONE == flag) {
            group.bytesOutgoingCompleted += item.comparableSize
            group.numberOfOutgoingCompleted++
        } else if (TransferItem.Flag.IN_PROGRESS == flag) group.bytesOutgoingCompleted += flag.bytesValue else if (isError(
                flag
            )
        ) group.hasIssues = true
    }

    @Throws(TaskStoppedException::class)
    fun createFolderStructure(
        list: MutableList<TransferItem>, transferId: Long, file: DocumentFile,
        directory: String?, task: AsyncTask
    ) {
        val files = file.listFiles()
        if (files == null || files.size <= 0) return
        task.progress().addToTotal(files.size)
        for (thisFile in files) {
            task.throwIfStopped()
            task.ongoingContent = thisFile.name
            task.progress().addToCurrent(1)
            if (thisFile.isDirectory) {
                createFolderStructure(
                    list,
                    transferId,
                    thisFile,
                    (if (directory == null) null else directory + File.separator) + thisFile.name,
                    task
                )
                continue
            }
            list.add(from(thisFile, transferId, directory))
        }
    }

    @SuppressLint("DefaultLocale")
    fun createUniqueTransferId(transferId: Long, deviceId: String?, type: TransferItem.Type?): Long {
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

    fun createAddressSelection(deviceId: String?): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_DEVICEADDRESS)
            .setWhere(Kuick.FIELD_DEVICEADDRESS_DEVICEID + "=?", deviceId)
            .setOrderBy(Kuick.FIELD_DEVICEADDRESS_LASTCHECKEDDATE + " DESC")
    }

    fun getPercentageByFlag(flag: TransferItem.Flag, size: Long): Double {
        if (TransferItem.Flag.DONE == flag) return 1
        val bytesValue = flag.bytesValue
        return if (bytesValue == 0L || size == 0L) 0 else (bytesValue.toFloat() / size).toDouble()
    }

    fun fetchFirstMember(kuick: Kuick, transferId: Long): LoadedMember? {
        val select: SQLQuery.Select = SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER)
            .setWhere(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=?", transferId.toString())
        val memberList: List<LoadedMember> = kuick.castQuery<Transfer, LoadedMember>(
            select,
            LoadedMember::class.java,
            CastQueryListener<LoadedMember> { db: KuickDb, item: ContentValues?, item: LoadedMember ->
                item.device = Device(item.deviceId)
                try {
                    db.reconstruct<Void, Device>(item.device)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        return if (memberList.size == 0) null else memberList[0]
    }

    fun fetchFirstMember(snackbar: SnackbarPlacementProvider, kuick: Kuick, transferId: Long): LoadedMember? {
        val member: LoadedMember? = fetchFirstMember(kuick, transferId)
        if (member == null) {
            snackbar.createSnackbar(R.string.mesg_noReceiverOrSender).show()
            return null
        }
        return member
    }

    fun fetchFirstValidIncomingTransferItem(context: Context?, transferId: Long): TransferItem? {
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
    fun getAddressListFor(kuick: KuickDb?, deviceId: String?): List<DeviceAddress> {
        val addressList: List<DeviceAddress> =
            kuick.castQuery<Device, DeviceAddress>(createAddressSelection(deviceId), DeviceAddress::class.java)
        if (addressList.size <= 0) throw ConnectionNotFoundException(deviceId)
        return addressList
    }

    fun isError(flag: TransferItem.Flag): Boolean {
        return TransferItem.Flag.INTERRUPTED == flag || TransferItem.Flag.REMOVED == flag
    }

    fun loadMemberInfo(context: Context?, member: LoadedMember) {
        loadMemberInfo(AppUtils.getKuick(context), member)
    }

    fun loadMemberInfo(kuick: KuickDb?, member: LoadedMember) {
        member.device = Device(member.deviceId)
        try {
            kuick.reconstruct<Void, Device>(member.device)
        } catch (ignored: Exception) {
        }
    }

    fun loadMemberList(
        context: Context?, transferId: Long,
        type: TransferItem.Type?
    ): List<LoadedMember> {
        val selection: SQLQuery.Select = SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER)
        if (type == null) selection.setWhere(
            Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=?",
            transferId.toString()
        ) else selection.setWhere(
            Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=? AND "
                    + Kuick.FIELD_TRANSFERMEMBER_TYPE + "=?", transferId.toString(),
            type.toString()
        )
        return AppUtils.getKuick(context).castQuery<Transfer, LoadedMember>(selection, LoadedMember::class.java,
            CastQueryListener<LoadedMember> { db: KuickDb?, item: ContentValues?, item: LoadedMember? ->
                loadMemberInfo(
                    db,
                    item
                )
            })
    }

    fun loadTransferInfo(context: Context?, transfer: TransferIndex?, member: TransferMember?) {
        if (member == null) loadTransferInfo(context, transfer) else loadTransferInfo(
            context,
            transfer,
            member.deviceId,
            member.type
        )
    }

    @JvmOverloads
    fun loadTransferInfo(
        context: Context?, index: TransferIndex?, deviceId: String? = null,
        type: TransferItem.Type? = null
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
        index.members = arrayOfNulls<LoadedMember>(memberList.size)
        memberList.toArray(index.members)
        for (item in objectList) {
            if (TransferItem.Type.INCOMING == item.type) {
                index.bytesIncoming += item.comparableSize
                index.numberOfIncoming++
                val flag = item.flag
                if (TransferItem.Flag.DONE == flag) {
                    index.bytesIncomingCompleted += item.comparableSize
                    index.numberOfIncomingCompleted++
                } else if (TransferItem.Flag.IN_PROGRESS == flag) index.bytesIncomingCompleted += flag.bytesValue else if (isError(
                        flag
                    )
                ) index.hasIssues = true
            } else if (TransferItem.Type.OUTGOING == item.type) {
                if (deviceId != null) appendOutgoingData(
                    index,
                    item,
                    item.getFlag(deviceId)
                ) else if (memberList.size < 1) appendOutgoingData(index, item, TransferItem.Flag.PENDING) else {
                    for (member in memberList) {
                        if (TransferItem.Type.OUTGOING != member.type) continue
                        appendOutgoingData(index, item, item.getFlag(member.deviceId))
                    }
                }
            }
        }
    }

    fun pauseTransfer(activity: Activity?, member: TransferMember?) {
        pauseTransfer(activity, member.transferId, member.deviceId, member.type)
    }

    fun pauseTransfer(
        activity: Activity?, transferId: Long, deviceId: String?,
        type: TransferItem.Type?
    ) {
        App.interruptTasksBy(
            activity,
            FileTransferTask.identifyWith(transferId, deviceId, type),
            true
        )
    }

    fun recoverIncomingInterruptions(context: Context?, transferId: Long) {
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

    fun startTransferWithTest(
        activity: Activity, transfer: Transfer?,
        member: TransferMember?
    ) {
        val context = activity.applicationContext
        if (activity.isFinishing) return
        if (TransferItem.Type.INCOMING == member.type && fetchFirstValidIncomingTransferItem(
                activity,
                transfer!!.id
            ) == null
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
        } else if (TransferItem.Type.INCOMING == member.type && Files.getSavePath(activity, transfer)
                .uri.toString() != transfer!!.savePath
        ) {
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setMessage(
                        context.getString(
                            R.string.mesg_notSavingToChosenLocation,
                            Files.getReadableUri(transfer.savePath)
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

    fun startTransfer(activity: Activity?, member: TransferMember?) {
        if (activity != null && !activity.isFinishing) activity.runOnUiThread(Runnable {
            try {
                val task: FileTransferTask = FileTransferTask.createFrom(
                    AppUtils.getKuick(activity),
                    member.transferId, member.deviceId, member.type
                )
                FindConnectionDialog.show(
                    activity,
                    task.device,
                    OnDeviceResolvedListener { device: Device?, address: DeviceAddress? ->
                        try {
                            App.run<FileTransferTask>(activity, task)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    })
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
        })
    }
}