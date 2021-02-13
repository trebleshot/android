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
package com.genonbeta.TrebleShot.fragment

import android.app.Activity
import android.content.*
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.BuildConfig
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter.*
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.DeviceAddress
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog
import com.genonbeta.TrebleShot.dialog.FindConnectionDialog
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask
import com.genonbeta.TrebleShot.ui.callback.IconProvider
import com.genonbeta.TrebleShot.util.Connections
import com.genonbeta.TrebleShot.util.DeviceLoader
import com.genonbeta.TrebleShot.util.NsdDaemon.Companion.ACTION_DEVICE_STATUS
import com.genonbeta.TrebleShot.util.P2pDaemon
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder

open class DeviceListFragment :
    EditableListFragment<VirtualDevice, ViewHolder, DeviceListAdapter>(), IconProvider,
    DeviceLoader.OnDeviceResolvedListener {
    override val iconRes: Int = R.drawable.ic_devices_white_24dp

    private val intentFilter = IntentFilter()

    private val statusReceiver: StatusReceiver = StatusReceiver()

    private var connections: Connections? = null

    private lateinit var hiddenDeviceTypes: Array<Device.Type>

    private val p2pDaemon: P2pDaemon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFilteringSupported = true
        isSortingSupported = false
        itemOffsetDecorationEnabled = true
        itemOffsetForEdgesEnabled = true
        defaultItemOffsetPadding = resources.getDimension(R.dimen.padding_list_content_parent_layout)
        intentFilter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
        intentFilter.addAction(ACTION_DEVICE_STATUS)
        intentFilter.addAction(SCAN_RESULTS_AVAILABLE_ACTION)
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        arguments?.let {
            if (it.containsKey(ARG_HIDDEN_DEVICES_LIST)) {
                val hiddenTypes: List<String>? = it.getStringArrayList(ARG_HIDDEN_DEVICES_LIST)
                if (hiddenTypes != null && hiddenTypes.isNotEmpty()) {
                    val containerList = ArrayList<Device.Type>()

                    for (i in hiddenTypes.indices) {
                        containerList.add(Device.Type.valueOf(hiddenTypes[i]))
                    }

                    hiddenDeviceTypes = containerList.toTypedArray()
                }
            }
        }

        // TODO: 2/1/21 Wifi Direct daemon? Might not be supported by Android TV.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon = new P2pDaemon(getConnections());
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = DeviceListAdapter(
            this, getConnections(),
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
        if (REQUEST_LOCATION_PERMISSION == requestCode) getConnections().showConnectionOptions(
            activity!!, this, REQUEST_LOCATION_PERMISSION
        )
    }

    override fun onDeviceResolved(device: Device, address: DeviceAddress) {
        AddDeviceActivity.returnResult(requireActivity(), device, address)
    }

    fun getConnections(): Connections {
        if (connections == null) connections = Connections(requireContext())
        return connections!!
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_allDevices)
    }

    override fun isHorizontalOrientation(): Boolean {
        return (arguments != null && arguments!!.getBoolean(ARG_USE_HORIZONTAL_VIEW)
                || super.isHorizontalOrientation())
    }

    fun setHiddenDeviceTypes(types: Array<Device.Type>) {
        hiddenDeviceTypes = types
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
        } else openInfo(requireActivity(), getConnections(), target)
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
        const val ARG_USE_HORIZONTAL_VIEW = "useHorizontalView"
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