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

package com.genonbeta.TrebleShot.service.backgroundservice;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.HomeActivity;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class TaskMessageImpl implements TaskMessage
{
    private String mTitle;
    private String mMessage;
    private Tone mTone = Tone.Neutral;
    private final List<Action> mActionList = new ArrayList<>();

    @Override
    public TaskMessage addAction(Action action)
    {
        synchronized (mActionList) {
            mActionList.add(action);
        }
        return this;
    }

    @Override
    public TaskMessage addAction(Context context, int nameRes, Callback callback)
    {
        return addAction(context.getString(nameRes), callback);
    }

    @Override
    public TaskMessage addAction(String name, Callback callback)
    {
        return addAction(name, Tone.Neutral, callback);
    }

    @Override
    public TaskMessage addAction(Context context, int nameRes, Tone tone, Callback callback)
    {
        return addAction(context.getString(nameRes), tone, callback);
    }

    @Override
    public TaskMessage addAction(String name, Tone tone, Callback callback)
    {
        Action action = new Action();
        action.name = name;
        action.tone = tone;
        action.callback = callback;
        return addAction(action);
    }

    @Override
    public List<Action> getActionList()
    {
        synchronized (mActionList) {
            return new ArrayList<>(mActionList);
        }
    }

    @Override
    public String getMessage()
    {
        return mMessage;
    }

    @Override
    public String getTitle()
    {
        return mTitle;
    }

    @Override
    public Tone getTone()
    {
        return mTone;
    }

    @DrawableRes
    public static int iconFor(Tone tone)
    {
        switch (tone) {
            case Confused:
                return R.drawable.ic_help_white_24_static;
            case Positive:
                return R.drawable.ic_check_white_24dp_static;
            case Negative:
                return R.drawable.ic_close_white_24dp_static;
            default:
            case Neutral:
                return R.drawable.ic_trebleshot_white_24dp_static;
        }
    }

    @Override
    public TaskMessage removeAction(Action action)
    {
        synchronized (mActionList) {
            mActionList.remove(action);
        }
        return this;
    }

    @Override
    public TaskMessage setMessage(Context context, int msgRes)
    {
        return setMessage(context.getString(msgRes));
    }

    @Override
    public TaskMessage setMessage(String msg)
    {
        mMessage = msg;
        return this;
    }

    @Override
    public TaskMessage setTitle(Context context, int titleRes)
    {
        return setTitle(context.getString(titleRes));
    }

    @Override
    public TaskMessage setTitle(String title)
    {
        mTitle = title;
        return this;
    }

    @Override
    public TaskMessage setTone(Tone tone)
    {
        mTone = tone;
        return this;
    }

    @Override
    public int sizeOfActions()
    {
        synchronized (mActionList) {
            return mActionList.size();
        }
    }

    @Override
    public AlertDialog.Builder toDialogBuilder(Activity activity)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(getTitle())
                .setMessage(getMessage());

        synchronized (mActionList) {
            boolean[] appliedTones = new boolean[Tone.values().length];
            for (Action action : mActionList) {
                if (appliedTones[action.tone.ordinal()])
                    continue;

                switch (action.tone) {
                    case Positive:
                        builder.setPositiveButton(action.name, (dialog, which) -> action.callback.call(activity));
                        break;
                    case Negative:
                        builder.setNegativeButton(action.name, (dialog, which) -> action.callback.call(activity));
                        break;
                    case Neutral:
                        builder.setNeutralButton(action.name, (dialog, which) -> action.callback.call(activity));
                }

                appliedTones[action.tone.ordinal()] = true;
            }

            if (appliedTones.length < 1 || !appliedTones[Tone.Negative.ordinal()])
                builder.setNegativeButton(R.string.butn_close, null);
        }

        return builder;
    }

    @Override
    public DynamicNotification toNotification(AsyncTask task)
    {
        Context context = task.getContext().getApplicationContext();
        NotificationUtils utils = task.getNotificationHelper().getUtils();
        DynamicNotification notification = utils.buildDynamicNotification(task.hashCode(),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        PendingIntent intent = PendingIntent.getActivity(context, 0, new Intent(context, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);

        notification.setSmallIcon(iconFor(mTone))
                .setGroup(task.getTaskGroup())
                .setContentTitle(mTitle)
                .setContentText(mMessage)
                .setContentIntent(intent)
                .setAutoCancel(true);

        for (Action action : mActionList)
            notification.addAction(iconFor(action.tone), action.name, PendingIntent.getActivity(context,
                    0, new Intent(context, HomeActivity.class), 0));

        return notification;
    }

    @Override
    public Snackbar toSnackbar(View view)
    {
        Snackbar snackbar = Snackbar.make(view, getMessage(), Snackbar.LENGTH_LONG);

        if (sizeOfActions() > 0) {
            synchronized (mActionList) {
                Action action = mActionList.get(0);
                snackbar.setAction(action.name, v -> action.callback.call(v.getContext()));
            }
        }

        return snackbar;
    }

    @NonNull
    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Title=")
                .append(getTitle())
                .append(" Msg=")
                .append(getMessage())
                .append(" Tone=")
                .append(getTone());

        for (Action action : mActionList)
            stringBuilder.append(action);

        return stringBuilder.toString();
    }
}
