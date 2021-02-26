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
package org.monora.uprotocol.client.android.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.dialog.DialogUtils
import org.monora.uprotocol.client.android.exception.TransferNotFoundException
import org.monora.uprotocol.client.android.fragment.TransferItemExplorerFragment
import org.monora.uprotocol.client.android.fragment.TransferItemListFragment
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.BaseAttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage
import org.monora.uprotocol.client.android.task.FileTransferTask
import org.monora.uprotocol.client.android.util.Files
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToColor
import org.monora.uprotocol.core.transfer.TransferItem
import javax.inject.Inject

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */
@AndroidEntryPoint
class TransferDetailActivity : Activity(), SnackbarPlacementProvider, AttachedTaskListener {
    @Inject
    lateinit var appDatabase: AppDatabase

    private var backPressedListener: OnBackPressedListener? = null

    private var transfer: Transfer? = null

    private lateinit var openWebShareButton: Button

    private lateinit var noDevicesNoticeText: View

    private lateinit var retryMenu: MenuItem

    private lateinit var showFilesMenu: MenuItem

    private lateinit var addDeviceMenu: MenuItem


    private lateinit var toggleBrowserShare: MenuItem

    private var colorActive = 0

    private var colorNormal = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_transfer)

        var transferItem: UTransferItem? = null
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val intentData = intent.data

        setSupportActionBar(toolbar)

        transfer = savedInstanceState?.getParcelable(EXTRA_TRANSFER)
        openWebShareButton = findViewById(R.id.activity_transfer_detail_open_web_share_button)
        noDevicesNoticeText = findViewById(R.id.activity_transfer_detail_no_devices_warning)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        colorActive = R.attr.colorError.attrToRes(this).resToColor(this)
        colorNormal = R.attr.colorAccent.attrToRes(this).resToColor(this)

        openWebShareButton.setOnClickListener {
            startActivity(Intent(this, WebShareActivity::class.java))
        }

        if (Intent.ACTION_VIEW == intent.action && intentData != null) {
            try {
                val streamInfo = StreamInfo.from(this, intentData)

                transferItem = appDatabase.transferItemDao().get(
                    streamInfo.friendlyName, TransferItem.Type.Incoming
                ) ?: throw java.lang.Exception("File is not found in the database")

                transfer = appDatabase.transferDao().get(
                    transferItem.itemGroupId
                ) ?: throw TransferNotFoundException(transferItem.itemGroupId)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, R.string.mesg_notValidTransfer, Toast.LENGTH_SHORT).show()
            }
        } else if (ACTION_LIST_TRANSFERS == intent.action && intent.hasExtra(EXTRA_TRANSFER)) {
            transfer = intent.getParcelableExtra(EXTRA_TRANSFER)
        }

        val transfer = transfer

        if (transfer == null)
            finish()
        else {
            if (transferItem != null) {
                // TODO: 2/25/21 Display the transfer item info
            }

            val bundle = Bundle()
            bundle.putLong(TransferItemListFragment.ARG_TRANSFER_ID, transfer.id)
            bundle.putString(
                TransferItemListFragment.ARG_PATH,
                transferItem?.location
            )
            var fragment: TransferItemExplorerFragment? = getExplorerFragment()
            if (fragment == null) {
                fragment = supportFragmentManager.fragmentFactory.instantiate(
                    classLoader,
                    TransferItemExplorerFragment::class.java.name
                ) as TransferItemExplorerFragment
                fragment.arguments = bundle
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.activity_transaction_content_frame, fragment)
                transaction.commit()
            }
            attachListeners(fragment)
        }
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
        if (!hasTaskOf(FileTransferTask::class.java)) {
            // TODO: 2/25/21 No tasks update UI
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.actions_transfer, menu)
        retryMenu = menu.findItem(R.id.actions_transfer_receiver_retry_receiving)
        showFilesMenu = menu.findItem(R.id.actions_transfer_receiver_show_files)
        addDeviceMenu = menu.findItem(R.id.actions_transfer_sender_add_device)
        toggleBrowserShare = menu.findItem(R.id.actions_transfer_toggle_browser_share)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val transfer = transfer

        if (id == android.R.id.home) {
            finish()
        } else if (id == R.id.actions_transfer_remove) {
            if (transfer != null) {
                DialogUtils.showRemoveDialog(this, transfer)
            }
        } else if (id == R.id.actions_transfer_receiver_retry_receiving) {
            transfer?.let {
                appDatabase.transferItemDao().updateTemporaryFailuresAsPending(transfer.id)
                createSnackbar(R.string.mesg_retryReceivingNotice)?.show()
            }
        } else if (id == R.id.actions_transfer_receiver_show_files) {
            if (transfer != null) {
                startActivity(
                    Intent(this, FileExplorerActivity::class.java)
                        .putExtra(
                            FileExplorerActivity.EXTRA_FILE_PATH,
                            Files.getSavePath(this, transfer).getUri()
                        )
                )
            }
        } else if (id == R.id.actions_transfer_sender_add_device) {
            // TODO: 2/25/21 add device
            //startDeviceAddingActivity()
        } else if (item.itemId == R.id.actions_transfer_toggle_browser_share) {
            lifecycleScope.launch {
                transfer?.let {
                    it.web = !it.web
                    appDatabase.transferDao().update(it)
                }
            }
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        if (backPressedListener == null || !backPressedListener!!.onBackPressed()) super.onBackPressed()
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State) {
        if (task is FileTransferTask) {
            when (state) {
                AsyncTask.State.Finished, AsyncTask.State.Starting -> {
                    // TODO: 2/25/21 Update the menus
                }
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

    fun getExplorerFragment(): TransferItemExplorerFragment? {
        return supportFragmentManager.findFragmentById(
            R.id.activity_transaction_content_frame
        ) as TransferItemExplorerFragment?
    }

    companion object {
        val TAG = TransferDetailActivity::class.java.simpleName

        const val ACTION_LIST_TRANSFERS = "org.monora.uprotocol.client.android.action.LIST_TRANSFERS"

        const val EXTRA_TRANSFER = "extraTransfer"

        const val EXTRA_DEVICE = "extraDevice"

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