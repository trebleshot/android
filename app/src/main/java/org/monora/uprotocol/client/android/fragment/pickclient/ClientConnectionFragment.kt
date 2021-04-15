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

package org.monora.uprotocol.client.android.fragment.pickclient

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutClientConnectionBinding
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ClientConnectionFragment : Fragment(R.layout.layout_client_connection) {
    private val args: ClientConnectionFragmentArgs by navArgs()

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    private val clientConnectionViewModel: ClientConnectionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutClientConnectionBinding.bind(view)
        binding.client = args.client
        binding.image.animation = AnimationUtils.loadAnimation(binding.image.context, R.anim.pulse)

        binding.executePendingBindings()
    }
}

@HiltViewModel
class ClientConnectionViewModel @Inject constructor() : ViewModel() {

}