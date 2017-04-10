package com.genonbeta.TrebleShot.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.service.CommunicationService;

public class GActivity extends AppCompatActivity
{
	private SharedPreferences mPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (this.getDefaultPreferences().getBoolean("serviceLock", false))
		{
			this.getDefaultPreferences().edit().putBoolean("serviceLock", false).commit();
			Toast.makeText(this, R.string.service_unlocked_notice, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		startService(new Intent(this, CommunicationService.class));
	}

	protected SharedPreferences getDefaultPreferences()
	{
		if (this.mPreferences == null)
			this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		return this.mPreferences;
	}
}
