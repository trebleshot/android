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
package com.genonbeta.TrebleShot.view

import android.content.*
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.genonbeta.TrebleShot.R

/**
 * created by: Veli
 * date: 27.03.2018 22:32
 */
class ComparativeRelativeLayout(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var mAlwaysUseWidth = true
    private var mBaseOnSmaller = false
    private var mTallerExtraLength = 0
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Set a proportional layout.
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (mBaseOnSmaller) {
            if (widthMeasureSpec > heightMeasureSpec) widthMeasureSpec =
                heightMeasureSpec + mTallerExtraLength else if (heightMeasureSpec > widthMeasureSpec) heightMeasureSpec =
                widthMeasureSpec + mTallerExtraLength
        } else if (mAlwaysUseWidth) heightMeasureSpec = widthMeasureSpec + mTallerExtraLength else widthMeasureSpec =
            heightMeasureSpec + mTallerExtraLength
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    init {
        val typedAttributes: TypedArray = context.theme
            .obtainStyledAttributes(attrs, R.styleable.ComparativeRelativeLayout, defStyleAttr, 0)
        mBaseOnSmaller = typedAttributes.getBoolean(
            R.styleable.ComparativeRelativeLayout_baseOnSmallerLength, mBaseOnSmaller
        )
        mTallerExtraLength = typedAttributes.getDimensionPixelSize(
            R.styleable.ComparativeRelativeLayout_tallerLengthExtra, mTallerExtraLength
        )
        mAlwaysUseWidth = typedAttributes.getBoolean(
            R.styleable.ComparativeRelativeLayout_alwaysUseWidth, mAlwaysUseWidth
        )
    }
}