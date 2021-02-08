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