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
import android.content.Intent
import android.provider.Settings
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage.Tone
import org.monora.uprotocol.client.android.task.DeviceIntroductionTask
import org.monora.uprotocol.core.protocol.communication.CommunicationException
import org.monora.uprotocol.core.protocol.communication.ContentException
import org.monora.uprotocol.core.protocol.communication.UndefinedErrorCodeException
import org.monora.uprotocol.core.protocol.communication.client.DifferentRemoteClientException
import org.monora.uprotocol.core.protocol.communication.client.UnauthorizedClientException
import org.monora.uprotocol.core.protocol.communication.client.UntrustedClientException
import java.net.ConnectException
import java.net.NoRouteToHostException

object CommonErrorHelper {
    fun messageOf(context: Context, exception: Exception?): TaskMessage {
        val title: String
        val message: String
        var action: TaskMessage.Action? = null

        when (exception) {
            is CommunicationException -> {
                title = context.getString(R.string.text_communicationError)
                message = when (exception) {
                    is DifferentRemoteClientException -> context.getString(R.string.mesg_errorDifferentDevice)
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
            is DeviceIntroductionTask.SuggestNetworkException -> {
                title = context.getString(R.string.text_networkSuggestionError)
                when (exception.type) {
                    DeviceIntroductionTask.SuggestNetworkException.Type.ExceededLimit -> {
                        message = context.getString(R.string.text_errorExceededMaximumSuggestions)
                        action = TaskMessage.Action(
                            context.getString(R.string.butn_openSettings),
                            Tone.Positive, object : TaskMessage.Callback {
                                override fun call(context: Context) {
                                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                }
                            }
                        )
                    }
                    DeviceIntroductionTask.SuggestNetworkException.Type.AppDisallowed -> {
                        message = context.getString(R.string.text_errorNetworkSuggestionsDisallowed)
                        action = TaskMessage.Action(
                            context.getString(R.string.butn_openSettings),
                            Tone.Positive,
                            object : TaskMessage.Callback {
                                override fun call(context: Context) {
                                    AppUtils.startApplicationDetails(context)
                                }
                            }
                        )
                    }
                    DeviceIntroductionTask.SuggestNetworkException.Type.ErrorInternal -> {
                        message = context.getString(R.string.text_errorNetworkSuggestionInternal)
                        action = TaskMessage.Action(
                            context.getString(R.string.butn_feedbackContact),
                            Tone.Positive,
                            object : TaskMessage.Callback {
                                override fun call(context: Context) {
                                    AppUtils.startFeedbackActivity(context)
                                }
                            }
                        )
                    }
                    else -> message = context.getString(R.string.mesg_unknownErrorOccurred)
                }
            }
            is ConnectException -> {
                title = context.getString(R.string.text_communicationError)
                message = context.getString(R.string.mesg_socketConnectionError)
            }
            is NoRouteToHostException -> {
                title = context.getString(R.string.text_communicationError)
                message = context.getString(R.string.mesg_noRouteToHostError)
            }
            else -> {
                title = context.getString(R.string.mesg_somethingWentWrong)
                message = context.getString(R.string.mesg_unknownErrorOccurred)
            }
        }

        val taskMessage = TaskMessage.newInstance(title, message, Tone.Negative)
        if (action != null) taskMessage.addAction(action)
        return taskMessage
    }
}