package com.genonbeta.TrebleShot.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;

import java.util.ArrayList;

public abstract class Activity extends AppCompatActivity
{
	private SharedPreferences mPreferences;
	private AlertDialog mOngoingRequest;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		boolean nsdDefined = defaultPreferences.contains("nsd_enabled");

		PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

		if (!nsdDefined)
			defaultPreferences.edit()
					.putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
					.apply();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class));
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (!AppUtils.checkRunningConditions(this))
			requestRequiredPermissions();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (AppUtils.checkRunningConditions(this))
			AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class));
		else
			requestRequiredPermissions();
	}

	protected SharedPreferences getDefaultPreferences()
	{
		if (mPreferences == null)
			mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		return mPreferences;
	}

	public boolean requestRequiredPermissions()
	{
		if (mOngoingRequest != null && mOngoingRequest.isShowing())
			return false;

		for (RationalePermissionRequest.PermissionRequest request : AppUtils.getRequiredPermissions(this))
			if ((mOngoingRequest = RationalePermissionRequest.requestIfNecessary(this, request)) != null)
				return false;

		return true;
	}
}
