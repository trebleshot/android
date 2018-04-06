package com.genonbeta.TrebleShot.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.InvalidatedMenuSupport;

/**
 * created by: Veli
 * date: 26.03.2018 16:00
 */

public class Fragment extends android.support.v4.app.Fragment
{
	private boolean mIsMenuShown;

	public AccessDatabase getDatabase()
	{
		return AppUtils.getAccessDatabase(getContext());
	}

	public SharedPreferences getDefaultPreferences()
	{
		return AppUtils.getDefaultPreferences(getContext());
	}

	public boolean isMenuShown()
	{
		return mIsMenuShown;
	}

	@Override
	public void setMenuVisibility(boolean menuVisible)
	{
		super.setMenuVisibility(menuVisible);
		mIsMenuShown = menuVisible;
	}
}
