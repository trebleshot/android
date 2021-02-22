package org.monora.uprotocol.client.android.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.SharedTextDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RoomModule {
    @Singleton
    @Provides
    fun provideRoomDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "Main.db")
            .build()
    }

    @Provides
    fun provideSharedTextDao(appDatabase: AppDatabase): SharedTextDao {
        return appDatabase.sharedTextDao()
    }
}