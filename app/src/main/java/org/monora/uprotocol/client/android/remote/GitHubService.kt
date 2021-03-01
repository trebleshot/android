package org.monora.uprotocol.client.android.remote

import org.monora.uprotocol.client.android.config.AppConfig.URI_REPO
import org.monora.uprotocol.client.android.remote.model.Contributor
import org.monora.uprotocol.client.android.remote.model.Release
import retrofit2.http.GET
import retrofit2.http.Headers

interface GitHubService {
    @GET("$URI_REPO/contributors")
    suspend fun contributors(): List<Contributor>

    @GET("$URI_REPO/releases")
    suspend fun releases(): List<Release>
}