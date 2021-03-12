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
import org.monora.uprotocol.client.android.activity.result.contract.PickClient
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.databinding.LayoutConnectionOptionsBinding
import org.monora.uprotocol.client.android.fragment.ClientsFragment
import org.monora.uprotocol.client.android.fragment.NetworkManagerFragment
import org.monora.uprotocol.client.android.fragment.OnlineClientsFragment
import org.monora.uprotocol.client.android.viewmodel.ClientsViewModel
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import java.util.*

@AndroidEntryPoint
class PickClientActivity : Activity() {
    private lateinit var networkManagerFragment: NetworkManagerFragment

    private lateinit var clientsFragment: ClientsFragment

    private lateinit var optionsFragment: OptionsFragment

    private val pickClient = registerForActivityResult(PickClient()) { clientRoute ->
        if (clientRoute == null) return@registerForActivityResult

        if (PickClient.ConnectionMode.Return == connectionMode) {
            PickClient.returnResult(this, clientRoute.client, clientRoute.address)
        } else if (PickClient.ConnectionMode.WaitForRequests == connectionMode) {
            // TODO: 3/13/21 Snackbar creation was here.
            //createSnackbar(R.string.mesg_completing).show()
        }
    }

    private val toolbar: Toolbar by lazy {
        findViewById(R.id.toolbar)
    }

    private val connectionMode by lazy {
        intent?.getSerializableExtra(PickClient.EXTRA_CONNECTION_MODE) ?: PickClient.ConnectionMode.WaitForRequests
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_pick_client)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val factory = supportFragmentManager.fragmentFactory
        optionsFragment = factory.instantiate(classLoader, OptionsFragment::class.java.name) as OptionsFragment
        networkManagerFragment = factory.instantiate(classLoader,
            NetworkManagerFragment::class.java.name
        ) as NetworkManagerFragment
        clientsFragment = factory.instantiate(classLoader, ClientsFragment::class.java.name) as ClientsFragment
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
            when (v.id) {
                R.id.clientsButton -> {
                }
                R.id.generateQrCodeButton -> {
                }
                R.id.manualAddressButton -> {
                }
                R.id.scanQrCodeButton -> {
                }
                else -> {
                }
            }
        }

        connectionOptions.executePendingBindings()
        clientsViewModel.onlineClients.observe(viewLifecycleOwner) {
            adapter.submitList(it.map { clientRoute -> clientRoute.client })
            emptyContentViewModel.with(connectionOptions.recyclerView, it.isNotEmpty())
            TransitionManager.beginDelayedTransition(connectionOptions.emptyView.root.parent as ViewGroup)
        }
    }
}