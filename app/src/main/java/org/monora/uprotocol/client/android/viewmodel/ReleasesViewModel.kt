package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.GitHubDataRepository
import org.monora.uprotocol.client.android.remote.model.Release
import javax.inject.Inject

@HiltViewModel
class ReleasesViewModel @Inject internal constructor(
    gitHubDataRepository: GitHubDataRepository,
) : ViewModel() {
    val releases = liveData(viewModelScope.coroutineContext) {
        try {
            emit(gitHubDataRepository.getReleases())
        } catch (e: Exception) {
            emit(emptyList<Release>())
        }
    }
}