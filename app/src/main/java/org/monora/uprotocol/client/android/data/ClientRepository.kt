package org.monora.uprotocol.client.android.data

import androidx.lifecycle.LiveData
import org.monora.uprotocol.client.android.database.ClientDao
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.util.NsdDaemon
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val clientDao: ClientDao,
    private val nsdDaemon: NsdDaemon,
) {
    fun getOnlineClients(): LiveData<List<ClientRoute>> = nsdDaemon.onlineClients

    fun getAll(): LiveData<List<UClient>> = clientDao.getAll()
}