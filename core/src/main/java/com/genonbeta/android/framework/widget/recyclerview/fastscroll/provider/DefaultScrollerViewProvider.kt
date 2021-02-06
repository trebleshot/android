package com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider

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
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.Utils

/**
 * Created by Michal on 05/08/16.
 */
class DefaultScrollerViewProvider : ScrollerViewProvider() {
    protected var mBubble: View? = null
    protected var mHandle: View? = null
    override fun provideHandleView(container: ViewGroup?): View? {
        mHandle = View(context)
        val verticalInset =
            if (scroller.isVertical) 0 else context.resources.getDimensionPixelSize(R.dimen.genfw_fastscroll_handle_inset)
        val horizontalInset =
            if (!scroller.isVertical) 0 else context.resources.getDimensionPixelSize(R.dimen.genfw_fastscroll_handle_inset)
        val handleBg = InsetDrawable(
            ContextCompat.getDrawable(context, R.drawable.genfw_fastscroll_default_handle),
            horizontalInset,
            verticalInset,
            horizontalInset,
            verticalInset
        )
        Utils.setBackground(mHandle, handleBg)
        val handleWidth =
            context.resources.getDimensionPixelSize(if (scroller.isVertical) R.dimen.genfw_fastscroll_handle_clickable_width else R.dimen.genfw_fastscroll_handle_height)
        val handleHeight =
            context.resources.getDimensionPixelSize(if (scroller.isVertical) R.dimen.genfw_fastscroll_handle_height else R.dimen.genfw_fastscroll_handle_clickable_width)
        val params: ViewGroup.LayoutParams = ViewGroup.LayoutParams(handleWidth, handleHeight)
        mHandle.setLayoutParams(params)
        return mHandle
    }

    override fun provideBubbleView(container: ViewGroup?): View? {
        return LayoutInflater.from(context).inflate(R.layout.genfw_fastscroll_default_bubble, container, false)
            .also { mBubble = it }
    }

    override fun provideBubbleTextView(): TextView? {
        return mBubble as TextView?
    }

    override fun getBubbleOffset(): Int {
        return (if (scroller.isVertical) mHandle.getHeight() as Float / 2f - mBubble.getHeight() else mHandle.getWidth() as Float / 2f - mBubble.getWidth()) as Int
    }

    override fun provideHandleBehavior(): ViewBehavior? {
        return null
    }

    override fun provideBubbleBehavior(): ViewBehavior? {
        return DefaultBubbleBehavior(VisibilityAnimationManager.Builder(mBubble).withPivotX(1f).withPivotY(1f).build())
    }
}