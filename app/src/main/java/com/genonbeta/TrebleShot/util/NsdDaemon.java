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

package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceRoute;

import java.net.InetAddress;
import java.util.Map;

/**
 * created by: Veli
 * date: 22.01.2018 15:35
 */

public class NsdDaemon
{
    public static final String TAG = NsdDaemon.class.getSimpleName();

    public static final String ACTION_DEVICE_STATUS = "org.monora.trebleshot.android.intent.action.DEVICE_STATUS";

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mNsdDiscoveryListener;
    private NsdManager.RegistrationListener mNsdRegistrationListener;
    private final Context mContext;
    private final Kuick mKuick;
    private final SharedPreferences mPreferences;
    private final Map<String, DeviceRoute> mOnlineDeviceList = new ArrayMap<>();

    public NsdDaemon(Context context, Kuick kuick, SharedPreferences preferences)
    {
        mContext = context;
        mKuick = kuick;
        mPreferences = preferences;
    }

    public boolean isDeviceOnline(Device device)
    {
        synchronized (mOnlineDeviceList) {
            for (DeviceRoute deviceRoute : mOnlineDeviceList.values())
                if (deviceRoute.device.equals(device))
                    return true;
        }

        return false;
    }

    public Context getContext()
    {
        return mContext;
    }

    public Kuick getDatabase()
    {
        return mKuick;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private NsdManager.DiscoveryListener getDiscoveryListener()
    {
        if (mNsdDiscoveryListener == null)
            mNsdDiscoveryListener = new DiscoveryListener();

        return mNsdDiscoveryListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private NsdManager getNsdManager()
    {
        if (mNsdManager == null)
            mNsdManager = (NsdManager) getContext().getSystemService(Context.NSD_SERVICE);

        return mNsdManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private NsdManager.RegistrationListener getRegistrationListener()
    {
        if (mNsdRegistrationListener == null)
            mNsdRegistrationListener = new RegistrationListener();

        return mNsdRegistrationListener;
    }

    public boolean isServiceEnabled()
    {
        return mPreferences.getBoolean("nsd_enabled", false);
    }

    public void registerService()
    {
        if (isServiceEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final NsdServiceInfo localServiceInfo = new NsdServiceInfo();

            localServiceInfo.setServiceName(AppUtils.getLocalDeviceName(getContext()));
            localServiceInfo.setServiceType(AppConfig.NSD_SERVICE_TYPE);
            localServiceInfo.setPort(AppConfig.SERVER_PORT_COMMUNICATION);

            try {
                getNsdManager().registerService(localServiceInfo, NsdManager.PROTOCOL_DNS_SD, getRegistrationListener());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startDiscovering()
    {
        try {
            if (isServiceEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                getNsdManager().discoverServices(AppConfig.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,
                        getDiscoveryListener());
        } catch (Exception ignored) {
        }
    }

    public void stopDiscovering()
    {
        try {
            if (isServiceEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                getNsdManager().stopServiceDiscovery(getDiscoveryListener());
        } catch (Exception ignored) {
        }
    }

    public void unregisterService()
    {
        try {
            if (isServiceEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                getNsdManager().unregisterService(getRegistrationListener());
        } catch (Exception ignored) {
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private static class RegistrationListener implements NsdManager.RegistrationListener
    {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
        {
            Log.e(TAG, "Failed to register self service with error code " + errorCode);
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
        {
            Log.e(TAG, "Failed to unregister self service with error code " + errorCode);
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo)
        {
            Log.v(TAG, "Self service is now registered: " + serviceInfo.getServiceName());
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo)
        {
            Log.i(TAG, "Self service is unregistered: " + serviceInfo.getServiceName());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private class DiscoveryListener implements NsdManager.DiscoveryListener
    {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode)
        {
            Log.e(TAG, "NSD discovery failed to start with error code " + errorCode);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode)
        {
            Log.e(TAG, "NSD discovery failed to stop with error code " + errorCode);
        }

        @Override
        public void onDiscoveryStarted(String serviceType)
        {
            Log.v(TAG, "NSD discovery started");
        }

        @Override
        public void onDiscoveryStopped(String serviceType)
        {
            Log.v(TAG, "NSD discovery stopped");
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo)
        {
            Log.v(TAG, "'" + serviceInfo.getServiceName() + "' service has been discovered.");

            if (serviceInfo.getServiceType().equals(AppConfig.NSD_SERVICE_TYPE))
                getNsdManager().resolveService(serviceInfo, new ResolveListener());
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo)
        {
            Log.i(TAG, "'" + serviceInfo.getServiceName() + "' service is now lost.");
            synchronized (mOnlineDeviceList) {
                if (mOnlineDeviceList.remove(serviceInfo.getServiceName()) != null)
                    getContext().sendBroadcast(new Intent(ACTION_DEVICE_STATUS));
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private class ResolveListener implements NsdManager.ResolveListener
    {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode)
        {
            Log.e(TAG, "Could not resolve " + serviceInfo.getServiceName());
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo)
        {
            Log.v(TAG, "Resolved " + serviceInfo.getServiceName() + " with success has the IP address of "
                    + serviceInfo.getHost().getHostAddress());

            DeviceLoader.load(getDatabase(), serviceInfo.getHost(), (device, address) -> {
                synchronized (mOnlineDeviceList) {
                    mOnlineDeviceList.put(serviceInfo.getServiceName(), new DeviceRoute(device, address));
                }

                getContext().sendBroadcast(new Intent(ACTION_DEVICE_STATUS));
            });
        }
    }
}
