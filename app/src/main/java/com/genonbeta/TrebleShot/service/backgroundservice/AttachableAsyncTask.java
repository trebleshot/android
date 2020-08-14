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

public abstract class AttachableAsyncTask<T extends AttachedTaskListener> extends BaseAttachableAsyncTask
{
    private T mAnchor;
    private Handler mHandler;
    private final Runnable mPostStatus = this::notifyAnchor;

    @Override
    public boolean hasAnchor()
    {
        return mAnchor != null;
    }

    public T getAnchor() throws TaskStoppedException
    {
        throwIfStopped();
        return mAnchor;
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

    @Override
    public void post(TaskMessage message) throws TaskStoppedException
    {
        T anchor = getAnchor();

        if (anchor == null || !anchor.onTaskMessage(message))
            super.post(message);
    }

    public void post(Runnable runnable)
    {
        getHandler().post(runnable);
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
    }
}