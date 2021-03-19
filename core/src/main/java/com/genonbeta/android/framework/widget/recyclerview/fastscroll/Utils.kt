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

package com.genonbeta.android.framework.widget.recyclerview.fastscroll

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import kotlin.math.max
import kotlin.math.min

object Utils {
    fun getViewRawY(view: View): Int {
        val location = IntArray(2)
        location[0] = 0
        location[1] = view.y.toInt()
        (view.parent as View).getLocationInWindow(location)
        return location[1]
    }

    fun getViewRawX(view: View): Float {
        val location = IntArray(2)
        location[0] = view.x.toInt()
        location[1] = 0
        (view.parent as View).getLocationInWindow(location)
        return location[0].toFloat()
    }

    fun getValueInRange(min: Float, max: Float, value: Float): Float {
        val minimum = max(min, value)
        return min(minimum, max)
    }

    fun setBackground(view: View, drawable: Drawable?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.background = drawable
        } else {
            view.setBackgroundDrawable(drawable)
        }
    }
}