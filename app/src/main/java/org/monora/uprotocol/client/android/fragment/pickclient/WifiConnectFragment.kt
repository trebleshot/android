/*
 * Copyright (C) 2021 Veli Tasalı
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

package org.monora.uprotocol.client.android.fragment.pickclient

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutWifiConnectBinding
import org.monora.uprotocol.client.android.util.Connections
import org.monora.uprotocol.client.android.util.InetAddresses
import org.monora.uprotocol.client.android.util.NotificationBackend
import org.monora.uprotocol.client.android.util.Notifications

class WifiConnectFragment : BottomSheetDialogFragment() {
    private val args: WifiConnectFragmentArgs by navArgs()

    private var waitingForResult = false

    private val connections by lazy {
        Connections(requireContext())
    }

    private val notifications by lazy {
        Notifications(NotificationBackend(requireContext()))
    }

    private val notification by lazy {
        with(args.networkDescription) { notifications.createAddingWifiNetworkNotification(ssid, password) }
    }

    private lateinit var binding: LayoutWifiConnectBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LayoutWifiConnectBinding.inflate(inflater, container, false)
        binding.viewModel = args.networkDescription

        binding.clickListener = View.OnClickListener {
            if (Build.VERSION.SDK_INT <= 29) {
                val clipboardManager = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?

                if (clipboardManager != null) {
                    val clipData = ClipData(
                        ClipDescription(getString(R.string.text_wifiPassword), arrayOf("text/plain")),
                        ClipData.Item(args.networkDescription.password)
                    )
                    clipboardManager.setPrimaryClip(clipData)

                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    waitingForResult = true

                    notification.show()
                }
            }
        }
        binding.executePendingBindings()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        if (waitingForResult) {
            waitingForResult = false

            notification.cancel()

            if (!connections.isConnectedToNetwork(args.networkDescription)) {
                binding.notConnectedText.visibility = View.VISIBLE
            } else {
                setFragmentResult(
                    REQUEST_INET_ADDRESS,
                    bundleOf(
                        EXTRA_INET_ADDRESS to InetAddresses.from(connections.wifiManager.dhcpInfo.gateway),
                        EXTRA_PIN to args.pin
                    )
                )
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (waitingForResult) {
            // In case the user won't come back and kills the app instead.
            notification.cancel()
        }
    }

    companion object {
        const val REQUEST_INET_ADDRESS = "requestInetAddress"

        const val EXTRA_INET_ADDRESS = "extraInetAddress"

        const val EXTRA_PIN = "extraPin"
    }
}
