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

import java.util.ArrayList;
import java.util.List;

public class TaskMessageImpl implements TaskMessage
{
    private String mTitle;
    private String mMessage;
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
        return addAction(name, -1, callback);
    }

    @Override
    public TaskMessage addAction(Context context, int nameRes, int type, Callback callback)
    {
        return addAction(context.getString(nameRes), type, callback);
    }

    @Override
    public TaskMessage addAction(String name, int type, Callback callback)
    {
        Action action = new Action();
        action.name = name;
        action.type = type;
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
}
