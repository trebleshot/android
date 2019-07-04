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

package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * created by: veli
 * date: 31.03.2018 13:48
 */
public class PreferenceUtils extends com.genonbeta.android.framework.util.PreferenceUtils
{
    public static void syncDefaults(Context context)
    {
        syncDefaults(context, true, false);
    }

    public static void syncDefaults(Context context, boolean compare, boolean fromXml)
    {
        SharedPreferences preferences = AppUtils.getDefaultLocalPreferences(context);
        SharedPreferences binaryPreferences = AppUtils.getDefaultPreferences(context);

        if (compare)
            sync(preferences, binaryPreferences);
        else {
            if (fromXml)
                syncPreferences(preferences, binaryPreferences);
            else
                syncPreferences(binaryPreferences, preferences);
        }
    }
}
