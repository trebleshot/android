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

package com.genonbeta.TrebleShot.fragment;

import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import com.genonbeta.TrebleShot.R;

public class PreferencesFragment extends PreferenceFragmentCompat
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        addPreferencesFromResource(R.xml.preferences_main_app);

        if (Build.VERSION.SDK_INT < 26)
            addPreferencesFromResource(R.xml.preferences_main_notification);
        else
            addPreferencesFromResource(R.xml.preferences_main_notification_oreo);

        addPreferencesFromResource(R.xml.preferences_main_advanced);
    }
}