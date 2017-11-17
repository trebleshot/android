package com.genonbeta.TrebleShot.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.service.CommunicationService;

import static com.genonbeta.TrebleShot.activity.HomeActivity.REQUEST_PERMISSION_ALL;

public abstract class Activity extends AppCompatActivity
{
	private SharedPreferences mPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		startService(new Intent(this, CommunicationService.class));

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE))
				Snackbar
						.make(findViewById(android.R.id.content), R.string.mesg_permissionRequired, Snackbar.LENGTH_LONG)
						.setAction(R.string.butn_settings, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								Intent intent = new Intent();

								intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
								intent.setData(Uri.fromParts("package", getPackageName(), null));

								startActivity(intent);
							}
						}).show();
			else
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_ALL);
		}
	}

	protected SharedPreferences getDefaultPreferences()
	{
		if (mPreferences == null)
			mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		return mPreferences;
	}
}
