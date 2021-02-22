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
package org.monora.uprotocol.client.android.util

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Created by: veli
 * Date: 4/28/17 2:22 AM
 */
class DynamicNotification(
    val context: Context,
    val manager: NotificationManagerCompat,
    notificationChannel: String,
    val notificationId: Int
) : NotificationCompat.Builder(context, notificationChannel) {
    fun cancel(): DynamicNotification {
        manager.cancel(notificationId)
        return this
    }

    fun show(): DynamicNotification {
        manager.notify(notificationId, build())
        return this
    }

    fun updateProgress(max: Int, percent: Int, indeterminate: Boolean): DynamicNotification {
        setProgress(max, percent, indeterminate)
        show()
        return this
    }
}