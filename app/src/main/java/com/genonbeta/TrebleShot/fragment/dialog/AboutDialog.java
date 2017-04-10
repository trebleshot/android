package com.genonbeta.TrebleShot.fragment.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;

public class AboutDialog extends DialogFragment
{
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setTitle(R.string.about);
		builder.setMessage(R.string.about_summary);
		builder.setNegativeButton(R.string.close, null);
		builder.setPositiveButton(R.string.see_source_code, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.APPLICATION_REPO)));
			}
		});

		return builder.create();
	}
}
