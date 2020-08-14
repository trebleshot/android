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
import android.media.MediaScannerConnection;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Identifiable;
import com.genonbeta.TrebleShot.object.Identifier;
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

public abstract class AsyncTask extends StoppableJob implements Stoppable, Identifiable
{
    public static final String TASK_GROUP_DEFAULT = "TASK_GROUP_DEFAULT";

    private final ProgressListener mProgressListener = new ProgressListener();
    private Kuick mKuick;
    private App mApp;
    private Stoppable mStoppable;
    private PendingIntent mActivityIntent;
    private DynamicNotification mCustomNotification; // The notification that is not part of the default notification.
    private String mCurrentContent;
    private boolean mPublishRequested = false;
    private boolean mFinished = false;
    private boolean mStarted = false;
    private boolean mScheduleRerun = false;
    private int mHash = 0;

    protected abstract void onRun() throws TaskStoppedException;

    protected void onProgressChange(Progress progress)
    {

    }

    @Override
    public boolean addCloser(Closer closer)
    {
        return getStoppable().addCloser(closer);
    }

    public boolean consumeChanges()
    {
        if (mPublishRequested) {
            mPublishRequested = false;
            return true;
        }
        return false;
    }

    public void forceQuit()
    {
        if (!isInterrupted())
            interrupt();
    }

    protected App getApp()
    {
        return mApp;
    }

    protected Context getContext()
    {
        return getApp().getApplicationContext();
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
        return Identity.withORs(Identifier.from(Id.HashCode, hashCode()));
    }

    protected MediaScannerConnection getMediaScanner()
    {
        return getApp().getMediaScanner();
    }

    protected NotificationHelper getNotificationHelper()
    {
        return getApp().getNotificationHelper();
    }

    private Stoppable getStoppable()
    {
        if (mStoppable == null)
            mStoppable = new StoppableImpl();

        return mStoppable;
    }

    public String getTaskGroup()
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

    public boolean isFinished()
    {
        return mFinished;
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

    public boolean isStarted()
    {
        return mStarted;
    }

    public Kuick kuick()
    {
        if (mKuick == null)
            mKuick = AppUtils.getKuick(getContext());
        return mKuick;
    }

    public void post(TaskMessage message) throws TaskStoppedException
    {
        throwIfStopped();

        DynamicNotification notification = message.toNotification(this).show();
        setCustomNotification(notification);
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

    public void rerun(App app)
    {
        if (!isStarted() || isFinished()) {
            reset(true);
            app.run(this);
        } else
            mScheduleRerun = true;
    }

    @Override
    public void reset()
    {
        resetInternal();
        getStoppable().reset();
    }

    @Override
    public void reset(boolean resetClosers)
    {
        resetInternal();
        getStoppable().reset(resetClosers);
    }

    private void resetInternal()
    {
        if (isStarted() && !isFinished())
            throw new IllegalStateException("Can't reset when the task is running");

        setStarted(false);
        setFinished(false);
        progressListener().setProgress(null);
    }

    @Override
    public void removeClosers()
    {
        getStoppable().removeClosers();
    }

    public void run(App app)
    {
        if (isStarted() || isFinished() || isInterrupted())
            throw new IllegalStateException(getClass().getName() + " is already in interrupted state. To run it "
                    + "again with the same configuration you need to use rerun().");

        setStarted(true);
        setApp(app);
        publishStatus();

        try {
            run(getStoppable());
        } catch (TaskStoppedException ignored) {
        } finally {
            setFinished(true);
            publishStatus();
            setApp(null);
        }

        if (mScheduleRerun)
            rerun(app);
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

    public void setCustomNotification(DynamicNotification notification)
    {
        mCustomNotification = notification;
    }

    private void setFinished(boolean finished)
    {
        mFinished = finished;
    }

    public void setOngoingContent(String content)
    {
        mCurrentContent = content;
    }

    private void setApp(@Nullable App service)
    {
        mApp = service;
    }

    private void setStarted(boolean started)
    {
        mStarted = started;
    }

    public void setStoppable(Stoppable stoppable)
    {
        mStoppable = stoppable;
    }

    public void throwIfStopped() throws TaskStoppedException
    {
        if (isInterrupted())
            throw new TaskStoppedException("This task been interrupted", isInterruptedByUser());
    }

    private class ProgressListener extends Progress.SimpleListener
    {
        @Override
        public boolean onProgressChange(Progress progress)
        {
            AsyncTask.this.onProgressChange(progress);
            publishStatus();
            return !isInterrupted();
        }
    }

    public enum Id
    {
        HashCode
    }
}
