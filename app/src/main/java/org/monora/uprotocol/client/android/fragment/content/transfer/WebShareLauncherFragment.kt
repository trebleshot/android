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

package org.monora.uprotocol.client.android.fragment.content.transfer

import android.bluetooth.BluetoothA2dp
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.SelectionRepository
import org.monora.uprotocol.client.android.data.WebDataRepository
import org.monora.uprotocol.client.android.databinding.LayoutWebShareLauncherBinding
import org.monora.uprotocol.client.android.databinding.ListConnectionBinding
import org.monora.uprotocol.client.android.fragment.NetworkManagerFragment
import org.monora.uprotocol.client.android.util.Networks.getFirstInet4Address
import org.monora.uprotocol.client.android.util.TextManipulators
import org.monora.uprotocol.client.android.util.TextManipulators.toNetworkTitle
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import java.lang.ref.WeakReference
import java.net.NetworkInterface
import javax.inject.Inject

@AndroidEntryPoint
class WebShareLauncherFragment : BottomSheetDialogFragment() {
    private val viewModel: WebShareViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_web_share_launcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = LayoutWebShareLauncherBinding.bind(view)
        val emptyView = binding.emptyView
        val connectionsAdapter = ConnectionsAdapter()
        val emptyContentViewModel = EmptyContentViewModel()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.empty_connections_list)
        emptyView.emptyImage.setImageResource(R.drawable.ic_ip_white_24dp)
        binding.viewModel = viewModel
        binding.executePendingBindings()
        connectionsAdapter.setHasStableIds(true)
        binding.recyclerView.adapter = connectionsAdapter

        viewModel.changes.observe(viewLifecycleOwner) {
            connectionsAdapter.submitList(it)
            emptyContentViewModel.with(binding.recyclerView, it.isNotEmpty())
        }
    }
}

class ConnectionItemCallback : DiffUtil.ItemCallback<NamedInterface>() {
    override fun areItemsTheSame(oldItem: NamedInterface, newItem: NamedInterface): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: NamedInterface, newItem: NamedInterface): Boolean {
        return oldItem == newItem
    }
}

class ConnectionContentViewModel(context: Context, namedInterface: NamedInterface) {
    val title = namedInterface.title

    val address = TextManipulators.getWebShareAddress(
        context, namedInterface.network.getFirstInet4Address()?.hostAddress
    )
}

class ConnectionViewHolder(private val binding: ListConnectionBinding) : ViewHolder(binding.root) {
    fun bind(namedInterface: NamedInterface) {
        binding.viewModel = ConnectionContentViewModel(binding.root.context, namedInterface)
        binding.executePendingBindings()
    }
}

class ConnectionsAdapter : ListAdapter<NamedInterface, ConnectionViewHolder>(ConnectionItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        return ConnectionViewHolder(
            ListConnectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).network.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_CONNECTION
    }

    companion object {
        const val VIEW_TYPE_CONNECTION = 0
    }
}

@HiltViewModel
class WebShareViewModel @Inject internal constructor(
    @ApplicationContext context: Context,
    selectionRepository: SelectionRepository,
    private val webDataRepository: WebDataRepository,
) : ViewModel() {
    private val context = WeakReference(context)

    private val filter = IntentFilter().apply {
        addAction(NetworkManagerFragment.WIFI_AP_STATE_CHANGED)
        addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
    }

    private val list = selectionRepository.getSelections()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // We don't check for the intent's action type because all of them only trigger a refresh via a life data.
            reloadInterfaces()
        }
    }

    private val _changes = MutableLiveData<List<NamedInterface>>()

    val changes = liveData {
        reloadInterfaces()
        emitSource(_changes)
    }

    val sharedCount = list.size

    private fun reloadInterfaces() {
        val context = context.get() ?: return
        val result = webDataRepository.getNetworkInterfaces().map {
            NamedInterface(it.toNetworkTitle(context), it)
        }
        _changes.value = result
    }

    init {
        context.registerReceiver(receiver, filter)
        webDataRepository.serve(list)
    }

    override fun onCleared() {
        super.onCleared()
        context.get()?.unregisterReceiver(receiver)
        webDataRepository.clear()
    }
}

data class NamedInterface(val title: String, val network: NetworkInterface)
