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
package org.monora.uprotocol.client.android.fragment.inner

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import org.monora.uprotocol.client.android.R

class LookPreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_introduction_look)
        loadThemeOptionsTo(requireContext(), findPreference("theme"))
    }

    companion object {
        fun loadThemeOptionsTo(context: Context, themePreference: ListPreference?) {
            if (themePreference == null) return

            val valueList: MutableList<String> = arrayListOf("light", "dark")
            val titleList: MutableList<String> = arrayListOf(
                context.getString(R.string.text_lightTheme),
                context.getString(R.string.text_darkTheme)
            )

            if (Build.VERSION.SDK_INT >= 26) {
                valueList.add("system")
                titleList.add(context.getString(R.string.text_followSystemTheme))
            } else if (Build.VERSION.SDK_INT >= 21) {
                valueList.add("battery")
                titleList.add(context.getString(R.string.text_batterySaverTheme))
            }

            themePreference.entries = titleList.toTypedArray()
            themePreference.entryValues = valueList.toTypedArray()
        }
    }
}