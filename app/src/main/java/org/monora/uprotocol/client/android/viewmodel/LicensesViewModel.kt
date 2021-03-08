package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.data.LicensesRepository
import javax.inject.Inject

@HiltViewModel
class LicensesViewModel @Inject internal constructor(
    private val licensesRepository: LicensesRepository,
) : ViewModel() {
    val licenses = liveData(viewModelScope.coroutineContext) {
            emit(licensesRepository.licenses())
    }
}