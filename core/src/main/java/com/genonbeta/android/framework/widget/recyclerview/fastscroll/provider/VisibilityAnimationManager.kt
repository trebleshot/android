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

package com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.annotation.AnimatorRes
import com.genonbeta.android.framework.R

class VisibilityAnimationManager private constructor(
    private val view: View,
    @AnimatorRes showAnimator: Int,
    @AnimatorRes hideAnimator: Int,
    private val pivotXRelative: Float,
    private val pivotYRelative: Float,
    hideDelay: Int
) {
    private var hideAnimator: Animator = AnimatorInflater.loadAnimator(view.context, hideAnimator)

    private var showAnimator: Animator = AnimatorInflater.loadAnimator(view.context, showAnimator)

    fun show() {
        hideAnimator.cancel()

        if (view.visibility == View.INVISIBLE) {
            view.visibility = View.VISIBLE
            updatePivot()
            showAnimator.start()
        }
    }

    fun hide() {
        updatePivot()
        hideAnimator.start()
    }

    private fun updatePivot() {
        view.pivotX = pivotXRelative * view.measuredWidth
        view.pivotY = pivotYRelative * view.measuredHeight
    }

    abstract class AbsBuilder<T : VisibilityAnimationManager?>(protected val view: View) {
        protected var mShowAnimatorResource: Int = R.animator.genfw_fastscroll_default_show
        protected var mHideAnimatorResource: Int = R.animator.genfw_fastscroll_default_hide
        protected var mHideDelay = 1000
        protected var mPivotX = 0.5f
        protected var mPivotY = 0.5f
        fun withShowAnimator(@AnimatorRes showAnimatorResource: Int): AbsBuilder<T> {
            mShowAnimatorResource = showAnimatorResource
            return this
        }

        fun withHideAnimator(@AnimatorRes hideAnimatorResource: Int): AbsBuilder<T> {
            mHideAnimatorResource = hideAnimatorResource
            return this
        }

        fun withHideDelay(hideDelay: Int): AbsBuilder<T> {
            mHideDelay = hideDelay
            return this
        }

        fun withPivotX(pivotX: Float): AbsBuilder<T> {
            mPivotX = pivotX
            return this
        }

        fun withPivotY(pivotY: Float): AbsBuilder<T> {
            mPivotY = pivotY
            return this
        }

        abstract fun build(): T
    }

    class Builder(view: View) : AbsBuilder<VisibilityAnimationManager>(view) {
        override fun build(): VisibilityAnimationManager {
            return VisibilityAnimationManager(
                view,
                mShowAnimatorResource,
                mHideAnimatorResource,
                mPivotX,
                mPivotY,
                mHideDelay
            )
        }
    }

    init {
        this.hideAnimator.startDelay = hideDelay.toLong()
        this.hideAnimator.setTarget(view)
        this.showAnimator.setTarget(view)
        this.hideAnimator.addListener(object : AnimatorListenerAdapter() {
            var wasCanceled = false

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                if (!wasCanceled)
                    view.visibility = View.INVISIBLE

                wasCanceled = false
            }

            override fun onAnimationCancel(animation: Animator?) {
                super.onAnimationCancel(animation)
                wasCanceled = true
            }
        })

        updatePivot()
    }
}