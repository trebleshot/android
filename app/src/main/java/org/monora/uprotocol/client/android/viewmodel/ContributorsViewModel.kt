package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.GitHubDataRepository
import org.monora.uprotocol.client.android.remote.model.Contributor
import javax.inject.Inject

@HiltViewModel
class ContributorsViewModel @Inject internal constructor(
    gitHubDataRepository: GitHubDataRepository,
) : ViewModel() {
    val contributors = liveData(viewModelScope.coroutineContext) {
        try {
            emit(gitHubDataRepository.getContributors())
        } catch (e: Exception) {
            emit(emptyList<Contributor>())
        }
    }
}