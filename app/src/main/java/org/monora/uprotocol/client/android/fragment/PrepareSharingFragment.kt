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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.genonbeta.android.framework.io.OpenableContent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.databinding.LayoutPrepareSharingBinding
import org.monora.uprotocol.core.protocol.Direction
import java.lang.ref.WeakReference
import java.text.Collator
import java.util.*
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class PrepareSharingFragment : Fragment(R.layout.layout_prepare_sharing) {
    private val preparationViewModel: PreparationViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = activity
        val intent = activity?.intent

        if (activity != null && intent != null) {
            if (Intent.ACTION_SEND == intent.action && intent.hasExtra(Intent.EXTRA_TEXT)) {
                findNavController().navigate(
                    PrepareSharingFragmentDirections.actionPrepareSharingFragmentToNavTextEditor(
                        text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    )
                )
            } else {
                val list: List<Uri>? = try {
                    when (intent.action) {
                        Intent.ACTION_SEND -> (intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri?)?.let {
                            Collections.singletonList(it)
                        }
                        Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                        else -> null
                    }
                } catch (e: Throwable) {
                    null
                }

                if (list.isNullOrEmpty()) {
                    activity.finish()
                    return
                }

                preparationViewModel.consume(list)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutPrepareSharingBinding.bind(view)

        binding.button.setOnClickListener {
            if (!findNavController().navigateUp()) activity?.finish()
        }

        preparationViewModel.shared.observe(viewLifecycleOwner) {
            when (it) {
                is PreparationState.Progress -> {
                    binding.progressBar.max = it.total

                    if (Build.VERSION.SDK_INT >= 24) {
                        binding.progressBar.setProgress(it.index, true)
                    } else {
                        binding.progressBar.progress = it.index
                    }
                }
                is PreparationState.Ready -> {
                    findNavController().navigate(
                        PrepareSharingFragmentDirections.actionPrepareSharingFragmentToSharingFragment(
                            it.list.toTypedArray(), it.id
                        )
                    )
                }
            }
        }
    }
}

@HiltViewModel
class PreparationViewModel @Inject internal constructor(
    @ApplicationContext context: Context,
) : ViewModel() {
    private var consumer: Job? = null

    private val context = WeakReference(context)

    val shared = MutableLiveData<PreparationState>()

    @Synchronized
    fun consume(contents: List<Uri>) {
        if (consumer != null) return

        consumer = viewModelScope.launch(Dispatchers.IO) {
            val groupId = Random.nextLong()
            val list = mutableListOf<UTransferItem>()
            val direction = Direction.Outgoing

            contents.forEachIndexed { index, uri ->
                val context = context.get() ?: return@launch
                val id = index.toLong()

                OpenableContent.from(context, uri).runCatching {
                    shared.postValue(PreparationState.Progress(index, contents.size, name))
                    list.add(UTransferItem(id, groupId, name, mimeType, size, null, uri.toString(), direction))
                }
            }

            val collator = Collator.getInstance()
            list.sortWith { o1, o2 -> collator.compare(o1.name, o2.name) }

            shared.postValue(PreparationState.Ready(groupId, list))

            consumer = null
        }
    }
}

sealed class PreparationState {
    class Progress(val index: Int, val total: Int, val title: String) : PreparationState()

    class Ready(val id: Long, val list: List<UTransferItem>) : PreparationState()
}
