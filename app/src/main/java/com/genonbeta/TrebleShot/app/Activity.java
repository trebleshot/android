package com.genonbeta.TrebleShot.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.genonbeta.TrebleShot.service.CommunicationService;

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
	}

	protected SharedPreferences getDefaultPreferences()
	{
		if (mPreferences == null)
			mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		return mPreferences;
	}
}
