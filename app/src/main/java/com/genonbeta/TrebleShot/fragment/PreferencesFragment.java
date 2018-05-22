package com.genonbeta.TrebleShot.fragment;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.genonbeta.TrebleShot.R;

public class PreferencesFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences_main_app);

		if (Build.VERSION.SDK_INT < 26)
			addPreferencesFromResource(R.xml.preferences_main_notification);
		else
			addPreferencesFromResource(R.xml.preferences_main_notification_oreo);

		addPreferencesFromResource(R.xml.preferences_main_advaced);
	}
}