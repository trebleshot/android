package org.monora.uprotocol.client.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.util.NsdDaemon
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NsdModule {
    @Singleton
    @Provides
    fun provideNsdDaemon(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        persistenceProvider: PersistenceProvider,
        connectionFactory: ConnectionFactory
    ): NsdDaemon {
        return NsdDaemon(context, appDatabase, persistenceProvider, connectionFactory)
    }
}