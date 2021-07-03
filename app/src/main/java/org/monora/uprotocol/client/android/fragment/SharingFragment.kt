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
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.io.StreamInfo
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.databinding.LayoutSharingBinding
import org.monora.uprotocol.client.android.databinding.ListSharingItemBinding
import org.monora.uprotocol.client.android.itemcallback.UTransferItemCallback
import org.monora.uprotocol.client.android.viewmodel.content.TransferItemContentViewModel
import org.monora.uprotocol.core.transfer.TransferItem
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class SharingFragment : Fragment(R.layout.layout_sharing) {
    private val sharingActivityViewModel: SharingActivityViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = LayoutSharingBinding.bind(view)
        val adapter = SharingContentAdapter()

        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener {
            findNavController().navigate(SharingFragmentDirections.pickClient())
        }

        binding.button.setOnClickListener {
            TODO("Implement cancellation button")
        }

        sharingActivityViewModel.shared.observe(viewLifecycleOwner) {
            when (it) {
                is SharingActivityState.Progress -> {
                    binding.textMain.text = it.title
                    binding.progressBar.max = it.total

                    if (Build.VERSION.SDK_INT >= 24) {
                        binding.progressBar.setProgress(it.index, true)
                    } else {
                        binding.progressBar.progress = it.index
                    }
                }
                is SharingActivityState.Ready -> {
                    binding.groupPreparing.visibility = View.GONE
                    binding.listParent.visibility = View.VISIBLE

                    adapter.submitList(it.list)
                }
            }
        }
    }
}

class SharingContentAdapter : ListAdapter<UTransferItem, SharingViewHolder>(UTransferItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharingViewHolder {
        return SharingViewHolder(
            ListSharingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SharingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = with(getItem(position)) { groupId + id }
}

class SharingViewHolder(private val binding: ListSharingItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(transferItem: UTransferItem) {
        binding.viewModel = TransferItemContentViewModel(transferItem)
        binding.executePendingBindings()
    }
}

@HiltViewModel
class SharingActivityViewModel @Inject internal constructor(
    transferRepository: TransferRepository,
) : ViewModel() {
    private var consumer: Job? = null

    val shared = MutableLiveData<SharingActivityState>()

    @Synchronized
    fun consume(context: Context, contents: List<Uri>) {
        if (consumer != null) return

        consumer = viewModelScope.launch(Dispatchers.IO) {
            val id = Random.nextLong()
            val list = mutableListOf<UTransferItem>()

            contents.forEachIndexed { index, it ->
                StreamInfo.from(context, it).runCatching {
                    shared.postValue(SharingActivityState.Progress(index, contents.size, name))
                    list.add(
                        UTransferItem(
                            index.toLong(), id, name, mimeType, size, null, uri.toString(), TransferItem.Type.Outgoing
                        )
                    )
                }
            }

            shared.postValue(SharingActivityState.Ready(id, list))
        }
    }
}

sealed class SharingActivityState {
    class Progress(val index: Int, val total: Int, val title: String) : SharingActivityState()

    class Ready(val id: Long, val list: List<UTransferItem>) : SharingActivityState()
}