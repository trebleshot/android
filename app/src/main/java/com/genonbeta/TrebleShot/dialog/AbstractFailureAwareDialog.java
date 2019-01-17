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
