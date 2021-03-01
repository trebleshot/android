package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monora.uprotocol.client.android.data.SharedTextRepository
import javax.inject.Inject

@HiltViewModel
class SharedTextDataViewModel @Inject internal constructor(
    sharedTextRepository: SharedTextRepository,
) : ViewModel() {
    val sharedTexts = sharedTextRepository.getSharedTexts()
}