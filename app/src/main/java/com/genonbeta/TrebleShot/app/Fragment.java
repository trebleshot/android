package com.genonbeta.TrebleShot.app;

import android.content.SharedPreferences;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.AppUtils;

/**
 * created by: Veli
 * date: 26.03.2018 16:00
 */

public class Fragment extends android.support.v4.app.Fragment
{
	private SharedPreferences mDefaultPreferences;
	private AccessDatabase mDatabase;

	public AccessDatabase getDatabase()
	{
		if (mDatabase == null && getContext() != null)
			mDatabase = AppUtils.getAccessDatabase(getContext());

		return mDatabase;
	}

	public SharedPreferences getDefaultPreferences()
	{
		if (mDefaultPreferences == null && getContext() != null)
			mDefaultPreferences = AppUtils.getDefaultPreferences(getContext());

		return mDefaultPreferences;
	}
}
