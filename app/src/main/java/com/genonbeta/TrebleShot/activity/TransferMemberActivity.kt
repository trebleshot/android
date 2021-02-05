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
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.Exception

class TransferMemberActivity : Activity(), SnackbarPlacementProvider, AttachedTaskListener {
    private var mTransfer: Transfer? = null
    private var mActionButton: ExtendedFloatingActionButton? = null
    private var mProgressBar: ProgressBar? = null
    private var mAddingFirstDevice = false
    private var mColorActive = 0
    private var mColorNormal = 0
    private val mFilter: IntentFilter = IntentFilter(KuickDb.ACTION_DATABASE_CHANGE)
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: BroadcastData = KuickDb.toData(intent)
                if (Kuick.Companion.TABLE_TRANSFER == data.tableName && !checkTransferIntegrity()) finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_devices_to_transfer)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (checkTransferIntegrity()) {
            mAddingFirstDevice = intent.getBooleanExtra(EXTRA_ADDING_FIRST_DEVICE, false)
            if (mAddingFirstDevice) startConnectionManagerActivity()
        } else return
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val memberFragmentArgs = Bundle()
        memberFragmentArgs.putLong(TransferMemberListFragment.Companion.ARG_TRANSFER_ID, mTransfer!!.id)
        memberFragmentArgs.putBoolean(TransferMemberListFragment.Companion.ARG_USE_HORIZONTAL_VIEW, false)
        mColorActive = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorError))
        mColorNormal = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorAccent))
        mProgressBar = findViewById(R.id.progressBar)
        mActionButton = findViewById<ExtendedFloatingActionButton>(R.id.content_fab)
        mActionButton.setOnClickListener(View.OnClickListener { v: View? ->
            if (hasTaskOf(
                    OrganizeLocalSharingTask::class.java
                )
            ) interruptAllTasks(true) else startConnectionManagerActivity()
        })
        var memberListFragment: TransferMemberListFragment? = supportFragmentManager
            .findFragmentById(R.id.membersListFragment) as TransferMemberListFragment?
        if (memberListFragment == null) {
            memberListFragment = supportFragmentManager.fragmentFactory
                .instantiate(
                    this.classLoader,
                    TransferMemberListFragment::class.java.getName()
                ) as TransferMemberListFragment
            memberListFragment.setArguments(memberFragmentArgs)
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.membersListFragment, memberListFragment)
            transaction.commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home || id == R.id.actions_add_devices_done) {
            interruptAllTasks(true)
            finish()
        } else if (id == R.id.actions_add_devices_help) {
            AlertDialog.Builder(this)
                .setTitle(R.string.text_help)
                .setMessage(R.string.text_addDeviceHelp)
                .setPositiveButton(R.string.butn_close, null)
                .show()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.actions_add_devices, menu)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE_DEVICE) {
            if (resultCode == RESULT_CANCELED && mAddingFirstDevice) {
                database.removeAsynchronous(this, mTransfer, null)
            } else if (resultCode == RESULT_OK && data != null && data.hasExtra(AddDeviceActivity.Companion.EXTRA_DEVICE)
                && data.hasExtra(AddDeviceActivity.Companion.EXTRA_DEVICE_ADDRESS)
            ) {
                val device: Device = data.getParcelableExtra(AddDeviceActivity.Companion.EXTRA_DEVICE)
                val connection: DeviceAddress =
                    data.getParcelableExtra(AddDeviceActivity.Companion.EXTRA_DEVICE_ADDRESS)
                if (device != null && connection != null) runUiTask(AddDeviceTask(mTransfer, device, connection))
            }
        }
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList) if (task is AddDeviceTask) (task as AddDeviceTask).setAnchor(this)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mFilter)
        if (!checkTransferIntegrity()) finish()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State?) {
        if (task is AddDeviceTask) {
            when (state) {
                AsyncTask.State.Starting -> setLoaderShowing(true)
                AsyncTask.State.Running -> {
                }
                AsyncTask.State.Finished -> setLoaderShowing(false)
            }
        }
    }

    override fun onTaskMessage(message: TaskMessage): Boolean {
        if (message.sizeOfActions() > 1) runOnUiThread {
            message.toDialogBuilder(this).show()
        } else if (message.sizeOfActions() <= 1) runOnUiThread {
            message.toSnackbar(findViewById<View>(R.id.content_fab)).show()
        } else return false
        return true
    }

    fun checkTransferIntegrity(): Boolean {
        try {
            if (intent == null || !intent.hasExtra(EXTRA_TRANSFER)) throw Exception()
            if (mTransfer == null) mTransfer = intent.getParcelableExtra(EXTRA_TRANSFER)
            if (mTransfer == null) throw Exception()
            database.reconstruct(mTransfer)
            return true
        } catch (e: Exception) {
            finish()
        }
        return false
    }

    override fun createSnackbar(resId: Int, vararg objects: Any): Snackbar {
        return Snackbar.make(findViewById<View>(R.id.container), getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    fun isAddingFirstDevice(): Boolean {
        return mAddingFirstDevice
    }

    private fun setLoaderShowing(showing: Boolean) {
        mProgressBar!!.visibility = if (showing) View.VISIBLE else View.GONE
        mActionButton.setIconResource(if (showing) R.drawable.ic_close_white_24dp else R.drawable.ic_add_white_24dp)
        mActionButton.setText(if (showing) R.string.butn_cancel else R.string.butn_addMore)
        mActionButton.setBackgroundTintList(ColorStateList.valueOf(if (showing) mColorActive else mColorNormal))
    }

    private fun startConnectionManagerActivity() {
        startActivityForResult(Intent(this, AddDeviceActivity::class.java), REQUEST_CODE_CHOOSE_DEVICE)
    }

    companion object {
        val TAG = TransferMemberActivity::class.java.simpleName
        const val EXTRA_DEVICE = "extraDevice"
        const val EXTRA_TRANSFER = "extraTransfer"
        const val EXTRA_ADDING_FIRST_DEVICE = "extraAddingFirstDevice"
        const val REQUEST_CODE_CHOOSE_DEVICE = 0
        fun startInstance(context: Context, transfer: Transfer?, addingFirstDevice: Boolean) {
            context.startActivity(
                Intent(context, TransferMemberActivity::class.java)
                    .putExtra(EXTRA_TRANSFER, transfer)
                    .putExtra(EXTRA_ADDING_FIRST_DEVICE, addingFirstDevice)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}