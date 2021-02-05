/*
 * Copyright (C) 2020 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.genonbeta.TrebleShot.task

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
import com.genonbeta.TrebleShot.database.Kuick
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
import android.net.Uri
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.protocol.communication.ContentException
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.FileUtils
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

class FileTransferTask : AttachableAsyncTask<AttachedTaskListener?>() {
    // Static objects
    var activeConnection: ActiveConnection? = null
    var device: Device? = null
    var index: TransferIndex? = null
    var transfer: Transfer? = null
    var member: TransferMember? = null
    var addressList: List<DeviceAddress?>? = null
    var type: TransferItem.Type? = null

    // Changing objects
    var item: TransferItem? = null
    var lastItem: TransferItem? = null
    var file: DocumentFile? = null
    var lastMovedBytes: Long = 0
    var currentBytes // moving
            : Long = 0
    var completedBytes: Long = 0
    var completedCount = 0
    private val mTimeTransactionSaved: Long = 0
    private var mDatabase: SQLiteDatabase? = null
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        if (activeConnection == null) startTransferAsClient() else if (TransferItem.Type.OUTGOING == type) handleTransferAsSender() else if (TransferItem.Type.INCOMING == type) handleTransferAsReceiver()
    }

    override fun onPublishStatus() {
        super.onPublishStatus()
        if (isInterrupted || isFinished) {
            if (isInterrupted) ongoingContent = context.getString(R.string.text_cancellingTransfer)
            kuick().broadcast()
            return
        }
        val bytesTransferred = completedBytes + currentBytes
        val text = StringBuilder()
        progress().total = 100
        if (bytesTransferred > 0 && index.bytesPending() > 0) progress().current =
            (100 * (bytesTransferred.toDouble() / index.bytesPending())) as Int
        if (lastMovedBytes > 0 && bytesTransferred > 0) {
            val change = bytesTransferred - lastMovedBytes
            text.append(FileUtils.sizeExpression(change, false))
            if (index.bytesPending() > 0 && change > 0) {
                val timeNeeded: Long = (index.bytesPending() - bytesTransferred) / change
                text.append(" (")
                text.append(
                    context.getString(
                        R.string.text_remainingTime,
                        TimeUtils.getDuration(timeNeeded, false)
                    )
                )
                text.append(")")
            }
        }
        lastMovedBytes = bytesTransferred
        if (item != null) {
            if (text.length > 0) text.append(" ").append(context.getString(R.string.mode_middleDot)).append(" ")
            text.append(item!!.name)
            try {
                val flag = TransferItem.Flag.IN_PROGRESS
                flag.bytesValue = currentBytes
                if (TransferItem.Type.INCOMING == type) item!!.flag =
                    flag else if (TransferItem.Type.OUTGOING == type) item!!.putFlag(
                    device!!.uid, flag
                )
                kuick().update(
                    database, item, transfer, null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ongoingContent = text.toString()
        kuick().broadcast()
    }

    override fun forceQuit() {
        super.forceQuit()
        try {
            if (activeConnection != null && activeConnection.getSocket() != null) activeConnection.getSocket().close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private val database: SQLiteDatabase?
        private get() {
            if (mDatabase == null) mDatabase = kuick().writableDatabase
            return mDatabase
        }
    override val identity: Identity
        get() = identityOf(this)

    override fun getName(context: Context?): String? {
        return context!!.getString(R.string.text_transfer)
    }

    private fun handleTransferAsReceiver() {
        try {
            Transfers.loadTransferInfo(context, index, member)
            while (Transfers.fetchFirstValidIncomingTransferItem(context, transfer!!.id).also { item = it } != null) {
                publishStatus()

                // We don't handle IO errors on the receiver side.
                // An IO error for this side means there is a permission/storage issue.
                file = com.genonbeta.TrebleShot.util.FileUtils.getIncomingFile(context, item, transfer)
                currentBytes = file!!.length()
                val streamInfo: StreamInfo = StreamInfo.getStreamInfo(context, file!!.uri)
                try {
                    streamInfo.openOutputStream().use { outputStream ->
                        CommunicationBridge.Companion.sendSecure(
                            activeConnection, true, JSONObject()
                                .put(Keyword.TRANSFER_REQUEST_ID, item!!.id)
                                .put(Keyword.SKIPPED_BYTES, currentBytes)
                        )
                        if (CommunicationBridge.Companion.receiveResult(activeConnection, device)) {
                            var len: Int
                            val description: ActiveConnection.Description = activeConnection.readBegin()
                            val writableByteChannel: WritableByteChannel = Channels.newChannel(outputStream)
                            while (description.hasAvailable() && activeConnection.read(description)
                                    .also { len = it } != -1
                            ) {
                                publishStatus()
                                currentBytes += len.toLong()
                                writableByteChannel.write(description.byteBuffer)
                            }
                            outputStream.flush()
                            item!!.flag = TransferItem.Flag.DONE
                            completedBytes += currentBytes
                            completedCount++
                            lastItem = item
                            if (file!!.parentFile != null) {
                                file = com.genonbeta.TrebleShot.util.FileUtils.saveReceivedFile(
                                    file!!.parentFile,
                                    file,
                                    item
                                )
                                Log.d(
                                    TAG, "handleTransferAsReceiver(): File is " + file!!.uri.toString()
                                            + " and name is " + item!!.file
                                )
                                context.sendBroadcast(
                                    Intent(FileListFragment.Companion.ACTION_FILE_LIST_CHANGED)
                                        .putExtra(FileListFragment.Companion.EXTRA_FILE_PARENT, file!!.parentFile.uri)
                                        .putExtra(FileListFragment.Companion.EXTRA_FILE_NAME, file!!.name)
                                )
                            }
                            if (file is LocalDocumentFile && mediaScanner.isConnected) mediaScanner.scanFile(
                                (file as LocalDocumentFile?).getFile().getAbsolutePath(),
                                item!!.mimeType
                            )
                            Log.d(TAG, "handleTransferAsReceiver(): File received " + item!!.name)
                        }
                    }
                } catch (e: CancelledException) {
                    item!!.flag = TransferItem.Flag.PENDING
                    throw e
                } catch (e: FileNotFoundException) {
                    throw e
                } catch (e: ContentException) {
                    when (e.error) {
                        ContentException.Error.NotFound -> item!!.flag = TransferItem.Flag.REMOVED
                        ContentException.Error.AlreadyExists, ContentException.Error.NotAccessible -> item!!.flag =
                            TransferItem.Flag.INTERRUPTED
                        else -> item!!.flag = TransferItem.Flag.INTERRUPTED
                    }
                } catch (e: Exception) {
                    item!!.flag = TransferItem.Flag.INTERRUPTED
                    throw e
                } finally {
                    kuick().update(
                        database, item, transfer, null
                    )
                    item = null
                }
            }
            CommunicationBridge.Companion.sendResult(activeConnection, false)
            if (completedCount > 0) {
                notificationHelper.notifyFileReceived(
                    this,
                    com.genonbeta.TrebleShot.util.FileUtils.getSavePath(context, transfer)
                )
                Log.d(TAG, "handleTransferAsReceiver(): Notify user")
            }
        } catch (ignored: TaskStoppedException) {
        } catch (ignored: CancelledException) {
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                CommunicationBridge.Companion.sendError(activeConnection, e)
            } catch (e1: Exception) {
                try {
                    post(
                        TaskMessage.Companion.newInstance()
                            .setTone(Tone.Negative)
                            .setTitle(context, R.string.text_communicationError)
                            .setMessage(context.getString(R.string.mesg_errorDuringTransfer, device!!.username))
                    )
                } catch (ignored: TaskStoppedException) {
                }
            }
        }
    }

    private fun handleTransferAsSender() {
        try {
            Transfers.loadTransferInfo(context, index, member)
            while (activeConnection.getSocket().isConnected()) {
                publishStatus()
                val request: JSONObject = CommunicationBridge.Companion.receiveSecure(activeConnection, device)
                if (!CommunicationBridge.Companion.resultOf(request)) break
                try {
                    item = TransferItem(transfer!!.id, request.getLong(Keyword.TRANSFER_REQUEST_ID), type!!)
                    kuick().reconstruct(
                        database, item
                    )
                    try {
                        file = FileUtils.fromUri(
                            context, Uri.parse(
                                item!!.file
                            )
                        )
                        if (item!!.comparableSize != file.length()) throw FileNotFoundException("File size has changed. Probably it is a different file.")
                        currentBytes = request.getLong(Keyword.SKIPPED_BYTES)
                        val length = item!!.comparableSize - currentBytes
                        context.contentResolver.openInputStream(
                            file.getUri()
                        ).use { inputStream ->
                            if (inputStream == null) throw FileNotFoundException("The input stream for the file has failed to open.")
                            if (currentBytes > 0 && inputStream.skip(currentBytes) != currentBytes) throw IOException("Failed to skip " + currentBytes + "bytes")
                            CommunicationBridge.Companion.sendResult(activeConnection, true)
                            item!!.putFlag(device!!.uid, TransferItem.Flag.IN_PROGRESS)
                            kuick().update(
                                database, item, transfer, null
                            )
                            val description: ActiveConnection.Description = activeConnection.writeBegin(0, length)
                            val bytes = ByteArray(8096)
                            var readLength: Int
                            try {
                                while (inputStream.read(bytes).also { readLength = it } != -1) {
                                    publishStatus()
                                    if (readLength > 0) {
                                        currentBytes += readLength.toLong()
                                        activeConnection.write(description, bytes, 0, readLength)
                                    }
                                }
                                activeConnection.writeEnd(description)
                            } catch (ignored: SizeOverflowException) {
                            }
                            completedBytes += currentBytes
                            completedCount++
                            item!!.putFlag(device!!.uid, TransferItem.Flag.DONE)
                            Log.d(TAG, "handleTransferAsSender(): File sent " + item!!.name)
                        }
                    } catch (e: CancelledException) {
                        item!!.putFlag(device!!.uid, TransferItem.Flag.PENDING)
                        throw e
                    } catch (e: FileNotFoundException) {
                        item!!.putFlag(device!!.uid, TransferItem.Flag.REMOVED)
                        throw e
                    } catch (e: Exception) {
                        item!!.putFlag(device!!.uid, TransferItem.Flag.INTERRUPTED)
                        throw e
                    } finally {
                        kuick().update(
                            database, item, transfer, null
                        )
                        item = null
                    }
                } catch (e: CancelledException) {
                    throw e
                } catch (e: FileNotFoundException) {
                    CommunicationBridge.Companion.sendError(activeConnection, Keyword.ERROR_NOT_FOUND)
                } catch (e: ReconstructionFailedException) {
                    CommunicationBridge.Companion.sendError(activeConnection, Keyword.ERROR_NOT_FOUND)
                } catch (e: IOException) {
                    CommunicationBridge.Companion.sendError(activeConnection, Keyword.ERROR_NOT_ACCESSIBLE)
                } catch (e: Exception) {
                    CommunicationBridge.Companion.sendError(activeConnection, Keyword.ERROR_UNKNOWN)
                }
            }
        } catch (ignored: CancelledException) {
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                post(
                    TaskMessage.Companion.newInstance()
                        .setTone(Tone.Negative)
                        .setTitle(context, R.string.text_communicationError)
                        .setMessage(context.getString(R.string.mesg_errorDuringTransfer, device!!.username))
                )
            } catch (ignored: TaskStoppedException) {
            }
        }
    }

    override fun interrupt(userAction: Boolean): Boolean {
        if (activeConnection != null) {
            try {
                activeConnection.closeSafely()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return super.interrupt(userAction)
    }

    @Throws(TaskStoppedException::class)
    fun startTransferAsClient() {
        try {
            CommunicationBridge.Companion.connect(kuick(), addressList, device, 0).use { bridge ->
                bridge.requestFileTransferStart(transfer!!.id, type)
                if (bridge.receiveResult()) {
                    activeConnection = bridge.getActiveConnection()
                    if (TransferItem.Type.INCOMING == type) {
                        handleTransferAsReceiver()
                    } else if (TransferItem.Type.OUTGOING == type) {
                        handleTransferAsSender()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            post(CommonErrorHelper.messageOf(context, e))
        }
    }

    enum class Id {
        TransferId, DeviceId, Type
    }

    companion object {
        val TAG = FileTransferTask::class.java.simpleName
        @Throws(
            TransferNotFoundException::class,
            DeviceNotFoundException::class,
            ConnectionNotFoundException::class,
            MemberNotFoundException::class
        )
        fun createFrom(kuick: Kuick, transferId: Long, deviceId: String?, type: TransferItem.Type?): FileTransferTask {
            val db: SQLiteDatabase = kuick.readableDatabase
            val device = Device(deviceId)
            try {
                kuick.reconstruct(db, device)
            } catch (e: ReconstructionFailedException) {
                throw DeviceNotFoundException(device)
            }
            val transfer = Transfer(transferId)
            try {
                kuick.reconstruct(db, transfer)
            } catch (e: ReconstructionFailedException) {
                throw TransferNotFoundException(transfer)
            }
            return createFrom(kuick, transfer, device, type)
        }

        @Throws(MemberNotFoundException::class, ConnectionNotFoundException::class)
        fun createFrom(kuick: Kuick?, transfer: Transfer, device: Device, type: TransferItem.Type?): FileTransferTask {
            val db: SQLiteDatabase = kuick.getReadableDatabase()
            val member = TransferMember(transfer, device, type)
            try {
                kuick.reconstruct<Transfer, TransferMember>(db, member)
            } catch (e: ReconstructionFailedException) {
                throw MemberNotFoundException(member)
            }
            val addressList: List<DeviceAddress?>? = Transfers.getAddressListFor(kuick, device.uid)
            Log.d(TAG, "createFrom: deviceId=" + device.uid + " transferId=" + transfer.id)
            val task = FileTransferTask()
            task.type = type
            task.device = device
            task.transfer = transfer
            task.member = member
            task.addressList = addressList
            task.index = TransferIndex(transfer)
            return task
        }

        fun identityOf(task: FileTransferTask): Identity {
            return identifyWith(task.transfer!!.id, task.device!!.uid, task.type)
        }

        fun identifyWith(transferId: Long): Identity {
            return withANDs(Identifier.from(Id.TransferId, transferId))
        }

        fun identifyWith(transferId: Long, type: TransferItem.Type?): Identity {
            return withANDs(Identifier.from(Id.TransferId, transferId), from(Id.Type, type))
        }

        fun identifyWith(transferId: Long, deviceId: String?): Identity {
            return withANDs(Identifier.from(Id.TransferId, transferId), from(Id.DeviceId, deviceId))
        }

        fun identifyWith(transferId: Long, deviceId: String?, type: TransferItem.Type?): Identity {
            return withANDs(
                Identifier.from(Id.TransferId, transferId),
                from(Id.DeviceId, deviceId),
                from(Id.Type, type)
            )
        }
    }
}