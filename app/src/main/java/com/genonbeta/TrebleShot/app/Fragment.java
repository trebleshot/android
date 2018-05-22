package com.genonbeta.TrebleShot.app;

import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.ui.callback.SnackbarSupport;
import com.genonbeta.TrebleShot.util.AppUtils;

/**
 * created by: Veli
 * date: 26.03.2018 16:00
 */

public class Fragment
		extends android.support.v4.app.Fragment
		implements FragmentImpl, SnackbarSupport
{
	private boolean mIsMenuShown;

	public Snackbar createSnackbar(int resId, Object... objects)
	{
		return getView() != null
				? Snackbar.make(getView(), getString(resId, objects), Snackbar.LENGTH_LONG)
				: null;
	}

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
