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

import android.content.Context;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.util.DynamicNotification;

import java.util.List;

public interface TaskMessage
{
    static TaskMessage newInstance()
    {
        return new TaskMessageImpl();
    }

    TaskMessage addAction(Action action);

    TaskMessage addAction(Context context, int nameRes, Callback callback);

    TaskMessage addAction(String name, Callback callback);

    TaskMessage addAction(Context context, int nameRes, Tone tone, Callback callback);

    TaskMessage addAction(String name, Tone tone, Callback callback);

    List<Action> getActionList();

    TaskMessage removeAction(Action action);

    TaskMessage setMessage(Context context, int msgRes);

    TaskMessage setMessage(String msg);

    TaskMessage setTitle(Context context, int titleRes);

    TaskMessage setTitle(String title);

    TaskMessage setTone(Tone tone);

    DynamicNotification toNotification(BackgroundTask task);

    enum Tone {
        Positive,
        Confused,
        Neutral,
        Negative
    }

    class Action
    {
        public Tone tone;
        public String name;
        public Callback callback;
    }

    interface Callback
    {
        void call(BackgroundService service, TaskMessage msg, Action action);
    }
}
