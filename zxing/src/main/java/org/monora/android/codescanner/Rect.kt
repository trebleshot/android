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

package org.monora.android.codescanner

import android.graphics.Matrix
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

fun Rect.bound(left: Int, top: Int, right: Int, bottom: Int): Rect {
    return if (this.left >= left && this.top >= top && this.right <= right && this.bottom <= bottom) {
        this
    } else Rect(this).also {
        it.set(max(it.left, left), max(it.top, top), min(it.right, right), min(it.bottom, bottom))
    }
}

fun Rect.fitIn(area: Rect): Rect {
    val a = Rect(this)
    val b = Rect(area)

    if (a.left >= b.left && a.top >= b.top && a.right <= b.right && a.bottom <= b.bottom) return this

    val fitWidth = min(a.width(), b.width())
    val fitHeight = min(a.height(), b.height())

    if (a.left < b.left) {
        a.left = b.left
        a.right = a.left + fitWidth
    } else if (a.right > b.right) {
        a.right = b.right
        a.left = a.right - fitWidth
    }
    if (a.top < b.top) {
        a.top = b.top
        a.bottom = a.top + fitHeight
    } else if (a.bottom > b.bottom) {
        a.bottom = b.bottom
        a.top = a.bottom - fitHeight
    }
    return Rect(left, top, right, bottom)
}

fun Rect.isPointInside(x: Int, y: Int): Boolean {
    return left < x && top < y && right > x && bottom > y
}

fun Rect.rotate(angle: Float, x: Float, y: Float): Rect {
    val rect = floatArrayOf(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    Matrix().apply {
        postRotate(angle, x, y)
        mapPoints(rect)
    }

    var left = rect[0].toInt()
    var top = rect[1].toInt()
    var right = rect[2].toInt()
    var bottom = rect[3].toInt()

    if (left > right) {
        val temp = left
        left = right
        right = temp
    }
    if (top > bottom) {
        val temp = top
        top = bottom
        bottom = temp
    }
    return Rect(left, top, right, bottom)
}