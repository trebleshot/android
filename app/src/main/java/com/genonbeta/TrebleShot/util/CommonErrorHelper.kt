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
package com.genonbeta.TrebleShot.util

import android.content.*
import android.provider.Settings
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.protocol.communication.CommunicationException
import com.genonbeta.TrebleShot.protocol.communication.ContentException
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask
import java.net.ConnectException
import java.net.NoRouteToHostException

object CommonErrorHelper {
    fun messageOf(context: Context, exception: Exception?): TaskMessage {
        val taskMessage: TaskMessage = TaskMessage.Companion.newInstance()
            .setTone(Tone.Negative)
        try {
            throw exception!!
        } catch (e: CommunicationException) {
            taskMessage.setTitle(context, R.string.text_communicationError)
            if (e is DifferentClientException) taskMessage.setMessage(
                context,
                R.string.mesg_errorDifferentDevice
            ) else if (e is NotAllowedException) taskMessage.setMessage(
                context,
                R.string.mesg_notAllowed
            ) else if (e is NotTrustedException) taskMessage.setMessage(
                context,
                R.string.mesg_errorNotTrusted
            ) else if (e is UnknownCommunicationErrorException) taskMessage.setMessage(
                context.getString(
                    R.string.mesg_unknownErrorOccurredWithCode,
                    (e as UnknownCommunicationErrorException).errorCode
                )
            ) else if (e is ContentException) {
                when (e.error) {
                    ContentException.Error.NotAccessible -> taskMessage.setMessage(
                        context,
                        R.string.text_contentNotAccessible
                    )
                    ContentException.Error.AlreadyExists -> taskMessage.setMessage(
                        context,
                        R.string.text_contentAlreadyExists
                    )
                    ContentException.Error.NotFound -> taskMessage.setMessage(context, R.string.text_contentNotFound)
                    else -> taskMessage.setMessage(context, R.string.mesg_unknownErrorOccurred)
                }
            } else taskMessage.setMessage(context, R.string.mesg_unknownErrorOccurred)
        } catch (e: SuggestNetworkException) {
            taskMessage.setTitle(context, R.string.text_networkSuggestionError)
            when (e.type) {
                DeviceIntroductionTask.SuggestNetworkException.Type.ExceededLimit -> taskMessage.setMessage(
                    context,
                    R.string.text_errorExceededMaximumSuggestions
                )
                    .addAction(
                        context,
                        R.string.butn_openSettings,
                        Tone.Positive,
                        TaskMessage.Callback { appContext: Context? ->
                            appContext?.startActivity(
                                Intent(
                                    Settings.ACTION_WIFI_SETTINGS
                                )
                            )
                        })
                DeviceIntroductionTask.SuggestNetworkException.Type.AppDisallowed -> taskMessage.setMessage(
                    context,
                    R.string.text_errorNetworkSuggestionsDisallowed
                )
                    .addAction(
                        context,
                        R.string.butn_openSettings,
                        Tone.Positive,
                        TaskMessage.Callback { appContext: Context? ->
                            if (appContext != null) AppUtils.startApplicationDetails(appContext)
                        })
                DeviceIntroductionTask.SuggestNetworkException.Type.ErrorInternal -> taskMessage.setMessage(
                    context,
                    R.string.text_errorNetworkSuggestionInternal
                )
                    .addAction(
                        context,
                        R.string.butn_feedbackContact,
                        Tone.Positive,
                        TaskMessage.Callback { appContext: Context? ->
                            if (appContext != null) AppUtils.startFeedbackActivity(appContext)
                        })
                else -> taskMessage.setMessage(context, R.string.mesg_unknownErrorOccurred)
            }
        } catch (e: ConnectException) {
            taskMessage.setTitle(context, R.string.text_communicationError)
                .setMessage(context, R.string.mesg_socketConnectionError)
        } catch (e: NoRouteToHostException) {
            taskMessage.setTitle(context, R.string.text_communicationError)
                .setMessage(context, R.string.mesg_noRouteToHostError)
        } catch (e: Exception) {
            taskMessage.setTitle(context, R.string.mesg_somethingWentWrong)
                .setMessage(context, R.string.mesg_unknownErrorOccurred)
        }
        return taskMessage
    }
}