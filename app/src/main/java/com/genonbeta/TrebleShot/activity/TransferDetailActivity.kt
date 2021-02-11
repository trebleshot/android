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
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.TrebleShot.util.Files
import java.lang.Exception

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */
class TransferDetailActivity : Activity(), SnackbarPlacementProvider, AttachedTaskListener {
    private var mBackPressedListener: OnBackPressedListener? = null
    private var mTransfer: Transfer? = null
    private var mIndex: TransferIndex? = null
    private var mOpenWebShareButton: Button? = null
    private var mNoDevicesNoticeText: View? = null
    private var mMember: LoadedMember? = null
    private var mRetryMenu: MenuItem? = null
    private var mShowFilesMenu: MenuItem? = null
    private var mAddDeviceMenu: MenuItem? = null
    private var mLimitMenu: MenuItem? = null
    private var mToggleBrowserShare: MenuItem? = null
    private var mColorActive = 0
    private var mColorNormal = 0
    private var mDataCruncher: CrunchLatestDataTask? = null
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: BroadcastData = KuickDb.toData(intent)
                if (Kuick.TABLE_TRANSFER == data.tableName) reconstructGroup() else if (Kuick.TABLE_TRANSFERITEM == data.tableName && (data.inserted || data.removed)
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
        setSupportActionBar(toolbar)
        mTransfer = savedInstanceState?.getParcelable(EXTRA_TRANSFER)
        mOpenWebShareButton = findViewById(R.id.activity_transfer_detail_open_web_share_button)
        mNoDevicesNoticeText = findViewById(R.id.activity_transfer_detail_no_devices_warning)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        mColorActive = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorError))
        mColorNormal = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorAccent))
        mOpenWebShareButton.setOnClickListener(View.OnClickListener { v: View? ->
            startActivity(
                Intent(
                    this,
                    WebShareActivity::class.java
                )
            )
        })
        if (mTransfer != null) {
            Log.d(TAG, "onCreate: Created transfer instance from the bundle")
            setTransfer(mTransfer!!)
        } else if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            try {
                val streamInfo: StreamInfo = StreamInfo.getStreamInfo(this, intent.data)
                Log.d(TAG, "Requested file is: " + streamInfo.friendlyName)
                val fileData = database.getFirstFromTable(
                    SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                        .setWhere(
                            Kuick.FIELD_TRANSFERITEM_FILE + "=? AND " + Kuick.FIELD_TRANSFERITEM_TYPE + "=?",
                            streamInfo.friendlyName, TransferItem.Type.INCOMING.toString()
                        )
                )
                    ?: throw Exception("File is not found in the database")
                transferItem = TransferItem()
                transferItem.reconstruct(database.writableDatabase, database, fileData)
                val transfer = Transfer(transferItem.transferId)
                database.reconstruct(transfer)
                setTransfer(transfer)
                TransferInfoDialog(this, mIndex, transferItem, null).show()
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
                    transferItem = TransferItem(mTransfer!!.id, requestId, type)
                    database.reconstruct(transferItem)
                    if (device != null) TransferInfoDialog(this, mIndex, transferItem, device.uid).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (mTransfer == null) finish() else {
            val bundle = Bundle()
            bundle.putLong(TransferItemListFragment.ARG_TRANSFER_ID, mTransfer!!.id)
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
        outState.putParcelable(EXTRA_TRANSFER, mTransfer)
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (attachableAsyncTask in taskList) {
            if (attachableAsyncTask is FileTransferTask) (attachableAsyncTask as FileTransferTask).setAnchor(this)
        }
        if (!hasTaskOf(FileTransferTask::class.java)) showMenus()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.actions_transfer, menu)
        mRetryMenu = menu.findItem(R.id.actions_transfer_receiver_retry_receiving)
        mShowFilesMenu = menu.findItem(R.id.actions_transfer_receiver_show_files)
        mAddDeviceMenu = menu.findItem(R.id.actions_transfer_sender_add_device)
        mLimitMenu = menu.findItem(R.id.actions_transfer_limit_to)
        mToggleBrowserShare = menu.findItem(R.id.actions_transfer_toggle_browser_share)
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
            DialogUtils.showRemoveDialog(this, mTransfer)
        } else if (id == R.id.actions_transfer_receiver_retry_receiving) {
            Transfers.recoverIncomingInterruptions(this@TransferDetailActivity, mTransfer!!.id)
            createSnackbar(R.string.mesg_retryReceivingNotice).show()
        } else if (id == R.id.actions_transfer_receiver_show_files) {
            startActivity(
                Intent(this, FileExplorerActivity::class.java)
                    .putExtra(
                        FileExplorerActivity.EXTRA_FILE_PATH,
                        Files.getSavePath(this, mTransfer).uri
                    )
            )
        } else if (id == R.id.actions_transfer_sender_add_device) {
            startDeviceAddingActivity()
        } else if (item.itemId == R.id.actions_transfer_toggle_browser_share) {
            mTransfer!!.isServedOnWeb = !mTransfer!!.isServedOnWeb
            database.update(mTransfer)
            database.broadcast()
            showMenus()
        } else if (item.groupId == R.id.actions_abs_view_transfer_activity_limit_to) {
            mMember = if (item.order < mIndex.members.length) mIndex.members.get(item.order) else null
            val fragment: TransferItemExplorerFragment? = supportFragmentManager
                .findFragmentById(R.id.activity_transaction_content_frame) as TransferItemExplorerFragment?
            if (fragment != null && fragment.getAdapter().setMember(mMember)) fragment.refreshList()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onBackPressed() {
        if (mBackPressedListener == null || !mBackPressedListener!!.onBackPressed()) super.onBackPressed()
    }

    override fun onTaskStateChange(
        task: BaseAttachableAsyncTask,
        state: AsyncTask.State?
    ) {
        if (task is FileTransferTask) {
            when (state) {
                AsyncTask.State.Finished, AsyncTask.State.Starting -> showMenus()
            }
        }
    }

    override fun onTaskMessage(message: TaskMessage): Boolean {
        runOnUiThread { message.toDialogBuilder(this).show() }
        return true
    }

    private fun attachListeners(initiatedItem: Fragment?) {
        mBackPressedListener = if (initiatedItem is OnBackPressedListener) initiatedItem else null
    }

    override fun createSnackbar(resId: Int, vararg objects: Any): Snackbar {
        val explorerFragment: TransferItemExplorerFragment? = supportFragmentManager
            .findFragmentById(R.id.activity_transaction_content_frame) as TransferItemExplorerFragment?
        return if (explorerFragment != null && explorerFragment.isAdded()) explorerFragment.createSnackbar(
            resId,
            *objects
        ) else Snackbar.make(
            findViewById<View>(R.id.activity_transaction_content_frame), getString(resId, *objects),
            Snackbar.LENGTH_LONG
        )
    }

    fun findCurrentDevicePosition(): Int {
        val members: Array<LoadedMember> = mIndex.members
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
        return FileTransferTask.identifyWith(mTransfer!!.id)
    }

    fun getToggleButton(): ExtendedFloatingActionButton? {
        val explorerFragment: TransferItemExplorerFragment? = getExplorerFragment()
        return if (explorerFragment != null) explorerFragment.getToggleButton() else null
    }

    fun isDeviceRunning(deviceId: String?): Boolean {
        return hasTaskWith(FileTransferTask.identifyWith(mTransfer!!.id, deviceId))
    }

    fun reconstructGroup() {
        try {
            database.reconstruct(mTransfer)
            showMenus()
        } catch (e: Exception) {
            finish()
        }
    }

    private fun setTransfer(transfer: Transfer) {
        mTransfer = transfer
        mIndex = TransferIndex(transfer)
    }

    fun showMenus() {
        val hasRunning = hasTaskOf(FileTransferTask::class.java)
        val hasAnyFiles: Boolean = mIndex.numberOfTotal() > 0
        val hasIncoming: Boolean = mIndex.hasIncoming()
        val hasOutgoing: Boolean = mIndex.hasOutgoing()
        val toggleButton: ExtendedFloatingActionButton? = getToggleButton()
        if (mRetryMenu == null || mShowFilesMenu == null) return
        if (toggleButton != null) {
            if (Build.VERSION.SDK_INT <= 14 || !toggleButton.hasOnClickListeners()) toggleButton.setOnClickListener(
                View.OnClickListener { v: View? -> toggleTask() })
            if (hasAnyFiles || hasRunning) {
                toggleButton.setIconResource(if (hasRunning) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp)
                toggleButton.setBackgroundTintList(ColorStateList.valueOf(if (hasRunning) mColorActive else mColorNormal))
                if (hasRunning) toggleButton.setText(R.string.butn_pause) else toggleButton.setText(if (hasIncoming == hasOutgoing) R.string.butn_start else if (hasIncoming) R.string.butn_receive else R.string.butn_send)
                toggleButton.setVisibility(View.VISIBLE)
            } else toggleButton.setVisibility(View.GONE)
        }
        mOpenWebShareButton!!.visibility = if (mTransfer!!.isServedOnWeb) View.VISIBLE else View.GONE
        mNoDevicesNoticeText!!.visibility =
            if (mIndex.members.length > 0 || mTransfer!!.isServedOnWeb) View.GONE else View.VISIBLE
        mToggleBrowserShare!!.setTitle(if (mTransfer!!.isServedOnWeb) R.string.butn_hideOnBrowser else R.string.butn_shareOnBrowser)
        mToggleBrowserShare!!.isVisible = hasOutgoing || mTransfer!!.isServedOnWeb
        mAddDeviceMenu!!.isVisible = hasOutgoing
        mRetryMenu!!.isVisible = hasIncoming
        mShowFilesMenu!!.isVisible = hasIncoming
        if (hasOutgoing && (mIndex.members.length > 0 || mMember != null)) {
            val dynamicMenu: Menu = mLimitMenu!!.setVisible(true).subMenu
            dynamicMenu.clear()
            var i = 0
            val members: Array<LoadedMember> = mIndex.members
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
        } else mLimitMenu!!.isVisible = false
        title = resources.getQuantityString(
            R.plurals.text_files, mIndex.numberOfTotal(),
            mIndex.numberOfTotal()
        )
    }

    fun startDeviceAddingActivity() {
        startActivityForResult(
            Intent(this, TransferMemberActivity::class.java)
                .putExtra(TransferMemberActivity.EXTRA_TRANSFER, mTransfer), REQUEST_ADD_DEVICES
        )
    }

    private fun toggleTask() {
        val memberList: List<LoadedMember?>? = Transfers.loadMemberList(this, mTransfer!!.id, null)
        if (memberList!!.size > 0) {
            if (memberList.size == 1) {
                val member: LoadedMember? = memberList[0]
                toggleTaskForMember(member)
            } else ToggleMultipleTransferDialog(this@TransferDetailActivity, mIndex).show()
        } else if (mIndex.hasOutgoing()) startDeviceAddingActivity()
    }

    fun toggleTaskForMember(member: LoadedMember?) {
        if (hasTaskWith(
                FileTransferTask.identifyWith(
                    mTransfer!!.id,
                    member.deviceId
                )
            )
        ) Transfers.pauseTransfer(this, member) else {
            try {
                Transfers.getAddressListFor(database, member.deviceId)
                Transfers.startTransferWithTest(this, mTransfer, member)
            } catch (e: ConnectionNotFoundException) {
                createSnackbar(R.string.mesg_transferConnectionNotSetUpFix).show()
            }
        }
    }

    @Synchronized
    fun updateCalculations() {
        if (mDataCruncher == null || !mDataCruncher!!.requestRestart()) {
            mDataCruncher = CrunchLatestDataTask(PostExecutionListener { showMenus() })
            mDataCruncher!!.execute(this)
        }
    }

    class CrunchLatestDataTask(private val mListener: PostExecutionListener) :
        android.os.AsyncTask<TransferDetailActivity?, Void?, Void?>() {
        private var mRestartRequested = false

        /* "possibility of having more than one ViewTransferActivity" < "sun turning into black hole" */
        protected override fun doInBackground(vararg activities: TransferDetailActivity): Void? {
            do {
                mRestartRequested = false
                for (activity in activities) if (activity.mTransfer != null) Transfers.loadTransferInfo(
                    activity,
                    activity.mIndex,
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