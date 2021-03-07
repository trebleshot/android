package org.monora.uprotocol.client.android.viewmodel

import android.widget.TextView
import androidx.databinding.adapters.TextViewBindingAdapter
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.UserDataRepository
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    userDataRepository: UserDataRepository,
) : ViewModel() {
    val client = userDataRepository.client()
}