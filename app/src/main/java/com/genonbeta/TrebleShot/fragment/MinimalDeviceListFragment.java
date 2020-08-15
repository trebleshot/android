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

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Device;

/**
 * created by: veli
 * date: 3/11/19 7:43 PM
 */
public class MinimalDeviceListFragment extends DeviceListFragment
{
    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        setHiddenDeviceTypes(new Device.Type[]{Device.Type.WEB, Device.Type.NORMAL});
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
        setFilteringSupported(false);
        setUseDefaultPaddingDecoration(false);
        setUseDefaultPaddingDecorationSpaceForEdges(false);

        if (isScreenLarge())
            setDefaultViewingGridSize(4, 5);
        else if (isScreenNormal())
            setDefaultViewingGridSize(3, 4);
        else
            setDefaultViewingGridSize(2, 3);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        getListView().setNestedScrollingEnabled(true);
        setDividerVisible(false);

        if (getContext() != null) {
            float padding = getContext().getResources().getDimension(R.dimen.short_content_width_padding);

            getListView().setClipToPadding(false);
            getListView().setPadding((int) padding, 0, (int) padding, 0);
        }
    }
}
