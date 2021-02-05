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
package com.genonbeta.TrebleShot.activity

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
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.FileUtils
import java.lang.Exception

/**
 * Created by: veli
 * Date: 5/29/17 3:18 PM
 */
class FilePickerActivity : Activity() {
    private var mFileExplorerFragment: FileExplorerFragment? = null
    private var mFAB: FloatingActionButton? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filepicker)
        mFileExplorerFragment = supportFragmentManager.findFragmentById(
            R.id.activity_filepicker_fragment_files
        ) as FileExplorerFragment?
        mFAB = findViewById<FloatingActionButton>(R.id.content_fab)
    }

    override fun onStart() {
        super.onStart()
        if (intent != null) {
            var hasTitlesDefined = false
            if (intent != null && supportActionBar != null) {
                supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                if (intent.hasExtra(EXTRA_ACTIVITY_TITLE).also { hasTitlesDefined = it }) supportActionBar!!.setTitle(
                    intent.getStringExtra(EXTRA_ACTIVITY_TITLE)
                )
            }
            if (ACTION_CHOOSE_DIRECTORY == intent.action) {
                if (supportActionBar != null) {
                    if (!hasTitlesDefined) supportActionBar!!.setTitle(R.string.text_chooseFolder) else supportActionBar!!.setSubtitle(
                        R.string.text_chooseFolder
                    )
                }
                mFileExplorerFragment.getAdapter()
                    .setConfiguration(true, false, null)
                mFileExplorerFragment.refreshList()
                val recyclerView: RecyclerView = mFileExplorerFragment.getListView()
                recyclerView.setPadding(0, 0, 0, 200)
                recyclerView.clipToPadding = false
                mFAB.show()
                mFAB.setOnClickListener(View.OnClickListener { v: View? ->
                    val selectedPath: DocumentFile = mFileExplorerFragment.getAdapter().getPath()
                    if (selectedPath != null && selectedPath.canWrite()) finishWithResult(selectedPath) else Snackbar.make(
                        v,
                        R.string.mesg_currentPathUnavailable,
                        Snackbar.LENGTH_SHORT
                    ).show()
                })
            } else if (ACTION_CHOOSE_FILE == intent.action) {
                if (supportActionBar != null) {
                    if (!hasTitlesDefined) supportActionBar!!.setTitle(R.string.text_chooseFile) else supportActionBar!!.setSubtitle(
                        R.string.text_chooseFolder
                    )
                }
                mFileExplorerFragment.setLayoutClickListener(LayoutClickListener<GroupViewHolder> { listFragment: EditableListFragmentBase<*>?, holder: GroupViewHolder?, longClick: Boolean ->
                    if (longClick) return@setLayoutClickListener false
                    val fileHolder: FileHolder = mFileExplorerFragment.getAdapter().getItem(holder)
                    if (fileHolder.file.isFile()) {
                        finishWithResult(fileHolder.file)
                        return@setLayoutClickListener true
                    }
                    false
                })
            } else finish()
            if (!isFinishing) if (intent.hasExtra(EXTRA_START_PATH)) {
                try {
                    mFileExplorerFragment.goPath(
                        FileUtils.fromUri(
                            this,
                            Uri.parse(intent.getStringExtra(EXTRA_START_PATH))
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) finish() else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onBackPressed() {
        if (mFileExplorerFragment == null || !mFileExplorerFragment.onBackPressed()) super.onBackPressed()
    }

    private fun finishWithResult(file: DocumentFile) {
        setResult(
            RESULT_OK, Intent(ACTION_CHOOSE_DIRECTORY)
                .putExtra(EXTRA_CHOSEN_PATH, file.uri)
        )
        finish()
    }

    companion object {
        const val ACTION_CHOOSE_DIRECTORY = "com.genonbeta.intent.action.CHOOSE_DIRECTORY"
        const val ACTION_CHOOSE_FILE = "com.genonbeta.intent.action.CHOOSE_FILE"
        const val EXTRA_ACTIVITY_TITLE = "activityTitle"
        const val EXTRA_START_PATH = "startPath"

        // belongs to returned result intent
        const val EXTRA_CHOSEN_PATH = "chosenPath"
    }
}