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

package com.genonbeta.TrebleShot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.HotspotUtils;

public class NetworkStatusReceiver extends BroadcastReceiver
{
    public static final String WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        SharedPreferences preferences = getSharedPreferences(context);

        if (WIFI_AP_STATE_CHANGED.equals(intent.getAction())) {
            HotspotUtils hotspotUtils = HotspotUtils.getInstance(context);

            if (WifiManager.WIFI_STATE_DISABLED
                    == intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1) % 10)
                hotspotUtils.unloadPreviousConfig();
        }

        if (intent.hasExtra("networkInfo"))
            evaluateTheCondition((NetworkInfo) intent.getParcelableExtra("networkInfo"), context, preferences);
    }

    protected void evaluateTheCondition(NetworkInfo info, final Context context, SharedPreferences preferences)
    {
        if (preferences.getBoolean("allow_autoconnect", false) && info.isConnected())
            AppUtils.startForegroundService(context, new Intent(context, CommunicationService.class));

        if (preferences.getBoolean("scan_devices_auto", false) && info.isConnected())
            new Thread()
            {
                @Override
                public void run()
                {
                    super.run();
                    try {
                        // The interface may not be created properly yet and we should give some time
                        Thread.sleep(1700);
                        context.startService(new Intent(context, DeviceScannerService.class)
                                .setAction(DeviceScannerService.ACTION_SCAN_DEVICES));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
    }

    protected SharedPreferences getSharedPreferences(Context context)
    {
        return AppUtils.getDefaultPreferences(context);
    }
}
