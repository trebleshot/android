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

package org.monora.uprotocol.client.android.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.concurrent.SingleLiveEvent
import org.monora.uprotocol.client.android.content.App
import org.monora.uprotocol.client.android.content.Image
import org.monora.uprotocol.client.android.content.Song
import org.monora.uprotocol.client.android.content.Video
import org.monora.uprotocol.client.android.data.SelectionRepository
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.model.TitleSectionContentModel
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class SharingSelectionViewModel @Inject internal constructor(
    @ApplicationContext context: Context,
    private val selectionRepository: SelectionRepository
) : ViewModel() {
    private val context = WeakReference(context)

    val externalState = MutableLiveData<Unit>()

    val selectionState = selectionRepository.selectionState

    override fun onCleared() {
        super.onCleared()
        selectionRepository.clearSelections()
    }

    fun getEditorList() = liveData(Dispatchers.IO) {
        val context = context.get() ?: return@liveData
        val sortedList = selectionRepository.getSelections().sortedWith { o1, o2 ->
            getItemOrder(o1) - getItemOrder(o2)
        }.toMutableList()
        val copyList = ArrayList(sortedList)

        var previous: Any? = null
        var increase = 0
        copyList.forEachIndexed { index, any ->
            if (any.javaClass != previous?.javaClass) {
                val titleRes = when (any) {
                    is App -> R.string.apps
                    is FileModel -> R.string.files
                    is Song -> R.string.songs
                    is Image -> R.string.images
                    is Video -> R.string.videos
                    else -> throw IllegalStateException()
                }
                sortedList.add(index + increase, TitleSectionContentModel(context.getString(titleRes)))
                increase += 1
            }

            previous = any
        }

        emit(sortedList)
    }

    private fun getItemOrder(any: Any): Int {
        return when (any) {
            is App -> 0
            is FileModel -> 1
            is Song -> 2
            is Image -> 3
            is Video -> 4
            else -> throw IllegalStateException()
        }
    }

    fun setSelected(obj: Any, selected: Boolean) = selectionRepository.setSelected(obj, selected)
}
