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
package org.monora.uprotocol.client.android.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.IntroductionPrefsFragment

class PreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_main_app)
        addPreferencesFromResource(R.xml.preferences_main_notification)
        IntroductionPrefsFragment.loadThemeOptionsTo(requireContext(), findPreference("theme"))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.preferences_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actions_preference_main_reset_to_defaults -> findNavController().navigate(
                PreferencesFragmentDirections.actionPreferencesFragment2ToResetPreferencesFragment()
            )
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}

class ResetPreferencesFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireActivity())
            .setTitle(R.string.preferences_reset_question)
            .setMessage(R.string.preferences_reset_notice)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.proceed) { _: DialogInterface?, _: Int ->
                PreferenceManager.getDefaultSharedPreferences(context).edit {
                    clear()
                }
                PreferenceManager.setDefaultValues(context, R.xml.preferences_defaults_main, true)

                activity?.finish()
            }
            .show()
    }
}
