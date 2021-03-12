package org.monora.uprotocol.client.android.viewmodel.content

import com.genonbeta.android.framework.util.Files
import org.monora.uprotocol.client.android.database.model.UTransferItem

class TransferItemContentViewModel(val transferItem: UTransferItem) {
    val name = transferItem.name

    val size = Files.formatLength(transferItem.size)
}