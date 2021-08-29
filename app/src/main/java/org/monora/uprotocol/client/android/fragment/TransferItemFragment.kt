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

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.Files
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.databinding.LayoutTransferItemBinding
import org.monora.uprotocol.client.android.databinding.ListTransferItemBinding
import org.monora.uprotocol.client.android.protocol.isIncoming
import org.monora.uprotocol.client.android.util.Activities
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.core.protocol.Direction
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

        val adapter = ItemAdapter { item, clickType ->
            when (clickType) {
                ItemAdapter.ClickType.Default -> viewModel.open(requireContext(), item)
                ItemAdapter.ClickType.Recover -> viewModel.recover(item)
            }
        }
        val binding = LayoutTransferItemBinding.bind(view)
        val emptyContentViewModel = EmptyContentViewModel()

        binding.emptyView.viewModel = emptyContentViewModel
        binding.emptyView.emptyText.setText(R.string.empty_files_list)
        binding.emptyView.emptyImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp)
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
    private val transferRepository: TransferRepository,
    @Assisted private val transfer: Transfer,
) : ViewModel() {
    val items = transferRepository.getTransferItems(transfer.id)

    fun open(context: Context, item: UTransferItem) {
        val uri = try {
            Uri.parse(item.location)
        } catch (e: Exception) {
            return
        }

        if (item.direction == Direction.Outgoing || item.state == TransferItem.State.Done) {
            try {
                Activities.view(context, DocumentFile.fromUri(context, uri))
            } catch (e: Exception) {
                Activities.view(context, uri, item.mimeType)
            }
        }
    }

    fun recover(item: UTransferItem) {
        if (item.state == TransferItem.State.InvalidatedTemporarily) {
            viewModelScope.launch {
                val originalState = item.state

                item.state = TransferItem.State.Pending
                transferRepository.update(item)

                // The list will not be refreshed if the DiffUtil finds the values are the same, so we give the
                // original value back (it will be refreshed anyway).
                item.state = originalState
            }
        }
    }

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

class ItemContentViewModel(val transferItem: UTransferItem, context: Context) {
    val name = transferItem.name

    val size = Files.formatLength(transferItem.size, false)

    val mimeType = transferItem.mimeType

    val shouldRecover =
        transferItem.direction.isIncoming && transferItem.state == TransferItem.State.InvalidatedTemporarily

    val state = context.getString(
        when (transferItem.state) {
            TransferItem.State.InvalidatedTemporarily -> R.string.interrupted
            TransferItem.State.Invalidated -> R.string.removed
            TransferItem.State.Done -> R.string.completed
            else -> R.string.pending
        }
    )
}

class ItemViewHolder(
    private val clickListener: (item: UTransferItem, clickType: ItemAdapter.ClickType) -> Unit,
    private val binding: ListTransferItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(transferItem: UTransferItem) {
        binding.viewModel = ItemContentViewModel(transferItem, binding.root.context)
        binding.root.setOnClickListener {
            clickListener(transferItem, ItemAdapter.ClickType.Default)
        }
        binding.recoverButton.setOnClickListener {
            clickListener(transferItem, ItemAdapter.ClickType.Recover)
        }
        binding.executePendingBindings()
    }
}

class ItemCallback : DiffUtil.ItemCallback<UTransferItem>() {
    override fun areItemsTheSame(oldItem: UTransferItem, newItem: UTransferItem): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: UTransferItem, newItem: UTransferItem): Boolean {
        return oldItem.dateModified == newItem.dateModified && oldItem.state == newItem.state
    }
}

class ItemAdapter(
    private val clickListener: (item: UTransferItem, clickType: ClickType) -> Unit,
) : ListAdapter<UTransferItem, ItemViewHolder>(ItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            clickListener,
            ListTransferItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_TRANSFER_ITEM
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).let { it.id + it.groupId }
    }

    enum class ClickType {
        Default,
        Recover,
    }

    companion object {
        const val VIEW_TYPE_TRANSFER_ITEM = 0
    }
}
