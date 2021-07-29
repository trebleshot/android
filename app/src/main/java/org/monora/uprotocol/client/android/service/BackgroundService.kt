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
package org.monora.uprotocol.client.android.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.util.*
import javax.inject.Inject

@AndroidEntryPoint
class BackgroundService : LifecycleService() {
    private val binder = LocalBinder()

    @Inject
    lateinit var backend: Backend

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(Notifications.ID_BG_SERVICE, backend.bgNotification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!Permissions.checkRunningConditions(this) || intent == null) {
            return START_NOT_STICKY
        } else if (intent.action == ACTION_STOP_BG_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        } else if (intent.action == ACTION_STOP_ALL) {
            backend.takeBgServiceFgIfNeeded(newlySwitchedGrounds = false, forceStop = true)
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(false)
    }

    inner class LocalBinder : Binder() {
        val service: BackgroundService
            get() = this@BackgroundService
    }

    companion object {
        const val ACTION_STOP_ALL = "org.monora.uprotocol.client.android.action.STOP_ALL"

        const val ACTION_STOP_BG_SERVICE = "org.monora.uprotocol.client.android.action.STOP_BG_SERVICE"
    }
}