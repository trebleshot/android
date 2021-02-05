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
package com.genonbeta.TrebleShot.dialog

import android.app.Activity
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
import android.content.DialogInterface
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import android.content.Intent
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
import com.genonbeta.TrebleShot.database.Kuick
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.content.IntentFilter
import android.content.BroadcastReceiver
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
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.DocumentFile
import java.lang.Exception
import java.text.NumberFormat

/**
 * created by: Veli
 * date: 10.11.2017 14:59
 */
class TransferInfoDialog(
    activity: Activity, loadedGroup: TransferIndex,
    `object`: TransferItem, deviceId: String?
) : AlertDialog.Builder(activity) {
    init {
        var attemptedFile: DocumentFile? = null
        val isIncoming = TransferItem.Type.INCOMING == `object`.type
        try {
            // If it is incoming than get the received or cache file
            // If not then try to reach to the source file that is being send
            attemptedFile = if (isIncoming) FileUtils.getIncomingPseudoFile(
                context, `object`, loadedGroup.transfer,
                false
            ) else com.genonbeta.android.framework.util.FileUtils.fromUri(context, Uri.parse(`object`.file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val pseudoFile = attemptedFile
        val fileExists = pseudoFile != null && pseudoFile.canRead()
        @SuppressLint("InflateParams") val rootView =
            LayoutInflater.from(activity).inflate(R.layout.layout_transfer_info, null)
        val nameText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_file_name)
        val sizeText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_file_size)
        val typeText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_file_mime)
        val flagText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_file_status)
        val incomingDetailsLayout = rootView.findViewById<View>(R.id.transfer_info_incoming_details_layout)
        val receivedSizeText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_received_size)
        val locationText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_pseudo_location)
        setTitle(R.string.text_transactionDetails)
        setView(rootView)
        nameText.setText(`object`.name)
        sizeText.setText(com.genonbeta.android.framework.util.FileUtils.sizeExpression(`object`.comparableSize, false))
        typeText.setText(`object`.mimeType)
        receivedSizeText.setText(
            if (fileExists) com.genonbeta.android.framework.util.FileUtils.sizeExpression(
                pseudoFile!!.length(),
                false
            ) else context.getString(R.string.text_unknown)
        )
        locationText.setText(if (fileExists) FileUtils.getReadableUri(pseudoFile!!.uri) else context.getString(R.string.text_unknown))
        flagText.setText(
            TextUtils.getTransactionFlagString(
                context, `object`,
                NumberFormat.getPercentInstance(), deviceId
            )
        )
        setPositiveButton(R.string.butn_close, null)
        setNegativeButton(
            R.string.butn_remove
        ) { dialogInterface: DialogInterface?, i: Int -> DialogUtils.showRemoveDialog(activity, `object`) }
        if (isIncoming) {
            incomingDetailsLayout.visibility = View.VISIBLE
            if (TransferItem.Flag.INTERRUPTED == `object`.flag || TransferItem.Flag.IN_PROGRESS == `object`.flag) {
                setNeutralButton(R.string.butn_retry) { dialogInterface: DialogInterface?, i: Int ->
                    `object`.flag = TransferItem.Flag.PENDING
                    AppUtils.getKuick(activity).publish(`object`)
                    AppUtils.getKuick(activity).broadcast()
                }
            } else if (fileExists) {
                if (TransferItem.Flag.REMOVED == `object`.flag && pseudoFile!!.parentFile != null) {
                    setNeutralButton(R.string.butn_saveAnyway) { dialogInterface: DialogInterface?, i: Int ->
                        val saveAnyway = AlertDialog.Builder(
                            context
                        )
                        saveAnyway.setTitle(R.string.ques_saveAnyway)
                        saveAnyway.setMessage(R.string.text_saveAnywaySummary)
                        saveAnyway.setNegativeButton(R.string.butn_cancel, null)
                        saveAnyway.setPositiveButton(R.string.butn_proceed) { dialog: DialogInterface?, which: Int ->
                            try {
                                val savedFile = FileUtils.saveReceivedFile(
                                    pseudoFile.parentFile, pseudoFile, `object`
                                )
                                `object`.flag = TransferItem.Flag.DONE
                                AppUtils.getKuick(activity).update(`object`)
                                AppUtils.getKuick(activity).broadcast()
                                Toast.makeText(context, R.string.mesg_fileSaved, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show()
                            }
                        }
                        saveAnyway.show()
                    }
                } else if (TransferItem.Flag.DONE == `object`.flag) {
                    setNeutralButton(R.string.butn_open) { dialog: DialogInterface?, which: Int ->
                        try {
                            com.genonbeta.android.framework.util.FileUtils.openUri(context, pseudoFile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else if (fileExists) try {
            val startIntent = com.genonbeta.android.framework.util.FileUtils.getOpenIntent(context, attemptedFile)
            setNeutralButton(R.string.butn_open) { dialog: DialogInterface?, which: Int ->
                try {
                    context.startActivity(startIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (ignored: Exception) {
        }
    }
}