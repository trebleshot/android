/*
 * Copyright (C) 2021 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.monora.uprotocol.client.android.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.ClientAddressDao
import org.monora.uprotocol.client.android.database.ClientDao
import org.monora.uprotocol.client.android.database.SafFolderDao
import org.monora.uprotocol.client.android.database.SharedTextDao
import org.monora.uprotocol.client.android.database.TransferDao
import org.monora.uprotocol.client.android.database.TransferItemDao
import org.monora.uprotocol.client.android.database.WebTransferDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    @Singleton
    @Provides
    fun provideRoomDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "Main.db")
            .build()
    }

    @Provides
    fun provideClientDao(appDatabase: AppDatabase): ClientDao {
        return appDatabase.clientDao()
    }

    @Provides
    fun provideClientAddressDao(appDatabase: AppDatabase): ClientAddressDao {
        return appDatabase.clientAddressDao()
    }

    @Provides
    fun provideSafFolderDao(appDatabase: AppDatabase): SafFolderDao {
        return appDatabase.safFolderDao()
    }

    @Provides
    fun provideSharedTextDao(appDatabase: AppDatabase): SharedTextDao {
        return appDatabase.sharedTextDao()
    }

    @Provides
    fun provideTransferDao(appDatabase: AppDatabase): TransferDao {
        return appDatabase.transferDao()
    }

    @Provides
    fun provideTransferItemDao(appDatabase: AppDatabase): TransferItemDao {
        return appDatabase.transferItemDao()
    }

    @Provides
    fun provideWebTransferDao(appDatabase: AppDatabase): WebTransferDao {
        return appDatabase.webTransferDao()
    }
}
