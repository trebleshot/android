package com.genonbeta.TrebleShot.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.genonbeta.TrebleShot.R;

public class PreferencesFragment extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_main);
	}
}
