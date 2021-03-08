package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.SharedTextRepository
import javax.inject.Inject

@HiltViewModel
class SharedTextsViewModel @Inject internal constructor(
    sharedTextRepository: SharedTextRepository,
) : ViewModel() {
    val sharedTexts = sharedTextRepository.getSharedTexts()
}