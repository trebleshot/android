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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;

public class CommonErrorHelper
{
    public static TaskMessage messageOf(Exception e)
    {
        return TaskMessage.newInstance();
    }

    public static TaskMessage messageOf(DeviceIntroductionTask.SuggestNetworkException e, Context appContext)
    {
        TaskMessage message = TaskMessage.newInstance()
                .setTitle(appContext, R.string.text_error)
                .addAction(appContext, R.string.butn_close, null);

        switch (e.type) {
            case ExceededLimit:
                message.setMessage(appContext, R.string.text_errorExceededMaximumSuggestions)
                        .addAction(appContext, R.string.butn_openSettings, Dialog.BUTTON_POSITIVE,
                                (context, msg, action) -> context.startActivity(new Intent(
                                        Settings.ACTION_WIFI_SETTINGS)));
                break;
            case AppDisallowed:
                message.setMessage(appContext, R.string.text_errorNetworkSuggestionsDisallowed)
                        .addAction(appContext, R.string.butn_openSettings, Dialog.BUTTON_POSITIVE,
                                (context, msg, action) -> AppUtils.startApplicationDetails(context));
                break;
            case ErrorInternal:
                message.setMessage(appContext, R.string.text_errorNetworkSuggestionInternal)
                        .addAction(appContext, R.string.butn_feedbackContact, Dialog.BUTTON_POSITIVE,
                                (context, msg, action) -> AppUtils.startFeedbackActivity(context));
                break;
        }

        return message;
    }
}
