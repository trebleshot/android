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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.databinding.LayoutClientDetailBinding
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ClientDetailsFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var factory: ClientDetailsViewModel.Factory

    private val args: ClientDetailsFragmentArgs by navArgs()

    private val viewModel: ClientDetailsViewModel by viewModels {
        ClientDetailsViewModel.ModelFactory(factory, args.client)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_client_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutClientDetailBinding.bind(view)

        viewModel.client.observe(viewLifecycleOwner) {
            if (it == null) {
                findNavController().navigateUp()
                return@observe
            }

            binding.viewModel = ClientContentViewModel(args.client)
            binding.executePendingBindings()
        }
    }
}

class ClientDetailsViewModel @AssistedInject internal constructor(
    clientRepository: ClientRepository,
    @Assisted client: UClient,
) : ViewModel() {
    val client = clientRepository.get(client.uid)

    @AssistedFactory
    interface Factory {
        fun create(client: UClient): ClientDetailsViewModel
    }

    class ModelFactory(
        private val factory: Factory,
        private val client: UClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(ClientDetailsViewModel::class.java)) {
                "Unknown type of view model requested"
            }
            return factory.create(client) as T
        }
    }
}