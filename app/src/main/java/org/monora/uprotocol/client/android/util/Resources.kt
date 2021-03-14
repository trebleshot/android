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
}