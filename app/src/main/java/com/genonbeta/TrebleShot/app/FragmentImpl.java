package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;

import com.genonbeta.TrebleShot.database.AccessDatabase;

/**
 * created by: veli
 * date: 14/04/18 10:52
 */
public interface FragmentImpl
{
	FragmentActivity getActivity();

	Context getContext();

	AccessDatabase getDatabase();

	SharedPreferences getDefaultPreferences();
}
