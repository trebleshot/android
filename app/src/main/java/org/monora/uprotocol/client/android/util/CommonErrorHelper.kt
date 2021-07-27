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
package org.monora.uprotocol.client.android.util

import android.content.Context
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.protocol.NoAddressException
import org.monora.uprotocol.core.io.DefectiveAddressListException
import org.monora.uprotocol.core.protocol.communication.CommunicationException
import org.monora.uprotocol.core.protocol.communication.ContentException
import org.monora.uprotocol.core.protocol.communication.UndefinedErrorCodeException
import org.monora.uprotocol.core.protocol.communication.client.DifferentRemoteClientException
import org.monora.uprotocol.core.protocol.communication.client.UnauthorizedClientException
import org.monora.uprotocol.core.protocol.communication.client.UntrustedClientException
import java.net.ConnectException
import java.net.NoRouteToHostException

object CommonErrorHelper {
    fun messageOf(context: Context, exception: Exception): String = when (exception) {
        is CommunicationException -> {
            when (exception) {
                is UnauthorizedClientException -> context.getString(R.string.mesg_notAllowed)
                is UntrustedClientException -> context.getString(R.string.mesg_errorNotTrusted)
                is UndefinedErrorCodeException -> context.getString(
                    R.string.mesg_unknownErrorOccurredWithCode,
                    exception.errorCode
                )
                is ContentException -> context.getString(
                    when (exception.error) {
                        ContentException.Error.NotAccessible -> R.string.text_contentNotAccessible
                        ContentException.Error.AlreadyExists -> R.string.text_contentAlreadyExists
                        ContentException.Error.NotFound -> R.string.text_contentNotFound
                        else -> R.string.mesg_unknownErrorOccurred
                    }
                )
                else -> context.getString(R.string.mesg_unknownErrorOccurred)
            }
        }
        is NoAddressException -> context.getString(R.string.mesg_clientOffline)
        is DifferentRemoteClientException -> context.getString(R.string.mesg_errorDifferentDevice)
        is ConnectException, is DefectiveAddressListException -> context.getString(
            R.string.mesg_socketConnectionError
        )
        is NoRouteToHostException -> context.getString(R.string.mesg_noRouteToHostError)
        else -> context.getString(R.string.mesg_unknownErrorOccurred)
    }
}