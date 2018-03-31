package com.genonbeta.TrebleShot.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.ironz.binaryprefs.BinaryPreferencesBuilder;

public abstract class Activity extends AppCompatActivity
{
	private AccessDatabase mDatabase;
	private SharedPreferences mPreferences;
	private AlertDialog mOngoingRequest;

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

	public AccessDatabase getDatabase()
	{
		if (mDatabase == null)
			mDatabase = AppUtils.getAccessDatabase(this);

		return mDatabase;
	}

	protected SharedPreferences getDefaultPreferences()
	{
		if (mPreferences == null)
			mPreferences = AppUtils.getDefaultPreferences(this);

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

	public interface OnBackPressedListener
	{
		boolean onBackPressed();
	}
}
