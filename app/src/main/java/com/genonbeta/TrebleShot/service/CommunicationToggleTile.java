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

package com.genonbeta.TrebleShot.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

/**
 * created by: Veli
 * date: 10.10.2017 07:58
 */

@RequiresApi(api = Build.VERSION_CODES.N)
public class CommunicationToggleTile extends TileService
{
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onTileAdded()
    {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved()
    {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening()
    {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onStopListening()
    {
        super.onStopListening();
    }

    @Override
    public void onClick()
    {
        super.onClick();

        Intent serviceIntent = new Intent(getApplicationContext(), BackgroundService.class);
        if (isMyServiceRunning(BackgroundService.class))
            stopService(serviceIntent);
        else
            ContextCompat.startForegroundService(this, serviceIntent);

        updateTileState();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // FIXME: 22.03.2020 Deprecated
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (serviceClass.getName().equals(service.service.getClassName()))
                return true;

        return false;
    }


    private void updateTileState()
    {
        updateTileState(isMyServiceRunning(BackgroundService.class) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
    }

    private void updateTileState(int state)
    {
        Tile tile = getQsTile();

        if (tile != null) {
            tile.setState(state);
            Icon icon = tile.getIcon();

            switch (state) {
                case Tile.STATE_ACTIVE:
                    icon.setTint(Color.WHITE);
                    break;
                case Tile.STATE_INACTIVE:
                case Tile.STATE_UNAVAILABLE:
                default:
                    icon.setTint(Color.GRAY);
                    break;
            }

            tile.updateTile();
        }
    }
}
