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

import android.view.View
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.UserDataRepository
import org.monora.uprotocol.client.android.util.Graphics
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject internal constructor(
    private val userDataRepository: UserDataRepository,
) : ViewModel() {
    val client = userDataRepository.client()

    val clientStatic
        get() = userDataRepository.clientStatic()
    
    val deletePictureListener = View.OnClickListener {
        Graphics.deleteLocalClientPicture(it.context)
    }
}