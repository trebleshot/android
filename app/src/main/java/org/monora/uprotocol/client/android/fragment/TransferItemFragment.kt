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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.genonbeta.android.framework.util.Files
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.databinding.LayoutTransferItemBinding
import org.monora.uprotocol.client.android.databinding.ListSectionTitleBinding
import org.monora.uprotocol.client.android.databinding.ListTransferItemBinding
import org.monora.uprotocol.client.android.model.TitleSectionContentModel
import org.monora.uprotocol.client.android.viewholder.TitleSectionViewHolder
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.core.persistence.PersistenceProvider.*
import org.monora.uprotocol.core.transfer.TransferItem
import javax.inject.Inject

@AndroidEntryPoint
class TransferItemFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var factory: ItemViewModel.Factory

    private val args: TransferItemFragmentArgs by navArgs()

    private val viewModel: ItemViewModel by viewModels {
        ItemViewModel.ModelFactory(factory, args.transfer)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_transfer_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ItemAdapter()
        val binding = LayoutTransferItemBinding.bind(view)
        val emptyContentViewModel = EmptyContentViewModel()

        binding.emptyView.viewModel = emptyContentViewModel
        binding.emptyView.emptyText.setText(R.string.text_listEmptyTextStream)
        binding.emptyView.emptyImage.setImageResource(R.drawable.ic_short_text_white_24dp)
        binding.emptyView.executePendingBindings()
        adapter.setHasStableIds(true)
        binding.recyclerView.adapter = adapter

        viewModel.items.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(binding.recyclerView, it.isNotEmpty())
        }
    }
}

class ItemViewModel @AssistedInject internal constructor(
    transferRepository: TransferRepository,
    @Assisted val transfer: Transfer,
) : ViewModel() {
    val items = transferRepository.getTransferItems(transfer.id)

    @AssistedFactory
    interface Factory {
        fun create(transfer: Transfer): ItemViewModel
    }

    class ModelFactory(
        private val factory: Factory,
        private val transfer: Transfer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(ItemViewModel::class.java)) {
                "Unknown type of view model was requested"
            }
            return factory.create(transfer) as T
        }
    }
}

class ItemContentViewModel(val transferItem: UTransferItem) {
    val name = transferItem.name

    val size = Files.formatLength(transferItem.size, false)

    val state = when (transferItem.state) {
        TransferItem.State.InvalidatedTemporarily -> "Interrupted"
        TransferItem.State.Invalidated -> "Removed"
        TransferItem.State.Done -> "Completed"
        else -> "Pending"
    }
}

class ItemViewHolder(private val binding: ListTransferItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(transferItem: UTransferItem) {
        binding.viewModel = ItemContentViewModel(transferItem)
        binding.executePendingBindings()
    }
}

class ItemCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem
    }
}

class ItemAdapter : ListAdapter<Any, ViewHolder>(ItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        VIEW_TYPE_TRANSFER_ITEM -> ItemViewHolder(
            ListTransferItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        VIEW_TYPE_SECTION -> TitleSectionViewHolder(
            ListSectionTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        else -> throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is UTransferItem -> if (holder is ItemViewHolder) holder.bind(item)
            is TitleSectionContentModel -> if (holder is TitleSectionViewHolder) holder.bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is UTransferItem -> VIEW_TYPE_TRANSFER_ITEM
        is TitleSectionContentModel -> VIEW_TYPE_SECTION
        else -> throw UnsupportedOperationException()
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    companion object {
        const val VIEW_TYPE_SECTION = 0

        const val VIEW_TYPE_TRANSFER_ITEM = 1
    }
}