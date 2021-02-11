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
package com.genonbeta.TrebleShot.fragment.inner

class LookPreferencesFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        addPreferencesFromResource(R.xml.preference_introduction_look)
        loadThemeOptionsTo(getContext(), findPreference<ListPreference>("theme"))
    }

    override fun onResume() {
        super.onResume()
        getPreferenceManager()
            .getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        getPreferenceManager()
            .getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (("custom_fonts" == key || "theme" == key || "amoled_theme" == key)
            && getActivity() != null
        ) getActivity().recreate()
    }

    companion object {
        fun loadThemeOptionsTo(context: Context, themePreference: ListPreference) {
            val valueList: MutableList<String> = ArrayList()
            val titleList: MutableList<String> = ArrayList()
            valueList.add("light")
            valueList.add("dark")
            titleList.add(context.getString(R.string.text_lightTheme))
            titleList.add(context.getString(R.string.text_darkTheme))
            if (Build.VERSION.SDK_INT >= 26) {
                valueList.add("system")
                titleList.add(context.getString(R.string.text_followSystemTheme))
            } else if (Build.VERSION.SDK_INT >= 21) {
                valueList.add("battery")
                titleList.add(context.getString(R.string.text_batterySaverTheme))
            }
            val values: Array<CharSequence?> = arrayOfNulls<String>(valueList.size)
            val titles: Array<CharSequence?> = arrayOfNulls<String>(titleList.size)
            valueList.toArray<CharSequence>(values)
            titleList.toArray<CharSequence>(titles)
            themePreference.entries = titles
            themePreference.entryValues = values
        }
    }
}