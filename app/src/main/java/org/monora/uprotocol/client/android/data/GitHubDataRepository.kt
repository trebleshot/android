package org.monora.uprotocol.client.android.data

import org.monora.uprotocol.client.android.remote.GitHubService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubDataRepository @Inject constructor(
    private val gitHubService: GitHubService,
) {
    suspend fun getContributors() = gitHubService.contributors()

    suspend fun getReleases() = gitHubService.releases()
}