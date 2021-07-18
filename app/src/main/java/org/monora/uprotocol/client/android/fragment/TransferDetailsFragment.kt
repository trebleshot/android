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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutTransferDetailsBinding
import org.monora.uprotocol.client.android.viewmodel.TransferDetailsViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.TransferDetailContentViewModel
import org.monora.uprotocol.core.persistence.PersistenceProvider
import javax.inject.Inject

@AndroidEntryPoint
class TransferDetailsFragment : Fragment(R.layout.layout_transfer_details) {
    @Inject
    lateinit var factory: TransferDetailsViewModel.Factory

    private val args: TransferDetailsFragmentArgs by navArgs()

    private val viewModel: TransferDetailsViewModel by viewModels {
        TransferDetailsViewModel.ModelFactory(factory, args.transfer)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutTransferDetailsBinding.bind(view)

        binding.image.setOnClickListener {
            viewModel.client.value?.let {
                findNavController().navigate(
                    TransferDetailsFragmentDirections.actionTransferDetailsFragmentToClientDetailsFragment2(it)
                )
            }
        }
        binding.showFilesButton.setOnClickListener {
            findNavController().navigate(
                TransferDetailsFragmentDirections.actionTransferDetailsFragmentToTransferItemFragment(args.transfer)
            )
        }

        viewModel.transferDetail.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.transferViewModel = TransferDetailContentViewModel(it)
                binding.executePendingBindings()
            }
        }

        viewModel.client.observe(viewLifecycleOwner) {
            if (it == null) {
                findNavController().popBackStack()
            } else {
                binding.clientViewModel = ClientContentViewModel(it)
                binding.executePendingBindings()
            }
        }

        // TODO: 7/13/21 Remove
        lifecycleScope.launch(Dispatchers.IO) {
            var finishing = true

            repeat(2) {
                viewModel.transferItemDao.getAllDirect(args.transfer.id).forEach {
                    val finished = it.state == PersistenceProvider.STATE_DONE

                    if (finishing == finished) return@forEach

                    it.state = if (finishing) PersistenceProvider.STATE_DONE else PersistenceProvider.STATE_PENDING

                    viewModel.transferItemDao.update(it)
                }

                finishing = !finishing
            }
        }
    }
}