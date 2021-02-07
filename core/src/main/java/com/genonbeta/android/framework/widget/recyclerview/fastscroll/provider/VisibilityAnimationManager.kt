package com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.view.View
import androidx.annotation.AnimatorRes
import com.genonbeta.android.framework.R

/**
 * Created by Michal on 05/08/16.
 * Animates showing and hiding elements of the [FastScroller] (handle and bubble).
 * The decision when to show/hide the element should be implemented via [ViewBehavior].
 */
class VisibilityAnimationManager protected constructor(
    private val view: View,
    @AnimatorRes showAnimator: Int,
    @AnimatorRes hideAnimator: Int,
    private val pivotXRelative: Float,
    private val pivotYRelative: Float,
    hideDelay: Int
) {
    protected var hideAnimator: Animator = AnimatorInflater.loadAnimator(view.context, hideAnimator)

    protected var showAnimator: Animator = AnimatorInflater.loadAnimator(view.context, showAnimator)

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

    protected fun updatePivot() {
        view.pivotX = pivotXRelative * view.getMeasuredWidth()
        view.pivotY = pivotYRelative * view.getMeasuredHeight()
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

        abstract fun build(): T?
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
            //because onAnimationEnd() goes off even for canceled animations
            var mWasCanceled = false
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                if (!mWasCanceled) view.setVisibility(View.INVISIBLE)
                mWasCanceled = false
            }

            override fun onAnimationCancel(animation: Animator?) {
                super.onAnimationCancel(animation)
                mWasCanceled = true
            }
        })
        updatePivot()
    }
}