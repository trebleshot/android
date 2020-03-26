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

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public abstract class AttachableBgTask<T extends AttachedTaskListener> extends BaseAttachableBgTask
{
    public static final int OVERRIDE_BY_ALL = 1;
    public static final int OVERRIDE_BY_NONE = 2;
    public static final int OVERRIDE_BY_SELF = 4;

    private T mAnchor;
    private Handler mHandler;
    private final List<Call<T>> mCallList = new ArrayList<>();
    private Runnable mPostStatus = this::notifyAnchor;

    protected boolean doesOverride(Call<T> call, Call<T> currentCall)
    {
        if ((call.overrideBy & OVERRIDE_BY_NONE) != 0)
            return false;

        return ((call.overrideBy & OVERRIDE_BY_SELF) != 0 && call.taskId.ordinal() == currentCall.taskId.ordinal())
                || (call.overrideBy & OVERRIDE_BY_ALL) != 0;
    }

    @Override
    public boolean hasAnchor()
    {
        return mAnchor != null;
    }

    private Handler getHandler()
    {
        if (mHandler == null) {
            Looper myLooper = Looper.myLooper();
            mHandler = new Handler(myLooper == null ? Looper.getMainLooper() : myLooper);
        }
        return mHandler;
    }

    private void notifyAnchor()
    {
        if (hasAnchor())
            mAnchor.onTaskStateChanged(this);
    }

    public void post(Call<T> currentCall)
    {
        postAll();

        if (hasAnchor())
            currentCall.now(mAnchor);
        else {
            synchronized (mCallList) {
                List<Call<T>> newList = new ArrayList<>();
                for (Call<T> call : mCallList)
                    if (doesOverride(call, currentCall))
                        newList.add(call);

                mCallList.clear();
                mCallList.addAll(newList);
            }
        }
    }

    private void postAll()
    {
        if (mCallList.size() <= 0)
            return;

        synchronized (mCallList) {
            boolean doneAll = true;
            for (Call<T> call : mCallList) {
                if (hasAnchor()) {
                    call.now(mAnchor);
                    call.done = true;
                } else {
                    doneAll = false;
                    break;
                }
            }

            if (doneAll)
                mCallList.clear();
            else {
                List<Call<T>> newList = new ArrayList<>();
                for (Call<T> call : mCallList)
                    if (!call.done)
                        newList.add(call);

                mCallList.clear();
                mCallList.addAll(newList);
            }
        }
    }

    @Override
    public boolean publishStatus()
    {
        getHandler().post(mPostStatus);
        return super.publishStatus();
    }

    @Override
    public void removeAnchor()
    {
        mAnchor = null;
    }

    public void setAnchor(T anchor)
    {
        mAnchor = anchor;
        publishStatus();
        postAll();
    }

    public static abstract class Call<T extends AttachedTaskListener>
    {
        public int overrideBy;
        public Enum<?> taskId;
        boolean done;

        public Call(Enum<?> taskId, int overrideBy)
        {
            this.taskId = taskId;
            this.overrideBy = overrideBy;
        }

        public abstract void now(T anchor);
    }
}