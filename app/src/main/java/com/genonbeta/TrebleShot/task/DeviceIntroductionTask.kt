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
package com.genonbeta.TrebleShot.task

import android.content.*
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.util.Connections
import java.net.InetAddress

class DeviceIntroductionTask : AttachableAsyncTask<ResultListener?> {
    private val mPin: Int
    private var mDescription: NetworkDescription? = null
    private var mAddress: InetAddress? = null

    constructor(address: InetAddress?, pin: Int) {
        assert(address != null)
        mAddress = address
        mPin = pin
    }

    constructor(address: DeviceAddress, pin: Int) : this(address.inetAddress, pin) {}
    constructor(description: NetworkDescription?, pin: Int) {
        assert(description != null)
        mDescription = description
        mPin = pin
    }

    @Throws(TaskStoppedException::class)
    public override fun onRun() {
        try {
            val deviceRoute: DeviceRoute
            deviceRoute = if (mAddress == null) {
                val connections = Connections(context)
                connections.connectToNetwork(this, mDescription, mPin)
            } else Connections.Companion.setupConnection(context, mAddress, mPin)
            val anchor: ResultListener? = anchor
            if (anchor != null) post { anchor.onDeviceReached(deviceRoute) }
        } catch (e: Exception) {
            e.printStackTrace()
            post(CommonErrorHelper.messageOf(context, e))
        }
    }

    override fun getName(context: Context?): String? {
        return context!!.getString(R.string.text_addNewDevice)
    }

    interface ResultListener : AttachedTaskListener {
        fun onDeviceReached(deviceRoute: DeviceRoute?)
    }

    class SuggestNetworkException(description: NetworkDescription, type: Type) : Exception() {
        var description: NetworkDescription
        var type: Type

        enum class Type {
            ExceededLimit, ErrorInternal, Duplicate, AppDisallowed, NetworkDuplicate, DidNotConnect
        }

        init {
            this.description = description
            this.type = type
        }
    }

    companion object {
        val TAG = DeviceIntroductionTask::class.java.simpleName
    }
}