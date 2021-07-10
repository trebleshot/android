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
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutTransferDetailsBinding
import org.monora.uprotocol.client.android.viewmodel.TransferDetailsViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel

@AndroidEntryPoint
class TransferDetailsFragment : Fragment(R.layout.layout_transfer_details) {
    private val args: TransferDetailsFragmentArgs by navArgs()

    private val viewModel: TransferDetailsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutTransferDetailsBinding.bind(view)

        binding.progressBar.max = 100
        binding.showFilesButton.setOnClickListener {
            findNavController().navigate(
                TransferDetailsFragmentDirections.actionTransferDetailsFragmentToTransferContentsFragment(args.transfer)
            )
        }

        lifecycleScope.launch {
            repeat(101) {
                binding.progressBar.setProgress(it, true)
                binding.progressText.text = "$it%"
                binding.speedText.text = "${if (it % 4 == 0) 117 else 112}.${(100f * Math.random()).toInt()} Mbps"
                delay(100)
                println("yeah I work")
            }
        }

        viewModel.userRepository.get(args.transfer.clientUid).observe(viewLifecycleOwner) {
            if (it == null) {
                findNavController().popBackStack()
                return@observe
            }

            binding.viewModel = ClientContentViewModel(it)
            binding.executePendingBindings()
        }

        viewModel.transferRepository.getTransferItems(args.transfer.id).observe(viewLifecycleOwner) {
            println(it[0].name)
        }
    }
}