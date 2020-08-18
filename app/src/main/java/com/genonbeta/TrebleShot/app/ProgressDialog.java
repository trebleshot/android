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

package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;

public class ProgressDialog extends AlertDialog
{
    /**
     * Creates a ProgressDialog with a circular, spinning progress
     * bar. This is the default.
     */
    public static final int STYLE_SPINNER = 0;

    /**
     * Creates a ProgressDialog with a horizontal progress bar.
     */
    public static final int STYLE_HORIZONTAL = 1;

    private ProgressBar mProgress;

    private int mProgressStyle = STYLE_SPINNER;

    private int mMax;
    private int mProgressVal;
    private int mSecondaryProgressVal;
    private int mIncrementBy;
    private int mIncrementSecondaryBy;
    private Drawable mProgressDrawable;
    private Drawable mIndeterminateDrawable;
    private boolean mIndeterminate;

    private boolean mHasStarted;

    /**
     * Creates a Progress dialog.
     *
     * @param context the parent context
     */
    public ProgressDialog(Context context)
    {
        super(context);
    }

    /**
     * Creates a Progress dialog.
     *
     * @param context the parent context
     * @param theme   the resource ID of the theme against which to inflate
     *                this dialog, or {@code 0} to use the parent
     *                {@code context}'s default alert dialog theme
     */
    public ProgressDialog(Context context, int theme)
    {
        super(context, theme);
    }

    /**
     * Creates and shows a ProgressDialog.
     *
     * @param context the parent context
     * @param title   the title text for the dialog's window
     * @param message the text to be displayed in the dialog
     * @return the ProgressDialog
     */
    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message)
    {
        return show(context, title, message, false);
    }

    /**
     * Creates and shows a ProgressDialog.
     *
     * @param context       the parent context
     * @param title         the title text for the dialog's window
     * @param message       the text to be displayed in the dialog
     * @param indeterminate true if the dialog should be {@link #setIndeterminate(boolean)
     *                      indeterminate}, false otherwise
     * @return the ProgressDialog
     */
    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate)
    {
        return show(context, title, message, indeterminate, false, null);
    }

    /**
     * Creates and shows a ProgressDialog.
     *
     * @param context       the parent context
     * @param title         the title text for the dialog's window
     * @param message       the text to be displayed in the dialog
     * @param indeterminate true if the dialog should be {@link #setIndeterminate(boolean)
     *                      indeterminate}, false otherwise
     * @param cancelable    true if the dialog is {@link #setCancelable(boolean) cancelable},
     *                      false otherwise
     * @return the ProgressDialog
     */
    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate, boolean cancelable)
    {
        return show(context, title, message, indeterminate, cancelable, null);
    }

    /**
     * Creates and shows a ProgressDialog.
     *
     * @param context        the parent context
     * @param title          the title text for the dialog's window
     * @param message        the text to be displayed in the dialog
     * @param indeterminate  true if the dialog should be {@link #setIndeterminate(boolean)
     *                       indeterminate}, false otherwise
     * @param cancelable     true if the dialog is {@link #setCancelable(boolean) cancelable},
     *                       false otherwise
     * @param cancelListener the {@link #setOnCancelListener(OnCancelListener) listener}
     *                       to be invoked when the dialog is canceled
     * @return the ProgressDialog
     */
    public static ProgressDialog show(Context context, CharSequence title,
                                      CharSequence message, boolean indeterminate,
                                      boolean cancelable, OnCancelListener cancelListener)
    {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(indeterminate);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        dialog.show();
        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.internal_layout_alert_dialog_progress, null);

        /* Use a separate handler to update the text views as they
         * must be updated on the same thread that created them.
         */
        mProgress = view.findViewById(R.id.progress);
        setView(view);

        if (mMax > 0)
            setMax(mMax);

        if (mProgressVal > 0)
            setProgress(mProgressVal);

        if (mSecondaryProgressVal > 0)
            setSecondaryProgress(mSecondaryProgressVal);

        if (mIncrementBy > 0)
            incrementProgressBy(mIncrementBy);

        if (mIncrementSecondaryBy > 0)
            incrementSecondaryProgressBy(mIncrementSecondaryBy);

        if (mProgressDrawable != null)
            setProgressDrawable(mProgressDrawable);

        if (mIndeterminateDrawable != null)
            setIndeterminateDrawable(mIndeterminateDrawable);

        setIndeterminate(mIndeterminate);
        onProgressChanged();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mHasStarted = true;
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mHasStarted = false;
    }

    /**
     * Gets the current progress.
     *
     * @return the current progress, a value between 0 and {@link #getMax()}
     */
    public int getProgress()
    {
        if (mProgress != null) {
            return mProgress.getProgress();
        }
        return mProgressVal;
    }

    /**
     * Sets the current progress.
     *
     * @param value the current progress, a value between 0 and {@link #getMax()}
     * @see ProgressBar#setProgress(int)
     */
    public void setProgress(int value)
    {
        if (mHasStarted) {
            mProgress.setProgress(value);
            onProgressChanged();
        } else {
            mProgressVal = value;
        }
    }

    /**
     * Gets the current secondary progress.
     *
     * @return the current secondary progress, a value between 0 and {@link #getMax()}
     */
    public int getSecondaryProgress()
    {
        if (mProgress != null) {
            return mProgress.getSecondaryProgress();
        }
        return mSecondaryProgressVal;
    }

    /**
     * Sets the secondary progress.
     *
     * @param secondaryProgress the current secondary progress, a value between 0 and
     *                          {@link #getMax()}
     * @see ProgressBar#setSecondaryProgress(int)
     */
    public void setSecondaryProgress(int secondaryProgress)
    {
        if (mProgress != null) {
            mProgress.setSecondaryProgress(secondaryProgress);
            onProgressChanged();
        } else {
            mSecondaryProgressVal = secondaryProgress;
        }
    }

    /**
     * Gets the maximum allowed progress value. The default value is 100.
     *
     * @return the maximum value
     */
    public int getMax()
    {
        if (mProgress != null) {
            return mProgress.getMax();
        }
        return mMax;
    }

    /**
     * Sets the maximum allowed progress value.
     */
    public void setMax(int max)
    {
        if (mProgress != null) {
            mProgress.setMax(max);
            onProgressChanged();
        } else {
            mMax = max;
        }
    }

    /**
     * Increments the current progress value.
     *
     * @param diff the amount by which the current progress will be incremented,
     *             up to {@link #getMax()}
     */
    public void incrementProgressBy(int diff)
    {
        if (mProgress != null) {
            mProgress.incrementProgressBy(diff);
            onProgressChanged();
        } else {
            mIncrementBy += diff;
        }
    }

    /**
     * Increments the current secondary progress value.
     *
     * @param diff the amount by which the current secondary progress will be incremented,
     *             up to {@link #getMax()}
     */
    public void incrementSecondaryProgressBy(int diff)
    {
        if (mProgress != null) {
            mProgress.incrementSecondaryProgressBy(diff);
            onProgressChanged();
        } else {
            mIncrementSecondaryBy += diff;
        }
    }

    /**
     * Sets the drawable to be used to display the progress value.
     *
     * @param d the drawable to be used
     * @see ProgressBar#setProgressDrawable(Drawable)
     */
    public void setProgressDrawable(Drawable d)
    {
        if (mProgress != null) {
            mProgress.setProgressDrawable(d);
        } else {
            mProgressDrawable = d;
        }
    }

    /**
     * Sets the drawable to be used to display the indeterminate progress value.
     *
     * @param d the drawable to be used
     * @see ProgressBar#setProgressDrawable(Drawable)
     * @see #setIndeterminate(boolean)
     */
    public void setIndeterminateDrawable(Drawable d)
    {
        if (mProgress != null) {
            mProgress.setIndeterminateDrawable(d);
        } else {
            mIndeterminateDrawable = d;
        }
    }

    /**
     * Whether this ProgressDialog is in indeterminate mode.
     *
     * @return true if the dialog is in indeterminate mode, false otherwise
     */
    public boolean isIndeterminate()
    {
        if (mProgress != null) {
            return mProgress.isIndeterminate();
        }
        return mIndeterminate;
    }

    /**
     * Change the indeterminate mode for this ProgressDialog. In indeterminate
     * mode, the progress is ignored and the dialog shows an infinite
     * animation instead.
     *
     * <p><strong>Note:</strong> A ProgressDialog with style {@link #STYLE_SPINNER}
     * is always indeterminate and will ignore this setting.</p>
     *
     * @param indeterminate true to enable indeterminate mode, false otherwise
     * @see #setProgressStyle(int)
     */
    public void setIndeterminate(boolean indeterminate)
    {
        if (mProgress != null) {
            mProgress.setIndeterminate(indeterminate);
        } else {
            mIndeterminate = indeterminate;
        }
    }

    /**
     * Sets the style of this ProgressDialog, either {@link #STYLE_SPINNER} or
     * {@link #STYLE_HORIZONTAL}. The default is {@link #STYLE_SPINNER}.
     *
     * <p><strong>Note:</strong> A ProgressDialog with style {@link #STYLE_SPINNER}
     * is always indeterminate and will ignore the {@link #setIndeterminate(boolean)
     * indeterminate} setting.</p>
     *
     * @param style the style of this ProgressDialog, either {@link #STYLE_SPINNER} or
     *              {@link #STYLE_HORIZONTAL}
     */
    public void setProgressStyle(int style)
    {
        mProgressStyle = style;
    }

    private void onProgressChanged()
    {
        if (mProgressStyle == STYLE_HORIZONTAL) {

        }
    }
}
