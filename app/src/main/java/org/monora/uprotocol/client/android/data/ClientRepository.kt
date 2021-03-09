package org.monora.uprotocol.client.android.data

import org.monora.uprotocol.client.android.database.ClientDao
import org.monora.uprotocol.client.android.util.NsdDaemon
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val clientDao: ClientDao,
    private val nsdDaemon: NsdDaemon,
) {
    fun getOnlineClients() = nsdDaemon.onlineClients

    fun getAll() = clientDao.getAll()
}