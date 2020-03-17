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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaScannerConnection;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Identifiable;
import com.genonbeta.TrebleShot.object.Identity;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.NotificationHelper;
import com.genonbeta.TrebleShot.util.StoppableJob;
import com.genonbeta.android.database.Progress;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.framework.util.StoppableImpl;

import java.util.List;

import static com.genonbeta.TrebleShot.service.BackgroundService.hashIntent;

public abstract class BackgroundTask extends StoppableJob implements Stoppable, Identifiable
{
    public static final int TASK_GROUP_DEFAULT = 0;

    private Kuick mKuick;
    private BackgroundService mService;
    private Stoppable mStoppable;
    private PendingIntent mActivityIntent;
    private DynamicNotification mCustomNotification; // The notification that is not part of the default notification.
    private ProgressListener mProgressListener = new ProgressListener();
    private String mCurrentContent;
    private boolean mPublishRequested = false;
    private int mHash = 0;

    protected abstract void onRun() throws InterruptedException;

    protected void onProgressChange(Progress progress)
    {

    }

    @Override
    public boolean addCloser(Closer closer)
    {
        return getStoppable().addCloser(closer);
    }

    public boolean consumeInfoChanges()
    {
        if (mPublishRequested) {
            mPublishRequested = false;
            return true;
        }
        return false;
    }

    public void forceQuit()
    {

    }

    @Override
    public List<Closer> getClosers()
    {
        return getStoppable().getClosers();
    }

    @Nullable
    public PendingIntent getContentIntent()
    {
        return mActivityIntent;
    }

    public String getCurrentContent()
    {
        return mCurrentContent;
    }

    @Nullable
    public DynamicNotification getCustomNotification()
    {
        return mCustomNotification;
    }

    public abstract String getDescription();

    @Override
    public Identity getIdentity()
    {
        return Identity.withORs(hashCode());
    }

    public Kuick getKuick()
    {
        if (mKuick == null)
            mKuick = AppUtils.getKuick(getService());
        return mKuick;
    }

    protected MediaScannerConnection getMediaScanner()
    {
        return getService().getMediaScanner();
    }

    protected NotificationHelper getNotificationHelper()
    {
        return getService().getNotificationHelper();
    }

    protected BackgroundService getService()
    {
        return mService;
    }

    private Stoppable getStoppable()
    {
        if (mStoppable == null)
            mStoppable = new StoppableImpl();

        return mStoppable;
    }

    public int getTaskGroup()
    {
        return TASK_GROUP_DEFAULT;
    }

    public abstract String getTitle();

    @Override
    public boolean hasCloser(Closer closer)
    {
        return getStoppable().hasCloser(closer);
    }

    @Override
    public int hashCode()
    {
        return mHash != 0 ? mHash : super.hashCode();
    }

    public boolean interrupt()
    {
        return getStoppable().interrupt();
    }

    public boolean interrupt(boolean userAction)
    {
        return getStoppable().interrupt(userAction);
    }

    @Override
    public boolean isInterrupted()
    {
        return getStoppable().isInterrupted();
    }

    @Override
    public boolean isInterruptedByUser()
    {
        return getStoppable().isInterruptedByUser();
    }

    public Progress progress()
    {
        return Progress.dissect(mProgressListener);
    }

    public Progress.Listener progressListener()
    {
        // This ensures when Progress.Listener.getProgress() is called, it doesn't return a null object.
        // Of course, if the user needs the progress itself, then, he or she should use #progress() method.
        progress();
        return mProgressListener;
    }

    public boolean publishStatus()
    {
        mPublishRequested = true;
        return false;
    }

    @Override
    public boolean removeCloser(Closer closer)
    {
        return getStoppable().removeCloser(closer);
    }

    @Override
    public void reset()
    {
        getStoppable().reset();
    }

    @Override
    public void reset(boolean resetClosers)
    {
        getStoppable().reset(resetClosers);
    }

    @Override
    public void removeClosers()
    {
        getStoppable().removeClosers();
    }

    public void run(BackgroundService service)
    {
        try {
            setService(service);
            run(getStoppable());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            setService(null);
        }
    }

    public boolean run(final Context context)
    {
        ServiceConnection serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {
                AppUtils.startService(context, new Intent(context, BackgroundService.class));

                BackgroundService workerService = ((BackgroundService.LocalBinder) service).getService();
                workerService.run(BackgroundTask.this);

                context.unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name)
            {

            }
        };

        return context.bindService(new Intent(context, BackgroundService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
    }

    public void setContentIntent(PendingIntent intent)
    {
        mActivityIntent = intent;
    }

    public void setContentIntent(Context context, Intent intent)
    {
        mHash = hashIntent(intent);
        setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
    }

    public void setCurrentContent(String content)
    {
        mCurrentContent = content;
    }

    public void setCustomNotification(DynamicNotification notification)
    {
        mCustomNotification = notification;
    }

    private void setService(@Nullable BackgroundService service)
    {
        mService = service;
    }

    public BackgroundTask setStoppable(Stoppable stoppable)
    {
        mStoppable = stoppable;
        return this;
    }

    private class ProgressListener extends Progress.SimpleListener
    {
        @Override
        public boolean onProgressChange(Progress progress)
        {
            BackgroundTask.this.onProgressChange(progress);
            publishStatus();
            return !isInterrupted();
        }
    }
}
