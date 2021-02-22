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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.BaseAttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage
import org.monora.uprotocol.client.android.task.OrganizeSharingTask
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.snackbar.Snackbar
import java.util.*

class ShareActivity : Activity(), SnackbarPlacementProvider, AttachedTaskListener {
    private lateinit var progressBar: ProgressBar

    private lateinit var textMain: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        val action = if (intent != null) intent.action else null
        if (ACTION_SEND == action || ACTION_SEND_MULTIPLE == action || Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                startActivity(
                    Intent(this@ShareActivity, TextEditorActivity::class.java)
                        .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                        .putExtra(
                            TextEditorActivity.EXTRA_TEXT,
                            intent.getStringExtra(Intent.EXTRA_TEXT)
                        )
                )
                finish()
            } else {
                val fileUris: MutableList<Uri> = ArrayList()
                if (ACTION_SEND_MULTIPLE == action || Intent.ACTION_SEND_MULTIPLE == action) {
                    val pendingFileUris: List<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    if (pendingFileUris != null) fileUris.addAll(pendingFileUris)
                } else {
                    val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    if (uri != null) fileUris.add(uri)
                }
                if (fileUris.size == 0) {
                    Toast.makeText(this, R.string.mesg_nothingToShare, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    progressBar = findViewById(R.id.progressBar)
                    textMain = findViewById(R.id.textMain)
                    findViewById<View>(R.id.cancelButton).setOnClickListener { v: View? -> interruptAllTasks(true) }
                    runUiTask(OrganizeSharingTask(fileUris))
                }
            }
        } else {
            Toast.makeText(this, R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State) {
        if (task is OrganizeSharingTask) {
            when (state) {
                AsyncTask.State.Running -> {
                    val progress = task.progress.getProgress()
                    val total = task.progress.getTotal()
                    runOnUiThread { textMain.text = task.ongoingContent }
                    progressBar.progress = progress
                    progressBar.max = total
                }
                AsyncTask.State.Finished -> finish()
            }
        }
    }

    override fun onTaskMessage(taskMessage: TaskMessage): Boolean {
        return false
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList) if (task is OrganizeSharingTask) task.anchor = this
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar {
        return Snackbar.make(window.decorView, getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    companion object {
        const val TAG = "ShareActivity"
        const val ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND"
        const val ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE"
        const val EXTRA_DEVICE_ID = "extraDeviceId"
    }
}