package com.genonbeta.TrebleShot.fragment.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialog;

import com.genonbeta.TrebleShot.R;

public class AboutDialog extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AppCompatDialog builder = new AppCompatDialog(getActivity());

        builder.setTitle(R.string.about);
        builder.setContentView(R.layout.layout_about_trebleshot);

        return builder;
    }
}
