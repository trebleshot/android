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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.DeviceAddress
import com.genonbeta.TrebleShot.fragment.DeviceListFragment
import com.genonbeta.TrebleShot.fragment.NetworkManagerFragment
import com.genonbeta.TrebleShot.service.BackgroundService
import com.genonbeta.TrebleShot.ui.callback.TitleProvider
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar

class AddDeviceActivity : Activity(), SnackbarPlacementProvider {
    private val filter = IntentFilter()

    private lateinit var networkManagerFragment: NetworkManagerFragment

    private lateinit var deviceListFragment: DeviceListFragment

    private lateinit var optionsFragment: OptionsFragment

    private lateinit var appBarLayout: AppBarLayout

    private lateinit var toolbar: Toolbar

    private lateinit var progressBar: ProgressBar

    private var connectionMode = ConnectionMode.Return

    private val selfReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_CHANGE_FRAGMENT == intent.action && intent.hasExtra(EXTRA_FRAGMENT_ENUM)) {
                val fragmentEnum = intent.getSerializableExtra(EXTRA_FRAGMENT_ENUM) as AvailableFragment?
                setFragment(fragmentEnum)
            } else if (BackgroundService.ACTION_DEVICE_ACQUAINTANCE == intent.action
                && intent.hasExtra(BackgroundService.EXTRA_DEVICE)
                && intent.hasExtra(BackgroundService.EXTRA_DEVICE_ADDRESS)
            ) {
                val device: Device = intent.getParcelableExtra(BackgroundService.EXTRA_DEVICE)
                val address: DeviceAddress = intent.getParcelableExtra(BackgroundService.EXTRA_DEVICE_ADDRESS)
                handleResult(device, address)
            } else if (BackgroundService.ACTION_INCOMING_TRANSFER_READY == intent.action
                && intent.hasExtra(BackgroundService.EXTRA_TRANSFER)
            ) {
                TransferDetailActivity.startInstance(
                    this@AddDeviceActivity,
                    intent.getParcelableExtra(BackgroundService.EXTRA_TRANSFER)
                )
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.hasExtra(EXTRA_CONNECTION_MODE) == true) {
            connectionMode = intent.getSerializableExtra(EXTRA_CONNECTION_MODE) as ConnectionMode
        }

        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_connection_manager)

        val deviceListArgs = Bundle()
        deviceListArgs.putStringArrayList(
            DeviceListFragment.ARG_HIDDEN_DEVICES_LIST,
            arrayListOf(Device.Type.Web.toString())
        )
        val factory = supportFragmentManager.fragmentFactory
        appBarLayout = findViewById(R.id.app_bar)
        progressBar = findViewById(R.id.activity_connection_establishing_progress_bar)
        toolbar = findViewById(R.id.toolbar)
        optionsFragment = factory.instantiate(classLoader, OptionsFragment::class.java.name) as OptionsFragment
        networkManagerFragment = factory.instantiate(
            classLoader, NetworkManagerFragment::class.java.name
        ) as NetworkManagerFragment
        deviceListFragment = factory.instantiate(classLoader, DeviceListFragment::class.java.name) as DeviceListFragment
        deviceListFragment.arguments = deviceListArgs
        filter.addAction(ACTION_CHANGE_FRAGMENT)
        filter.addAction(BackgroundService.ACTION_DEVICE_ACQUAINTANCE)
        filter.addAction(BackgroundService.ACTION_INCOMING_TRANSFER_READY)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        checkFragment()
        registerReceiver(selfReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(selfReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null)
            if (requestCode == REQUEST_BARCODE_SCAN) {
                val device: Device = data.getParcelableExtra(BarcodeScannerActivity.EXTRA_DEVICE)
                val address: DeviceAddress = data.getParcelableExtra(BarcodeScannerActivity.EXTRA_DEVICE_ADDRESS)

                handleResult(device, address)
            } else if (requestCode == REQUEST_IP_DISCOVERY) {
                val device: Device = data.getParcelableExtra(ManualConnectionActivity.EXTRA_DEVICE)
                val address: DeviceAddress = data.getParcelableExtra(ManualConnectionActivity.EXTRA_DEVICE_ADDRESS)

                handleResult(device, address)
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

    fun applyViewChanges(fragment: Fragment) {
        val isOptions = fragment is OptionsFragment
        if (supportActionBar != null) {
            toolbar.title =
                if (fragment is TitleProvider)
                    fragment.getDistinctiveTitle(this@AddDeviceActivity)
                else
                    getString(R.string.text_chooseDevice)
        }
        appBarLayout.setExpanded(isOptions, true)
    }

    private fun checkFragment() {
        val currentFragment = getShowingFragment()
        if (currentFragment == null) setFragment(AvailableFragment.Options) else applyViewChanges(currentFragment)
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar {
        return Snackbar.make(
            findViewById(R.id.activity_connection_establishing_content_view),
            getString(resId, *objects), Snackbar.LENGTH_LONG
        )
    }

    fun getShowingFragmentId(): AvailableFragment {
        val fragment = getShowingFragment()
        if (fragment is NetworkManagerFragment)
            return AvailableFragment.GenerateQrCode
        else if (fragment is DeviceListFragment)
            return AvailableFragment.AllDevices

        // Probably OptionsFragment
        return AvailableFragment.Options
    }

    fun getShowingFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_connection_establishing_content_view)
    }

    private fun handleResult(device: Device, address: DeviceAddress?) {
        if (ConnectionMode.Return == connectionMode) returnResult(
            this,
            device,
            address
        ) else if (ConnectionMode.WaitForRequests == connectionMode) createSnackbar(R.string.mesg_completing).show()
    }

    fun setFragment(fragment: AvailableFragment?) {
        val activeFragment = getShowingFragment()
        val fragmentCandidate = when (fragment) {
            AvailableFragment.EnterAddress -> {
                startManualConnectionActivity()
                return
            }
            AvailableFragment.ScanQrCode -> {
                startCodeScanner()
                return
            }
            AvailableFragment.GenerateQrCode -> networkManagerFragment
            AvailableFragment.AllDevices -> deviceListFragment
            AvailableFragment.Options -> optionsFragment
            else -> optionsFragment
        }
        if (activeFragment == null || fragmentCandidate !== activeFragment) {
            val transaction = supportFragmentManager.beginTransaction()
            if (activeFragment != null) transaction.remove(activeFragment)
            if (activeFragment != null && fragmentCandidate is OptionsFragment) transaction.setCustomAnimations(
                R.anim.enter_from_left,
                R.anim.exit_to_right
            ) else transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
            transaction.add(R.id.activity_connection_establishing_content_view, fragmentCandidate)
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
            savedInstanceState: Bundle?,
        ): View {
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
            if (context != null)
                context?.sendBroadcast(Intent(ACTION_CHANGE_FRAGMENT).putExtra(EXTRA_FRAGMENT_ENUM, fragment))
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