package com.genonbeta.TrebleShot.fragment.inner;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.PreferenceUtils;

public class LookPreferencesFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        addPreferencesFromResource(R.xml.preference_introduction_look);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        getPreferenceManager()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        getPreferenceManager()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (("custom_fonts".equals(key) || "dark_theme".equals(key) || "amoled_theme".equals(key))
                && getActivity() != null) {
            PreferenceUtils.syncPreferences(AppUtils.getDefaultLocalPreferences(getContext()),
                    AppUtils.getDefaultPreferences(getContext()).getWeakManager());

            getActivity().recreate();
        }
    }
}
