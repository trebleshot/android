/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.fragment.inner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import com.genonbeta.TrebleShot.R;

import java.util.ArrayList;
import java.util.List;

public class LookPreferencesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        addPreferencesFromResource(R.xml.preference_introduction_look);
        loadThemeOptionsTo(getContext(), findPreference("theme"));
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
        if (("custom_fonts".equals(key) || "theme".equals(key) || "amoled_theme".equals(key))
                && getActivity() != null)
            getActivity().recreate();
    }

    public static void loadThemeOptionsTo(Context context, ListPreference themePreference)
    {
        List<String> valueList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        valueList.add("light");
        valueList.add("dark");

        titleList.add(context.getString(R.string.text_lightTheme));
        titleList.add(context.getString(R.string.text_darkTheme));

        if (Build.VERSION.SDK_INT >= 26) {
            valueList.add("system");
            titleList.add(context.getString(R.string.text_followSystemTheme));
        } else if (Build.VERSION.SDK_INT >= 21) {
            valueList.add("battery");
            titleList.add(context.getString(R.string.text_batterySaverTheme));
        }

        CharSequence[] values = new String[valueList.size()];
        CharSequence[] titles = new String[titleList.size()];

        valueList.toArray(values);
        titleList.toArray(titles);

        themePreference.setEntries(titles);
        themePreference.setEntryValues(values);
    }
}
