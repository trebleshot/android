/*
 * Copyright (C) 2020 Veli Tasalı
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
package org.monora.uprotocol.client.android.task

import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory

class IndexTransferTaskRegistry(
    private val connectionFactory: ConnectionFactory,
    private val persistenceProvider: PersistenceProvider,
    private val clientRepository: ClientRepository,
    private val transferRepository: TransferRepository,
    private val transferId: Long,
    private val jsonIndex: String,
    private val client: UClient,
    private val noPrompt: Boolean,
) {
    /*
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        // Do not let it add the same transfer id again.
        try {
            if (runBlocking { transferRepository.containsTransfer(transferId) }) {
                Log.d(TAG, "onRun: Transfer already exists: $transferId. Skipping...")
                return
            }

            val saveLocation = Files.getApplicationDirectory(context).toString()
            val transfer = Transfer(transferId, client.clientUid, Incoming, saveLocation)
            val items = persistenceProvider.toTransferItemList(transferId, jsonIndex).map {
                // TODO: 7/4/21 PersistenceProvider types can be improved?
                check(it is UTransferItem) {
                    "Unexpected type"
                }

                it
            }

            if (items.isNotEmpty()) runBlocking {
                transferRepository.insert(transfer)

                transferRepository.insert(items)

                if (noPrompt) {
                    try {
                        backend.run(
                            FileTransferStarterTaskRegistry.createFrom(
                                connectionFactory, persistenceProvider, clientRepository, transfer, client, Incoming
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    backend.notifyFileRequest(client, transfer, items)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_preparingFiles)
    }

    companion object {
        const val TAG = "IndexTransferTask"
    }*/
}