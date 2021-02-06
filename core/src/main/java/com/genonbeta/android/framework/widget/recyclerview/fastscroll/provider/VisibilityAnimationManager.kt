package com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider

import android.animation.Animator
import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import android.view.View
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * Created by Michal on 05/08/16.
 * Animates showing and hiding elements of the [FastScroller] (handle and bubble).
 * The decision when to show/hide the element should be implemented via [ViewBehavior].
 */
class VisibilityAnimationManager protected constructor(
    protected val mView: View?,
    @AnimatorRes showAnimator: Int,
    @AnimatorRes hideAnimator: Int,
    private val mPivotXRelative: Float,
    private val mPivotYRelative: Float,
    hideDelay: Int
) {
    protected var mHideAnimator: AnimatorSet?
    protected var mShowAnimator: AnimatorSet?
    fun show() {
        mHideAnimator.cancel()
        if (mView.getVisibility() == View.INVISIBLE) {
            mView.setVisibility(View.VISIBLE)
            updatePivot()
            mShowAnimator.start()
        }
    }

    fun hide() {
        updatePivot()
        mHideAnimator.start()
    }

    protected fun updatePivot() {
        mView.setPivotX(mPivotXRelative * mView.getMeasuredWidth())
        mView.setPivotY(mPivotYRelative * mView.getMeasuredHeight())
    }

    abstract class AbsBuilder<T : VisibilityAnimationManager?>(protected val mView: View?) {
        protected var mShowAnimatorResource: Int = R.animator.genfw_fastscroll_default_show
        protected var mHideAnimatorResource: Int = R.animator.genfw_fastscroll_default_hide
        protected var mHideDelay = 1000
        protected var mPivotX = 0.5f
        protected var mPivotY = 0.5f
        fun withShowAnimator(@AnimatorRes showAnimatorResource: Int): AbsBuilder<T?>? {
            mShowAnimatorResource = showAnimatorResource
            return this
        }

        fun withHideAnimator(@AnimatorRes hideAnimatorResource: Int): AbsBuilder<T?>? {
            mHideAnimatorResource = hideAnimatorResource
            return this
        }

        fun withHideDelay(hideDelay: Int): AbsBuilder<T?>? {
            mHideDelay = hideDelay
            return this
        }

        fun withPivotX(pivotX: Float): AbsBuilder<T?>? {
            mPivotX = pivotX
            return this
        }

        fun withPivotY(pivotY: Float): AbsBuilder<T?>? {
            mPivotY = pivotY
            return this
        }

        abstract fun build(): T?
    }

    class Builder(view: View?) : AbsBuilder<VisibilityAnimationManager?>(view) {
        override fun build(): VisibilityAnimationManager? {
            return VisibilityAnimationManager(
                mView,
                mShowAnimatorResource,
                mHideAnimatorResource,
                mPivotX,
                mPivotY,
                mHideDelay
            )
        }
    }

    init {
        mHideAnimator = AnimatorInflater.loadAnimator(mView.getContext(), hideAnimator) as AnimatorSet
        mHideAnimator.setStartDelay(hideDelay.toLong())
        mHideAnimator.setTarget(mView)
        mShowAnimator = AnimatorInflater.loadAnimator(mView.getContext(), showAnimator) as AnimatorSet
        mShowAnimator.setTarget(mView)
        mHideAnimator.addListener(object : AnimatorListenerAdapter() {
            //because onAnimationEnd() goes off even for canceled animations
            var mWasCanceled = false
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                if (!mWasCanceled) mView.setVisibility(View.INVISIBLE)
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