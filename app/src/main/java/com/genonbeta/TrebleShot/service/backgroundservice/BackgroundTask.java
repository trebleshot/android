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
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Identifiable;
import com.genonbeta.TrebleShot.object.Identity;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.util.*;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.framework.util.StoppableImpl;

import java.util.List;

import static com.genonbeta.TrebleShot.service.BackgroundService.*;

public abstract class BackgroundTask extends StoppableJob implements Stoppable, Identifiable
{
    private Stoppable mStoppable;
    private BackgroundService mService;
    private String mStatusText;
    private String mTitle;
    private int mIconRes;
    private long mLastNotified = 0;
    private int mHash = 0;
    private DynamicNotification mNotification;
    private PendingIntent mActivityIntent;

    protected abstract void onRun() throws InterruptedException;

    @Override
    public boolean addCloser(Closer closer)
    {
        return getStoppable().addCloser(closer);
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

    @Override
    public Identity getIdentity()
    {
        return Identity.withORs(hashCode());
    }

    public int getIconRes()
    {
        return mIconRes;
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

    public String getStatusText()
    {
        return mStatusText;
    }

    private Stoppable getStoppable()
    {
        if (mStoppable == null)
            mStoppable = new StoppableImpl();

        return mStoppable;
    }

    public String getTitle()
    {
        return mTitle;
    }

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

    private void publishNotification()
    {
        if (mNotification == null) {
            PendingIntent cancelIntent = PendingIntent.getService(getService(), AppUtils.getUniqueNumber(),
                    new Intent(getService(), BackgroundService.class)
                            .setAction(ACTION_KILL_SIGNAL)
                            .putExtra(EXTRA_IDENTITY, getIdentity()), 0);

            mNotification = getService().getNotificationUtils().buildDynamicNotification(hashCode(),
                    NotificationUtils.NOTIFICATION_CHANNEL_LOW);

            mNotification.setSmallIcon(getIconRes() == 0
                    ? R.drawable.ic_autorenew_white_24dp_static : getIconRes())
                    .setContentTitle(getService().getString(R.string.text_taskOngoing))
                    .addAction(R.drawable.ic_close_white_24dp_static,
                            getService().getString(R.string.butn_cancel), cancelIntent);

            if (mActivityIntent != null)
                mNotification.setContentIntent(mActivityIntent);
        }

        mNotification.setContentTitle(getTitle())
                .setContentText(getStatusText());

        mNotification.show();
    }

    public boolean publishStatusText(String text)
    {
        mStatusText = text;
        long time = System.nanoTime();

        if (time - mLastNotified > 2e9) {
            publishNotification();
            mLastNotified = time;

            return true;
        }
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

    protected void removeNotification()
    {
        if (mNotification != null)
            mNotification.cancel();
    }


    public void run(BackgroundService service)
    {
        try {
            setService(service);
            publishNotification();
            run(getStoppable());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            removeNotification();
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

    public BackgroundTask setContentIntent(PendingIntent intent)
    {
        mActivityIntent = intent;
        return this;
    }

    public BackgroundTask setContentIntent(Context context, Intent intent)
    {
        mHash = hashIntent(intent);
        return setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
    }

    public BackgroundTask setIconRes(int iconRes)
    {
        mIconRes = iconRes;
        return this;
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

    public BackgroundTask setTitle(String title)
    {
        mTitle = title;
        return this;
    }
}
