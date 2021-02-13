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
package com.genonbeta.TrebleShot.activity

import com.genonbeta.TrebleShot.R
import android.content.DialogInterface
import com.genonbeta.TrebleShot.util.AppUtils
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.app.Activity

class PreferencesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) onBackPressed() else if (id == R.id.actions_preference_main_reset_to_defaults) {
            AlertDialog.Builder(this)
                .setTitle(R.string.ques_resetToDefault)
                .setMessage(R.string.text_resetPreferencesToDefaultSummary)
                .setNegativeButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_proceed) { dialog: DialogInterface?, which: Int ->
                    AppUtils.getDefaultPreferences(applicationContext).edit()
                        .clear()
                        .apply()
                    finish()
                }
                .show()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actions_preferences_main, menu)
        return super.onCreateOptionsMenu(menu)
    }
}