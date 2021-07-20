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
package org.monora.uprotocol.client.android.viewmodel.content

import com.genonbeta.android.framework.util.Files
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.core.transfer.TransferItem
import java.text.NumberFormat

class TransferDetailContentViewModel(transferDetail: TransferDetail) {
    val clientNickname = transferDetail.clientNickname

    val sizeText = Files.formatLength(transferDetail.size, false)

    val isReceiving = transferDetail.type == TransferItem.Type.Incoming

    val count = transferDetail.itemsCount

    val icon = if (isReceiving) {
        R.drawable.ic_arrow_down_white_24dp
    } else {
        R.drawable.ic_arrow_up_white_24dp
    }

    val percentage: Double = if (transferDetail.sizeOfDone <= 0) {
        if (transferDetail.size <= 0) 1.0 else 0.01
    } else {
        transferDetail.sizeOfDone.toDouble() / transferDetail.size
    }

    val percentageInt = (percentage * 100).toInt()

    val percentageText = percentageInt.toString()
}