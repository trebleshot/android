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

package org.monora.uprotocol.client.android.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectionRepository @Inject constructor() {
    private val selections = mutableListOf<Any>()

    private val _selectionState = MutableLiveData<List<Any>>(selections)

    val selectionState = liveData {
        emitSource(_selectionState)
    }

    fun addAll(list: List<Any>) {
        synchronized(selections) {
            selections.addAll(list)
        }
    }

    fun clearSelections() {
        selections.clear()
    }

    fun getSelections() = ArrayList(selections)

    fun setSelected(obj: Any, selected: Boolean) {
        synchronized(selections) {
            val result = if (selected) selections.add(obj) else selections.remove(obj)

            if (result) {
                _selectionState.postValue(selections)
            }
        }
    }

    fun <T : Any> whenContains(list: List<T>, handler: (item: T, selected: Boolean) -> Unit) {
        synchronized(selections) {
            list.forEach {
                handler(it, selections.contains(it))
            }
        }
    }
}
