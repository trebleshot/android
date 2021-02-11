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
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import android.os.*
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.BuildConfig
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.util.Connections

open class DeviceListFragment :
    EditableListFragment<VirtualDevice?, RecyclerViewAdapter.ViewHolder?, DeviceListAdapter?>(), IconProvider,
    OnDeviceResolvedListener {
    private val mIntentFilter = IntentFilter()
    private val mStatusReceiver: StatusReceiver = StatusReceiver()
    private var mConnections: Connections? = null
    private var mHiddenDeviceTypes: Array<Device.Type?>
    private val mP2pDaemon: P2pDaemon? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilteringSupported(true)
        isSortingSupported = false
        setItemOffsetDecorationEnabled(true)
        setItemOffsetForEdgesEnabled(true)
        setDefaultItemOffsetPadding(resources.getDimension(R.dimen.padding_list_content_parent_layout))
        mIntentFilter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
        mIntentFilter.addAction(NsdDaemon.ACTION_DEVICE_STATUS)
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        if (arguments != null) {
            val args = arguments
            if (args!!.containsKey(ARG_HIDDEN_DEVICES_LIST)) {
                val hiddenTypes: List<String>? = args.getStringArrayList(ARG_HIDDEN_DEVICES_LIST)
                if (hiddenTypes != null && hiddenTypes.size > 0) {
                    mHiddenDeviceTypes = arrayOfNulls(hiddenTypes.size)
                    for (i in hiddenTypes.indices) {
                        val type = Device.Type.valueOf(hiddenTypes[i])
                        mHiddenDeviceTypes[i] = type
                    }
                }
            }
        }

        // TODO: 2/1/21 Wifi Direct daemon? Might not be supported by Android TV.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon = new P2pDaemon(getConnections());
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listAdapter = DeviceListAdapter(
            this, getConnections(),
            App.from(requireActivity()).getNsdDaemon(), mHiddenDeviceTypes
        )
        setEmptyListImage(R.drawable.ic_devices_white_24dp)
        setEmptyListText(getString(R.string.text_findDevicesHint))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(mStatusReceiver, mIntentFilter)
        // TODO: 2/1/21 Fix regression issue of Wifi Direct.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon.start(requireContext());
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(mStatusReceiver)
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

    override fun onDeviceResolved(device: Device?, address: DeviceAddress?) {
        AddDeviceActivity.returnResult(requireActivity(), device, address)
    }

    fun getConnections(): Connections {
        if (mConnections == null) mConnections = Connections(requireContext())
        return mConnections!!
    }

    override fun getIconRes(): Int {
        return R.drawable.ic_devices_white_24dp
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_allDevices)
    }

    override fun isHorizontalOrientation(): Boolean {
        return (arguments != null && arguments!!.getBoolean(ARG_USE_HORIZONTAL_VIEW)
                || super.isHorizontalOrientation())
    }

    fun setHiddenDeviceTypes(types: Array<Device.Type?>) {
        mHiddenDeviceTypes = types
    }

    override fun performDefaultLayoutClick(holder: RecyclerViewAdapter.ViewHolder, target: VirtualDevice): Boolean {
        if (requireActivity() is AddDeviceActivity) {
            if (target is DescriptionVirtualDevice) App.run<DeviceIntroductionTask>(
                requireActivity(), DeviceIntroductionTask(
                    (target as DescriptionVirtualDevice).description,
                    0
                )
            ) else if (target is DbVirtualDevice) {
                val device: Device = (target as DbVirtualDevice).device
                if (BuildConfig.PROTOCOL_VERSION_MIN > device.protocolVersionMin) createSnackbar(R.string.mesg_versionNotSupported).show() else FindConnectionDialog.show(
                    activity, device, this
                )
            } else return false
        } else openInfo(activity, getConnections(), target)
        return true
    }

    private inner class StatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent.action || NsdDaemon.ACTION_DEVICE_STATUS == intent.action) refreshList() else if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: BroadcastData = KuickDb.toData(intent)
                if (Kuick.TABLE_DEVICES == data.tableName) refreshList()
            }
        }
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 643
        const val ARG_USE_HORIZONTAL_VIEW = "useHorizontalView"
        const val ARG_HIDDEN_DEVICES_LIST = "hiddenDeviceList"
        fun openInfo(activity: Activity?, utils: Connections, virtualDevice: VirtualDevice) {
            if (virtualDevice is DescriptionVirtualDevice) {
                val description: NetworkDescription = (virtualDevice as DescriptionVirtualDevice).description
                val builder: AlertDialog.Builder = AlertDialog.Builder(
                    activity!!
                )
                    .setTitle(virtualDevice.name())
                    .setMessage(R.string.text_trebleshotHotspotDescription)
                    .setNegativeButton(R.string.butn_close, null)
                if (Build.VERSION.SDK_INT < 29) builder.setPositiveButton(if (utils.isConnectedToNetwork(description)) R.string.butn_disconnect else R.string.butn_connect) { dialog: DialogInterface?, which: Int ->
                    App.from(activity).run(
                        DeviceIntroductionTask(description, 0)
                    )
                }
                builder.show()
            } else if (virtualDevice is DbVirtualDevice) DeviceInfoDialog(
                activity,
                (virtualDevice as DbVirtualDevice).device
            ).show()
        }
    }
}