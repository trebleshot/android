package com.genonbeta.TrebleShot.app;

import android.content.SharedPreferences;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.NotificationUtils;

/**
 * created by: veli
 * date: 31.03.2018 15:23
 */
abstract public class Service extends android.app.Service
{
	private AccessDatabase mDatabase;
	private SharedPreferences mDefaultPreferences;
	private NotificationUtils mNotificationUtils;

	public AccessDatabase getDatabase()
	{
		if (mDatabase == null)
			mDatabase = AppUtils.getAccessDatabase(this);

		return mDatabase;
	}

	public SharedPreferences getDefaultPreferences()
	{
		if (mDefaultPreferences == null)
			mDefaultPreferences = AppUtils.getDefaultPreferences(getApplicationContext());

		return mDefaultPreferences;
	}

	public NotificationUtils getNotificationUtils()
	{
		if (mNotificationUtils == null)
			mNotificationUtils = new NotificationUtils(getApplicationContext(), getDatabase(), getDefaultPreferences());

		return mNotificationUtils;
	}
}
