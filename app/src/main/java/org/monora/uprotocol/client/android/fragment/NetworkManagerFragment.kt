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
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import com.genonbeta.android.framework.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONException
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.util.Connections
import org.monora.uprotocol.client.android.util.HotspotManager
import org.monora.uprotocol.client.android.util.InetAddresses
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * created by: veli
 * date: 11/04/18 20:53
 */
@AndroidEntryPoint
class NetworkManagerFragment : Fragment() {
    @Inject
    lateinit var backgroundBackend: BackgroundBackend

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connections = Connections(requireContext())
        manager = HotspotManager.newInstance(requireContext())
        intentFilter.addAction(BackgroundBackend.ACTION_OREO_HOTSPOT_STARTED)
        intentFilter.addAction(BackgroundService.ACTION_PIN_USED)
        intentFilter.addAction(WIFI_AP_STATE_CHANGED)
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilter.addAction(BackgroundService.ACTION_PIN_USED)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return layoutInflater.inflate(R.layout.layout_network_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toggleButtonEnabledStateList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                AppUtils.getReference(requireContext(), R.attr.colorError)
            )
        )
        colorPassiveState = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(), AppUtils.getReference(
                    requireContext(), R.attr.colorPassive
                )
            )
        )
        codeView = view.findViewById(R.id.layout_network_manager_qr_image)
        codeText = view.findViewById(R.id.layout_network_manager_qr_help_text)
        toggleButton = view.findViewById(R.id.layout_network_manager_info_toggle_button)
        secondButton = view.findViewById(R.id.layout_network_manager_info_second_toggle_button)
        containerText1 = view.findViewById(R.id.layout_netowrk_manager_info_container_text1_container)
        containerText2 = view.findViewById(R.id.layout_network_manager_info_container_text2_container)
        containerText3 = view.findViewById(R.id.layout_network_manager_info_container_text3_container)
        text1 = view.findViewById(R.id.layout_network_manager_info_container_text1)
        text2 = view.findViewById(R.id.layout_network_manager_info_container_text2)
        text3 = view.findViewById(R.id.layout_network_manager_info_container_text3)
        imageView2 = view.findViewById(R.id.layout_network_manager_info_container_text2_icon)
        imageView3 = view.findViewById(R.id.layout_network_manager_info_container_text3_icon)
        toggleButtonDefaultStateList = ViewCompat.getBackgroundTintList(toggleButton)
        toggleButton.setOnClickListener { v: View -> toggle(v) }
        secondButton.setOnClickListener { v: View -> toggle(v) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_hotspot_manager, menu)
        helpMenuItem = menu.findItem(R.id.show_help)
        showMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val configuration = manager.configuration
        if (id == R.id.show_help && configuration != null) {
            val hotspotName: String = configuration.SSID
            val friendlyName = AppUtils.getFriendlySSID(hotspotName)
            AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.mesg_hotspotCreatedInfo, hotspotName, friendlyName))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT == requestCode) {
            toggleHotspot()
        } else if (REQUEST_LOCATION_PERMISSION == requestCode) {
            updateState()
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

    fun getWifiConfiguration(): WifiConfiguration? {
        if (Build.VERSION.SDK_INT < 26) return manager.configuration
        try {
            return backgroundBackend.getHotspotConfig()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        return null
    }

    fun openWifiSettings() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun toggleHotspot() {
        connections.toggleHotspot(
            backgroundBackend, requireActivity(), this, manager, true, REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT
        )
    }

    fun toggle(v: View) {
        if (v.id == R.id.layout_network_manager_info_toggle_button) {
            when (activeType) {
                Type.LocationPermissionNeeded -> connections.validateLocationPermission(
                    requireActivity(), REQUEST_LOCATION_PERMISSION
                )
                Type.WiFi, Type.HotspotExternal -> openWifiSettings()
                Type.Hotspot, Type.None -> toggleHotspot()
                else -> toggleHotspot()
            }
        } else if (v.id == R.id.layout_network_manager_info_second_toggle_button) {
            when (activeType) {
                Type.LocationPermissionNeeded, Type.WiFi -> toggleHotspot()
                Type.HotspotExternal, Type.Hotspot, Type.None -> openWifiSettings()
                else -> openWifiSettings()
            }
        }
    }

    private fun showMenu() {
        helpMenuItem?.isVisible = manager.configuration != null && manager.enabled
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
        val pin = AppUtils.generateNetworkPin(requireContext())
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
                text2.setText(ssid)
                text3.setText(key)
            } else {
                activeType = Type.HotspotExternal
                text1.setText(R.string.text_hotspotStartedExternallyNotice)
            }
            toggleButton.setText(R.string.butn_stopHotspot)
            secondButton.setText(R.string.butn_wifiSettings)
        } else if (!connections.canReadWifiInfo() && connections.wifiManager.isWifiEnabled) {
            activeType = Type.LocationPermissionNeeded
            text1.setText(R.string.mesg_locationPermissionRequiredAny)
            toggleButton.setText(R.string.butn_enable)
            secondButton.setText(R.string.text_startHotspot)
        } else if (connections.isConnectedToAnyNetwork()) {
            activeType = Type.WiFi
            val ssid: String? = connectionInfo.ssid
            val bssid: String? = connectionInfo.bssid
            val hostAddress: String? = try {
                InetAddress.getByAddress(InetAddresses.toByteArray(connectionInfo.ipAddress))
                    .hostAddress
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
            text2.setText(Connections.getCleanSsid(connectionInfo.getSSID()))
            text3.setText(hostAddress)
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
                toggleButton,
                toggleButtonEnabledStateList
            )
            else -> ViewCompat.setBackgroundTintList(toggleButton, toggleButtonDefaultStateList)
        }
        when (activeType) {
            Type.LocationPermissionNeeded, Type.None, Type.HotspotExternal -> {
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
            if (WIFI_AP_STATE_CHANGED == intent.action || BackgroundService.ACTION_PIN_USED == intent.action
                || WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action
                || ConnectivityManager.CONNECTIVITY_ACTION == intent.action
                || BackgroundService.ACTION_PIN_USED == intent.action
                || BackgroundBackend.ACTION_OREO_HOTSPOT_STARTED == intent.action
            ) updateState()
        }
    }

    private enum class Type {
        None, WiFi, Hotspot, HotspotExternal, LocationPermissionNeeded
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
        const val REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT = 2
        const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
    }
}