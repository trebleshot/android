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

package com.genonbeta.TrebleShot.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.PreferenceUtils;

public class PreferencesActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            onBackPressed();
        else if (id == R.id.actions_preference_main_reset_to_defaults) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.ques_resetToDefault)
                    .setMessage(R.string.text_resetPreferencesToDefaultSummary)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {

                            // TODO: 10/7/18 This will cause two seperate sync operations to start
                            AppUtils.getDefaultPreferences(getApplicationContext()).edit()
                                    .clear()
                                    .apply();

                            AppUtils.getDefaultLocalPreferences(getApplicationContext()).edit()
                                    .clear()
                                    .apply();

                            finish();
                        }
                    })
                    .show();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actions_preferences_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        PreferenceUtils.syncPreferences(AppUtils.getDefaultLocalPreferences(this),
                AppUtils.getDefaultPreferences(this).getWeakManager());
    }
}
