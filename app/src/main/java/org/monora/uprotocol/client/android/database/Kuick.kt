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
package org.monora.uprotocol.client.android.database

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.android.database.*
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask

/**
 * Created by: veli
 * Date: 4/14/17 11:47 PM
 */
class Kuick(context: Context) : KuickDb(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {

    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, current: Int) {

    }

    companion object {
        const val DATABASE_VERSION = 13
        val TAG = Kuick::class.java.simpleName
        val DATABASE_NAME = Kuick::class.java.simpleName + ".db"
    }
}