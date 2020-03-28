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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.DrawableRes;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.HomeActivity;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.NotificationUtils;

import java.util.ArrayList;
import java.util.List;

public class TaskMessageImpl implements TaskMessage
{
    private String mTitle;
    private String mMessage;
    private Tone mTone = Tone.Neutral;
    private final List<Action> mActions = new ArrayList<>();

    @Override
    public TaskMessage addAction(Action action)
    {
        synchronized (mActions) {
            mActions.add(action);
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
        return this;
    }

    @Override
    public List<Action> getActionList()
    {
        synchronized (mActions) {
            return new ArrayList<>(mActions);
        }
    }

    public String getMessage()
    {
        return mMessage;
    }

    public String getTitle()
    {
        return mTitle;
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
        synchronized (mActions) {
            mActions.remove(action);
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
    public DynamicNotification toNotification(BackgroundTask task)
    {
        Context context = task.getService().getApplicationContext();
        NotificationUtils utils = task.getNotificationHelper().getUtils();
        DynamicNotification notification = utils.buildDynamicNotification(task.hashCode(),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification.setSmallIcon(iconFor(mTone))
                .setGroup(task.getTaskGroup())
                .setContentTitle(mTitle)
                .setContentText(mMessage);

        for (Action action : mActions)
            notification.addAction(iconFor(action.tone), action.name, PendingIntent.getActivity(context,
                    0, new Intent(context, HomeActivity.class), 0));

        return notification;
    }
}
