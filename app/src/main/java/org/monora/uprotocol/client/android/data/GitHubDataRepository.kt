package org.monora.uprotocol.client.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.monora.uprotocol.client.android.remote.GitHubService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubDataRepository @Inject constructor(
    private val gitHubService: GitHubService,
) {
    suspend fun getContributors() = withContext(Dispatchers.IO) { gitHubService.contributors() }

    suspend fun getReleases() = withContext(Dispatchers.IO) { gitHubService.releases() }
}