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

/**
 * Created by gabm on 11/01/18.
 */

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ImageListAdapter;
import com.genonbeta.TrebleShot.app.GalleryGroupEditableListFragment;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

public class ImageListFragment extends GalleryGroupEditableListFragment<ImageListAdapter.ImageHolder,
        GroupEditableListAdapter.GroupViewHolder, ImageListAdapter>
{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setDefaultOrderingCriteria(ImageListAdapter.MODE_SORT_ORDER_DESCENDING);
        setDefaultSortingCriteria(ImageListAdapter.MODE_SORT_BY_DATE);
        setDefaultViewingGridSize(3, 5);
        setItemOffsetDecorationEnabled(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new ImageListAdapter(this));
        setEmptyListImage(R.drawable.ic_photo_white_24dp);
        setEmptyListText(getString(R.string.text_listEmptyImage));
    }

    @Override
    public void onResume()
    {
        super.onResume();

        requireContext().getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true, getDefaultContentObserver());
    }

    @Override
    public void onPause()
    {
        super.onPause();
        requireContext().getContentResolver().unregisterContentObserver(getDefaultContentObserver());
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_photo);
    }

    @Override
    public boolean performDefaultLayoutClick(GroupEditableListAdapter.GroupViewHolder holder,
                                             ImageListAdapter.ImageHolder object)
    {
        return performLayoutClickOpen(holder, object);
    }
}
