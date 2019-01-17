package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;

/**
 * created by: Veli
 * date: 22.01.2018 15:35
 */

public class NsdDiscovery
{
    public static final String TAG = NsdDiscovery.class.getSimpleName();

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mNsdDiscoveryListener;
    private NsdManager.RegistrationListener mNsdRegistrationListener;
    private Context mContext;
    private AccessDatabase mDatabase;
    private SharedPreferences mPreferences;

    public NsdDiscovery(Context context, AccessDatabase database, SharedPreferences preferences)
    {
        mContext = context;
        mDatabase = database;
        mPreferences = preferences;
    }

    public Context getContext()
    {
        return mContext;
    }

    public AccessDatabase getDatabase()
    {
        return mDatabase;
    }

    public NsdManager.DiscoveryListener getDiscoveryListener()
    {
        if (mNsdDiscoveryListener == null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNsdDiscoveryListener = new NsdManager.DiscoveryListener()
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

                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onServiceFound(NsdServiceInfo serviceInfo)
                {
                    Log.v(TAG, "NSD service is now found which is of " + serviceInfo.getServiceName());

                    if (!serviceInfo.getServiceType().equals(AppConfig.NDS_COMM_SERVICE_TYPE)) {
                        Log.d(TAG, "Unknown Service Type: " + serviceInfo.getServiceType());
                    } else if (serviceInfo.getServiceName().startsWith(AppConfig.NDS_COMM_SERVICE_NAME)) {
                        getNsdManager().resolveService(serviceInfo, new NsdManager.ResolveListener()
                        {
                            @Override
                            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode)
                            {
                                Log.e(TAG, "Resolve failed for " + serviceInfo.getServiceName());
                            }

                            @Override
                            public void onServiceResolved(NsdServiceInfo serviceInfo)
                            {
                                Log.v(TAG, "Resolved with success " + serviceInfo.getServiceName()
                                        + " with IP address of " + serviceInfo.getHost().getHostAddress());

                                NetworkDeviceLoader.load(getDatabase(), serviceInfo.getHost().getHostAddress(), null);
                            }
                        });
                    }
                }

                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onServiceLost(NsdServiceInfo serviceInfo)
                {
                    Log.i(TAG, "NSD service is now lost which is of " + serviceInfo.getServiceName());
                }
            };
        }

        return mNsdDiscoveryListener;
    }

    public NsdManager getNsdManager()
    {
        if (mNsdManager == null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mNsdManager = (NsdManager) getContext().getSystemService(Context.NSD_SERVICE);

        return mNsdManager;
    }

    public NsdManager.RegistrationListener getRegistrationListener()
    {
        if (isServiceEnabled()
                && mNsdRegistrationListener == null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mNsdRegistrationListener = new NsdManager.RegistrationListener()
            {
                @Override
                public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
                {
                    Log.e(TAG, "NDS registration failed with error code " + errorCode);
                }

                @Override
                public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
                {
                    Log.e(TAG, "NDS failed to unregister with error code " + errorCode);
                }

                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onServiceRegistered(NsdServiceInfo serviceInfo)
                {
                    Log.v(TAG, "NDS registered with success " + serviceInfo.getServiceName());
                }

                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onServiceUnregistered(NsdServiceInfo serviceInfo)
                {
                    Log.i(TAG, "NDS unregistered with success " + serviceInfo.getServiceName());
                }
            };

        return mNsdRegistrationListener;
    }

    public boolean isServiceEnabled()
    {
        return mPreferences.getBoolean("nsd_enabled", false);
    }

    public void registerService()
    {
        if (isServiceEnabled()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final NsdServiceInfo localServiceInfo = new NsdServiceInfo();

            localServiceInfo.setServiceName(AppConfig.NDS_COMM_SERVICE_NAME + "_" + AppUtils.getUniqueNumber());
            localServiceInfo.setServiceType(AppConfig.NDS_COMM_SERVICE_TYPE);
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
        if (isServiceEnabled()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            try {
                getNsdManager().discoverServices(AppConfig.NDS_COMM_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, getDiscoveryListener());
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public void stopDiscovering()
    {
        if (isServiceEnabled()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            try {
                getNsdManager().stopServiceDiscovery(getDiscoveryListener());
            } catch (Exception e) {
                // Listener may not have been initialized
            }
    }

    public void unregisterService()
    {
        if (isServiceEnabled()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            try {
                getNsdManager().unregisterService(getRegistrationListener());
            } catch (Exception e) {
                // Listener may not have been initialized
            }
    }
}
