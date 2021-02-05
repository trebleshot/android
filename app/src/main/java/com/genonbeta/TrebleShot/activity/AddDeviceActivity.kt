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
package com.genonbeta.TrebleShot.activity

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
import android.os.Parcelable
import android.os.Parcel
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
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.os.Bundle
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
import android.os.PowerManager
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
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.Exception
import java.util.ArrayList

class AddDeviceActivity : Activity(), SnackbarPlacementProvider {
    private val mFilter = IntentFilter()
    private var mNetworkManagerFragment: NetworkManagerFragment? = null
    private var mDeviceListFragment: DeviceListFragment? = null
    private var mOptionsFragment: OptionsFragment? = null
    private var mAppBarLayout: AppBarLayout? = null
    private var mToolbar: Toolbar? = null
    private var mProgressBar: ProgressBar? = null
    private var mConnectionMode = ConnectionMode.Return
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_CHANGE_FRAGMENT == intent.action && intent.hasExtra(EXTRA_FRAGMENT_ENUM)) {
                val fragmentEnum = intent.getStringExtra(EXTRA_FRAGMENT_ENUM)
                try {
                    setFragment(AvailableFragment.valueOf(fragmentEnum))
                } catch (e: Exception) {
                    // do nothing
                }
            } else if (BackgroundService.ACTION_DEVICE_ACQUAINTANCE == intent.action && intent.hasExtra(
                    BackgroundService.EXTRA_DEVICE
                )
                && intent.hasExtra(BackgroundService.EXTRA_DEVICE_ADDRESS)
            ) {
                val device: Device = intent.getParcelableExtra(BackgroundService.EXTRA_DEVICE)
                val address: DeviceAddress = intent.getParcelableExtra(BackgroundService.EXTRA_DEVICE_ADDRESS)
                handleResult(device, address)
            } else if (BackgroundService.ACTION_INCOMING_TRANSFER_READY == intent.action && intent.hasExtra(
                    BackgroundService.EXTRA_TRANSFER
                )
            ) {
                TransferDetailActivity.Companion.startInstance(
                    this@AddDeviceActivity,
                    intent.getParcelableExtra(BackgroundService.EXTRA_TRANSFER)
                )
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent != null && intent.hasExtra(EXTRA_CONNECTION_MODE)) {
            mConnectionMode = intent.getSerializableExtra(EXTRA_CONNECTION_MODE) as ConnectionMode
        }
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_connection_manager)
        val hiddenDeviceTypes = ArrayList<String>()
        hiddenDeviceTypes.add(Device.Type.WEB.toString())
        val deviceListArgs = Bundle()
        deviceListArgs.putStringArrayList(DeviceListFragment.Companion.ARG_HIDDEN_DEVICES_LIST, hiddenDeviceTypes)
        val factory: FragmentFactory = supportFragmentManager.fragmentFactory
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        mAppBarLayout = findViewById<AppBarLayout>(R.id.app_bar)
        mProgressBar = findViewById(R.id.activity_connection_establishing_progress_bar)
        mToolbar = findViewById(R.id.toolbar)
        mOptionsFragment = factory.instantiate(classLoader, OptionsFragment::class.java.name)
        mNetworkManagerFragment = factory.instantiate(
            classLoader,
            NetworkManagerFragment::class.java.getName()
        ) as NetworkManagerFragment
        mDeviceListFragment = factory.instantiate(
            classLoader,
            DeviceListFragment::class.java.getName()
        ) as DeviceListFragment
        mDeviceListFragment.setArguments(deviceListArgs)
        mFilter.addAction(ACTION_CHANGE_FRAGMENT)
        mFilter.addAction(BackgroundService.ACTION_DEVICE_ACQUAINTANCE)
        mFilter.addAction(BackgroundService.ACTION_INCOMING_TRANSFER_READY)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        checkFragment()
        registerReceiver(mReceiver, mFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) if (requestCode == REQUEST_BARCODE_SCAN) {
            val device: Device = data.getParcelableExtra(BarcodeScannerActivity.Companion.EXTRA_DEVICE)
            val address: DeviceAddress = data.getParcelableExtra(BarcodeScannerActivity.Companion.EXTRA_DEVICE_ADDRESS)
            if (device != null && address != null) handleResult(device, address)
        } else if (requestCode == REQUEST_IP_DISCOVERY) {
            val device: Device = data.getParcelableExtra(ManualConnectionActivity.Companion.EXTRA_DEVICE)
            val address: DeviceAddress =
                data.getParcelableExtra(ManualConnectionActivity.Companion.EXTRA_DEVICE_ADDRESS)
            if (device != null && address != null) handleResult(device, address)
        }
    }

    override fun onBackPressed() {
        if (getShowingFragment() is OptionsFragment) super.onBackPressed() else setFragment(AvailableFragment.Options)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) onBackPressed() else return super.onOptionsItemSelected(item)
        return true
    }

    fun applyViewChanges(fragment: Fragment?) {
        val isOptions = fragment is OptionsFragment
        if (supportActionBar != null) {
            mToolbar!!.title =
                if (fragment is TitleProvider) (fragment as TitleProvider?).getDistinctiveTitle(this@AddDeviceActivity) else getString(
                    R.string.text_chooseDevice
                )
        }
        mAppBarLayout.setExpanded(isOptions, true)
    }

    private fun checkFragment() {
        val currentFragment = getShowingFragment()
        if (currentFragment == null) setFragment(AvailableFragment.Options) else applyViewChanges(currentFragment)
    }

    override fun createSnackbar(resId: Int, vararg objects: Any): Snackbar {
        return Snackbar.make(
            findViewById<View>(R.id.activity_connection_establishing_content_view),
            getString(resId, *objects), Snackbar.LENGTH_LONG
        )
    }

    fun getShowingFragmentId(): AvailableFragment {
        val fragment = getShowingFragment()
        if (fragment is NetworkManagerFragment) return AvailableFragment.GenerateQrCode else if (fragment is DeviceListFragment) return AvailableFragment.AllDevices

        // Probably OptionsFragment
        return AvailableFragment.Options
    }

    fun getShowingFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_connection_establishing_content_view)
    }

    private fun handleResult(device: Device, address: DeviceAddress?) {
        if (ConnectionMode.Return == mConnectionMode) returnResult(
            this,
            device,
            address
        ) else if (ConnectionMode.WaitForRequests == mConnectionMode) createSnackbar(R.string.mesg_completing).show()
    }

    fun setFragment(fragment: AvailableFragment?) {
        val activeFragment = getShowingFragment()
        val fragmentCandidate: Fragment?
        fragmentCandidate = when (fragment) {
            AvailableFragment.EnterAddress -> {
                startManualConnectionActivity()
                return
            }
            AvailableFragment.ScanQrCode -> {
                startCodeScanner()
                return
            }
            AvailableFragment.GenerateQrCode -> mNetworkManagerFragment
            AvailableFragment.AllDevices -> mDeviceListFragment
            AvailableFragment.Options -> mOptionsFragment
            else -> mOptionsFragment
        }
        if (activeFragment == null || fragmentCandidate !== activeFragment) {
            val transaction = supportFragmentManager.beginTransaction()
            if (activeFragment != null) transaction.remove(activeFragment)
            if (activeFragment != null && fragmentCandidate is OptionsFragment) transaction.setCustomAnimations(
                R.anim.enter_from_left,
                R.anim.exit_to_right
            ) else transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
            transaction.add(R.id.activity_connection_establishing_content_view, fragmentCandidate!!)
            transaction.commit()
            applyViewChanges(fragmentCandidate)
        }
    }

    private fun startCodeScanner() {
        startActivityForResult(Intent(this, BarcodeScannerActivity::class.java), REQUEST_BARCODE_SCAN)
    }

    protected fun startManualConnectionActivity() {
        startActivityForResult(Intent(this, ManualConnectionActivity::class.java), REQUEST_IP_DISCOVERY)
    }

    enum class AvailableFragment {
        Options, GenerateQrCode, AllDevices, ScanQrCode, CreateHotspot, EnterAddress
    }

    class OptionsFragment : com.genonbeta.android.framework.app.Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.layout_connection_options_fragment, container, false)
            val listener = View.OnClickListener { v: View ->
                when (v.id) {
                    R.id.connection_option_devices -> updateFragment(AvailableFragment.AllDevices)
                    R.id.connection_option_generate_qr_code -> updateFragment(AvailableFragment.GenerateQrCode)
                    R.id.connection_option_manual_ip -> updateFragment(AvailableFragment.EnterAddress)
                    R.id.connection_option_scan -> updateFragment(AvailableFragment.ScanQrCode)
                }
            }
            view.findViewById<View>(R.id.connection_option_devices).setOnClickListener(listener)
            view.findViewById<View>(R.id.connection_option_generate_qr_code).setOnClickListener(listener)
            view.findViewById<View>(R.id.connection_option_scan).setOnClickListener(listener)
            view.findViewById<View>(R.id.connection_option_manual_ip).setOnClickListener(listener)
            return view
        }

        fun updateFragment(fragment: AvailableFragment) {
            if (context != null) context!!.sendBroadcast(
                Intent(ACTION_CHANGE_FRAGMENT)
                    .putExtra(EXTRA_FRAGMENT_ENUM, fragment.toString())
            )
        }
    }

    enum class ConnectionMode {
        WaitForRequests, Return
    }

    companion object {
        const val ACTION_CHANGE_FRAGMENT = "com.genonbeta.intent.action.CHANGE_FRAGMENT"
        const val EXTRA_FRAGMENT_ENUM = "extraFragmentEnum"
        const val EXTRA_DEVICE = "extraDevice"
        const val EXTRA_DEVICE_ADDRESS = "extraDeviceAddress"
        const val EXTRA_CONNECTION_MODE = "exraConnectionMode"
        const val REQUEST_BARCODE_SCAN = 100
        const val REQUEST_IP_DISCOVERY = 110
        fun returnResult(activity: android.app.Activity, device: Device?, address: DeviceAddress?) {
            activity.setResult(
                RESULT_OK, Intent()
                    .putExtra(EXTRA_DEVICE, device)
                    .putExtra(EXTRA_DEVICE_ADDRESS, address)
            )
            activity.finish()
        }
    }
}