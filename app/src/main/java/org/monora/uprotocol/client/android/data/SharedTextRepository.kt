package org.monora.uprotocol.client.android.data

import org.monora.uprotocol.client.android.database.SharedTextDao
import org.monora.uprotocol.client.android.database.model.SharedText
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedTextRepository @Inject constructor(
    private val sharedTextDao: SharedTextDao,
) {
    suspend fun delete(sharedText: SharedText) = sharedTextDao.delete(sharedText)

    fun getSharedTexts() = sharedTextDao.getAll()

    suspend fun insert(sharedText: SharedText) = sharedTextDao.insert(sharedText)

    suspend fun update(sharedText: SharedText) = sharedTextDao.update(sharedText)
}