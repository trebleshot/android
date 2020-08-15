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

package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;
import com.genonbeta.TrebleShot.protocol.communication.*;

import java.net.ConnectException;
import java.net.NoRouteToHostException;

public class CommonErrorHelper
{
    public static TaskMessage messageOf(Context context, Exception exception)
    {
        TaskMessage taskMessage = TaskMessage.newInstance()
                .setTone(TaskMessage.Tone.Negative);

        try {
            throw exception;
        } catch (CommunicationException e) {
            taskMessage.setTitle(context, R.string.text_communicationError);

            if (e instanceof NotAllowedException)
                taskMessage.setMessage(context, R.string.mesg_notAllowed);
            else if (e instanceof DifferentClientException)
                taskMessage.setMessage(context, R.string.mesg_errorDifferentDevice);
            else if (e instanceof NotTrustedException)
                taskMessage.setMessage(context, R.string.mesg_errorNotTrusted);
            else if (e instanceof UnknownCommunicationErrorException)
                taskMessage.setMessage(context.getString(R.string.mesg_unknownErrorOccurredWithCode,
                        ((UnknownCommunicationErrorException) e).errorCode));
            else
                taskMessage.setMessage(context, R.string.mesg_unknownErrorOccurred);
        } catch (DeviceIntroductionTask.SuggestNetworkException e) {
            taskMessage.setTitle(context, R.string.text_networkSuggestionError);

            switch (e.type) {
                case ExceededLimit:
                    taskMessage.setMessage(context, R.string.text_errorExceededMaximumSuggestions)
                            .addAction(context, R.string.butn_openSettings, TaskMessage.Tone.Positive, (appContext) -> {
                                if (appContext != null)
                                    appContext.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            });
                    break;
                case AppDisallowed:
                    taskMessage.setMessage(context, R.string.text_errorNetworkSuggestionsDisallowed)
                            .addAction(context, R.string.butn_openSettings, TaskMessage.Tone.Positive, (appContext) -> {
                                if (appContext != null)
                                    AppUtils.startApplicationDetails(appContext);
                            });
                    break;
                case ErrorInternal:
                    taskMessage.setMessage(context, R.string.text_errorNetworkSuggestionInternal)
                            .addAction(context, R.string.butn_feedbackContact, TaskMessage.Tone.Positive, (appContext) -> {
                                if (appContext != null)
                                    AppUtils.startFeedbackActivity(appContext);
                            });
                    break;
            }
        } catch (ConnectException e) {
            taskMessage.setTitle(context, R.string.text_communicationError)
                    .setMessage(context, R.string.mesg_socketConnectionError);
        } catch (NoRouteToHostException e) {
            taskMessage.setTitle(context, R.string.text_communicationError)
                    .setMessage(context, R.string.mesg_noRouteToHostError);
        } catch (Exception e) {
            taskMessage.setTitle(context, R.string.mesg_somethingWentWrong)
                    .setMessage(context, R.string.mesg_unknownErrorOccurred);
        }
        return taskMessage;
    }
}
