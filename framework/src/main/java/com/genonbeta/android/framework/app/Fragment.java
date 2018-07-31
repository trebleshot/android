package com.genonbeta.android.framework.app;

import android.support.design.widget.Snackbar;

/**
 * created by: veli
 * date: 7/31/18 11:54 AM
 */
public class Fragment
		extends android.support.v4.app.Fragment
		implements FragmentImpl
{
	public Snackbar createSnackbar(int resId, Object... objects)
	{
		return getView() != null
				? Snackbar.make(getView(), getString(resId, objects), Snackbar.LENGTH_LONG)
				: null;
	}
}
