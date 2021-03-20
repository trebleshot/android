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
package org.monora.uprotocol.client.android.task

import android.content.Context
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.model.NetworkDescription
import org.monora.uprotocol.client.android.service.backgroundservice.AttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import org.monora.uprotocol.client.android.util.CommonErrorHelper
import java.net.InetAddress

class DeviceIntroductionTask(
    private val description: NetworkDescription? = null,
    private val address: InetAddress? = null,
    private val pin: Int = 0,
) : AttachableAsyncTask<DeviceIntroductionTask.ResultListener>() {
    @Throws(TaskStoppedException::class)
    public override fun onRun() {
        try {

        } catch (e: Exception) {
            e.printStackTrace()
            post(CommonErrorHelper.messageOf(context, e))
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_addNewClient)
    }

    interface ResultListener : AttachedTaskListener {
        fun onDeviceReached(clientRoute: ClientRoute)
    }

    class SuggestNetworkException(val description: NetworkDescription, val type: Type) : Exception() {
        enum class Type {
            ExceededLimit, ErrorInternal, Duplicate, AppDisallowed
        }
    }

    companion object {
        private val TAG = DeviceIntroductionTask::class.simpleName
    }
}