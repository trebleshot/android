/*
 * Copyright (C) 2019 Veli Tasalı
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

package com.genonbeta.TrebleShot.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.Kuick;

/**
 * Created by: veli
 * Date: 4/28/17 2:00 AM
 */

public class NotificationUtils
{
    public static final String TAG = "NotificationUtils";
    public static final String NOTIFICATION_CHANNEL_HIGH = "tsHighPriority";
    public static final String NOTIFICATION_CHANNEL_LOW = "tsLowPriority";

    public static final String EXTRA_NOTIFICATION_ID = "notificationId";

    private Context mContext;
    private NotificationManagerCompat mManager;
    private Kuick mDatabase;
    private SharedPreferences mPreferences;

    public NotificationUtils(Context context, Kuick kuick, SharedPreferences preferences)
    {
        mContext = context;
        mManager = NotificationManagerCompat.from(context);
        mDatabase = kuick;
        mPreferences = preferences;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);

            NotificationChannel channelHigh = new NotificationChannel(NOTIFICATION_CHANNEL_HIGH,
                    mContext.getString(R.string.text_notificationChannelHigh), NotificationManager.IMPORTANCE_HIGH);

            channelHigh.enableLights(mPreferences.getBoolean("notification_light", false));
            channelHigh.enableVibration(mPreferences.getBoolean("notification_vibrate", false));
            notificationManager.createNotificationChannel(channelHigh);

            NotificationChannel channelLow = new NotificationChannel(NOTIFICATION_CHANNEL_LOW,
                    mContext.getString(R.string.text_notificationChannelLow), NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channelLow);
        }
    }

    public DynamicNotification buildDynamicNotification(long notificationId, String channelId)
    {
        // Let's hope it will turn out to be less painful
        return new DynamicNotification(getContext(), getManager(), channelId,
                (int) (notificationId > Integer.MAX_VALUE ? notificationId / 100000 : notificationId));
    }

    public void cancel(int notificationId)
    {
        mManager.cancel(notificationId);
    }

    public Context getContext()
    {
        return mContext;
    }

    public Kuick getDatabase()
    {
        return mDatabase;
    }

    public NotificationManagerCompat getManager()
    {
        return mManager;
    }

    public int getNotificationSettings()
    {
        int makeSound = (mPreferences.getBoolean("notification_sound", true))
                ? NotificationCompat.DEFAULT_SOUND : 0;
        int vibrate = (mPreferences.getBoolean("notification_vibrate", true))
                ? NotificationCompat.DEFAULT_VIBRATE : 0;
        int light = (mPreferences.getBoolean("notification_light", false))
                ? NotificationCompat.DEFAULT_LIGHTS : 0;

        return makeSound | vibrate | light;
    }

    public SharedPreferences getPreferences()
    {
        return mPreferences;
    }
}
