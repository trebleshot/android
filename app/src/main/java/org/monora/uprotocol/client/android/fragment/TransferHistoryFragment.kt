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

import android.annotation.SuppressLint
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
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.client.android.database.model.WebTransfer
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListSectionDateBinding
import org.monora.uprotocol.client.android.databinding.ListSharedTextBinding
import org.monora.uprotocol.client.android.databinding.ListTransferBinding
import org.monora.uprotocol.client.android.databinding.ListWebTransferBinding
import org.monora.uprotocol.client.android.model.DateSectionContentModel
import org.monora.uprotocol.client.android.model.ListItem
import org.monora.uprotocol.client.android.viewholder.DateSectionViewHolder
import org.monora.uprotocol.client.android.viewholder.SharedTextViewHolder
import org.monora.uprotocol.client.android.viewholder.TransferDetailViewHolder
import org.monora.uprotocol.client.android.viewholder.WebTransferViewHolder
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
        val adapter = TransferHistoryAdapter(gibSubscriberListener) {
            when (it) {
                is TransferHistoryAdapter.Click.Local -> {
                    lifecycleScope.launch {
                        val transfer = viewModel.getTransfer(it.detail.id) ?: return@launch
                        when (it.clickType) {
                            TransferDetailViewHolder.ClickType.Default -> findNavController().navigate(
                                TransferHistoryFragmentDirections.actionTransferHistoryFragmentToNavTransferDetails(
                                    transfer
                                )
                            )
                            TransferDetailViewHolder.ClickType.Reject -> {
                                val client = viewModel.getClient(transfer.clientUid) ?: return@launch
                                managerViewModel.rejectTransferRequest(transfer, client)
                            }
                            else -> {
                                val client = viewModel.getClient(transfer.clientUid) ?: return@launch
                                managerViewModel.toggleTransferOperation(transfer, client, it.detail)
                            }
                        }
                    }
                }
                is TransferHistoryAdapter.Click.Text -> findNavController().navigate(
                    TransferHistoryFragmentDirections.actionTransferHistoryFragmentToNavTextEditor(
                        sharedText = it.text
                    )
                )
                is TransferHistoryAdapter.Click.Web -> findNavController().navigate(
                    TransferHistoryFragmentDirections.actionTransferHistoryFragmentToWebTransferDetailsFragment(
                        it.transfer
                    )
                )
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

        viewModel.transfers.observe(viewLifecycleOwner) {
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
    private val clickListener: (click: Click) -> Unit
) : ListAdapter<ListItem, ViewHolder>(TransfersItemCallback()) {
    private val detailClick: (TransferDetail, TransferDetailViewHolder.ClickType) -> Unit = { detail, clickType ->
        clickListener(Click.Local(detail, clickType))
    }

    private val textClick: (SharedText) -> Unit = {
        clickListener(Click.Text(it))
    }

    private val webClick: (WebTransfer) -> Unit = {
        clickListener(Click.Web(it))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION -> DateSectionViewHolder(
                ListSectionDateBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_LOCAL_TRANSFER -> TransferDetailViewHolder(
                gibSubscriberListener, detailClick, ListTransferBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_SHARED_TEXT -> SharedTextViewHolder(
                ListSharedTextBinding.inflate(inflater, parent, false),
                textClick,
            )
            VIEW_TYPE_WEB_TRANSFER -> WebTransferViewHolder(
                ListWebTransferBinding.inflate(inflater, parent, false),
                webClick,
            )
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is DateSectionViewHolder -> if (item is DateSectionContentModel) holder.bind(item)
            is TransferDetailViewHolder -> if (item is TransferDetail) holder.bind(item)
            is SharedTextViewHolder -> if (item is SharedText) holder.bind(item)
            is WebTransferViewHolder -> if (item is WebTransfer) holder.bind(item)
            else -> throw IllegalStateException()
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

    override fun getItemId(position: Int): Long {
        return getItem(position).listId
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DateSectionContentModel -> VIEW_TYPE_SECTION
            is TransferDetail -> VIEW_TYPE_LOCAL_TRANSFER
            is SharedText -> VIEW_TYPE_SHARED_TEXT
            is WebTransfer -> VIEW_TYPE_WEB_TRANSFER
            else -> throw IllegalStateException()
        }
    }

    sealed class Click {
        class Local(val detail: TransferDetail, val clickType: TransferDetailViewHolder.ClickType) : Click()

        class Text(val text: SharedText) : Click()

        class Web(val transfer: WebTransfer) : Click()
    }

    companion object {
        const val VIEW_TYPE_SECTION = 0

        const val VIEW_TYPE_LOCAL_TRANSFER = 1

        const val VIEW_TYPE_SHARED_TEXT = 2

        const val VIEW_TYPE_WEB_TRANSFER = 3
    }
}

class TransfersItemCallback : DiffUtil.ItemCallback<ListItem>() {
    override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem.listId == newItem.listId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
        return oldItem == newItem
    }
}
