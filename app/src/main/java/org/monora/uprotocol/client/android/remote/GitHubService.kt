package org.monora.uprotocol.client.android.remote

import org.monora.uprotocol.client.android.config.AppConfig.URI_REPO
import org.monora.uprotocol.client.android.remote.model.Contributor
import org.monora.uprotocol.client.android.remote.model.Release
import retrofit2.http.GET
import retrofit2.http.Headers

interface GitHubService {
    //@Headers("Authorization: Basic dmVsaXRhc2FsaTplMjY0MmM3MzU0ODQwOWJhOGFiYzg2M2NlNDIyODgwYzJmNmE5Yjg1")
    @GET("$URI_REPO/contributors")
    suspend fun contributors(): List<Contributor>

    //@Headers("Authorization: Basic dmVsaXRhc2FsaTplMjY0MmM3MzU0ODQwOWJhOGFiYzg2M2NlNDIyODgwYzJmNmE5Yjg1")
    @GET("$URI_REPO/releases")
    suspend fun releases(): List<Release>
}