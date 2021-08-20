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
package org.monora.uprotocol.client.android.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONException
import org.monora.android.codescanner.BarcodeEncoder
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.NavPickClientDirections
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.backend.Services
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.receiver.BgBroadcastReceiver
import org.monora.uprotocol.client.android.util.Connections
import org.monora.uprotocol.client.android.util.HotspotManager
import org.monora.uprotocol.client.android.util.InetAddresses
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToColor
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Direction
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * created by: veli
 * date: 11/04/18 20:53
 */
@AndroidEntryPoint
class NetworkManagerFragment : Fragment(R.layout.layout_network_manager) {
    @Inject
    lateinit var backend: Backend

    @Inject
    lateinit var persistenceProvider: PersistenceProvider

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    private val intentFilter = IntentFilter()

    private val statusReceiver: StatusReceiver = StatusReceiver()

    private lateinit var connections: Connections

    private lateinit var containerText1: View

    private lateinit var containerText2: View

    private lateinit var containerText3: View

    private lateinit var codeText: TextView

    private lateinit var text1: TextView

    private lateinit var text2: TextView

    private lateinit var text3: TextView

    private lateinit var imageView2: ImageView

    private lateinit var imageView3: ImageView

    private lateinit var codeView: ImageView

    private lateinit var toggleButton: Button

    private lateinit var secondButton: Button

    private var helpMenuItem: MenuItem? = null

    private lateinit var colorPassiveState: ColorStateList

    private lateinit var manager: HotspotManager

    private var toggleButtonDefaultStateList: ColorStateList? = null

    private var toggleButtonEnabledStateList: ColorStateList? = null

    private var activeType: Type? = null

    private val requestHotspotPermission = registerForActivityResult(RequestMultiplePermissions()) {
        toggleHotspot()
    }

    private val requestLocationPermission = registerForActivityResult(RequestMultiplePermissions()) {
        updateState()
    }

    private val snackbarPlacementProvider = SnackbarPlacementProvider { resId, objects ->
        view?.let {
            Snackbar.make(it, getString(resId, objects), Snackbar.LENGTH_LONG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connections = Connections(requireContext())
        manager = HotspotManager.newInstance(requireContext())
        intentFilter.addAction(Services.ACTION_OREO_HOTSPOT_STARTED)
        intentFilter.addAction(BgBroadcastReceiver.ACTION_PIN_USED)
        intentFilter.addAction(WIFI_AP_STATE_CHANGED)
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilter.addAction(BgBroadcastReceiver.ACTION_PIN_USED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toggleButtonEnabledStateList = ColorStateList.valueOf(
            R.attr.colorError.attrToRes(requireContext()).resToColor(requireContext())
        )
        colorPassiveState = ColorStateList.valueOf(
            R.attr.colorPassive.attrToRes(requireContext()).resToColor(requireContext())
        )
        codeView = view.findViewById(R.id.layout_network_manager_qr_image)
        codeText = view.findViewById(R.id.layout_network_manager_qr_help_text)
        toggleButton = view.findViewById(R.id.layout_network_manager_info_toggle_button)
        secondButton = view.findViewById(R.id.layout_network_manager_info_second_toggle_button)
        text1 = view.findViewById(R.id.layout_network_manager_info_container_text1)
        text2 = view.findViewById(R.id.layout_network_manager_info_container_text2)
        text3 = view.findViewById(R.id.layout_network_manager_info_container_text3)
        containerText1 = text1
        containerText2 = view.findViewById(R.id.layout_network_manager_info_container_text2_container)
        containerText3 = view.findViewById(R.id.layout_network_manager_info_container_text3_container)
        imageView2 = view.findViewById(R.id.layout_network_manager_info_container_text2_icon)
        imageView3 = view.findViewById(R.id.layout_network_manager_info_container_text3_icon)
        toggleButtonDefaultStateList = ViewCompat.getBackgroundTintList(toggleButton)
        toggleButton.setOnClickListener { v: View -> toggle(v) }
        secondButton.setOnClickListener { v: View -> toggle(v) }

        clientPickerViewModel.registerForAcquaintanceRequests(viewLifecycleOwner, Direction.Outgoing) { bridge ->
            clientPickerViewModel.bridge.postValue(bridge)
            findNavController().navigate(NavPickClientDirections.xmlPop())
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(statusReceiver, intentFilter)
        updateState()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(statusReceiver)
    }

    private fun getWifiConfiguration(): WifiConfiguration? {
        if (Build.VERSION.SDK_INT < 26) {
            return manager.configuration
        }
        try {
            return backend.getHotspotConfig()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        return null
    }

    private fun openWifiSettings() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun showMenu() {
        helpMenuItem?.isVisible = manager.configuration != null && manager.enabled
    }

    private fun toggleHotspot() {
        connections.toggleHotspot(
            backend, snackbarPlacementProvider, manager, true, requestHotspotPermission
        )
    }

    fun toggle(v: View) {
        if (v.id == R.id.layout_network_manager_info_toggle_button) {
            when (activeType) {
                Type.LocationAccess -> connections.validateLocationAccessNoPrompt(requestLocationPermission)
                Type.WiFi, Type.HotspotExternal -> openWifiSettings()
                Type.Hotspot, Type.None -> toggleHotspot()
                else -> toggleHotspot()
            }
        } else if (v.id == R.id.layout_network_manager_info_second_toggle_button) {
            when (activeType) {
                Type.LocationAccess, Type.WiFi -> toggleHotspot()
                Type.HotspotExternal, Type.Hotspot, Type.None -> openWifiSettings()
                else -> openWifiSettings()
            }
        }
    }

    private fun updateState() {
        showMenu()
        try {
            updateViews()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @Throws(JSONException::class)
    private fun updateViews() {
        showMenu()
        val pin = persistenceProvider.networkPin
        val delimiter = ";"
        val code = StringBuilder()
        val config: WifiConfiguration? = getWifiConfiguration()
        val connectionInfo: WifiInfo = connections.wifiManager.connectionInfo
        if (manager.enabled) {
            if (config != null) {
                activeType = Type.Hotspot
                val ssid: String = config.SSID
                val bssid: String? = config.BSSID
                val key: String? = config.preSharedKey
                code.append(Keyword.QR_CODE_TYPE_HOTSPOT)
                    .append(delimiter)
                    .append(pin)
                    .append(delimiter)
                    .append(ssid)
                    .append(delimiter)
                    .append(bssid ?: "")
                    .append(delimiter)
                    .append(key ?: "")
                imageView2.setImageResource(R.drawable.ic_wifi_tethering_white_24dp)
                imageView3.setImageResource(R.drawable.ic_vpn_key_white_24dp)
                text1.setText(R.string.text_qrCodeAvailableHelp)
                text2.text = ssid
                text3.text = key
            } else {
                activeType = Type.HotspotExternal
                text1.setText(R.string.text_hotspotStartedExternallyNotice)
            }
            toggleButton.setText(R.string.butn_stopHotspot)
            secondButton.setText(R.string.butn_wifiSettings)
        } else if (!connections.canReadWifiInfo() && connections.wifiManager.isWifiEnabled) {
            activeType = Type.LocationAccess
            text1.setText(
                if (connections.isLocationServiceEnabled()) {
                    R.string.mesg_locationPermissionRequiredAny
                } else {
                    R.string.mesg_locationServiceDisabled
                }
            )
            toggleButton.setText(R.string.butn_enable)
            secondButton.setText(R.string.text_startHotspot)
        } else if (connections.isConnectedToAnyNetwork()) {
            activeType = Type.WiFi
            val ssid: String? = connectionInfo.ssid
            val bssid: String? = connectionInfo.bssid
            val hostAddress: String? = try {
                InetAddresses.from(connectionInfo.ipAddress).hostAddress
            } catch (e: UnknownHostException) {
                "0.0.0.0"
            }
            code.append(Keyword.QR_CODE_TYPE_WIFI)
                .append(delimiter)
                .append(pin)
                .append(delimiter)
                .append(ssid ?: "")
                .append(delimiter)
                .append(bssid ?: "")
                .append(delimiter)
                .append(hostAddress)
            imageView2.setImageResource(R.drawable.ic_wifi_white_24dp)
            imageView3.setImageResource(R.drawable.ic_ip_white_24dp)
            text1.setText(R.string.help_scanQRCode)
            text2.text = Connections.getCleanSsid(connectionInfo.ssid)
            text3.text = hostAddress
            toggleButton.setText(R.string.butn_wifiSettings)
            secondButton.setText(R.string.text_startHotspot)
        } else {
            activeType = Type.None
            text1.setText(R.string.help_setUpNetwork)
            toggleButton.setText(R.string.text_startHotspot)
            secondButton.setText(R.string.butn_wifiSettings)
        }
        when (activeType) {
            Type.Hotspot, Type.WiFi, Type.HotspotExternal -> ViewCompat.setBackgroundTintList(
                toggleButton, toggleButtonEnabledStateList
            )
            else -> ViewCompat.setBackgroundTintList(toggleButton, toggleButtonDefaultStateList)
        }
        when (activeType) {
            Type.LocationAccess, Type.None, Type.HotspotExternal -> {
                text2.text = null
                text3.text = null
            }
        }
        containerText1.visibility = if (text1.length() > 0) View.VISIBLE else View.GONE
        containerText2.visibility = if (text2.length() > 0) View.VISIBLE else View.GONE
        containerText3.visibility = if (text3.length() > 0) View.VISIBLE else View.GONE
        val showQRCode = code.isNotEmpty()
        if (showQRCode) {
            code.append(delimiter)
                .append("end")
            try {
                val formatWriter = MultiFormatWriter()
                val bitMatrix: BitMatrix = formatWriter.encode(code.toString(), BarcodeFormat.QR_CODE, 400, 400)
                val encoder = BarcodeEncoder()
                val bitmap: Bitmap = encoder.createBitmap(bitMatrix)
                GlideApp.with(requireContext())
                    .load(bitmap)
                    .into(codeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else codeView.setImageResource(R.drawable.ic_qrcode_white_128dp)
        codeText.visibility = if (showQRCode) View.GONE else View.VISIBLE
        ImageViewCompat.setImageTintList(codeView, if (showQRCode) null else colorPassiveState)
    }

    private inner class StatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (WIFI_AP_STATE_CHANGED == intent.action || BgBroadcastReceiver.ACTION_PIN_USED == intent.action
                || WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action
                || ConnectivityManager.CONNECTIVITY_ACTION == intent.action
                || BgBroadcastReceiver.ACTION_PIN_USED == intent.action
                || Services.ACTION_OREO_HOTSPOT_STARTED == intent.action
            ) updateState()
        }
    }

    private enum class Type {
        None, WiFi, Hotspot, HotspotExternal, LocationAccess
    }

    companion object {
        const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
    }
}
