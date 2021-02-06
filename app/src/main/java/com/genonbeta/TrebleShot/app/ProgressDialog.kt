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
    private var mProgress: ProgressBar? = null
    private var mProgressStyle = STYLE_SPINNER
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
    constructor(context: Context?) : super(context!!) {}

    /**
     * Creates a Progress dialog.
     *
     * @param context the parent context
     * @param theme   the resource ID of the theme against which to inflate
     * this dialog, or `0` to use the parent
     * `context`'s default alert dialog theme
     */
    constructor(context: Context?, theme: Int) : super(context!!, theme) {}

    override fun onCreate(savedInstanceState: Bundle) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.internal_layout_alert_dialog_progress, null)

        /* Use a separate handler to update the text views as they
         * must be updated on the same thread that created them.
         */mProgress = view.findViewById(R.id.progress)
        setView(view)
        if (mMax > 0) max = mMax
        if (mProgressVal > 0) progress = mProgressVal
        if (mSecondaryProgressVal > 0) secondaryProgress = mSecondaryProgressVal
        if (mIncrementBy > 0) incrementProgressBy(mIncrementBy)
        if (mIncrementSecondaryBy > 0) incrementSecondaryProgressBy(mIncrementSecondaryBy)
        if (mProgressDrawable != null) setProgressDrawable(mProgressDrawable)
        if (mIndeterminateDrawable != null) setIndeterminateDrawable(mIndeterminateDrawable)
        isIndeterminate = mIndeterminate
        onProgressChanged()
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
        get() = if (mProgress != null) {
            mProgress!!.progress
        } else mProgressVal
        set(value) {
            if (mHasStarted) {
                mProgress!!.progress = value
                onProgressChanged()
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
        get() = if (mProgress != null) {
            mProgress!!.secondaryProgress
        } else mSecondaryProgressVal
        set(secondaryProgress) {
            if (mProgress != null) {
                mProgress!!.secondaryProgress = secondaryProgress
                onProgressChanged()
            } else {
                mSecondaryProgressVal = secondaryProgress
            }
        }
    /**
     * Gets the maximum allowed progress value. The default value is 100.
     *
     * @return the maximum value
     */
    /**
     * Sets the maximum allowed progress value.
     */
    var max: Int
        get() = if (mProgress != null) {
            mProgress!!.max
        } else mMax
        set(max) {
            if (mProgress != null) {
                mProgress!!.max = max
                onProgressChanged()
            } else {
                mMax = max
            }
        }

    /**
     * Increments the current progress value.
     *
     * @param diff the amount by which the current progress will be incremented,
     * up to [.getMax]
     */
    fun incrementProgressBy(diff: Int) {
        if (mProgress != null) {
            mProgress!!.incrementProgressBy(diff)
            onProgressChanged()
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
        if (mProgress != null) {
            mProgress!!.incrementSecondaryProgressBy(diff)
            onProgressChanged()
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
        if (mProgress != null) {
            mProgress!!.progressDrawable = d
        } else {
            mProgressDrawable = d
        }
    }

    /**
     * Sets the drawable to be used to display the indeterminate progress value.
     *
     * @param d the drawable to be used
     * @see ProgressBar.setProgressDrawable
     * @see .setIndeterminate
     */
    fun setIndeterminateDrawable(d: Drawable?) {
        if (mProgress != null) {
            mProgress!!.indeterminateDrawable = d
        } else {
            mIndeterminateDrawable = d
        }
    }
    /**
     * Whether this ProgressDialog is in indeterminate mode.
     *
     * @return true if the dialog is in indeterminate mode, false otherwise
     */
    /**
     * Change the indeterminate mode for this ProgressDialog. In indeterminate
     * mode, the progress is ignored and the dialog shows an infinite
     * animation instead.
     *
     *
     * **Note:** A ProgressDialog with style [.STYLE_SPINNER]
     * is always indeterminate and will ignore this setting.
     *
     * @param indeterminate true to enable indeterminate mode, false otherwise
     * @see .setProgressStyle
     */
    var isIndeterminate: Boolean
        get() = if (mProgress != null) {
            mProgress!!.isIndeterminate
        } else mIndeterminate
        set(indeterminate) {
            if (mProgress != null) {
                mProgress!!.isIndeterminate = indeterminate
            } else {
                mIndeterminate = indeterminate
            }
        }

    /**
     * Sets the style of this ProgressDialog, either [.STYLE_SPINNER] or
     * [.STYLE_HORIZONTAL]. The default is [.STYLE_SPINNER].
     *
     *
     * **Note:** A ProgressDialog with style [.STYLE_SPINNER]
     * is always indeterminate and will ignore the [ indeterminate][.setIndeterminate] setting.
     *
     * @param style the style of this ProgressDialog, either [.STYLE_SPINNER] or
     * [.STYLE_HORIZONTAL]
     */
    fun setProgressStyle(style: Int) {
        mProgressStyle = style
    }

    private fun onProgressChanged() {
        if (mProgressStyle == STYLE_HORIZONTAL) {
        }
    }

    companion object {
        /**
         * Creates a ProgressDialog with a circular, spinning progress
         * bar. This is the default.
         */
        const val STYLE_SPINNER = 0

        /**
         * Creates a ProgressDialog with a horizontal progress bar.
         */
        const val STYLE_HORIZONTAL = 1
        /**
         * Creates and shows a ProgressDialog.
         *
         * @param context        the parent context
         * @param title          the title text for the dialog's window
         * @param message        the text to be displayed in the dialog
         * @param indeterminate  true if the dialog should be [                       indeterminate][.setIndeterminate], false otherwise
         * @param cancelable     true if the dialog is [cancelable][.setCancelable],
         * false otherwise
         * @param cancelListener the [listener][.setOnCancelListener]
         * to be invoked when the dialog is canceled
         * @return the ProgressDialog
         */
        /**
         * Creates and shows a ProgressDialog.
         *
         * @param context       the parent context
         * @param title         the title text for the dialog's window
         * @param message       the text to be displayed in the dialog
         * @param indeterminate true if the dialog should be [                      indeterminate][.setIndeterminate], false otherwise
         * @return the ProgressDialog
         */
        /**
         * Creates and shows a ProgressDialog.
         *
         * @param context the parent context
         * @param title   the title text for the dialog's window
         * @param message the text to be displayed in the dialog
         * @return the ProgressDialog
         */
        /**
         * Creates and shows a ProgressDialog.
         *
         * @param context       the parent context
         * @param title         the title text for the dialog's window
         * @param message       the text to be displayed in the dialog
         * @param indeterminate true if the dialog should be [                      indeterminate][.setIndeterminate], false otherwise
         * @param cancelable    true if the dialog is [cancelable][.setCancelable],
         * false otherwise
         * @return the ProgressDialog
         */
        @JvmOverloads
        fun show(
            context: Context?, title: CharSequence?,
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