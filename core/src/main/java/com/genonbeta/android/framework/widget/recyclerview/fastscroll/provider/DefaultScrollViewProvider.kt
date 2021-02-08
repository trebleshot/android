package com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider

import android.graphics.drawable.InsetDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.genonbeta.android.framework.R
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.Utils

/**
 * Created by Michal on 05/08/16.
 */
class DefaultScrollViewProvider : ScrollViewProvider() {
    override lateinit var bubbleTextView: TextView

    override lateinit var bubbleView: View

    override lateinit var handleView: View

    override fun getBubbleOffset(): Int = if (scroller.horizontalLayout)
        (handleView.width.toFloat() / 2f - bubbleView.width).toInt()
    else
        (handleView.height.toFloat() / 2f - bubbleView.height).toInt()

    override fun recreateViews(container: ViewGroup?) {
        bubbleTextView = LayoutInflater.from(context).inflate(
            R.layout.genfw_fastscroll_default_bubble, container, false
        ) as TextView
        bubbleView = bubbleTextView

        handleView = View(context).also {
            val inset = context.resources.getDimensionPixelSize(R.dimen.genfw_fastscroll_handle_inset)
            val verticalInset = if (scroller.horizontalLayout) inset else 0
            val horizontalInset = if (scroller.horizontalLayout) 0 else inset
            val handleBackground = InsetDrawable(
                ContextCompat.getDrawable(context, R.drawable.genfw_fastscroll_default_handle),
                horizontalInset,
                verticalInset,
                horizontalInset,
                verticalInset
            )

            Utils.setBackground(it, handleBackground)

            val handleWidth = context.resources.getDimensionPixelSize(
                if (scroller.horizontalLayout)
                    R.dimen.genfw_fastscroll_handle_height
                else
                    R.dimen.genfw_fastscroll_handle_clickable_width
            )
            val handleHeight = context.resources.getDimensionPixelSize(
                if (scroller.horizontalLayout)
                    R.dimen.genfw_fastscroll_handle_clickable_width
                else
                    R.dimen.genfw_fastscroll_handle_height
            )

            it.layoutParams = ViewGroup.LayoutParams(handleWidth, handleHeight)
        }

        bubbleBehavior = DefaultBubbleBehavior(VisibilityAnimationManager.Builder(bubbleView)
            .withPivotX(1f)
            .withPivotY(1f)
            .build())
    }
}