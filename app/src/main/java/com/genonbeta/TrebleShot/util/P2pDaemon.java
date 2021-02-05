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

package com.genonbeta.TrebleShot.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.dataobject.Device;

import java.util.HashMap;
import java.util.Map;

@RequiresApi(16)
public class P2pDaemon
{
    public static final String TAG = P2pDaemon.class.getSimpleName();

    private final Connections mConnections;
    private final WifiP2pManager.PeerListListener mPeerListener = new PeerListener();
    private final WifiP2pDnsSdServiceRequest mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(
            AppConfig.NSD_SERVICE_TYPE);
    private final DnsSdTxtRecordListener mDnsSdTxtRecordListener = new DnsSdTxtRecordListener();
    private final DnsSdServiceResponseListener mDnsSdServiceResponseListener = new DnsSdServiceResponseListener();
    private final WifiP2pManager.Channel mChannel;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private final BroadcastReceiver mBroadcastReceiver = new InternalBroadcastReceiver();
    private WifiP2pDnsSdServiceInfo mWifiP2pServiceInfo;

    public P2pDaemon(Connections connections)
    {
        mConnections = connections;
        mChannel = getWifiP2pManager().initialize(connections.getContext(), Looper.getMainLooper(), null);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public WifiP2pManager.Channel getChannel()
    {
        return mChannel;
    }

    public Connections getConnections()
    {
        return mConnections;
    }

    public WifiP2pDnsSdServiceInfo getWifiP2pServiceInfo()
    {
        Device us = AppUtils.getLocalDevice(getConnections().getContext());
        Map<String, String> recordMap = new HashMap<>();
        recordMap.put(Keyword.DEVICE_UID, us.uid);
        recordMap.put(Keyword.DEVICE_PROTOCOL_VERSION, String.valueOf(us.protocolVersion));
        recordMap.put(Keyword.DEVICE_PROTOCOL_VERSION_MIN, String.valueOf(us.protocolVersionMin));
        recordMap.put(Keyword.DEVICE_BRAND, us.brand);
        recordMap.put(Keyword.DEVICE_MODEL, us.model);
        recordMap.put(Keyword.DEVICE_VERSION_CODE, String.valueOf(us.versionCode));
        recordMap.put(Keyword.DEVICE_VERSION_NAME, us.versionName);

        return WifiP2pDnsSdServiceInfo.newInstance(us.username, AppConfig.NSD_SERVICE_TYPE, recordMap);
    }

    public WifiP2pManager getWifiP2pManager()
    {
        return getConnections().getP2pManager();
    }

    public void start(@NonNull Context context)
    {
        getWifiP2pManager().setDnsSdResponseListeners(getChannel(), mDnsSdServiceResponseListener,
                mDnsSdTxtRecordListener);
        getWifiP2pManager().clearLocalServices(getChannel(), mClearLocalServicesActionListener);
        getWifiP2pManager().addServiceRequest(getChannel(), mServiceRequest, mAddServiceRequestActionListener);
        //getWifiP2pManager().discoverPeers(getChannel(), mDiscoverPeersActionListener);
        getWifiP2pManager().discoverServices(getChannel(), mDiscoverServicesActionListener);

        context.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    public void stop(@NonNull Context context)
    {
        //getWifiP2pManager().stopPeerDiscovery(getChannel(), mStopPeerDiscoveryActionListener);
        getWifiP2pManager().removeServiceRequest(getChannel(), mServiceRequest, mRemoveServiceRequestActionListener);
        if (mWifiP2pServiceInfo != null) {
            getWifiP2pManager().removeLocalService(getChannel(), mWifiP2pServiceInfo, mRemoveLocalServiceActionListener);
            mWifiP2pServiceInfo = null;
        }

        context.unregisterReceiver(mBroadcastReceiver);
    }

    private final class PeerListener implements WifiP2pManager.PeerListListener
    {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers)
        {
            Log.d(TAG, "onPeersAvailable: " + peers.toString());
        }
    }

    private final class DnsSdServiceResponseListener implements WifiP2pManager.DnsSdServiceResponseListener
    {
        @Override
        public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice)
        {
            Log.d(TAG, "onDnsSdServiceAvailable: insName=" + instanceName + "; type=" + registrationType
                    + "; srcDevice=" + srcDevice.toString());
        }
    }

    private final class DnsSdTxtRecordListener implements WifiP2pManager.DnsSdTxtRecordListener
    {
        @Override
        public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap,
                                              WifiP2pDevice srcDevice)
        {
            Log.d(TAG, "onDnsSdTxtRecordAvailable: " + txtRecordMap.toString());
        }
    }

    private final class InternalBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, "onReceive: " + intent.getAction());
            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(intent.getAction())) {
                if (Build.VERSION.SDK_INT >= 18) {
                    WifiP2pDeviceList deviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                    if (deviceList != null)
                        Log.d(TAG, "onReceive: " + deviceList.toString());
                    else
                        Log.d(TAG, "onReceive: The intent doesn't contain the P2P device list.");
                } else
                    getWifiP2pManager().requestPeers(getChannel(), mPeerListener);
            }
        }
    }

    private final WifiP2pManager.ActionListener mClearLocalServicesActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mClearLocalServicesActionListener.onSuccess");
            mWifiP2pServiceInfo = getWifiP2pServiceInfo();
            getWifiP2pManager().addLocalService(getChannel(), mWifiP2pServiceInfo, mAddLocalServiceActionListener);
        }

        @Override
        public void onFailure(int reason)
        {
            Log.d(TAG, "mClearLocalServicesActionListener.onFailure: " + reason);
        }
    };

    private final WifiP2pManager.ActionListener mDiscoverPeersActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mDiscoverPeersActionListener.onSuccess");
            getWifiP2pManager().requestPeers(getChannel(), mPeerListener);
        }

        @Override
        public void onFailure(int reason)
        {
            Log.d(TAG, "mDiscoverPeersActionListener.onFailure: " + reason);
        }
    };

    private final WifiP2pManager.ActionListener mDiscoverServicesActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mDiscoverServicesActionListener.onSuccess");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.d(TAG, "mDiscoverServicesActionListener.onFailure: " + reason);
        }
    };

    private final WifiP2pManager.ActionListener mRequestServiceInfoActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mRequestServiceInfoActionListener.onSuccess");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "mRequestServiceInfoActionListener.onFailure: " + reason);
        }
    };

    private final WifiP2pManager.ActionListener mAddLocalServiceActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mAddLocalServiceActionListener.onSuccess");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "mAddLocalServiceActionListener.onFailure: " + reason);
        }
    };

    private final WifiP2pManager.ActionListener mAddServiceRequestActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mAddServiceRequestActionListener.onSuccess");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "mAddServiceRequestActionListener.onFailure: " + reason);
        }
    };

    private final WifiP2pManager.ActionListener mRemoveServiceRequestActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mRemoveServiceRequestActionListener.onSuccess");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "mRemoveServiceRequestActionListener.onFailure: " + reason);
        }
    };

    private final WifiP2pManager.ActionListener mStopPeerDiscoveryActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mStopPeerDiscoveryActionListener.onSuccess");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "mStopPeerDiscoveryActionListener.onFailure: " + reason);
        }
    };

    private final WifiP2pManager.ActionListener mRemoveLocalServiceActionListener = new WifiP2pManager.ActionListener()
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "mRemoveLocalServiceActionListener.onSuccess");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "mRemoveLocalServiceActionListener.onFailure: " + reason);
        }
    };
}
