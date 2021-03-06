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

import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * created by: Veli
 * date: 10.10.2017 07:58
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class CommunicationToggleTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        update()
    }

    override fun onClick() {
        super.onClick()
        val serviceIntent = Intent(applicationContext, BackgroundService::class.java)
        if (isRunning(BackgroundService::class.java)) {
            stopService(serviceIntent)
        } else {
            ContextCompat.startForegroundService(this, serviceIntent)
        }
        update()
    }

    private fun isRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        // FIXME: 22.03.2020 Deprecated
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) return true
        }
        return false
    }

    private fun update(
        state: Int = if (isRunning(BackgroundService::class.java)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
    ) {
        val tile = qsTile ?: return
        tile.state = state
        val icon = tile.icon
        when (state) {
            Tile.STATE_ACTIVE -> icon.setTint(Color.WHITE)
            Tile.STATE_INACTIVE, Tile.STATE_UNAVAILABLE -> icon.setTint(Color.GRAY)
            else -> icon.setTint(Color.GRAY)
        }
        tile.updateTile()
    }
}