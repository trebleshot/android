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
package org.monora.uprotocol.client.android.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.ContentBrowserActivity
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListTransferBinding
import org.monora.uprotocol.client.android.fragment.TransferHistoryAdapter.ClickType
import org.monora.uprotocol.client.android.viewholder.TransferDetailViewHolder
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.TransferManagerViewModel
import org.monora.uprotocol.client.android.viewmodel.TransfersViewModel
import org.monora.uprotocol.client.android.viewmodel.content.TransferStateContentViewModel

@AndroidEntryPoint
class TransferHistoryFragment : Fragment(R.layout.layout_transfer_history) {
    private val clientPickerViewModel: ClientPickerViewModel by viewModels()

    private val managerViewModel: TransferManagerViewModel by viewModels()

    private val viewModel: TransfersViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val gibSubscriberListener = { transferDetail: TransferDetail ->
            viewModel.subscribe(transferDetail)
        }
        val adapter = TransferHistoryAdapter(gibSubscriberListener) { detail, clickType ->
            lifecycleScope.launch {
                val transfer = viewModel.getTransfer(detail.id) ?: return@launch
                when (clickType) {
                    ClickType.Default -> findNavController().navigate(
                        TransferHistoryFragmentDirections.actionTransferHistoryFragmentToNavTransferDetails(transfer)
                    )
                    ClickType.Reject -> {
                        val client = viewModel.getClient(transfer.clientUid) ?: return@launch
                        managerViewModel.rejectTransferRequest(transfer, client)
                    }
                    else -> {
                        val client = viewModel.getClient(transfer.clientUid) ?: return@launch
                        managerViewModel.toggleTransferOperation(transfer, client, detail)
                    }
                }
            }
        }
        val emptyContentViewModel = EmptyContentViewModel()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.empty_transfers_list)
        emptyView.emptyImage.setImageResource(R.drawable.ic_compare_arrows_white_24dp)
        emptyView.executePendingBindings()
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.sendButton).setOnClickListener {
            startActivity(Intent(it.context, ContentBrowserActivity::class.java))
        }
        view.findViewById<View>(R.id.receiveButton).setOnClickListener {
            findNavController().navigate(TransferHistoryFragmentDirections.actionTransferHistoryFragmentToNavReceive())
        }

        viewModel.transferDetails.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }

        clientPickerViewModel.registerForTransferRequests(viewLifecycleOwner) { transfer, hasPin ->
            recyclerView.smoothScrollToPosition(0)
        }
    }
}

class TransferHistoryAdapter(
    private val gibSubscriberListener: (detail: TransferDetail) -> LiveData<TransferStateContentViewModel>,
    private val clickListener: (detail: TransferDetail, clickType: ClickType) -> Unit
) : ListAdapter<TransferDetail, ViewHolder>(TransferDetailItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == VIEW_TYPE_TRANSFER) {
            return TransferDetailViewHolder(
                gibSubscriberListener,
                clickListener,
                ListTransferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            throw UnsupportedOperationException()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is TransferDetailViewHolder) {
            holder.bind(getItem(position))
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is TransferDetailViewHolder) {
            holder.onAppear()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is TransferDetailViewHolder) {
            holder.onDisappear()
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is TransferDetailViewHolder) {
            holder.onDestroy()
        }
    }

    enum class ClickType {
        Default,
        ToggleTask,
        Reject,
    }

    companion object {
        const val VIEW_TYPE_TRANSFER = 0
    }
}

class TransferDetailItemCallback : DiffUtil.ItemCallback<TransferDetail>() {
    override fun areItemsTheSame(oldItem: TransferDetail, newItem: TransferDetail): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: TransferDetail, newItem: TransferDetail): Boolean {
        return oldItem == newItem
    }
}
