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
package org.monora.uprotocol.client.android.viewmodel

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.data.TaskRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.data.WebDataRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.WebTransfer
import org.monora.uprotocol.client.android.model.DateSectionContentModel
import org.monora.uprotocol.client.android.model.ListItem
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.client.android.viewmodel.content.TransferStateContentViewModel
import javax.inject.Inject

@HiltViewModel
class TransfersViewModel @Inject internal constructor(
    @ApplicationContext context: Context,
    private val clientRepository: ClientRepository,
    private val sharedTextRepository: SharedTextRepository,
    private val taskRepository: TaskRepository,
    private val transferRepository: TransferRepository,
    private val webDataRepository: WebDataRepository,
) : ViewModel() {
    val transfers = liveData {
        val merger = MediatorLiveData<List<ListItem>>()
        val texts = sharedTextRepository.getSharedTexts()
        val details = transferRepository.getTransferDetails()
        val webTransfers = webDataRepository.getReceivedContents()
        val observer = Observer<List<ListItem>> {
            val mergedList = mutableListOf<ListItem>().apply {
                texts.value?.let { addAll(it) }
                details.value?.let { addAll(it) }
                webTransfers.value?.let { addAll(it) }

                sortByDescending {
                    when (it) {
                        is TransferDetail -> it.dateCreated
                        is SharedText -> it.created
                        is WebTransfer -> it.dateCreated
                        else -> throw IllegalStateException()
                    }
                }
            }

            val resultList = mutableListOf<ListItem>()
            var previous: DateSectionContentModel? = null

            mergedList.forEach {
                val date = when (it) {
                    is TransferDetail -> it.dateCreated
                    is SharedText -> it.created
                    is WebTransfer -> it.dateCreated
                    else -> throw IllegalStateException()
                }
                val dateText = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE)
                if (dateText != previous?.dateText) {
                    resultList.add(DateSectionContentModel(dateText, date).also { model -> previous = model })
                }
                resultList.add(it)
            }

            merger.value = resultList
        }

        merger.addSource(texts, observer)
        merger.addSource(details, observer)
        merger.addSource(webTransfers, observer)

        emitSource(merger)
    }

    suspend fun getTransfer(groupId: Long): Transfer? = transferRepository.getTransfer(groupId)

    suspend fun getClient(clientUid: String): UClient? = clientRepository.getDirect(clientUid)

    fun subscribe(transferDetail: TransferDetail) = taskRepository.subscribeToTask {
        if (it.params is TransferParams && it.params.transfer.id == transferDetail.id) it.params else null
    }.map {
        TransferStateContentViewModel.from(it)
    }
}
