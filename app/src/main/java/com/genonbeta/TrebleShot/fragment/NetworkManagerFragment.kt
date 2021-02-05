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
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.database.Kuick
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
import android.provider.Settings
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.app.Fragment
import org.json.JSONException
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * created by: veli
 * date: 11/04/18 20:53
 */
class NetworkManagerFragment : Fragment(), IconProvider, TitleProvider {
    private val mIntentFilter = IntentFilter()
    private val mStatusReceiver: StatusReceiver = StatusReceiver()
    private var mConnections: Connections? = null
    private var mContainerText1: View? = null
    private var mContainerText2: View? = null
    private var mContainerText3: View? = null
    private var mCodeText: TextView? = null
    private var mText1: TextView? = null
    private var mText2: TextView? = null
    private var mText3: TextView? = null
    private var mImageView2: ImageView? = null
    private var mImageView3: ImageView? = null
    private var mCodeView: ImageView? = null
    private var mToggleButton: Button? = null
    private var mSecondButton: Button? = null
    private var mHelpMenuItem: MenuItem? = null
    private var mColorPassiveState: ColorStateList? = null
    private var mManager: HotspotManager? = null
    private var mActiveType: Type? = null
    private var mToggleButtonDefaultStateList: ColorStateList? = null
    private var mToggleButtonEnabledStateList: ColorStateList? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mConnections = Connections(requireContext())
        mManager = HotspotManager.Companion.newInstance(requireContext())
        mIntentFilter.addAction(App.Companion.ACTION_OREO_HOTSPOT_STARTED)
        mIntentFilter.addAction(BackgroundService.ACTION_PIN_USED)
        mIntentFilter.addAction(WIFI_AP_STATE_CHANGED)
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        mIntentFilter.addAction(BackgroundService.ACTION_PIN_USED)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.layout_network_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mToggleButtonEnabledStateList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                AppUtils.getReference(requireContext(), R.attr.colorError)
            )
        )
        mColorPassiveState = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(), AppUtils.getReference(
                    requireContext(), R.attr.colorPassive
                )
            )
        )
        mCodeView = view.findViewById(R.id.layout_network_manager_qr_image)
        mCodeText = view.findViewById<TextView>(R.id.layout_network_manager_qr_help_text)
        mToggleButton = view.findViewById(R.id.layout_network_manager_info_toggle_button)
        mSecondButton = view.findViewById(R.id.layout_network_manager_info_second_toggle_button)
        mContainerText1 = view.findViewById(R.id.layout_netowrk_manager_info_container_text1_container)
        mContainerText2 = view.findViewById(R.id.layout_network_manager_info_container_text2_container)
        mContainerText3 = view.findViewById(R.id.layout_network_manager_info_container_text3_container)
        mText1 = view.findViewById<TextView>(R.id.layout_network_manager_info_container_text1)
        mText2 = view.findViewById<TextView>(R.id.layout_network_manager_info_container_text2)
        mText3 = view.findViewById<TextView>(R.id.layout_network_manager_info_container_text3)
        mImageView2 = view.findViewById(R.id.layout_network_manager_info_container_text2_icon)
        mImageView3 = view.findViewById(R.id.layout_network_manager_info_container_text3_icon)
        mToggleButtonDefaultStateList = ViewCompat.getBackgroundTintList(mToggleButton)
        mToggleButton.setOnClickListener(View.OnClickListener { v: View -> toggle(v) })
        mSecondButton.setOnClickListener(View.OnClickListener { v: View -> toggle(v) })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_hotspot_manager, menu)
        mHelpMenuItem = menu.findItem(R.id.show_help)
        showMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.show_help && mManager.getConfiguration() != null) {
            val hotspotName: String = mManager.getConfiguration().SSID
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
        if (REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT == requestCode) toggleHotspot() else if (REQUEST_LOCATION_PERMISSION == requestCode) updateState()
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(mStatusReceiver, mIntentFilter)
        updateState()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(mStatusReceiver)
    }

    override fun getIconRes(): Int {
        return R.drawable.ic_qrcode_white_24dp
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.butn_generateQrCode)
    }

    fun getWifiConfiguration(): WifiConfiguration? {
        if (Build.VERSION.SDK_INT < 26) return mManager.getConfiguration()
        try {
            return App.Companion.from(requireActivity()).getHotspotConfig()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        return null
    }

    fun openWifiSettings() {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun toggleHotspot() {
        mConnections!!.toggleHotspot(
            requireActivity(), this, mManager, true,
            REQUEST_LOCATION_PERMISSION_FOR_HOTSPOT
        )
    }

    fun toggle(v: View) {
        if (v.id == R.id.layout_network_manager_info_toggle_button) {
            when (mActiveType) {
                Type.LocationPermissionNeeded -> mConnections!!.validateLocationPermission(
                    activity!!, REQUEST_LOCATION_PERMISSION
                )
                Type.WiFi, Type.HotspotExternal -> openWifiSettings()
                Type.Hotspot, Type.None -> toggleHotspot()
                else -> toggleHotspot()
            }
        } else if (v.id == R.id.layout_network_manager_info_second_toggle_button) {
            when (mActiveType) {
                Type.LocationPermissionNeeded, Type.WiFi -> toggleHotspot()
                Type.HotspotExternal, Type.Hotspot, Type.None -> openWifiSettings()
                else -> openWifiSettings()
            }
        }
    }

    private fun showMenu() {
        if (mHelpMenuItem != null) mHelpMenuItem!!.isVisible =
            mManager.getConfiguration() != null && mManager.isEnabled()
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
        val pin = AppUtils.generateNetworkPin(context)
        val delimiter = ";"
        val code = StringBuilder()
        val config: WifiConfiguration? = getWifiConfiguration()
        val connectionInfo: WifiInfo = mConnections.getWifiManager().connectionInfo
        if (mManager.isEnabled()) {
            if (config != null) {
                mActiveType = Type.Hotspot
                val ssid: String = config.SSID
                val bssid: String = config.BSSID
                val key: String = config.preSharedKey
                code.append(Keyword.QR_CODE_TYPE_HOTSPOT)
                    .append(delimiter)
                    .append(pin)
                    .append(delimiter)
                    .append(ssid)
                    .append(delimiter)
                    .append(bssid ?: "")
                    .append(delimiter)
                    .append(key ?: "")
                mImageView2!!.setImageResource(R.drawable.ic_wifi_tethering_white_24dp)
                mImageView3!!.setImageResource(R.drawable.ic_vpn_key_white_24dp)
                mText1.setText(R.string.text_qrCodeAvailableHelp)
                mText2.setText(ssid)
                mText3.setText(key)
            } else {
                mActiveType = Type.HotspotExternal
                mText1.setText(R.string.text_hotspotStartedExternallyNotice)
            }
            mToggleButton!!.setText(R.string.butn_stopHotspot)
            mSecondButton!!.setText(R.string.butn_wifiSettings)
        } else if (!mConnections!!.canReadWifiInfo() && mConnections.getWifiManager().isWifiEnabled) {
            mActiveType = Type.LocationPermissionNeeded
            mText1.setText(R.string.mesg_locationPermissionRequiredAny)
            mToggleButton!!.setText(R.string.butn_enable)
            mSecondButton!!.setText(R.string.text_startHotspot)
        } else if (mConnections!!.isConnectedToAnyNetwork) {
            mActiveType = Type.WiFi
            val hostAddress: String?
            val ssid: String = connectionInfo.getSSID()
            val bssid: String = connectionInfo.getBSSID()
            hostAddress = try {
                InetAddress.getByAddress(InetAddresses.toByteArray(connectionInfo.getIpAddress()))
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
            mImageView2!!.setImageResource(R.drawable.ic_wifi_white_24dp)
            mImageView3!!.setImageResource(R.drawable.ic_ip_white_24dp)
            mText1.setText(R.string.help_scanQRCode)
            mText2.setText(Connections.Companion.getCleanSsid(connectionInfo.getSSID()))
            mText3.setText(hostAddress)
            mToggleButton!!.setText(R.string.butn_wifiSettings)
            mSecondButton!!.setText(R.string.text_startHotspot)
        } else {
            mActiveType = Type.None
            mText1.setText(R.string.help_setUpNetwork)
            mToggleButton!!.setText(R.string.text_startHotspot)
            mSecondButton!!.setText(R.string.butn_wifiSettings)
        }
        when (mActiveType) {
            Type.Hotspot, Type.WiFi, Type.HotspotExternal -> ViewCompat.setBackgroundTintList(
                mToggleButton,
                mToggleButtonEnabledStateList
            )
            else -> ViewCompat.setBackgroundTintList(mToggleButton, mToggleButtonDefaultStateList)
        }
        when (mActiveType) {
            Type.LocationPermissionNeeded, Type.None, Type.HotspotExternal -> {
                mText2.setText(null)
                mText3.setText(null)
            }
        }
        mContainerText1!!.visibility = if (mText1.length() > 0) View.VISIBLE else View.GONE
        mContainerText2!!.visibility = if (mText2.length() > 0) View.VISIBLE else View.GONE
        mContainerText3!!.visibility = if (mText3.length() > 0) View.VISIBLE else View.GONE
        val showQRCode = code.length > 0 && context != null
        if (showQRCode) {
            code.append(delimiter)
                .append("end")
            try {
                val formatWriter = MultiFormatWriter()
                val bitMatrix: BitMatrix = formatWriter.encode(code.toString(), BarcodeFormat.QR_CODE, 400, 400)
                val encoder = BarcodeEncoder()
                val bitmap: Bitmap = encoder.createBitmap(bitMatrix)
                GlideApp.with(context!!)
                    .load(bitmap)
                    .into(mCodeView!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else mCodeView!!.setImageResource(R.drawable.ic_qrcode_white_128dp)
        mCodeText.setVisibility(if (showQRCode) View.GONE else View.VISIBLE)
        ImageViewCompat.setImageTintList(mCodeView, if (showQRCode) null else mColorPassiveState)
    }

    private inner class StatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (WIFI_AP_STATE_CHANGED == intent.action || BackgroundService.ACTION_PIN_USED == intent.action || WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action || ConnectivityManager.CONNECTIVITY_ACTION == intent.action || BackgroundService.ACTION_PIN_USED == intent.action || App.Companion.ACTION_OREO_HOTSPOT_STARTED == intent.action) updateState()
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