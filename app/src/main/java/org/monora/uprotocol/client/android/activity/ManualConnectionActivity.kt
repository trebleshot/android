/*
 * Copyright (C) 2020 Veli Tasalı
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
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.AppCompatEditText
import androidx.transition.TransitionManager
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.model.DeviceRoute
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.BaseAttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage
import org.monora.uprotocol.client.android.task.DeviceIntroductionTask
import java.net.InetAddress
import java.net.UnknownHostException
import android.os.AsyncTask as UiTask

class ManualConnectionActivity : Activity(), DeviceIntroductionTask.ResultListener {
    private val hostnameListener = CheckHostnameListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_address_connection)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<View>(R.id.confirm_button).setOnClickListener { v: View? ->
            val editText: AppCompatEditText = getEditText()
            val editable = editText.text

            if (editable.isNullOrEmpty())
                editText.error = getString(R.string.mesg_enterValidHostAddress)
            else {
                val asyncTask = CheckAddressAsyncTask(hostnameListener)
                asyncTask.execute(editable.toString())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getEditText().requestFocus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home)
            onBackPressed()
        else
            return super.onOptionsItemSelected(item)
        return true
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList)
            if (task is DeviceIntroductionTask)
                task.anchor = this
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State) {
        val running = task is DeviceIntroductionTask && !task.finished
        setShowProgress(running)
        getButton().isEnabled = !running
    }

    override fun onTaskMessage(taskMessage: TaskMessage): Boolean {
        Log.d(TAG, taskMessage.message)
        runOnUiThread { getEditText().error = taskMessage.message }
        return true
    }

    override fun onDeviceReached(deviceRoute: DeviceRoute) {
        setResult(
            RESULT_OK, Intent()
                .putExtra(EXTRA_DEVICE, deviceRoute.device)
                .putExtra(EXTRA_DEVICE_ADDRESS, deviceRoute.address)
        )
        finish()
    }

    private fun getButton(): Button {
        return findViewById(R.id.confirm_button)
    }

    private fun getEditText(): AppCompatEditText {
        return findViewById(R.id.editText)
    }

    private fun setShowProgress(show: Boolean) {
        findViewById<View>(R.id.progressBar).visibility = if (show) View.VISIBLE else View.GONE
        TransitionManager.beginDelayedTransition(findViewById(R.id.layout_main))
    }

    class CheckAddressAsyncTask(private val listener: CheckHostnameListener) : UiTask<String, Void, InetAddress?>() {
        override fun doInBackground(vararg address: String): InetAddress? {
            try {
                if (address.isNotEmpty())
                    return InetAddress.getByName(address[0])
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(address: InetAddress?) {
            super.onPostExecute(address)
            if (address == null)
                listener.onHostnameError()
            else
                listener.onConnect(address)
        }
    }

    inner class CheckHostnameListener {
        fun onConnect(address: InetAddress) {
            runUiTask(DeviceIntroductionTask(address, 0), this@ManualConnectionActivity)
        }

        fun onHostnameError() {
            getEditText().error = getString(R.string.mesg_unknownHostError)
        }
    }

    companion object {
        val TAG = ManualConnectionActivity::class.java.simpleName
        const val EXTRA_DEVICE = "extraDevice"
        const val EXTRA_DEVICE_ADDRESS = "extraDeviceAddress"
    }
}