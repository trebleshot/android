package com.genonbeta.TrebleShot.dialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;

import static com.genonbeta.TrebleShot.activity.HomeActivity.REQUEST_PERMISSION_ALL;

/**
 * created by: Veli
 * date: 18.11.2017 20:16
 */

public class RationalePermissionRequest extends AlertDialog.Builder
{
	public Activity mActivity;
	public String mPermission;

	public RationalePermissionRequest(Activity activity, @NonNull String permission)
	{
		super(activity);

		mActivity = activity;
		mPermission = permission;

		setCancelable(false);
		setTitle(R.string.text_permissionRequest);
	}

	@Override
	public AlertDialog show()
	{
		if (ActivityCompat.checkSelfPermission(mActivity, mPermission) != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, mPermission))
				setPositiveButton(R.string.butn_settings, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						Intent intent = new Intent()
								.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
								.setData(Uri.fromParts("package", mActivity.getPackageName(), null));

						mActivity.startActivity(intent);
						mActivity.finish();
					}
				});
			else
				setPositiveButton(R.string.butn_ask, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						ActivityCompat.requestPermissions(mActivity, new String[]{mPermission}, REQUEST_PERMISSION_ALL);
					}
				});

			setNegativeButton(R.string.butn_reject, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int i)
				{
					mActivity.finish();
				}
			});

			super.show();
		}

		return null;
	}
}
