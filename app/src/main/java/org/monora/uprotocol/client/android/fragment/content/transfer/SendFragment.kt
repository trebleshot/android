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

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutSendBinding
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingState
import org.monora.uprotocol.client.android.viewmodel.SharingViewModel

@AndroidEntryPoint
class SendFragment : Fragment(R.layout.layout_send) {
    private val args: SendFragmentArgs by navArgs()

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    private val sharingViewModel: SharingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutSendBinding.bind(view)

        binding.retryButton.setOnClickListener {
            findNavController().navigate(SendFragmentDirections.pickClient())
        }

        sharingViewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is SharingState.Initial -> if (it.consume()) findNavController().navigate(
                    SendFragmentDirections.pickClient()
                )
                is SharingState.Running -> {
                    binding.retryViewsGroup.visibility = View.GONE
                    binding.progressBar.visibility = View.VISIBLE
                }
                is SharingState.Error -> {
                    binding.retryViewsGroup.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
                is SharingState.Success -> findNavController().navigate(
                    SendFragmentDirections.actionSendFragmentToNavTransferDetails(it.transfer)
                )
            }
        }

        clientPickerViewModel.bridge.observe(viewLifecycleOwner) { bridge ->
            sharingViewModel.consume(bridge, args.groupId, args.items.toList())
        }
    }
}
