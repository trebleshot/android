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
package com.genonbeta.TrebleShot.app

import android.content.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R

open class ProgressDialog : AlertDialog {
    private var progressBar: ProgressBar? = null
    private var progressStyle = STYLE_SPINNER
    private var mMax = 0
    private var mProgressVal = 0
    private var mSecondaryProgressVal = 0
    private var mIncrementBy = 0
    private var mIncrementSecondaryBy = 0
    private var mProgressDrawable: Drawable? = null
    private var mIndeterminateDrawable: Drawable? = null
    private var mIndeterminate = false
    private var mHasStarted = false

    /**
     * Creates a Progress dialog.
     *
     * @param context the parent context
     */
    constructor(context: Context) : super(context)

    /**
     * Creates a Progress dialog.
     *
     * @param context the parent context
     * @param theme   the resource ID of the theme against which to inflate
     * this dialog, or `0` to use the parent
     * `context`'s default alert dialog theme
     */
    constructor(context: Context, theme: Int) : super(context, theme)

    override fun onCreate(savedInstanceState: Bundle?) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.internal_layout_alert_dialog_progress, null)

        /* Use a separate handler to update the text views as they
         * must be updated on the same thread that created them.
         */
        progressBar = view.findViewById(R.id.progress)
        setView(view)
        if (mMax > 0) max = mMax
        if (mProgressVal > 0) progress = mProgressVal
        if (mSecondaryProgressVal > 0) secondaryProgress = mSecondaryProgressVal
        if (mIncrementBy > 0) incrementProgressBy(mIncrementBy)
        if (mIncrementSecondaryBy > 0) incrementSecondaryProgressBy(mIncrementSecondaryBy)
        if (mProgressDrawable != null) setProgressDrawable(mProgressDrawable)
        if (mIndeterminateDrawable != null) setIndeterminateDrawable(mIndeterminateDrawable)
        isIndeterminate = mIndeterminate
        super.onCreate(savedInstanceState)
    }

    public override fun onStart() {
        super.onStart()
        mHasStarted = true
    }

    override fun onStop() {
        super.onStop()
        mHasStarted = false
    }
    /**
     * Gets the current progress.
     *
     * @return the current progress, a value between 0 and [.getMax]
     */
    /**
     * Sets the current progress.
     *
     * @param value the current progress, a value between 0 and [.getMax]
     * @see ProgressBar.setProgress
     */
    var progress: Int
        get() = if (progressBar != null) {
            progressBar!!.progress
        } else mProgressVal
        set(value) {
            if (mHasStarted) {
                progressBar!!.progress = value
            } else {
                mProgressVal = value
            }
        }
    /**
     * Gets the current secondary progress.
     *
     * @return the current secondary progress, a value between 0 and [.getMax]
     */
    /**
     * Sets the secondary progress.
     *
     * @param secondaryProgress the current secondary progress, a value between 0 and
     * [.getMax]
     * @see ProgressBar.setSecondaryProgress
     */
    var secondaryProgress: Int
        get() = if (progressBar != null) {
            progressBar!!.secondaryProgress
        } else mSecondaryProgressVal
        set(secondaryProgress) {
            if (progressBar != null) {
                progressBar!!.secondaryProgress = secondaryProgress
            } else {
                mSecondaryProgressVal = secondaryProgress
            }
        }

    var max: Int
        get() = if (progressBar != null) {
            progressBar!!.max
        } else mMax
        set(max) {
            if (progressBar != null) {
                progressBar!!.max = max
            } else {
                mMax = max
            }
        }

    fun incrementProgressBy(diff: Int) {
        if (progressBar != null) {
            progressBar!!.incrementProgressBy(diff)
        } else {
            mIncrementBy += diff
        }
    }

    /**
     * Increments the current secondary progress value.
     *
     * @param diff the amount by which the current secondary progress will be incremented,
     * up to [.getMax]
     */
    fun incrementSecondaryProgressBy(diff: Int) {
        if (progressBar != null) {
            progressBar!!.incrementSecondaryProgressBy(diff)
        } else {
            mIncrementSecondaryBy += diff
        }
    }

    /**
     * Sets the drawable to be used to display the progress value.
     *
     * @param d the drawable to be used
     * @see ProgressBar.setProgressDrawable
     */
    fun setProgressDrawable(d: Drawable?) {
        if (progressBar != null) {
            progressBar!!.progressDrawable = d
        } else {
            mProgressDrawable = d
        }
    }

    fun setIndeterminateDrawable(d: Drawable?) {
        if (progressBar != null) {
            progressBar!!.indeterminateDrawable = d
        } else {
            mIndeterminateDrawable = d
        }
    }

    var isIndeterminate: Boolean
        get() = if (progressBar != null) {
            progressBar!!.isIndeterminate
        } else mIndeterminate
        set(indeterminate) {
            if (progressBar != null) {
                progressBar!!.isIndeterminate = indeterminate
            } else {
                mIndeterminate = indeterminate
            }
        }

    fun setProgressStyle(style: Int) {
        progressStyle = style
    }

    companion object {

        const val STYLE_SPINNER = 0

        const val STYLE_HORIZONTAL = 1

        @JvmOverloads
        fun show(
            context: Context, title: CharSequence?,
            message: CharSequence?, indeterminate: Boolean = false,
            cancelable: Boolean = false, cancelListener: DialogInterface.OnCancelListener? = null
        ): ProgressDialog {
            val dialog = ProgressDialog(context)
            dialog.setTitle(title)
            dialog.setMessage(message)
            dialog.isIndeterminate = indeterminate
            dialog.setCancelable(cancelable)
            dialog.setOnCancelListener(cancelListener)
            dialog.show()
            return dialog
        }
    }
}