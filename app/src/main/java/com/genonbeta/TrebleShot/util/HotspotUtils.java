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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

abstract public class HotspotUtils
{
    private static final String TAG = "HotspotUtils";

    private static HotspotUtils mInstance = null;

    private WifiManager mWifiManager;

    private HotspotUtils(Context context)
    {
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static HotspotUtils getInstance(Context context)
    {
        if (mInstance == null)
            mInstance = Build.VERSION.SDK_INT >= 26 ? new OreoAPI(context) : new HackAPI(context);

        return mInstance;
    }

    private static Object invokeSilently(Method method, Object receiver, Object... args)
    {
        try {
            return method.invoke(receiver, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            Log.e(TAG, "exception in invoking methods: " + method.getName() + "(): " + e.getMessage());
        }

        return null;
    }

    public static boolean isSupported()
    {
        return Build.VERSION.SDK_INT >= 26 || HackAPI.supported();
    }

    public WifiManager getWifiManager()
    {
        return mWifiManager;
    }

    public abstract boolean disable();

    public abstract boolean enable();

    public abstract boolean enableConfigured(String apName, String passKeyWPA2);

    public abstract WifiConfiguration getConfiguration();

    public abstract WifiConfiguration getPreviousConfig();

    public abstract boolean isEnabled();

    public abstract boolean isStarted();

    public abstract boolean unloadPreviousConfig();

    @RequiresApi(26)
    public static class OreoAPI extends HotspotUtils
    {
        private WifiManager.LocalOnlyHotspotReservation mHotspotReservation;

        private OreoAPI(Context context)
        {
            super(context);
        }

        @Override
        public boolean disable()
        {
            if (mHotspotReservation == null)
                return false;

            mHotspotReservation.close();
            mHotspotReservation = null;

            return true;
        }

        @Override
        public boolean enable()
        {
            try {
                getWifiManager().startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback()
                {
                    @Override
                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation)
                    {
                        super.onStarted(reservation);
                        mHotspotReservation = reservation;
                    }

                    @Override
                    public void onStopped()
                    {
                        super.onStopped();
                        mHotspotReservation = null;
                    }

                    @Override
                    public void onFailed(int reason)
                    {
                        super.onFailed(reason);
                        mHotspotReservation = null;
                    }
                }, new Handler(Looper.getMainLooper()));

                return true;
            } catch (Throwable ignored) {
            }

            return false;
        }

        @Override
        public WifiConfiguration getConfiguration()
        {
            if (mHotspotReservation == null)
                return null;

            return mHotspotReservation.getWifiConfiguration();
        }

        @Override
        public WifiConfiguration getPreviousConfig()
        {
            return getConfiguration();
        }

        @Override
        public boolean enableConfigured(String apName, String passKeyWPA2)
        {
            return enable();
        }

        @Override
        public boolean isEnabled()
        {
            return HackAPI.enabled(getWifiManager());
        }

        @Override
        public boolean isStarted()
        {
            return mHotspotReservation != null;
        }

        @Override
        public boolean unloadPreviousConfig()
        {
            return mHotspotReservation != null;
        }
    }

    public static class HackAPI extends HotspotUtils
    {
        private static Method getWifiApConfiguration;
        private static Method getWifiApState;
        private static Method isWifiApEnabled;
        private static Method setWifiApEnabled;
        private static Method setWifiApConfiguration;

        static {
            for (Method method : WifiManager.class.getDeclaredMethods()) {
                switch (method.getName()) {
                    case "getWifiApConfiguration":
                        getWifiApConfiguration = method;
                        break;
                    case "getWifiApState":
                        getWifiApState = method;
                        break;
                    case "isWifiApEnabled":
                        isWifiApEnabled = method;
                        break;
                    case "setWifiApEnabled":
                        setWifiApEnabled = method;
                        break;
                    case "setWifiApConfiguration":
                        setWifiApConfiguration = method;
                        break;
                }
            }
        }

        private WifiConfiguration mPreviousConfig;

        private HackAPI(Context context)
        {
            super(context);
        }

        public static boolean enabled(WifiManager wifiManager)
        {
            Object result = invokeSilently(isWifiApEnabled, wifiManager);

            if (result == null)
                return false;

            return (Boolean) result;
        }

        public static boolean supported()
        {
            return getWifiApState != null
                    && isWifiApEnabled != null
                    && setWifiApEnabled != null
                    && getWifiApConfiguration != null;
        }

        public boolean disable()
        {
            unloadPreviousConfig();
            return setHotspotEnabled(mPreviousConfig, false);
        }

        public boolean enable()
        {
            getWifiManager().setWifiEnabled(false);
            return setHotspotEnabled(getConfiguration(), true);
        }

        public boolean enableConfigured(String apName, String passKeyWPA2)
        {
            getWifiManager().setWifiEnabled(false);

            if (mPreviousConfig == null)
                mPreviousConfig = getConfiguration();

            WifiConfiguration wifiConfiguration = new WifiConfiguration();

            wifiConfiguration.SSID = apName;

            if (passKeyWPA2 != null && passKeyWPA2.length() >= 8) {
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wifiConfiguration.preSharedKey = passKeyWPA2;
            } else
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            return setHotspotEnabled(wifiConfiguration, true);
        }

        @Override
        public boolean isEnabled()
        {
            return enabled(getWifiManager());
        }

        @Override
        public boolean isStarted()
        {
            return getPreviousConfig() != null;
        }

        public WifiConfiguration getConfiguration()
        {
            return (WifiConfiguration) invokeSilently(getWifiApConfiguration, getWifiManager());
        }

        public WifiConfiguration getPreviousConfig()
        {
            return mPreviousConfig;
        }

        private boolean setHotspotConfig(WifiConfiguration config)
        {
            Object result = invokeSilently(setWifiApConfiguration, getWifiManager(), config);

            if (result == null)
                return false;

            return (Boolean) result;
        }

        private boolean setHotspotEnabled(WifiConfiguration config, boolean enabled)
        {
            Object result = invokeSilently(setWifiApEnabled, getWifiManager(), config, enabled);

            if (result == null)
                return false;

            return (Boolean) result;
        }

        public boolean unloadPreviousConfig()
        {
            if (mPreviousConfig == null)
                return false;

            boolean result = setHotspotConfig(mPreviousConfig);
            mPreviousConfig = null;

            return result;
        }
    }
}
