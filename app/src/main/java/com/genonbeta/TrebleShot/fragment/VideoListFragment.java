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
import android.provider.MediaStore;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.VideoListAdapter;
import com.genonbeta.TrebleShot.app.GalleryGroupEditableListFragment;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

public class VideoListFragment extends GalleryGroupEditableListFragment<VideoListAdapter.VideoHolder,
        GroupEditableListAdapter.GroupViewHolder, VideoListAdapter>
{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setDefaultOrderingCriteria(VideoListAdapter.MODE_SORT_ORDER_DESCENDING);
        setDefaultSortingCriteria(VideoListAdapter.MODE_SORT_BY_DATE);
        setDefaultViewingGridSize(3, 5);
        setUseDefaultPaddingDecoration(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new VideoListAdapter(this));
        setEmptyListImage(R.drawable.ic_video_library_white_24dp);
        setEmptyListText(getString(R.string.text_listEmptyVideo));
    }

    @Override
    public void onResume()
    {
        super.onResume();

        requireContext().getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true, getDefaultContentObserver());
    }

    @Override
    public void onPause()
    {
        super.onPause();

        requireContext().getContentResolver().unregisterContentObserver(getDefaultContentObserver());
    }

    @Override
    public int onGridSpanSize(int viewType, int currentSpanSize)
    {
        return viewType == VideoListAdapter.VIEW_TYPE_TITLE ? currentSpanSize
                : super.onGridSpanSize(viewType, currentSpanSize);
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_video);
    }

    @Override
    public boolean performDefaultLayoutClick(GroupEditableListAdapter.GroupViewHolder holder,
                                             VideoListAdapter.VideoHolder object)
    {
        return performLayoutClickOpen(holder, object);
    }
}
