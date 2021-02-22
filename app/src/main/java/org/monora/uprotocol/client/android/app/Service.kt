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
package org.monora.uprotocol.client.android.app

import android.app.Service
import android.content.SharedPreferences
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.database.Kuick
import org.monora.uprotocol.client.android.util.AppUtils
import java.lang.IllegalStateException

/**
 * created by: veli
 * date: 31.03.2018 15:23
 */
abstract class Service : Service() {
    val app: App
        get() = if (application is App) application as App else throw IllegalStateException()

    val defaultPreferences: SharedPreferences
        get() = AppUtils.getDefaultPreferences(applicationContext)

    val kuick: Kuick
        get() = AppUtils.getKuick(this)
}