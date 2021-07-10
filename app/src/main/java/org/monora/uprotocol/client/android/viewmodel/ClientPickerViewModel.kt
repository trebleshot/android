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
import dagger.hilt.android.lifecycle.HiltViewModel
import org.apache.commons.lang3.tuple.MutablePair
import org.monora.uprotocol.core.CommunicationBridge
import javax.inject.Inject

typealias StatefulBridge = MutablePair<Boolean, CommunicationBridge>

@HiltViewModel
class ClientPickerViewModel @Inject internal constructor() : ViewModel() {
    val bridge = MutableLiveData<StatefulBridge>()
}

fun StatefulBridge.consume(): CommunicationBridge? {
    val (used, bridge) = this
    return if (used) null else {
        this.setLeft(true)
        bridge
    }
}

fun StatefulBridge.isValid(): Boolean = !this.left