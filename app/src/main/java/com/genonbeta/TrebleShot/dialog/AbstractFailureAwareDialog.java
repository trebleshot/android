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

package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 * created by: Veli
 * date: 26.02.2018 08:19
 */

abstract public class AbstractFailureAwareDialog extends AlertDialog.Builder
{
    private OnProceedClickListener mClickListener;

    public AbstractFailureAwareDialog(@NonNull Context context)
    {
        super(context);
    }

    public void setOnProceedClickListener(String buttonText, OnProceedClickListener listener)
    {
        setPositiveButton(buttonText, null);
        mClickListener = listener;
    }

    public void setOnProceedClickListener(int buttonRes, OnProceedClickListener listener)
    {
        setOnProceedClickListener(getContext().getString(buttonRes), listener);
    }

    @Override
    public AlertDialog show()
    {
        final AlertDialog dialog = super.show();

        if (mClickListener != null)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (mClickListener.onProceedClick(dialog))
                        dialog.dismiss();
                }
            });

        return dialog;
    }

    public interface OnProceedClickListener
    {
        boolean onProceedClick(AlertDialog dialog);
    }
}
