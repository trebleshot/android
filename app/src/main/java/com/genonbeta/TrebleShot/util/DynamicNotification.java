package com.genonbeta.TrebleShot.util;

import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Created by: veli
 * Date: 4/28/17 2:22 AM
 */

public class DynamicNotification extends NotificationCompat.Builder
{
    private NotificationManagerCompat mManager;
    private int mNotificationId;

    public DynamicNotification(Context context, NotificationManagerCompat manager, String notificationChannel, int notificationId)
    {
        super(context, notificationChannel);
        mManager = manager;
        mNotificationId = notificationId;
    }

    public DynamicNotification cancel()
    {
        mManager.cancel(mNotificationId);
        return this;
    }

    public int getNotificationId()
    {
        return mNotificationId;
    }

    public DynamicNotification setNotificationId(int notificationId)
    {
        mNotificationId = notificationId;
        return this;
    }

    public DynamicNotification show()
    {
        mManager.notify(mNotificationId, build());
        return this;
    }

    public DynamicNotification updateProgress(int max, int percent, boolean indeterminate)
    {
        setProgress(max, percent, indeterminate);
        mManager.notify(mNotificationId, build());

        return this;
    }
}
