package org.monora.uprotocol.client.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.remote.GitHubService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GitHubModule {
    @GitHubApi
    @Provides
    @Singleton
    fun provideGitHubApiRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConfig.URI_API_GITHUB)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubService(@GitHubApi retrofit: Retrofit): GitHubService {
        return retrofit.create(GitHubService::class.java)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubApi