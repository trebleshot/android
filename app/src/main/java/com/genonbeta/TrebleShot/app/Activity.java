package com.genonbeta.TrebleShot.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;

public abstract class Activity extends AppCompatActivity
{
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
		return AppUtils.getAccessDatabase(this);
	}

	protected SharedPreferences getDefaultPreferences()
	{
		return AppUtils.getDefaultPreferences(this);
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
