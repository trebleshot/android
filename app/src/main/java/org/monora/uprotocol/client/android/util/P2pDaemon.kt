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
package org.monora.uprotocol.client.android.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.spec.v1.Config
import org.monora.uprotocol.core.spec.v1.Keyword
import java.util.*

@RequiresApi(16)
class P2pDaemon(val persistenceProvider: PersistenceProvider, val connections: Connections) {
    private val peerListener: PeerListListener = PeerListener()

    private val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance(Config.SERVICE_UPROTOCOL_DNS_SD)

    private val dnsSdTxtRecordListener = DnsSdTxtRecordListener()

    private val dnsSdServiceResponseListener = DnsSdServiceResponseListener()

    private val channel = wifiP2pManager.initialize(connections.context, Looper.getMainLooper(), null)

    private val intentFilter = IntentFilter()

    private val broadcastReceiver = InternalBroadcastReceiver()

    private var p2pServiceInfo: WifiP2pDnsSdServiceInfo? = null

    val wifiP2pServiceInfo: WifiP2pDnsSdServiceInfo
        get() {
            val client = persistenceProvider.client
            val recordMap: MutableMap<String, String?> = HashMap()
            recordMap[Keyword.CLIENT_UID] = client.clientUid
            recordMap[Keyword.CLIENT_PROTOCOL_VERSION] = client.clientProtocolVersion.toString()
            recordMap[Keyword.CLIENT_PROTOCOL_VERSION_MIN] = client.clientProtocolVersionMin.toString()
            recordMap[Keyword.CLIENT_MANUFACTURER] = client.clientManufacturer
            recordMap[Keyword.CLIENT_PRODUCT] = client.clientProduct
            recordMap[Keyword.CLIENT_VERSION_CODE] = client.clientVersionCode.toString()
            recordMap[Keyword.CLIENT_VERSION_NAME] = client.clientVersionName
            return WifiP2pDnsSdServiceInfo.newInstance(client.clientNickname,
                Config.SERVICE_UPROTOCOL_DNS_SD,
                recordMap
            )
        }

    val wifiP2pManager: WifiP2pManager
        get() = connections.p2pManager

    private val clearLocalServicesActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mClearLocalServicesActionListener.onSuccess")
            p2pServiceInfo = wifiP2pServiceInfo
            //wifiP2pManager.addLocalService(channel, p2pServiceInfo, addLocalServiceActionListener)
        }

        override fun onFailure(reason: Int) {
            Log.d(TAG, "mClearLocalServicesActionListener.onFailure: $reason")
        }
    }

    private val discoverPeersActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mDiscoverPeersActionListener.onSuccess")
            //wifiP2pManager.requestPeers(channel, peerListener)
        }

        override fun onFailure(reason: Int) {
            Log.d(TAG, "mDiscoverPeersActionListener.onFailure: $reason")
        }
    }

    private val discoverServicesActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mDiscoverServicesActionListener.onSuccess")
        }

        override fun onFailure(reason: Int) {
            Log.d(TAG, "mDiscoverServicesActionListener.onFailure: $reason")
        }
    }

    private val requestServiceInfoActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mRequestServiceInfoActionListener.onSuccess")
        }

        override fun onFailure(reason: Int) {
            Log.e(TAG, "mRequestServiceInfoActionListener.onFailure: $reason")
        }
    }

    private val addLocalServiceActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mAddLocalServiceActionListener.onSuccess")
        }

        override fun onFailure(reason: Int) {
            Log.e(TAG, "mAddLocalServiceActionListener.onFailure: $reason")
        }
    }

    private val addServiceRequestActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mAddServiceRequestActionListener.onSuccess")
        }

        override fun onFailure(reason: Int) {
            Log.e(TAG, "mAddServiceRequestActionListener.onFailure: $reason")
        }
    }

    private val removeServiceRequestActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mRemoveServiceRequestActionListener.onSuccess")
        }

        override fun onFailure(reason: Int) {
            Log.e(TAG, "mRemoveServiceRequestActionListener.onFailure: $reason")
        }
    }

    private val stopPeerDiscoveryActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mStopPeerDiscoveryActionListener.onSuccess")
        }

        override fun onFailure(reason: Int) {
            Log.e(TAG, "mStopPeerDiscoveryActionListener.onFailure: $reason")
        }
    }

    private val removeLocalServiceActionListener = object : ActionListener {
        override fun onSuccess() {
            Log.d(TAG, "mRemoveLocalServiceActionListener.onSuccess")
        }

        override fun onFailure(reason: Int) {
            Log.e(TAG, "mRemoveLocalServiceActionListener.onFailure: $reason")
        }
    }


    fun start(context: Context) {
        wifiP2pManager.setDnsSdResponseListeners(channel, dnsSdServiceResponseListener, dnsSdTxtRecordListener)
        wifiP2pManager.clearLocalServices(channel, clearLocalServicesActionListener)
        wifiP2pManager.addServiceRequest(channel, serviceRequest, addServiceRequestActionListener)
        //getWifiP2pManager().discoverPeers(getChannel(), mDiscoverPeersActionListener);
        //wifiP2pManager.discoverServices(channel, discoverServicesActionListener)
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

    fun stop(context: Context) {
        //getWifiP2pManager().stopPeerDiscovery(getChannel(), mStopPeerDiscoveryActionListener);
        wifiP2pManager.removeServiceRequest(channel, serviceRequest, removeServiceRequestActionListener)
        if (p2pServiceInfo != null) {
            wifiP2pManager.removeLocalService(channel, p2pServiceInfo, removeLocalServiceActionListener)
            p2pServiceInfo = null
        }
        context.unregisterReceiver(broadcastReceiver)
    }

    private inner class PeerListener : PeerListListener {
        override fun onPeersAvailable(peers: WifiP2pDeviceList) {
            Log.d(TAG, "onPeersAvailable: $peers")
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
            srcDevice: WifiP2pDevice,
        ) {
            Log.d(TAG, "onDnsSdTxtRecordAvailable: $txtRecordMap")
        }
    }

    private inner class InternalBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            if (WIFI_P2P_PEERS_CHANGED_ACTION == intent.action) {
                if (Build.VERSION.SDK_INT >= 18) {
                    val deviceList: WifiP2pDeviceList? = intent.getParcelableExtra(EXTRA_P2P_DEVICE_LIST)
                    Log.d(TAG, "onReceive: $deviceList")
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    wifiP2pManager.requestPeers(channel, peerListener)
                }
            }
        }
    }

    companion object {
        private const val TAG = "P2pDaemon"
    }

    init {
        intentFilter.addAction(WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
}
