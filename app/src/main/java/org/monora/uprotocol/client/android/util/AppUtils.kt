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
package org.monora.uprotocol.client.android.util

import android.util.Log
import androidx.preference.PreferenceManager
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.ListingFragmentBase
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.model.ContentModel

object AppUtils {
    private val TAG = AppUtils::class.simpleName

    fun <T : ContentModel> showFolderSelectionHelp(fragment: ListingFragmentBase<T>) {
        val connection = fragment.engineConnection
        val preferences = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext())
        val selectedItemList = connection.getSelectionList() ?: return

        if (selectedItemList.isNotEmpty() && !preferences.getBoolean("helpFolderSelection", false))
            fragment.createSnackbar(R.string.mesg_helpFolderSelection)
                ?.setAction(R.string.butn_gotIt) {
                    preferences
                        .edit()
                        .putBoolean("helpFolderSelection", true)
                        .apply()
                }
                ?.show()
    }
}