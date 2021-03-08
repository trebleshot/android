package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.ClientRepository
import javax.inject.Inject

@HiltViewModel
class ClientsViewModel @Inject internal constructor(
    clientRepository: ClientRepository,
) : ViewModel() {
    val onlineClients = clientRepository.getOnlineClients()

    val clients = clientRepository.getAll()
}