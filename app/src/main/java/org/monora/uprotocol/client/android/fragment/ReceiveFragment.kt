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
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.genonbeta.android.framework.io.DocumentFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutReceiveBinding
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.FilesViewModel
import org.monora.uprotocol.client.android.viewmodel.consume
import org.monora.uprotocol.core.protocol.Direction

@AndroidEntryPoint
class ReceiveFragment : Fragment(R.layout.layout_receive) {
    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    private val filesViewModel: FilesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = LayoutReceiveBinding.bind(view)

        binding.button.setOnClickListener {
            findNavController().navigate(ReceiveFragmentDirections.pickClient())
        }
        binding.changeStorageButton.setOnClickListener {
            findNavController().navigate(ReceiveFragmentDirections.actionReceiveFragmentToFilePickerFragment())
        }
        binding.storageFolderText.text = filesViewModel.appDirectory.getName()

        setFragmentResultListener(FilePickerFragment.RESULT_FILE_PICKED) { _, bundle ->
            val file = bundle.getParcelable<DocumentFile?>(
                FilePickerFragment.EXTRA_DOCUMENT_FILE
            ) ?: return@setFragmentResultListener

            filesViewModel.appDirectory = file
            binding.storageFolderText.text = filesViewModel.appDirectory.getName()
        }

        clientPickerViewModel.bridge.observe(viewLifecycleOwner) { statefulBridge ->
            val bridge = statefulBridge.consume() ?: return@observe

            binding.progressBar.visibility = View.VISIBLE
            binding.button.isEnabled = false
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val result = bridge.use {
                        it.requestAcquaintance(Direction.Incoming)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    lifecycleScope.launchWhenResumed {
                        binding.progressBar.visibility = View.GONE
                        binding.button.isEnabled = true
                        TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
                    }
                }
            }
        }
    }
}
