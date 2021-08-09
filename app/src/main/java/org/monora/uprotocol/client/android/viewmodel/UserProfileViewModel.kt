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
import android.net.Uri
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.data.UserDataRepository
import org.monora.uprotocol.client.android.util.Graphics
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject internal constructor(
    @ApplicationContext context: Context,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {
    val client = userDataRepository.client

    var clientNickname
        get() = userDataRepository.clientNickname
        set(value) {
            userDataRepository.clientNickname = value
        }

    private val context = WeakReference(context)

    val deletePictureListener = View.OnClickListener {
        userDataRepository.deletePicture()
    }

    val hasPicture = userDataRepository.hasPicture

    fun saveProfilePicture(uri: Uri) {
        val context = context.get() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageBytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch

                Graphics.saveLocalClientPicture(context, userDataRepository.clientStatic, imageBytes)
                userDataRepository.notifyPictureChanges()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
