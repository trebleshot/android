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

package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;

import com.genonbeta.TrebleShot.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;

public class NavigationViewBottomSheetDialog extends BottomSheetDialog
{
    public NavigationViewBottomSheetDialog(@NonNull Activity activity,
                                           @MenuRes int menu,
                                           @IdRes int selectedItemId,
                                           NavigationView.OnNavigationItemSelectedListener listener)
    {
        super(activity);

        View view = LayoutInflater.from(activity).inflate(R.layout.layout_navigation_view_bottom_sheet, null, false);
        NavigationView navigationView = view.findViewById(R.id.nav_view);

        activity.getMenuInflater().inflate(menu, navigationView.getMenu());
        navigationView.setCheckedItem(selectedItemId);
        navigationView.setNavigationItemSelectedListener(listener);

        setContentView(view);
    }
}
