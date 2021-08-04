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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.WebDataRepository
import org.monora.uprotocol.client.android.databinding.LayoutWebShareLauncherBinding
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel
import javax.inject.Inject

@AndroidEntryPoint
class WebShareLauncherFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var factory: WebShareViewModel.Factory

    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    private val viewModel: WebShareViewModel by viewModels {
        WebShareViewModel.ModelFactory(factory, selectionViewModel.getSelections())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_web_share_launcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = LayoutWebShareLauncherBinding.bind(view)

        binding.viewModel = viewModel
        binding.executePendingBindings()
    }
}

class WebShareViewModel @AssistedInject internal constructor(
    private val webDataRepository: WebDataRepository,
    @Assisted private val list: List<Any>,
) : ViewModel() {
    val sharedCount = list.size

    init {
        webDataRepository.serve(list)
    }

    override fun onCleared() {
        super.onCleared()
        webDataRepository.clear()
    }

    @AssistedFactory
    interface Factory {
        fun create(selections: List<Any>): WebShareViewModel
    }

    class ModelFactory(
        private val factory: Factory,
        private val selections: List<Any>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(WebShareViewModel::class.java)) {
                "Requested unknown view model type"
            }

            return factory.create(selections) as T
        }
    }
}
