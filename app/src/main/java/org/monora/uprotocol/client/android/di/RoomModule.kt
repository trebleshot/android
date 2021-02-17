package org.monora.uprotocol.client.android.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.database.AppDatabase

@Module
@InstallIn(ActivityComponent::class)
class RoomModule {
    @Provides
    fun provideRoomDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "Main.db")
            .build()
    }
}