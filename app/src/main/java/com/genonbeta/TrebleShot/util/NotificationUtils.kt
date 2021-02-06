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
package com.genonbeta.TrebleShot.util

import android.content.*
import android.os.*
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.database.Kuick

/**
 * Created by: veli
 * Date: 4/28/17 2:00 AM
 */
class NotificationUtils(val context: Context, kuick: Kuick?, preferences: SharedPreferences?) {
    val database: Kuick?
    private val mManager: NotificationManagerCompat
    val preferences: SharedPreferences?
    fun buildDynamicNotification(notificationId: Long, channelId: String?): DynamicNotification {
        // Let's hope it will turn out to be less painful
        return DynamicNotification(
            context, manager, channelId,
            (if (notificationId > Int.MAX_VALUE) notificationId / 100000 else notificationId).toInt()
        )
    }

    fun cancel(notificationId: Int) {
        mManager.cancel(notificationId)
    }

    val manager: NotificationManagerCompat
        get() = mManager
    val notificationSettings: Int
        get() {
            val makeSound =
                if (preferences!!.getBoolean("notification_sound", true)) NotificationCompat.DEFAULT_SOUND else 0
            val vibrate =
                if (preferences.getBoolean("notification_vibrate", true)) NotificationCompat.DEFAULT_VIBRATE else 0
            val light =
                if (preferences.getBoolean("notification_light", false)) NotificationCompat.DEFAULT_LIGHTS else 0
            return makeSound or vibrate or light
        }

    companion object {
        const val TAG = "NotificationUtils"
        const val NOTIFICATION_CHANNEL_HIGH = "tsHighPriority"
        const val NOTIFICATION_CHANNEL_LOW = "tsLowPriority"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }

    init {
        mManager = NotificationManagerCompat.from(context)
        database = kuick
        this.preferences = preferences
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelHigh = NotificationChannel(
                NOTIFICATION_CHANNEL_HIGH,
                context.getString(R.string.text_notificationChannelHigh), NotificationManager.IMPORTANCE_HIGH
            )
            channelHigh.enableLights(preferences!!.getBoolean("notification_light", false))
            channelHigh.enableVibration(preferences.getBoolean("notification_vibrate", false))
            mManager.createNotificationChannel(channelHigh)
            val channelLow = NotificationChannel(
                NOTIFICATION_CHANNEL_LOW,
                context.getString(R.string.text_notificationChannelLow), NotificationManager.IMPORTANCE_LOW
            )
            mManager.createNotificationChannel(channelLow)
        }
    }
}