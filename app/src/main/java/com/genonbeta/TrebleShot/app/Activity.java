package com.genonbeta.TrebleShot.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.HomeActivity;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.service.CommunicationService;

public abstract class Activity extends AppCompatActivity
{
	private SharedPreferences mPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= 16) {
			RationalePermissionRequest storagePermission = new RationalePermissionRequest(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

			storagePermission.setTitle(R.string.text_requestPermissionStorage);
			storagePermission.setMessage(R.string.text_requestPermissionStorageSummary);
			storagePermission.show();
		}

		if (Build.VERSION.SDK_INT >= 26) {
			RationalePermissionRequest readPhonePermission = new RationalePermissionRequest(this, Manifest.permission.READ_PHONE_STATE);

			readPhonePermission.setTitle(R.string.text_requestPermissionReadPhoneState);
			readPhonePermission.setMessage(R.string.text_requestPermissionReadPhoneStateSummary);
			readPhonePermission.show();
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		if (Build.VERSION.SDK_INT >= 26)
			startForegroundService(new Intent(this, CommunicationService.class));
		else
			startService(new Intent(this, CommunicationService.class));
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		finish();
		startActivity(new Intent(this, HomeActivity.class));
	}

	protected SharedPreferences getDefaultPreferences()
	{
		if (mPreferences == null)
			mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		return mPreferences;
	}
}
