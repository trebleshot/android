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