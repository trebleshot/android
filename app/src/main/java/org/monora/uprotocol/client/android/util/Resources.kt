/*
 * Copyright (C) 2021 Veli Tasalı
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
import android.util.TypedValue
import androidx.annotation.AnyRes
import androidx.core.content.ContextCompat

object Resources {
    @AnyRes
    fun Int.attrToRes(context: Context): Int {
        val typedValue = TypedValue()
        if (!context.theme.resolveAttribute(this, typedValue, true)) {
            val values = context.theme.obtainStyledAttributes(context.applicationInfo.theme, intArrayOf(this))
            return if (values.length() > 0) values.getResourceId(0, 0) else 0
        }
        return typedValue.resourceId
    }

    fun Int.resToColor(context: Context) = ContextCompat.getColor(context, this)

    fun Int.resToDrawable(context: Context) = ContextCompat.getDrawable(context, this)
}