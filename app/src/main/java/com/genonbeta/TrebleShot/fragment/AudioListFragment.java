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
import com.genonbeta.TrebleShot.adapter.AudioListAdapter;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

import java.util.Map;

public class AudioListFragment extends GroupEditableListFragment<AudioListAdapter.AudioItemHolder,
        GroupEditableListAdapter.GroupViewHolder, AudioListAdapter>
{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setDefaultGroupingCriteria(AudioListAdapter.MODE_GROUP_BY_ALBUM);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new AudioListAdapter(this));
        setEmptyListImage(R.drawable.ic_library_music_white_24dp);
        setEmptyListText(getString(R.string.text_listEmptyMusic));
    }

    @Override
    public void onResume()
    {
        super.onResume();

        requireContext().getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true, getDefaultContentObserver());
    }

    @Override
    public void onPause()
    {
        super.onPause();

        requireContext().getContentResolver().unregisterContentObserver(getDefaultContentObserver());
    }

    @Override
    public void onGroupingOptions(Map<String, Integer> options)
    {
        super.onGroupingOptions(options);

        options.put(getString(R.string.text_groupByNothing), AudioListAdapter.MODE_GROUP_BY_NOTHING);
        options.put(getString(R.string.text_groupByDate), AudioListAdapter.MODE_GROUP_BY_DATE);
        options.put(getString(R.string.text_groupByAlbum), AudioListAdapter.MODE_GROUP_BY_ALBUM);
        options.put(getString(R.string.text_groupByArtist), AudioListAdapter.MODE_GROUP_BY_ARTIST);
        options.put(getString(R.string.text_groupByFolder), AudioListAdapter.MODE_GROUP_BY_FOLDER);
    }

    @Override
    public int onGridSpanSize(int viewType, int currentSpanSize)
    {
        return viewType == FileListAdapter.VIEW_TYPE_REPRESENTATIVE ? currentSpanSize
                : super.onGridSpanSize(viewType, currentSpanSize);
    }

    @Override
    public boolean performDefaultLayoutClick(GroupEditableListAdapter.GroupViewHolder holder,
                                             AudioListAdapter.AudioItemHolder object)
    {
        return performLayoutClickOpen(holder, object);
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_music);
    }
}
