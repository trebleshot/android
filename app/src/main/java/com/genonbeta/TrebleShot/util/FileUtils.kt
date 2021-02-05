/*
 * Copyright (C) 2019 Veli Tasalı
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
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
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
import android.os.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.FileUtils
import java.io.File
import java.io.IOException
import java.lang.Exception

object FileUtils : FileUtils() {
    @Throws(Exception::class)
    fun copy(context: Context?, source: DocumentFile?, destination: DocumentFile?, stoppable: Stoppable?) {
        copy(
            context, source, destination, stoppable, AppConfig.BUFFER_LENGTH_DEFAULT,
            AppConfig.DEFAULT_TIMEOUT_SOCKET
        )
    }

    fun getApplicationDirectory(context: Context?): DocumentFile {
        val defaultPath = getDefaultApplicationDirectoryPath(context)
        val defaultPreferences = AppUtils.getDefaultPreferences(context)
        if (defaultPreferences!!.contains("storage_path")) {
            try {
                val savePath = fromUri(
                    context, Uri.parse(
                        defaultPreferences.getString(
                            "storage_path",
                            null
                        )
                    )
                )
                if (savePath.isDirectory && savePath.canWrite()) return savePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (defaultPath!!.isFile) defaultPath.delete()
        if (!defaultPath.isDirectory) defaultPath.mkdirs()
        return DocumentFile.fromFile(defaultPath)
    }

    fun getDefaultApplicationDirectoryPath(context: Context?): File? {
        if (Build.VERSION.SDK_INT >= 29) return context!!.externalCacheDir
        var primaryDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!primaryDir.isDirectory && !primaryDir.mkdirs() || !primaryDir.canWrite()) primaryDir =
            Environment.getExternalStorageDirectory()
        return File(primaryDir.toString() + File.separator + context!!.getString(R.string.text_appName))
    }

    fun getFileFormat(fileName: String): String? {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0) fileName.substring(lastDot + 1).toLowerCase() else null
    }

    @Throws(IOException::class)
    fun getIncomingPseudoFile(
        context: Context?, transferItem: TransferItem?,
        transfer: Transfer?, createIfNotExists: Boolean
    ): DocumentFile {
        return fetchFile(getSavePath(context, transfer), transferItem!!.directory, transferItem.file, createIfNotExists)
    }

    @Throws(IOException::class)
    fun getIncomingFile(context: Context?, transferItem: TransferItem?, transfer: Transfer?): DocumentFile {
        val pseudoFile = getIncomingPseudoFile(context, transferItem, transfer, true)
        if (!pseudoFile.canWrite()) throw IOException("File cannot be created or you don't have permission write on it")
        return pseudoFile
    }

    fun getReadableUri(uri: String?): String? {
        return getReadableUri(Uri.parse(uri), uri)
    }

    fun getReadableUri(uri: Uri): String? {
        return getReadableUri(uri, uri.toString())
    }

    fun getReadableUri(uri: Uri, defaultValue: String?): String? {
        return if (uri.path == null) defaultValue else uri.path
    }

    @Throws(Exception::class)
    fun move(
        context: Context?, targetFile: DocumentFile?, destinationFile: DocumentFile?,
        stoppable: Stoppable?
    ): Boolean {
        return move(
            context, targetFile, destinationFile, stoppable, AppConfig.BUFFER_LENGTH_DEFAULT,
            AppConfig.DEFAULT_TIMEOUT_SOCKET
        )
    }

    fun getSavePath(context: Context?, transfer: Transfer?): DocumentFile {
        val defaultFolder = getApplicationDirectory(context)
        if (transfer!!.savePath != null) {
            try {
                val savePath = fromUri(
                    context, Uri.parse(
                        transfer.savePath
                    )
                )
                if (savePath.isDirectory && savePath.canWrite()) return savePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            transfer.savePath = defaultFolder.uri.toString()
            AppUtils.getKuick(context).publish(transfer)
        }
        return defaultFolder
    }

    fun openUriForeground(context: Context, file: DocumentFile): Boolean {
        if (!openUri(context, file)) {
            Toast.makeText(context, context.getString(R.string.mesg_openFailure, file.name), Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    /**
     * When the transfer is done, this saves the uniquely named file to its actual name held in [TransferItem].
     *
     * @param savePath     The save path that contains currentFile
     * @param currentFile  The file that should be renamed
     * @param transferItem The transfer request
     * @return File moved to its actual name
     * @throws IOException Thrown when rename fails
     */
    @Throws(Exception::class)
    fun saveReceivedFile(
        savePath: DocumentFile, currentFile: DocumentFile?,
        transferItem: TransferItem?
    ): DocumentFile {
        val uniqueName = getUniqueFileName(savePath, transferItem!!.name, true)

        // FIXME: 7/30/19 The rename always fails when renaming TreeDocumentFile
        if (!currentFile!!.renameTo(uniqueName)) throw IOException("Failed to rename object: $currentFile")
        transferItem.file = uniqueName
        savePath.sync()
        return savePath.findFile(uniqueName)
    }
}