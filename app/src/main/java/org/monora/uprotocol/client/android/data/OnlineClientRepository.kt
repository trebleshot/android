package org.monora.uprotocol.client.android.data

import org.monora.uprotocol.client.android.util.NsdDaemon
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineClientRepository @Inject constructor(
    private val nsdDaemon: NsdDaemon
){
    fun getOnlineClients() = nsdDaemon.onlineClients
}