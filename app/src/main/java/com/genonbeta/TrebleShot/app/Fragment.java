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
	public AccessDatabase getDatabase()
	{
		return AppUtils.getAccessDatabase(getContext());
	}

	public SharedPreferences getDefaultPreferences()
	{
		return AppUtils.getDefaultPreferences(getContext());
	}
}
