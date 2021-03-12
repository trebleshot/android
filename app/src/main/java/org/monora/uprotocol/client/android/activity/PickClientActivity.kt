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
package org.monora.uprotocol.client.android.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.TransitionManager
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.PickClientActivity.AvailableFragment.*
import org.monora.uprotocol.client.android.activity.PickClientActivity.Companion.ACTION_CHANGE_FRAGMENT
import org.monora.uprotocol.client.android.activity.PickClientActivity.Companion.EXTRA_FRAGMENT_ENUM
import org.monora.uprotocol.client.android.activity.result.contract.PickClient
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.databinding.LayoutConnectionOptionsBinding
import org.monora.uprotocol.client.android.fragment.ClientsFragment
import org.monora.uprotocol.client.android.fragment.NetworkManagerFragment
import org.monora.uprotocol.client.android.fragment.OnlineClientsFragment
import org.monora.uprotocol.client.android.receiver.BgBroadcastReceiver
import org.monora.uprotocol.client.android.viewmodel.ClientsViewModel
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import java.util.*

@AndroidEntryPoint
class PickClientActivity : Activity(), SnackbarPlacementProvider {
    private val filter = IntentFilter()

    private lateinit var networkManagerFragment: NetworkManagerFragment

    private lateinit var clientsFragment: ClientsFragment

    private lateinit var optionsFragment: OptionsFragment

    private val pickClient = registerForActivityResult(PickClient()) { clientRoute ->
        if (clientRoute == null) return@registerForActivityResult

        if (PickClient.ConnectionMode.Return == connectionMode) {
            PickClient.returnResult(this, clientRoute.client, clientRoute.address)
        } else if (PickClient.ConnectionMode.WaitForRequests == connectionMode) {
            createSnackbar(R.string.mesg_completing).show()
        }
    }

    private val toolbar: Toolbar by lazy {
        findViewById(R.id.toolbar)
    }

    private val connectionMode by lazy {
        intent?.getSerializableExtra(PickClient.EXTRA_CONNECTION_MODE) ?: PickClient.ConnectionMode.WaitForRequests
    }

    private val selfReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_CHANGE_FRAGMENT == intent.action && intent.hasExtra(EXTRA_FRAGMENT_ENUM)) {
                val fragmentEnum = intent.getSerializableExtra(EXTRA_FRAGMENT_ENUM) as AvailableFragment?
                setFragment(fragmentEnum)
            } else if (BgBroadcastReceiver.ACTION_INCOMING_TRANSFER_READY == intent.action
                && intent.hasExtra(BgBroadcastReceiver.EXTRA_TRANSFER)
            ) {
                TransferDetailActivity.startInstance(
                    this@PickClientActivity,
                    intent.getParcelableExtra(BgBroadcastReceiver.EXTRA_TRANSFER)
                )
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_add_device)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val factory = supportFragmentManager.fragmentFactory
        optionsFragment = factory.instantiate(classLoader, OptionsFragment::class.java.name) as OptionsFragment
        networkManagerFragment = factory.instantiate(classLoader,
            NetworkManagerFragment::class.java.name
        ) as NetworkManagerFragment
        clientsFragment = factory.instantiate(classLoader, ClientsFragment::class.java.name) as ClientsFragment

        filter.addAction(ACTION_CHANGE_FRAGMENT)
        filter.addAction(BgBroadcastReceiver.ACTION_INCOMING_TRANSFER_READY)
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

    override fun onBackPressed() {
        if (getShowingFragment() is OptionsFragment) {
            super.onBackPressed()
        } else {
            setFragment(Options)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun applyViewChanges(fragment: Fragment) {
        if (supportActionBar != null) {
            val titleRes = when (fragment) {
                is NetworkManagerFragment -> R.string.butn_generateQrCode
                else -> R.string.text_chooseClient
            }

            toolbar.title = getString(titleRes)
        }
    }

    private fun checkFragment() {
        val currentFragment = getShowingFragment()
        if (currentFragment == null) setFragment(Options) else applyViewChanges(currentFragment)
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar {
        return Snackbar.make(
            findViewById(R.id.activity_connection_establishing_content_view),
            getString(resId, *objects), Snackbar.LENGTH_LONG
        )
    }

    private fun getShowingFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.activity_connection_establishing_content_view)
    }

    fun setFragment(fragment: AvailableFragment?) {
        val activeFragment = getShowingFragment()
        val fragmentCandidate = when (fragment) {
            EnterAddress -> {
                pickClient.launch(PickClient.ConnectionMode.Manual)
                return
            }
            ScanQrCode -> {
                pickClient.launch(PickClient.ConnectionMode.Barcode)
                return
            }
            GenerateQrCode -> networkManagerFragment
            AllDevices -> clientsFragment
            Options -> optionsFragment
            else -> optionsFragment
        }
        if (activeFragment == null || fragmentCandidate !== activeFragment) {
            val transaction = supportFragmentManager.beginTransaction()
            if (activeFragment != null) {
                transaction.remove(activeFragment)
            }
            if (activeFragment != null && fragmentCandidate is OptionsFragment) {
                transaction.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            } else {
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left)
            }
            transaction.add(R.id.activity_connection_establishing_content_view, fragmentCandidate)
            transaction.commit()
            applyViewChanges(fragmentCandidate)
        }
    }

    enum class AvailableFragment {
        Options, GenerateQrCode, AllDevices, ScanQrCode, EnterAddress
    }

    companion object {
        const val ACTION_CHANGE_FRAGMENT = "com.genonbeta.intent.action.CHANGE_FRAGMENT"

        const val EXTRA_FRAGMENT_ENUM = "extraFragmentEnum"
    }
}

@AndroidEntryPoint
class OptionsFragment : Fragment(R.layout.layout_connection_options) {
    private val clientsViewModel: ClientsViewModel by viewModels()

    private val emptyContentViewModel: EmptyContentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val connectionOptions = LayoutConnectionOptionsBinding.bind(view)
        val adapter = OnlineClientsFragment.Adapter()

        adapter.setHasStableIds(true)
        connectionOptions.emptyView.emptyText.setText(R.string.text_noOnlineDevices)
        connectionOptions.emptyView.emptyImage.setImageResource(R.drawable.ic_devices_white_24dp)
        connectionOptions.emptyView.viewModel = emptyContentViewModel
        connectionOptions.recyclerView.adapter = adapter
        connectionOptions.recyclerView.layoutManager?.let {
            if (it is GridLayoutManager) {
                it.orientation = GridLayoutManager.HORIZONTAL
            }
        }
        connectionOptions.clickListener = View.OnClickListener { v: View ->
            updateFragment(
                when (v.id) {
                    R.id.clientsButton -> AllDevices
                    R.id.generateQrCodeButton -> GenerateQrCode
                    R.id.manualAddressButton -> EnterAddress
                    R.id.scanQrCodeButton -> ScanQrCode
                    else -> Options
                }
            )
        }

        connectionOptions.executePendingBindings()
        clientsViewModel.onlineClients.observe(viewLifecycleOwner) {
            adapter.submitList(it.map { clientRoute -> clientRoute.client })
            emptyContentViewModel.with(connectionOptions.recyclerView, it.isNotEmpty())
            TransitionManager.beginDelayedTransition(connectionOptions.emptyView.root.parent as ViewGroup)
        }
    }

    private fun updateFragment(fragment: PickClientActivity.AvailableFragment) {
        context?.sendBroadcast(Intent(ACTION_CHANGE_FRAGMENT).putExtra(EXTRA_FRAGMENT_ENUM, fragment))
    }
}