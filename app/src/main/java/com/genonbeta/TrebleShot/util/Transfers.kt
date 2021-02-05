package com.genonbeta.TrebleShot.util

import android.content.*
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import android.os.Parcelable
import android.os.Parcel
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.os.Bundle
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.os.PowerManager
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.DocumentFile
import java.io.File
import java.lang.Exception

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
object Transfers {
    val TAG = Transfers::class.java.simpleName
    private fun appendOutgoingData(group: TransferIndex?, `object`: TransferItem, flag: TransferItem.Flag) {
        group.bytesOutgoing += `object`.comparableSize
        group.numberOfOutgoing++
        if (TransferItem.Flag.DONE == flag) {
            group.bytesOutgoingCompleted += `object`.comparableSize
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
        return SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM).setWhere(
            String.format(
                "%s = ? AND %s = ?", Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID,
                Kuick.Companion.FIELD_TRANSFERITEM_TYPE
            ), transferId.toString(),
            TransferItem.Type.INCOMING.toString()
        )
    }

    fun createIncomingSelection(transferId: Long, flag: TransferItem.Flag, equals: Boolean): SQLQuery.Select {
        return SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM).setWhere(
            String.format(
                "%s = ? AND %s = ? AND %s " + (if (equals) "=" else "!=") + " ?",
                Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID, Kuick.Companion.FIELD_TRANSFERITEM_TYPE,
                Kuick.Companion.FIELD_TRANSFERITEM_FLAG
            ), transferId.toString(),
            TransferItem.Type.INCOMING.toString(), flag.toString()
        )
    }

    fun createAddressSelection(deviceId: String?): SQLQuery.Select {
        return SQLQuery.Select(Kuick.Companion.TABLE_DEVICEADDRESS)
            .setWhere(Kuick.Companion.FIELD_DEVICEADDRESS_DEVICEID + "=?", deviceId)
            .setOrderBy(Kuick.Companion.FIELD_DEVICEADDRESS_LASTCHECKEDDATE + " DESC")
    }

    fun getPercentageByFlag(flag: TransferItem.Flag, size: Long): Double {
        if (TransferItem.Flag.DONE == flag) return 1
        val bytesValue = flag.bytesValue
        return if (bytesValue == 0L || size == 0L) 0 else (bytesValue.toFloat() / size).toDouble()
    }

    fun fetchFirstMember(kuick: Kuick, transferId: Long): LoadedMember? {
        val select: SQLQuery.Select = SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERMEMBER)
            .setWhere(Kuick.Companion.FIELD_TRANSFERMEMBER_TRANSFERID + "=?", transferId.toString())
        val memberList: List<LoadedMember> = kuick.castQuery<Transfer, LoadedMember>(
            select,
            LoadedMember::class.java,
            CastQueryListener<LoadedMember> { db: KuickDb, item: ContentValues?, `object`: LoadedMember ->
                `object`.device = Device(`object`.deviceId)
                try {
                    db.reconstruct<Void, Device>(`object`.device)
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
                        "`%s` ASC, `%s` ASC", Kuick.Companion.FIELD_TRANSFERITEM_DIRECTORY,
                        Kuick.Companion.FIELD_TRANSFERITEM_NAME
                    )
                )
        ) ?: return null
        val `object` = TransferItem()
        `object`.reconstruct(kuick.writableDatabase, kuick, receiverInstance)
        return `object`
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
        val selection: SQLQuery.Select = SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERMEMBER)
        if (type == null) selection.setWhere(
            Kuick.Companion.FIELD_TRANSFERMEMBER_TRANSFERID + "=?",
            transferId.toString()
        ) else selection.setWhere(
            Kuick.Companion.FIELD_TRANSFERMEMBER_TRANSFERID + "=? AND "
                    + Kuick.Companion.FIELD_TRANSFERMEMBER_TYPE + "=?", transferId.toString(),
            type.toString()
        )
        return AppUtils.getKuick(context).castQuery<Transfer, LoadedMember>(selection, LoadedMember::class.java,
            CastQueryListener<LoadedMember> { db: KuickDb?, item: ContentValues?, `object`: LoadedMember? ->
                loadMemberInfo(
                    db,
                    `object`
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
        val selection: SQLQuery.Select = SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM).setWhere(
            Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID + "=?", transfer.id.toString()
        )
        if (type == null) selection.setWhere(
            Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID + "=?",
            transfer.id.toString()
        ) else selection.setWhere(
            Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID + "=? AND " + Kuick.Companion.FIELD_TRANSFERITEM_TYPE + "=?",
            transfer.id.toString(),
            type.toString()
        )
        val memberList: List<LoadedMember> = loadMemberList(context, transfer.id, type)
        val objectList = AppUtils.getKuick(context).castQuery(selection, TransferItem::class.java)
        index.members = arrayOfNulls<LoadedMember>(memberList.size)
        memberList.toArray(index.members)
        for (`object` in objectList) {
            if (TransferItem.Type.INCOMING == `object`.type) {
                index.bytesIncoming += `object`.comparableSize
                index.numberOfIncoming++
                val flag = `object`.flag
                if (TransferItem.Flag.DONE == flag) {
                    index.bytesIncomingCompleted += `object`.comparableSize
                    index.numberOfIncomingCompleted++
                } else if (TransferItem.Flag.IN_PROGRESS == flag) index.bytesIncomingCompleted += flag.bytesValue else if (isError(
                        flag
                    )
                ) index.hasIssues = true
            } else if (TransferItem.Type.OUTGOING == `object`.type) {
                if (deviceId != null) appendOutgoingData(
                    index,
                    `object`,
                    `object`.getFlag(deviceId)
                ) else if (memberList.size < 1) appendOutgoingData(index, `object`, TransferItem.Flag.PENDING) else {
                    for (member in memberList) {
                        if (TransferItem.Type.OUTGOING != member.type) continue
                        appendOutgoingData(index, `object`, `object`.getFlag(member.deviceId))
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
        App.Companion.interruptTasksBy(
            activity,
            FileTransferTask.Companion.identifyWith(transferId, deviceId, type),
            true
        )
    }

    fun recoverIncomingInterruptions(context: Context?, transferId: Long) {
        val kuick = AppUtils.getKuick(context)
        val contentValues = ContentValues()
        contentValues.put(Kuick.Companion.FIELD_TRANSFERITEM_FLAG, TransferItem.Flag.PENDING.toString())
        kuick.update(
            SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM)
                .setWhere(
                    Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID + "=? AND  " + Kuick.Companion.FIELD_TRANSFERITEM_FLAG + "=? AND "
                            + Kuick.Companion.FIELD_TRANSFERITEM_TYPE + "=?", transferId.toString(),
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
        } else if (TransferItem.Type.INCOMING == member.type && FileUtils.getSavePath(activity, transfer)
                .uri.toString() != transfer!!.savePath
        ) {
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setMessage(
                        context.getString(
                            R.string.mesg_notSavingToChosenLocation,
                            FileUtils.getReadableUri(transfer.savePath)
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
                val task: FileTransferTask = FileTransferTask.Companion.createFrom(
                    AppUtils.getKuick(activity),
                    member.transferId, member.deviceId, member.type
                )
                FindConnectionDialog.Companion.show(
                    activity,
                    task.device,
                    OnDeviceResolvedListener { device: Device?, address: DeviceAddress? ->
                        try {
                            App.Companion.run<FileTransferTask>(activity, task)
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