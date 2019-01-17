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

import com.genonbeta.TrebleShot.util.AppUtils;

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

        if (isMyServiceRunning(CommunicationService.class))
            stopService(new Intent(getApplicationContext(), CommunicationService.class));
        else
            AppUtils.startForegroundService(this, new Intent(getApplicationContext(), CommunicationService.class));

        updateTileState();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (serviceClass.getName().equals(service.service.getClassName()))
                return true;

        return false;
    }


    private void updateTileState()
    {
        updateTileState(isMyServiceRunning(CommunicationService.class) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
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
