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
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import android.os.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.BuildConfig
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.util.Connections
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

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
        mIntentFilter.addAction(NsdDaemon.Companion.ACTION_DEVICE_STATUS)
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
            App.Companion.from(requireActivity()).getNsdDaemon(), mHiddenDeviceTypes
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
        AddDeviceActivity.Companion.returnResult(requireActivity(), device, address)
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

    override fun performDefaultLayoutClick(holder: RecyclerViewAdapter.ViewHolder, `object`: VirtualDevice): Boolean {
        if (requireActivity() is AddDeviceActivity) {
            if (`object` is DescriptionVirtualDevice) App.Companion.run<DeviceIntroductionTask>(
                requireActivity(), DeviceIntroductionTask(
                    (`object` as DescriptionVirtualDevice).description,
                    0
                )
            ) else if (`object` is DbVirtualDevice) {
                val device: Device = (`object` as DbVirtualDevice).device
                if (BuildConfig.PROTOCOL_VERSION_MIN > device.protocolVersionMin) createSnackbar(R.string.mesg_versionNotSupported).show() else FindConnectionDialog.Companion.show(
                    activity, device, this
                )
            } else return false
        } else openInfo(activity, getConnections(), `object`)
        return true
    }

    private inner class StatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent.action || NsdDaemon.Companion.ACTION_DEVICE_STATUS == intent.action) refreshList() else if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: BroadcastData = KuickDb.toData(intent)
                if (Kuick.Companion.TABLE_DEVICES == data.tableName) refreshList()
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
                    App.Companion.from(activity).run(
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