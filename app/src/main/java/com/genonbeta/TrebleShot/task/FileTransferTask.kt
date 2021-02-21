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

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.model.*
import com.genonbeta.TrebleShot.model.Identifier.Companion.from
import com.genonbeta.TrebleShot.model.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.fragment.FileListFragment
import com.genonbeta.TrebleShot.protocol.communication.ContentException
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException
import com.genonbeta.TrebleShot.util.CommunicationBridge
import com.genonbeta.TrebleShot.util.CommunicationBridge.Companion.receiveResult
import com.genonbeta.TrebleShot.util.Files.getIncomingFile
import com.genonbeta.TrebleShot.util.Files.getSavePath
import com.genonbeta.TrebleShot.util.Files.saveReceivedFile
import com.genonbeta.TrebleShot.util.TimeUtils
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.android.database.exception.ReconstructionFailedException
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.util.Files
import org.json.JSONObject
import org.monora.coolsocket.core.response.SizeOverflowException
import org.monora.coolsocket.core.session.ActiveConnection
import org.monora.coolsocket.core.session.CancelledException
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

class FileTransferTask(
    private val activeConnection: ActiveConnection,
    val transfer: Transfer,
    val device: Device,
    val member: TransferMember,
    val index: TransferIndex,
    val type: TransferItem.Type,
) : AttachableAsyncTask<AttachedTaskListener>() {
    // Changing objects
    var item: TransferItem? = null

    var lastItem: TransferItem? = null

    var file: DocumentFile? = null

    var lastMovedBytes: Long = 0

    var currentBytes: Long = 0

    var completedBytes: Long = 0

    var completedCount = 0

    private var db: SQLiteDatabase? = null

    @Throws(TaskStoppedException::class)
    override fun onRun() {
        if (TransferItem.Type.OUTGOING == type)
            handleTransferAsSender()
        else if (TransferItem.Type.INCOMING == type)
            handleTransferAsReceiver()
    }

    override fun onPublishStatus() {
        super.onPublishStatus()
        if (interrupted() || finished) {
            if (interrupted()) ongoingContent = context.getString(R.string.text_cancellingTransfer)
            kuick.broadcast()
            return
        }
        val bytesTransferred = completedBytes + currentBytes
        val text = StringBuilder()
        progress.progress?.total = 100

        if (bytesTransferred > 0 && index.bytesPending() > 0) {
            progress.progress?.progress = (100 * (bytesTransferred.toDouble() / index.bytesPending())).toInt()
        }
        if (lastMovedBytes > 0 && bytesTransferred > 0) {
            val change = bytesTransferred - lastMovedBytes
            text.append(Files.formatLength(change, false))
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
        item?.let {
            if (text.isNotEmpty()) text.append(" ").append(context.getString(R.string.mode_middleDot)).append(" ")
            text.append(it.name)
            try {
                val flag = TransferItem.Flag.IN_PROGRESS
                flag.bytesValue = currentBytes

                if (TransferItem.Type.INCOMING == type) {
                    it.flag = flag
                } else if (TransferItem.Type.OUTGOING == type) {
                    it.putFlag(device.uid, flag)
                }

                kuick.update(database, it, transfer, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ongoingContent = text.toString()
        kuick.broadcast()
    }

    override fun forceQuit() {
        super.forceQuit()
        try {
            if (activeConnection.socket != null) activeConnection.socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private val database: SQLiteDatabase
        get() = db ?: kuick.writableDatabase.also { db = it }

    override val identity: Identity
        get() = identityOf(this)

    override fun getName(context: Context): String {
        return context.getString(R.string.text_transfer)
    }

    private fun handleTransferAsReceiver() {
        try {
            Transfers.loadTransferInfo(context, index, member)

            while (Transfers.fetchFirstValidIncomingTransferItem(context, transfer.id)?.also { item = it } != null) {
                val itemLocal = item ?: break

                publishStatus()

                // We don't handle IO errors on the receiver side.
                // An IO error for this side means there is a permission/storage issue.
                var fileLocal = getIncomingFile(context, itemLocal, transfer).also { file = it }
                val streamInfo = StreamInfo.from(context, fileLocal.getUri())

                currentBytes = fileLocal.getLength()
                try {
                    streamInfo.openOutputStream(context)?.use { outputStream ->
                        CommunicationBridge.sendSecure(
                            activeConnection, true, JSONObject()
                                .put(Keyword.TRANSFER_REQUEST_ID, itemLocal.id)
                                .put(Keyword.SKIPPED_BYTES, currentBytes)
                        )
                        if (receiveResult(activeConnection, device)) {
                            var len = 0
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
                            itemLocal.flag = TransferItem.Flag.DONE
                            completedBytes += currentBytes
                            completedCount++
                            lastItem = item

                            fileLocal.getParentFile()?.let { parentFile ->
                                saveReceivedFile(parentFile, fileLocal, itemLocal).also {
                                    fileLocal = it
                                    file = it
                                }

                                Log.d(TAG, "handleTransferAsReceiver(): Saved file is " + fileLocal.getUri())

                                context.sendBroadcast(
                                    Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
                                        .putExtra(FileListFragment.EXTRA_FILE_PARENT, parentFile.getUri())
                                        .putExtra(FileListFragment.EXTRA_FILE_NAME, fileLocal.getName())
                                )
                            }

                            fileLocal.also {
                                if (it is LocalDocumentFile && mediaScanner.isConnected) {
                                    mediaScanner.scanFile(it.file.absolutePath, itemLocal.mimeType)
                                    Log.d(TAG, "handleTransferAsReceiver(): File received " + itemLocal.name)
                                }
                            }
                        }
                    }
                } catch (e: CancelledException) {
                    itemLocal.flag = TransferItem.Flag.PENDING
                    throw e
                } catch (e: FileNotFoundException) {
                    throw e
                } catch (e: ContentException) {
                    when (e.error) {
                        ContentException.Error.NotFound -> itemLocal.flag = TransferItem.Flag.REMOVED
                        ContentException.Error.AlreadyExists, ContentException.Error.NotAccessible -> {
                            itemLocal.flag = TransferItem.Flag.INTERRUPTED
                        }
                        else -> itemLocal.flag = TransferItem.Flag.INTERRUPTED
                    }
                } catch (e: Exception) {
                    itemLocal.flag = TransferItem.Flag.INTERRUPTED
                    throw e
                } finally {
                    kuick.update(database, itemLocal, transfer, null)
                    item = null
                }
            }
            CommunicationBridge.sendResult(activeConnection, false)
            if (completedCount > 0) {
                notificationHelper.notifyFileReceived(this, getSavePath(context, transfer))
                Log.d(TAG, "handleTransferAsReceiver(): Notify user")
            }
        } catch (ignored: TaskStoppedException) {
        } catch (ignored: CancelledException) {
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                CommunicationBridge.sendError(activeConnection, e)
            } catch (e1: Exception) {
                try {
                    post(
                        TaskMessage.newInstance(
                            context.getString(R.string.text_communicationError),
                            context.getString(R.string.mesg_errorDuringTransfer, device.username),
                            TaskMessage.Tone.Negative
                        )
                    )
                } catch (ignored: TaskStoppedException) {
                }
            }
        }
    }

    private fun handleTransferAsSender() {
        try {
            Transfers.loadTransferInfo(context, index, member)
            while (activeConnection.socket.isConnected) {
                publishStatus()
                val request: JSONObject = CommunicationBridge.receiveSecure(activeConnection, device)
                if (!CommunicationBridge.resultOf(request)) break
                try {
                    val requestId = request.getLong(Keyword.TRANSFER_REQUEST_ID)
                    val itemLocal = TransferItem(transfer.id, requestId, type).also {
                        kuick.reconstruct(it)
                        item = it
                    }

                    try {
                        val fileLocal = Files.fromUri(context, Uri.parse(itemLocal.file)).also { file = it }

                        if (itemLocal.length != fileLocal.getLength()) {
                            throw FileNotFoundException("File size has changed. Probably it is a different file.")
                        }

                        currentBytes = request.getLong(Keyword.SKIPPED_BYTES)
                        val length = itemLocal.length - currentBytes

                        context.contentResolver.openInputStream(fileLocal.getUri()).use { inputStream ->
                            if (inputStream == null) {
                                throw FileNotFoundException("The input stream for the file has failed to open.")
                            }

                            if (currentBytes > 0 && inputStream.skip(currentBytes) != currentBytes) {
                                throw IOException("Failed to skip " + currentBytes + "bytes")
                            }

                            CommunicationBridge.sendResult(activeConnection, true)

                            itemLocal.putFlag(device.uid, TransferItem.Flag.IN_PROGRESS)
                            kuick.update(database, itemLocal, transfer, null)

                            val description = activeConnection.writeBegin(0, length)
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
                            itemLocal.putFlag(device.uid, TransferItem.Flag.DONE)
                            Log.d(TAG, "handleTransferAsSender(): File sent " + itemLocal.name)
                        }
                    } catch (e: CancelledException) {
                        itemLocal.putFlag(device.uid, TransferItem.Flag.PENDING)
                        throw e
                    } catch (e: FileNotFoundException) {
                        itemLocal.putFlag(device.uid, TransferItem.Flag.REMOVED)
                        throw e
                    } catch (e: Exception) {
                        itemLocal.putFlag(device.uid, TransferItem.Flag.INTERRUPTED)
                        throw e
                    } finally {
                        kuick.update(database, itemLocal, transfer, null)
                        item = null
                    }
                } catch (e: CancelledException) {
                    throw e
                } catch (e: FileNotFoundException) {
                    CommunicationBridge.sendError(activeConnection, Keyword.ERROR_NOT_FOUND)
                } catch (e: ReconstructionFailedException) {
                    CommunicationBridge.sendError(activeConnection, Keyword.ERROR_NOT_FOUND)
                } catch (e: IOException) {
                    CommunicationBridge.sendError(activeConnection, Keyword.ERROR_NOT_ACCESSIBLE)
                } catch (e: Exception) {
                    CommunicationBridge.sendError(activeConnection, Keyword.ERROR_UNKNOWN)
                }
            }
        } catch (ignored: CancelledException) {
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                post(
                    TaskMessage.newInstance(
                        context.getString(R.string.text_communicationError),
                        context.getString(R.string.mesg_errorDuringTransfer, device.username),
                        TaskMessage.Tone.Negative
                    )
                )
            } catch (ignored: TaskStoppedException) {
            }
        }
    }

    override fun interrupt(userAction: Boolean): Boolean {
        try {
            activeConnection.closeSafely()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return super.interrupt(userAction)
    }

    enum class Id {
        TransferId, DeviceId, Type
    }

    companion object {
        val TAG = FileTransferTask::class.java.simpleName

        fun identityOf(task: FileTransferTask): Identity {
            return identifyWith(task.transfer.id, task.device.uid, task.type)
        }

        fun identifyWith(transferId: Long): Identity {
            return withANDs(from(Id.TransferId, transferId))
        }

        fun identifyWith(transferId: Long, type: TransferItem.Type?): Identity {
            return withANDs(from(Id.TransferId, transferId), from(Id.Type, type))
        }

        fun identifyWith(transferId: Long, deviceId: String?): Identity {
            return withANDs(from(Id.TransferId, transferId), from(Id.DeviceId, deviceId))
        }

        fun identifyWith(transferId: Long, deviceId: String?, type: TransferItem.Type?): Identity {
            return withANDs(
                from(Id.TransferId, transferId),
                from(Id.DeviceId, deviceId),
                from(Id.Type, type)
            )
        }
    }
}