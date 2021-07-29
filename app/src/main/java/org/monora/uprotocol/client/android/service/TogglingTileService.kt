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

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.backend.Backend
import javax.inject.Inject

/**
 * created by: Veli
 * date: 10.10.2017 07:58
 */
@RequiresApi(api = Build.VERSION_CODES.N)
@AndroidEntryPoint
class TogglingTileService : TileService() {
    @Inject
    lateinit var backend: Backend

    private val observer = Observer<Boolean> { activated ->
        qsTile?.let { tile ->
            tile.state = if (activated) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        backend.tileState.observeForever(observer)
    }

    override fun onStopListening() {
        super.onStopListening()
        backend.tileState.removeObserver(observer)
    }

    override fun onClick() {
        super.onClick()
        backend.takeBgServiceFgThroughTogglingTile()
    }
}