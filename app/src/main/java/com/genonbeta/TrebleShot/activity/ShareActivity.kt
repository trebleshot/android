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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import java.util.*

class ShareActivity : Activity(), SnackbarPlacementProvider, AttachedTaskListener {
    private var mProgressBar: ProgressBar? = null
    private var mTextMain: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        val action = if (intent != null) intent.action else null
        if (ACTION_SEND == action || ACTION_SEND_MULTIPLE == action || Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                startActivity(
                    Intent(this@ShareActivity, TextEditorActivity::class.java)
                        .setAction(TextEditorActivity.Companion.ACTION_EDIT_TEXT)
                        .putExtra(
                            TextEditorActivity.Companion.EXTRA_TEXT_INDEX,
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
                    fileUris.add(intent.getParcelableExtra(Intent.EXTRA_STREAM))
                }
                if (fileUris.size == 0) {
                    Toast.makeText(this, R.string.mesg_nothingToShare, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    mProgressBar = findViewById(R.id.progressBar)
                    mTextMain = findViewById<TextView>(R.id.textMain)
                    findViewById<View>(R.id.cancelButton).setOnClickListener { v: View? -> interruptAllTasks(true) }
                    runUiTask(OrganizeSharingTask(fileUris))
                }
            }
        } else {
            Toast.makeText(this, R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State?) {
        if (task is OrganizeSharingTask) {
            when (state) {
                AsyncTask.State.Running -> {
                    val progress = task.progress().current
                    val total = task.progress().total
                    runOnUiThread { mTextMain.setText(task.ongoingContent) }
                    mProgressBar!!.progress = progress
                    mProgressBar!!.max = total
                }
                AsyncTask.State.Finished -> finish()
            }
        }
    }

    override fun onTaskMessage(message: TaskMessage): Boolean {
        return false
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList) if (task is OrganizeSharingTask) (task as OrganizeSharingTask).setAnchor(this)
    }

    override fun createSnackbar(resId: Int, vararg objects: Any): Snackbar {
        return Snackbar.make(window.decorView, getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    fun getProgressBar(): ProgressBar? {
        return mProgressBar
    }

    companion object {
        const val TAG = "ShareActivity"
        const val ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND"
        const val ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE"
        const val EXTRA_DEVICE_ID = "extraDeviceId"
    }
}