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
package com.genonbeta.TrebleShot.util

import android.content.*
import android.os.*
import android.util.Log
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.config.Keyword
import java.util.*

@RequiresApi(16)
class P2pDaemon(val connections: Connections) {
    private val mPeerListener: PeerListListener = PeerListener()
    private val mServiceRequest: WifiP2pDnsSdServiceRequest = WifiP2pDnsSdServiceRequest.newInstance(
        AppConfig.NSD_SERVICE_TYPE
    )
    private val mDnsSdTxtRecordListener: DnsSdTxtRecordListener = DnsSdTxtRecordListener()
    private val mDnsSdServiceResponseListener: DnsSdServiceResponseListener = DnsSdServiceResponseListener()
    private val mChannel: WifiP2pManager.Channel
    private val mIntentFilter = IntentFilter()
    private val mBroadcastReceiver: BroadcastReceiver = InternalBroadcastReceiver()
    private var mWifiP2pServiceInfo: WifiP2pDnsSdServiceInfo? = null
    val channel: WifiP2pManager.Channel
        get() = mChannel
    val wifiP2pServiceInfo: WifiP2pDnsSdServiceInfo
        get() {
            val us = AppUtils.getLocalDevice(connections.context)
            val recordMap: MutableMap<String, String?> = HashMap()
            recordMap[Keyword.DEVICE_UID] = us!!.uid
            recordMap[Keyword.DEVICE_PROTOCOL_VERSION] = java.lang.String.valueOf(us.protocolVersion)
            recordMap[Keyword.DEVICE_PROTOCOL_VERSION_MIN] =
                java.lang.String.valueOf(us.protocolVersionMin)
            recordMap[Keyword.DEVICE_BRAND] = us.brand
            recordMap[Keyword.DEVICE_MODEL] = us.model
            recordMap[Keyword.DEVICE_VERSION_CODE] = java.lang.String.valueOf(us.versionCode)
            recordMap[Keyword.DEVICE_VERSION_NAME] = us.versionName
            return WifiP2pDnsSdServiceInfo.newInstance(us.username, AppConfig.NSD_SERVICE_TYPE, recordMap)
        }
    val wifiP2pManager: WifiP2pManager?
        get() = connections.p2pManager

    fun start(context: Context) {
        wifiP2pManager.setDnsSdResponseListeners(
            channel, mDnsSdServiceResponseListener,
            mDnsSdTxtRecordListener
        )
        wifiP2pManager.clearLocalServices(channel, mClearLocalServicesActionListener)
        wifiP2pManager.addServiceRequest(channel, mServiceRequest, mAddServiceRequestActionListener)
        //getWifiP2pManager().discoverPeers(getChannel(), mDiscoverPeersActionListener);
        wifiP2pManager.discoverServices(channel, mDiscoverServicesActionListener)
        context.registerReceiver(mBroadcastReceiver, mIntentFilter)
    }

    fun stop(context: Context) {
        //getWifiP2pManager().stopPeerDiscovery(getChannel(), mStopPeerDiscoveryActionListener);
        wifiP2pManager.removeServiceRequest(channel, mServiceRequest, mRemoveServiceRequestActionListener)
        if (mWifiP2pServiceInfo != null) {
            wifiP2pManager.removeLocalService(channel, mWifiP2pServiceInfo, mRemoveLocalServiceActionListener)
            mWifiP2pServiceInfo = null
        }
        context.unregisterReceiver(mBroadcastReceiver)
    }

    private inner class PeerListener : PeerListListener {
        override fun onPeersAvailable(peers: WifiP2pDeviceList) {
            Log.d(TAG, "onPeersAvailable: " + peers.toString())
        }
    }

    private inner class DnsSdServiceResponseListener : WifiP2pManager.DnsSdServiceResponseListener {
        override fun onDnsSdServiceAvailable(instanceName: String, registrationType: String, srcDevice: WifiP2pDevice) {
            Log.d(
                TAG, "onDnsSdServiceAvailable: insName=" + instanceName + "; type=" + registrationType
                        + "; srcDevice=" + srcDevice.toString()
            )
        }
    }

    private inner class DnsSdTxtRecordListener : WifiP2pManager.DnsSdTxtRecordListener {
        override fun onDnsSdTxtRecordAvailable(
            fullDomainName: String, txtRecordMap: Map<String, String>,
            srcDevice: WifiP2pDevice
        ) {
            Log.d(TAG, "onDnsSdTxtRecordAvailable: $txtRecordMap")
        }
    }

    private inner class InternalBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == intent.action) {
                if (Build.VERSION.SDK_INT >= 18) {
                    val deviceList: WifiP2pDeviceList =
                        intent.getParcelableExtra<WifiP2pDeviceList>(WifiP2pManager.EXTRA_P2P_DEVICE_LIST)
                    if (deviceList != null) Log.d(TAG, "onReceive: " + deviceList.toString()) else Log.d(
                        TAG,
                        "onReceive: The intent doesn't contain the P2P device list."
                    )
                } else wifiP2pManager.requestPeers(channel, mPeerListener)
            }
        }
    }

    private val mClearLocalServicesActionListener: WifiP2pManager.ActionListener =
        object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "mClearLocalServicesActionListener.onSuccess")
                mWifiP2pServiceInfo = wifiP2pServiceInfo
                wifiP2pManager.addLocalService(channel, mWifiP2pServiceInfo, mAddLocalServiceActionListener)
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "mClearLocalServicesActionListener.onFailure: $reason")
            }
        }
    private val mDiscoverPeersActionListener: WifiP2pManager.ActionListener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mDiscoverPeersActionListener.onSuccess")
            wifiP2pManager.requestPeers(channel, mPeerListener)
        }

        override fun onFailure(reason: Int) {
            Log.d(TAG, "mDiscoverPeersActionListener.onFailure: $reason")
        }
    }
    private val mDiscoverServicesActionListener: WifiP2pManager.ActionListener =
        object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "mDiscoverServicesActionListener.onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.d(TAG, "mDiscoverServicesActionListener.onFailure: $reason")
            }
        }
    private val mRequestServiceInfoActionListener: WifiP2pManager.ActionListener =
        object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "mRequestServiceInfoActionListener.onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "mRequestServiceInfoActionListener.onFailure: $reason")
            }
        }
    private val mAddLocalServiceActionListener: WifiP2pManager.ActionListener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mAddLocalServiceActionListener.onSuccess")
        }

        override fun onFailure(reason: Int) {
            Log.e(TAG, "mAddLocalServiceActionListener.onFailure: $reason")
        }
    }
    private val mAddServiceRequestActionListener: WifiP2pManager.ActionListener =
        object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "mAddServiceRequestActionListener.onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "mAddServiceRequestActionListener.onFailure: $reason")
            }
        }
    private val mRemoveServiceRequestActionListener: WifiP2pManager.ActionListener =
        object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "mRemoveServiceRequestActionListener.onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "mRemoveServiceRequestActionListener.onFailure: $reason")
            }
        }
    private val mStopPeerDiscoveryActionListener: WifiP2pManager.ActionListener =
        object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "mStopPeerDiscoveryActionListener.onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "mStopPeerDiscoveryActionListener.onFailure: $reason")
            }
        }
    private val mRemoveLocalServiceActionListener: WifiP2pManager.ActionListener =
        object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "mRemoveLocalServiceActionListener.onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.e(TAG, "mRemoveLocalServiceActionListener.onFailure: $reason")
            }
        }

    companion object {
        val TAG = P2pDaemon::class.java.simpleName
    }

    init {
        mChannel = wifiP2pManager.initialize(connections.context, Looper.getMainLooper(), null)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
}