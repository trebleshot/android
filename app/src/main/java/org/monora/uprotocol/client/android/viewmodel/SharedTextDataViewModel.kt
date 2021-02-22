package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.database.model.SharedTextModel
import javax.inject.Inject

@HiltViewModel
class SharedTextDataViewModel @Inject internal constructor(
    sharedTextRepository: SharedTextRepository,
) : ViewModel() {
    val sharedTexts = sharedTextRepository.getSharedTexts()
}