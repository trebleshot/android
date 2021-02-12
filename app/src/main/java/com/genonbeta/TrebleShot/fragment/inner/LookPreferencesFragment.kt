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

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.*
import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.genonbeta.TrebleShot.R

class LookPreferencesFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        addPreferencesFromResource(R.xml.preference_introduction_look)
        loadThemeOptionsTo(requireContext(), findPreference("theme"))
    }

    override fun onResume() {
        super.onResume()
        preferenceManager
            .sharedPreferences
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager
            .sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (("custom_fonts" == key || "theme" == key || "amoled_theme" == key) && activity != null) {
            requireActivity().recreate()
        }
    }

    companion object {
        fun loadThemeOptionsTo(context: Context, themePreference: ListPreference?) {
            if (themePreference == null) return

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

            themePreference.entries = valueList.toTypedArray()
            themePreference.entryValues = titleList.toTypedArray()
        }
    }
}