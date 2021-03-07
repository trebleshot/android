package org.monora.uprotocol.client.android.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.monora.uprotocol.client.android.protocol.MainConnectionFactory
import org.monora.uprotocol.client.android.protocol.MainPersistenceProvider
import org.monora.uprotocol.client.android.protocol.MainTransportSeat
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.TransportSession
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SingletonUprotocolModule {
    @Singleton
    @Binds
    abstract fun provideConnectionFactory(
        mainConnectionFactory: MainConnectionFactory,
    ): ConnectionFactory

    @Singleton
    @Binds
    abstract fun providePersistenceProvider(
        mainPersistenceProvider: MainPersistenceProvider,
    ): PersistenceProvider

    @Singleton
    @Binds
    abstract fun provideTransportSeat(
        mainTransportSeat: MainTransportSeat,
    ): TransportSeat
}