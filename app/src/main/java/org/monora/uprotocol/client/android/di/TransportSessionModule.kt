package org.monora.uprotocol.client.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.TransportSession
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TransportSessionModule {
    @Singleton
    @Provides
    fun provideTransportSession(
        connectionFactory: ConnectionFactory,
        persistenceProvider: PersistenceProvider,
        transportSeat: TransportSeat,
    ): TransportSession {
        return TransportSession(connectionFactory, persistenceProvider, transportSeat)
    }
}