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
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activityimport.WebShareActivity
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.dialog.TransferInfoDialog
import com.genonbeta.TrebleShot.fragment.TransferItemExplorerFragment
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage
import com.genonbeta.TrebleShot.task.FileTransferTask
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.snackbar.Snackbar

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */
class TransferDetailActivity : Activity(), SnackbarPlacementProvider, AttachedTaskListener {
    private var backPressedListener: OnBackPressedListener? = null

    private var transferInternal: Transfer? = null

    private var indexInternal: TransferIndex? = null

    private val transfer: Transfer
        get() {

        }

    private val index: TransferIndex
        get() {}

    private lateinit var openWebShareButton: Button

    private lateinit var noDevicesNoticeText: View

    private var mMember: LoadedMember? = null

    private lateinit var retryMenu: MenuItem

    private lateinit var showFilesMenu: MenuItem

    private lateinit var addDeviceMenu: MenuItem

    private lateinit var limitMenu: MenuItem

    private lateinit var toggleBrowserShare: MenuItem

    private var colorActive = 0

    private var colorNormal = 0

    private var dataCruncher: CrunchLatestDataTask? = null

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: KuickDb.BroadcastData = KuickDb.toData(intent)
                if (Kuick.TABLE_TRANSFER == data.tableName) {
                    reconstructGroup()
                } else if (Kuick.TABLE_TRANSFERITEM == data.tableName && (data.inserted || data.removed)
                    || Kuick.TABLE_TRANSFERMEMBER == data.tableName && (data.inserted || data.removed)
                ) updateCalculations()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_transfer)

        var transferItem: TransferItem? = null
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val intentData = intent.data

        setSupportActionBar(toolbar)

        transferInternal = savedInstanceState?.getParcelable(EXTRA_TRANSFER)
        openWebShareButton = findViewById(R.id.activity_transfer_detail_open_web_share_button)
        noDevicesNoticeText = findViewById(R.id.activity_transfer_detail_no_devices_warning)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        colorActive = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorError))
        colorNormal = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorAccent))

        openWebShareButton.setOnClickListener { v: View? ->
            startActivity(Intent(this, WebShareActivity::class.java))
        }
        transferInternal?.let {
            Log.d(TAG, "onCreate: Created transfer instance from the bundle")
            setTransfer(it)
        } ?: if (Intent.ACTION_VIEW == intent.action && intentData != null) {
            try {
                val streamInfo: StreamInfo = StreamInfo.from(this, intentData)
                Log.d(TAG, "Requested file is: " + streamInfo.friendlyName)
                val fileData = database.getFirstFromTable(
                    SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                        .setWhere(
                            Kuick.FIELD_TRANSFERITEM_FILE + "=? AND " + Kuick.FIELD_TRANSFERITEM_TYPE + "=?",
                            streamInfo.friendlyName, TransferItem.Type.INCOMING.toString()
                        )
                ) ?: throw Exception("File is not found in the database")
                transferItem = TransferItem()
                transferItem.reconstruct(database.writableDatabase, database, fileData)
                val transfer = Transfer(transferItem.transferId)
                database.reconstruct(transfer)
                setTransfer(transfer)
                TransferInfoDialog(this, index, transferItem, null).show()
                Log.d(
                    TAG, "Created instance from an file intent. Original has been cleaned " +
                            "and changed to open intent"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, R.string.mesg_notValidTransfer, Toast.LENGTH_SHORT).show()
            }
        } else if (ACTION_LIST_TRANSFERS == intent.action && intent.hasExtra(EXTRA_TRANSFER)) {
            setTransfer(intent.getParcelableExtra(EXTRA_TRANSFER))
            try {
                if (intent.hasExtra(EXTRA_TRANSFER_ITEM_ID) && intent.hasExtra(EXTRA_DEVICE)
                    && intent.hasExtra(EXTRA_TRANSFER_TYPE)
                ) {
                    val requestId = intent.getLongExtra(EXTRA_TRANSFER_ITEM_ID, -1)
                    val device: Device = intent.getParcelableExtra(EXTRA_DEVICE)
                    val type = intent.getSerializableExtra(EXTRA_TRANSFER_TYPE) as TransferItem.Type
                    transferItem = TransferItem(transfer!!.id, requestId, type)
                    database.reconstruct(transferItem)
                    if (device != null) TransferInfoDialog(this, index, transferItem, device.uid).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (transfer == null) finish() else {
            val bundle = Bundle()
            bundle.putLong(TransferItemListFragment.ARG_TRANSFER_ID, transfer!!.id)
            bundle.putString(
                TransferItemListFragment.ARG_PATH, if (transferItem == null
                    || transferItem.directory == null
                ) null else transferItem.directory
            )
            var fragment: TransferItemExplorerFragment? = getExplorerFragment()
            if (fragment == null) {
                fragment = supportFragmentManager.fragmentFactory
                    .instantiate(
                        classLoader,
                        TransferItemExplorerFragment::class.java.getName()
                    ) as TransferItemExplorerFragment
                fragment.setArguments(bundle)
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.activity_transaction_content_frame, fragment)
                transaction.commit()
            }
            attachListeners(fragment)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, IntentFilter(KuickDb.ACTION_DATABASE_CHANGE))
        reconstructGroup()
        updateCalculations()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_TRANSFER, transfer)
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (attachableAsyncTask in taskList) {
            if (attachableAsyncTask is FileTransferTask) attachableAsyncTask.anchor = this
        }
        if (!hasTaskOf(FileTransferTask::class.java)) showMenus()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.actions_transfer, menu)
        retryMenu = menu.findItem(R.id.actions_transfer_receiver_retry_receiving)
        showFilesMenu = menu.findItem(R.id.actions_transfer_receiver_show_files)
        addDeviceMenu = menu.findItem(R.id.actions_transfer_sender_add_device)
        limitMenu = menu.findItem(R.id.actions_transfer_limit_to)
        toggleBrowserShare = menu.findItem(R.id.actions_transfer_toggle_browser_share)
        showMenus()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val devicePosition = findCurrentDevicePosition()
        val thisMenu: Menu = menu.findItem(R.id.actions_transfer_limit_to).subMenu
        var checkedItem: MenuItem? = null
        if ((devicePosition < 0 || thisMenu.getItem(devicePosition)
                .also { checkedItem = it } == null) && thisMenu.size() > 0
        ) checkedItem = thisMenu.getItem(thisMenu.size() - 1)
        if (checkedItem != null) checkedItem!!.isChecked = true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        } else if (id == R.id.actions_transfer_remove) {
            DialogUtils.showRemoveDialog(this, transfer)
        } else if (id == R.id.actions_transfer_receiver_retry_receiving) {
            Transfers.recoverIncomingInterruptions(this@TransferDetailActivity, transfer!!.id)
            createSnackbar(R.string.mesg_retryReceivingNotice).show()
        } else if (id == R.id.actions_transfer_receiver_show_files) {
            startActivity(
                Intent(this, FileExplorerActivity::class.java)
                    .putExtra(
                        FileExplorerActivity.EXTRA_FILE_PATH,
                        Files.getSavePath(this, transfer).uri
                    )
            )
        } else if (id == R.id.actions_transfer_sender_add_device) {
            startDeviceAddingActivity()
        } else if (item.itemId == R.id.actions_transfer_toggle_browser_share) {
            transfer!!.isServedOnWeb = !transfer!!.isServedOnWeb
            database.update(transfer)
            database.broadcast()
            showMenus()
        } else if (item.groupId == R.id.actions_abs_view_transfer_activity_limit_to) {
            mMember = if (item.order < index.members.length) index.members.get(item.order) else null
            val fragment: TransferItemExplorerFragment? = supportFragmentManager
                .findFragmentById(R.id.activity_transaction_content_frame) as TransferItemExplorerFragment?
            if (fragment != null && fragment.getAdapter().setMember(mMember)) fragment.refreshList()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onBackPressed() {
        if (backPressedListener == null || !backPressedListener!!.onBackPressed()) super.onBackPressed()
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State) {
        if (task is FileTransferTask) {
            when (state) {
                AsyncTask.State.Finished, AsyncTask.State.Starting -> showMenus()
            }
        }
    }

    override fun onTaskMessage(taskMessage: TaskMessage): Boolean {
        runOnUiThread { taskMessage.toDialogBuilder(this).show() }
        return true
    }

    private fun attachListeners(initiatedItem: Fragment?) {
        backPressedListener = if (initiatedItem is OnBackPressedListener) initiatedItem else null
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar? {
        val explorerFragment = supportFragmentManager.findFragmentById(
            R.id.activity_transaction_content_frame
        ) as TransferItemExplorerFragment?

        return if (explorerFragment != null && explorerFragment.isAdded) {
            explorerFragment.createSnackbar(resId, *objects)
        } else Snackbar.make(
            findViewById(R.id.activity_transaction_content_frame), getString(resId, *objects),
            Snackbar.LENGTH_LONG
        )
    }

    fun findCurrentDevicePosition(): Int {
        val members: Array<LoadedMember> = index.members
        if (mMember != null && members.size > 0) {
            for (i in members.indices) {
                val member: LoadedMember = members[i]
                if (mMember.deviceId == member.device.uid) return i
            }
        }
        return -1
    }

    fun getMember(): LoadedMember? {
        return mMember
    }

    fun getExplorerFragment(): TransferItemExplorerFragment? {
        return supportFragmentManager.findFragmentById(
            R.id.activity_transaction_content_frame
        ) as TransferItemExplorerFragment?
    }

    override fun getIdentity(): Identity {
        return FileTransferTask.identifyWith(transfer!!.id)
    }

    fun getToggleButton(): ExtendedFloatingActionButton? {
        val explorerFragment: TransferItemExplorerFragment? = getExplorerFragment()
        return if (explorerFragment != null) explorerFragment.getToggleButton() else null
    }

    fun isDeviceRunning(deviceId: String?): Boolean {
        return hasTaskWith(FileTransferTask.identifyWith(transfer!!.id, deviceId))
    }

    fun reconstructGroup() {
        try {
            database.reconstruct(transfer)
            showMenus()
        } catch (e: Exception) {
            finish()
        }
    }

    private fun setTransfer(transfer: Transfer) {
        this.transfer = transfer
        index = TransferIndex(transfer)
    }

    fun showMenus() {
        val hasRunning = hasTaskOf(FileTransferTask::class.java)
        val hasAnyFiles: Boolean = index.numberOfTotal() > 0
        val hasIncoming: Boolean = index.hasIncoming()
        val hasOutgoing: Boolean = index.hasOutgoing()
        val toggleButton: ExtendedFloatingActionButton? = getToggleButton()
        if (retryMenu == null || showFilesMenu == null) return
        if (toggleButton != null) {
            if (Build.VERSION.SDK_INT <= 14 || !toggleButton.hasOnClickListeners()) toggleButton.setOnClickListener(
                View.OnClickListener { v: View? -> toggleTask() })
            if (hasAnyFiles || hasRunning) {
                toggleButton.setIconResource(if (hasRunning) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp)
                toggleButton.setBackgroundTintList(ColorStateList.valueOf(if (hasRunning) colorActive else colorNormal))
                if (hasRunning) toggleButton.setText(R.string.butn_pause) else toggleButton.setText(if (hasIncoming == hasOutgoing) R.string.butn_start else if (hasIncoming) R.string.butn_receive else R.string.butn_send)
                toggleButton.setVisibility(View.VISIBLE)
            } else toggleButton.setVisibility(View.GONE)
        }
        openWebShareButton!!.visibility = if (transfer!!.isServedOnWeb) View.VISIBLE else View.GONE
        noDevicesNoticeText!!.visibility =
            if (index.members.length > 0 || transfer!!.isServedOnWeb) View.GONE else View.VISIBLE
        toggleBrowserShare!!.setTitle(if (transfer!!.isServedOnWeb) R.string.butn_hideOnBrowser else R.string.butn_shareOnBrowser)
        toggleBrowserShare!!.isVisible = hasOutgoing || transfer!!.isServedOnWeb
        addDeviceMenu!!.isVisible = hasOutgoing
        retryMenu!!.isVisible = hasIncoming
        showFilesMenu!!.isVisible = hasIncoming
        if (hasOutgoing && (index.members.length > 0 || mMember != null)) {
            val dynamicMenu: Menu = limitMenu!!.setVisible(true).subMenu
            dynamicMenu.clear()
            var i = 0
            val members: Array<LoadedMember> = index.members
            if (members.size > 0) while (i < members.size) {
                val member: LoadedMember = members[i]
                dynamicMenu.add(
                    R.id.actions_abs_view_transfer_activity_limit_to, 0, i,
                    member.device.username
                )
                i++
            }
            dynamicMenu.add(R.id.actions_abs_view_transfer_activity_limit_to, 0, i, R.string.text_none)
            dynamicMenu.setGroupCheckable(
                R.id.actions_abs_view_transfer_activity_limit_to, true,
                true
            )
        } else limitMenu!!.isVisible = false
        title = resources.getQuantityString(
            R.plurals.text_files, index.numberOfTotal(),
            index.numberOfTotal()
        )
    }

    fun startDeviceAddingActivity() {
        startActivityForResult(
            Intent(this, TransferMemberActivity::class.java)
                .putExtra(TransferMemberActivity.EXTRA_TRANSFER, transfer), REQUEST_ADD_DEVICES
        )
    }

    private fun toggleTask() {
        val memberList: List<LoadedMember?>? = Transfers.loadMemberList(this, transfer!!.id, null)
        if (memberList!!.size > 0) {
            if (memberList.size == 1) {
                val member: LoadedMember? = memberList[0]
                toggleTaskForMember(member)
            } else ToggleMultipleTransferDialog(this@TransferDetailActivity, index).show()
        } else if (index.hasOutgoing()) startDeviceAddingActivity()
    }

    fun toggleTaskForMember(member: LoadedMember?) {
        if (hasTaskWith(
                FileTransferTask.identifyWith(
                    transfer!!.id,
                    member.deviceId
                )
            )
        ) Transfers.pauseTransfer(this, member) else {
            try {
                Transfers.getAddressListFor(database, member.deviceId)
                Transfers.startTransferWithTest(this, transfer, member)
            } catch (e: ConnectionNotFoundException) {
                createSnackbar(R.string.mesg_transferConnectionNotSetUpFix).show()
            }
        }
    }

    @Synchronized
    fun updateCalculations() {
        if (dataCruncher == null || !dataCruncher!!.requestRestart()) {
            dataCruncher = CrunchLatestDataTask(PostExecutionListener { showMenus() })
            dataCruncher!!.execute(this)
        }
    }

    class CrunchLatestDataTask(private val mListener: PostExecutionListener) :
        android.os.AsyncTask<TransferDetailActivity?, Void?, Void?>() {
        private var mRestartRequested = false

        /* "possibility of having more than one ViewTransferActivity" < "sun turning into black hole" */
        protected override fun doInBackground(vararg activities: TransferDetailActivity): Void? {
            do {
                mRestartRequested = false
                for (activity in activities) if (activity.transfer != null) Transfers.loadTransferInfo(
                    activity,
                    activity.index,
                    activity.getMember()
                )
            } while (mRestartRequested && !isCancelled)
            return null
        }

        fun requestRestart(): Boolean {
            if (status == Status.RUNNING) mRestartRequested = true
            return mRestartRequested
        }

        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            if (!isCancelled) mListener.onPostExecute()
        }

        /* Should we have used a generic type class for this?
         * This interface aims to keep its parent class non-anonymous
         */
        interface PostExecutionListener {
            fun onPostExecute()
        }
    }

    companion object {
        val TAG = TransferDetailActivity::class.java.simpleName
        const val ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS"
        const val EXTRA_TRANSFER = "extraTransfer"
        const val EXTRA_TRANSFER_ITEM_ID = "extraTransferItemId"
        const val EXTRA_DEVICE = "extraDevice"
        const val EXTRA_TRANSFER_TYPE = "extraTransferType"
        const val REQUEST_ADD_DEVICES = 5045
        fun startInstance(context: Context, transfer: Transfer?) {
            context.startActivity(
                Intent(context, TransferDetailActivity::class.java)
                    .setAction(ACTION_LIST_TRANSFERS)
                    .putExtra(EXTRA_TRANSFER, transfer)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}