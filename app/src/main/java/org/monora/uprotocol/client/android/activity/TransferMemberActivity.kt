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
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.fragment.TransferMemberListFragment
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.BaseAttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage
import org.monora.uprotocol.client.android.task.AddDeviceTask
import org.monora.uprotocol.client.android.task.OrganizeLocalSharingTask
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToColor

class TransferMemberActivity : Activity(), SnackbarPlacementProvider, AttachedTaskListener {
    private val transfer: Transfer? by lazy {
        intent?.getParcelableExtra(EXTRA_TRANSFER)
    }

    private val addingInitialDevice by lazy {
        intent?.getBooleanExtra(EXTRA_ADDING_FIRST_DEVICE, false) ?: false
    }

    private lateinit var actionButton: ExtendedFloatingActionButton

    private lateinit var progressBar: ProgressBar

    private var colorActive = 0

    private var colorNormal = 0

    private val filter: IntentFilter = IntentFilter(KuickDb.ACTION_DATABASE_CHANGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_devices_to_transfer)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (addingInitialDevice) {
            startConnectionManagerActivity()
        }

        colorActive = R.attr.colorError.attrToRes(this).resToColor(this)
        colorNormal = R.attr.colorAccent.attrToRes(this).resToColor(this)
        progressBar = findViewById(R.id.progressBar)
        actionButton = findViewById(R.id.content_fab)

        actionButton.setOnClickListener {
            if (hasTaskOf(OrganizeLocalSharingTask::class.java)) {
                interruptAllTasks(true)
            } else {
                startConnectionManagerActivity()
            }
        }

        supportFragmentManager.findFragmentById(R.id.membersListFragment) ?: run {
            val memberListFragment = supportFragmentManager.fragmentFactory.instantiate(
                this.classLoader, TransferMemberListFragment::class.java.name
            ) as TransferMemberListFragment

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
        } else
            return super.onOptionsItemSelected(item)
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
            if (resultCode == RESULT_CANCELED && addingInitialDevice) {
                // TODO: 2/25/21 Because the user cancelled choosing the initial device, we assume the task is cancelled, so remove it.
            } else if (resultCode == RESULT_OK && data != null && data.hasExtra(AddDeviceActivity.EXTRA_DEVICE)
                && data.hasExtra(AddDeviceActivity.EXTRA_DEVICE_ADDRESS)
            ) {
                val client: UClient? = data.getParcelableExtra(AddDeviceActivity.EXTRA_DEVICE)
                val address: UClientAddress? = data.getParcelableExtra(AddDeviceActivity.EXTRA_DEVICE_ADDRESS)
                val transfer = transfer

                if (client != null && address != null && transfer != null) {
                    runUiTask(AddDeviceTask(transfer, client, address))
                }
            }
        }
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList) if (task is AddDeviceTask) task.anchor = this
    }


    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State) {
        if (task is AddDeviceTask) {
            when (state) {
                AsyncTask.State.Starting -> setLoaderShowing(true)
                AsyncTask.State.Running -> {
                }
                AsyncTask.State.Finished -> setLoaderShowing(false)
            }
        }
    }

    override fun onTaskMessage(taskMessage: TaskMessage): Boolean {
        if (taskMessage.sizeOfActions() > 1) runOnUiThread {
            taskMessage.toDialogBuilder(this).show()
        } else if (taskMessage.sizeOfActions() <= 1) runOnUiThread {
            taskMessage.toSnackbar(findViewById(R.id.content_fab)).show()
        } else
            return false
        return true
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar {
        return Snackbar.make(findViewById(R.id.container), getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    private fun setLoaderShowing(showing: Boolean) {
        progressBar.visibility = if (showing) View.VISIBLE else View.GONE
        actionButton.setIconResource(if (showing) R.drawable.ic_close_white_24dp else R.drawable.ic_add_white_24dp)
        actionButton.setText(if (showing) R.string.butn_cancel else R.string.butn_addMore)
        actionButton.backgroundTintList = ColorStateList.valueOf(if (showing) colorActive else colorNormal)
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

        fun startInstance(context: Context, transfer: Transfer, addingFirstDevice: Boolean) {
            context.startActivity(
                Intent(context, TransferMemberActivity::class.java)
                    .putExtra(EXTRA_TRANSFER, transfer)
                    .putExtra(EXTRA_ADDING_FIRST_DEVICE, addingFirstDevice)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}