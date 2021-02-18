package org.monora.uprotocol.client.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.monora.uprotocol.client.android.protocol.DefaultPersistenceProvider
import org.monora.uprotocol.core.persistence.PersistenceProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceProviderModule {
    @Singleton
    @Binds
    abstract fun providePersistenceProvider(
        defaultPersistenceProvider: DefaultPersistenceProvider,
    ): PersistenceProvider
}