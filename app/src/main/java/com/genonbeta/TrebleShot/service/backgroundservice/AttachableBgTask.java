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

import androidx.annotation.Nullable;

public abstract class AttachableBgTask<T extends AttachedTaskListener> extends BaseAttachableBgTask
{
    private T mAnchorListener;

    @Override
    public void detachAnchor()
    {
        mAnchorListener = null;
    }

    @Nullable
    public T getAnchorListener()
    {
        return mAnchorListener;
    }

    public AttachableBgTask<T> setAnchorListener(T listener)
    {
        mAnchorListener = listener;
        listener.onAttachedToTask(this);
        return this;
    }

    @Override
    public boolean publishStatusText(String text)
    {
        if (mAnchorListener != null)
            mAnchorListener.updateTaskStatus(text);

        return super.publishStatusText(text);
    }
}