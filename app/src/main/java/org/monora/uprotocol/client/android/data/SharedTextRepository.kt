package org.monora.uprotocol.client.android.data

import org.monora.uprotocol.client.android.database.SharedTextDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedTextRepository @Inject constructor(
    private val sharedTextDao: SharedTextDao,
) {
    fun getSharedTexts() = sharedTextDao.getAll()
}