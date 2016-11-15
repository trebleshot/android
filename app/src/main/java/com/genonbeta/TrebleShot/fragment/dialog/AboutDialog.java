package com.genonbeta.TrebleShot.fragment.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;

public class AboutDialog extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.about);
        builder.setMessage(R.string.about_summary);

        return builder.create();
    }
}
