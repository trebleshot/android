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
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.Device;

import java.util.HashMap;
import java.util.Map;

@RequiresApi(16)
public class P2pDaemon
{
    public static final String TAG = P2pDaemon.class.getSimpleName();

    private final Connections mConnections;
    private final WifiP2pManager.PeerListListener mPeerListener = new PeerListener();
    private final DiscoverPeersActionListener mDiscoverPeersActionListener = new DiscoverPeersActionListener();
    private final ServiceRequestActionListener mServiceRequestActionListener = new ServiceRequestActionListener();
    private final RequestServiceInfoActionListener mRequestServiceInfoActionLister =
            new RequestServiceInfoActionListener();
    private final AddLocalServiceActionListener mAddLocalServiceActionListener = new AddLocalServiceActionListener();
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
        mWifiP2pServiceInfo = getWifiP2pServiceInfo();

        getWifiP2pManager().setDnsSdResponseListeners(getChannel(), mDnsSdServiceResponseListener,
                mDnsSdTxtRecordListener);
        getWifiP2pManager().addLocalService(getChannel(), mWifiP2pServiceInfo, mAddLocalServiceActionListener);
        getWifiP2pManager().addServiceRequest(getChannel(), mServiceRequest, mServiceRequestActionListener);
        getWifiP2pManager().discoverPeers(getChannel(), mDiscoverPeersActionListener);

        context.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    public void stop(@NonNull Context context)
    {
        getWifiP2pManager().stopPeerDiscovery(getChannel(), mDiscoverPeersActionListener);
        getWifiP2pManager().removeServiceRequest(getChannel(), mServiceRequest, mServiceRequestActionListener);
        if (mWifiP2pServiceInfo != null) {
            getWifiP2pManager().removeLocalService(getChannel(), mWifiP2pServiceInfo, mAddLocalServiceActionListener);
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

    private final class DiscoverPeersActionListener implements WifiP2pManager.ActionListener
    {

        @Override
        public void onSuccess()
        {
            getWifiP2pManager().requestPeers(getChannel(), mPeerListener);
        }

        @Override
        public void onFailure(int reason)
        {
            Log.d(TAG, "RequestServiceInfoActionListener.onFailure: " + reason);
        }
    }

    private final class RequestServiceInfoActionListener implements WifiP2pManager.ActionListener
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "onSuccess: RequestServiceInfoActionListener");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "RequestServiceInfoActionListener.onFailure: " + reason);
        }
    }

    private final class AddLocalServiceActionListener implements WifiP2pManager.ActionListener
    {
        @Override
        public void onSuccess()
        {
            Log.d(TAG, "onSuccess: AddLocalServiceActionListener");
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "AddLocalServiceActionListener.onFailure: " + reason);
        }
    }

    private final class ServiceRequestActionListener implements WifiP2pManager.ActionListener
    {
        @Override
        public void onSuccess()
        {
            getWifiP2pManager().discoverServices(getChannel(), mServiceRequestActionListener);
        }

        @Override
        public void onFailure(int reason)
        {
            Log.e(TAG, "ServiceRequestActionListener.onFailure: " + reason);
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

    private final class DnsSdServiceResponseListener implements WifiP2pManager.DnsSdServiceResponseListener
    {
        @Override
        public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice)
        {
            Log.d(TAG, "onDnsSdServiceAvailable: insName=" + instanceName + "; type=" + registrationType
                    + "; srcDevice=" + srcDevice.toString());
        }
    }

    private final class InternalBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, "onReceive: " + intent.getAction());
        }
    }
}
