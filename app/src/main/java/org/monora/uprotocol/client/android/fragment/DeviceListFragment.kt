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

import android.app.Activity
import android.content.*
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.AddDeviceActivity
import org.monora.uprotocol.client.android.adapter.DeviceListAdapter
import org.monora.uprotocol.client.android.adapter.DeviceListAdapter.*
import org.monora.uprotocol.client.android.app.ListingFragment
import org.monora.uprotocol.client.android.database.Kuick
import org.monora.uprotocol.client.android.dialog.DeviceInfoDialog
import org.monora.uprotocol.client.android.dialog.FindConnectionDialog
import org.monora.uprotocol.client.android.model.Device
import org.monora.uprotocol.client.android.model.DeviceAddress
import org.monora.uprotocol.client.android.task.DeviceIntroductionTask
import org.monora.uprotocol.client.android.util.Connections
import org.monora.uprotocol.client.android.util.DeviceLoader.OnResolvedListener
import org.monora.uprotocol.client.android.util.NsdDaemon.Companion.ACTION_DEVICE_STATUS
import org.monora.uprotocol.client.android.util.P2pDaemon
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder

open class DeviceListFragment : ListingFragment<VirtualDevice, ViewHolder, DeviceListAdapter>(), OnResolvedListener {
    private val intentFilter = IntentFilter()

    private val statusReceiver: StatusReceiver = StatusReceiver()

    private val connections: Connections
        get() = Connections(requireContext())

    var hiddenDeviceTypes: Array<Device.Type> = emptyArray()

    private val p2pDaemon: P2pDaemon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filteringSupported = true
        intentFilter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
        intentFilter.addAction(ACTION_DEVICE_STATUS)
        intentFilter.addAction(SCAN_RESULTS_AVAILABLE_ACTION)
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)

        arguments?.getStringArrayList(ARG_HIDDEN_DEVICES_LIST)?.let {
            val containerList = ArrayList<Device.Type>()
            for (i in it.indices) containerList.add(Device.Type.valueOf(it[i]))
            hiddenDeviceTypes = containerList.toTypedArray()
        }

        // TODO: 2/1/21 Wifi Direct daemon? Might not be supported by Android TV.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon = new P2pDaemon(getConnections());
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = DeviceListAdapter(
            this, connections,
            App.from(requireActivity()).nsdDaemon, hiddenDeviceTypes
        )
        emptyListImageView.setImageResource(R.drawable.ic_devices_white_24dp)
        emptyListTextView.text = getString(R.string.text_findDevicesHint)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(statusReceiver, intentFilter)
        // TODO: 2/1/21 Fix regression issue of Wifi Direct.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon.start(requireContext());
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(statusReceiver)
        // TODO: 2/1/21 Enable stop implementation of the Wifi Direct daemon after fixing the regression issue.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon.stop(requireContext());
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_LOCATION_PERMISSION == requestCode) connections.showConnectionOptions(
            requireActivity(), this, REQUEST_LOCATION_PERMISSION
        )
    }

    override fun onDeviceResolved(device: Device, address: DeviceAddress) {
        AddDeviceActivity.returnResult(requireActivity(), device, address)
    }

    override fun performDefaultLayoutClick(holder: ViewHolder, target: VirtualDevice): Boolean {
        if (requireActivity() is AddDeviceActivity) {
            if (target is DescriptionVirtualDevice) App.run(
                requireActivity(), DeviceIntroductionTask(target.description, 0)
            ) else if (target is DbVirtualDevice) {
                val device: Device = target.device
                if (BuildConfig.PROTOCOL_VERSION_MIN > device.protocolVersionMin) {
                    createSnackbar(R.string.mesg_versionNotSupported)?.show()
                } else {
                    FindConnectionDialog.show(requireActivity(), device, this)
                }
            } else return false
        } else openInfo(requireActivity(), connections, target)
        return true
    }

    private inner class StatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (SCAN_RESULTS_AVAILABLE_ACTION == intent.action || ACTION_DEVICE_STATUS == intent.action) {
                refreshList()
            } else if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: KuickDb.BroadcastData = KuickDb.toData(intent)
                if (Kuick.TABLE_DEVICES == data.tableName) refreshList()
            }
        }
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 643

        const val ARG_HIDDEN_DEVICES_LIST = "hiddenDeviceList"

        fun openInfo(activity: Activity, utils: Connections, virtualDevice: VirtualDevice) {
            if (virtualDevice is DescriptionVirtualDevice) {
                val description: NetworkDescription = virtualDevice.description
                val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
                    .setTitle(virtualDevice.name())
                    .setMessage(R.string.text_trebleshotHotspotDescription)
                    .setNegativeButton(R.string.butn_close, null)
                if (Build.VERSION.SDK_INT < 29) builder.setPositiveButton(
                    if (utils.isConnectedToNetwork(description)) R.string.butn_disconnect else R.string.butn_connect
                ) { dialog: DialogInterface?, which: Int ->
                    App.from(activity).run(DeviceIntroductionTask(description, 0))
                }
                builder.show()
            } else if (virtualDevice is DbVirtualDevice) {
                DeviceInfoDialog(activity, virtualDevice.device).show()
            }
        }
    }
}