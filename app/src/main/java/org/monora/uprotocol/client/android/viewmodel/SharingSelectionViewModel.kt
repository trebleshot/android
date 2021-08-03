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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharingSelectionViewModel @Inject internal constructor() : ViewModel() {
    private val selections = mutableListOf<Any>()

    private val _selectionState = MutableLiveData<List<Any>>(emptyList())

    val selectionState = liveData {
        emitSource(_selectionState)
    }

    fun contains(obj: Any) = selections.contains(obj)

    fun getSelections() = selections.toList()

    fun setSelected(obj: Any, selected: Boolean) {
        synchronized(selections) {
            val result = if (selected) selections.add(obj) else selections.remove(obj)

            if (result) {
                _selectionState.postValue(selections)
            }
        }
    }
}
