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

package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.genonbeta.android.framework.util.listing.merger.StringMerger;

import java.util.List;

/**
 * created by: Veli
 * date: 30.03.2018 14:58
 */
abstract public class GalleryGroupEditableListAdapter<T extends GalleryGroupEditableListAdapter.GalleryGroupShareable, V extends GroupEditableListAdapter.GroupViewHolder>
        extends GroupEditableListAdapter<T, V>
        implements GroupEditableListAdapter.GroupLister.CustomGroupLister<T>
{
    public static final int MODE_GROUP_BY_ALBUM = MODE_GROUP_BY_DATE + 1;

    public GalleryGroupEditableListAdapter(Context context, int groupBy)
    {
        super(context, groupBy);
    }

    @Override
    public boolean onCustomGroupListing(GroupLister<T> lister, int mode, T object)
    {
        if (mode == MODE_GROUP_BY_ALBUM) {
            lister.offer(object, new StringMerger<T>(object.albumName));
            return true;
        }

        return false;
    }

    @Override
    public GroupLister<T> createLister(List<T> loadedList, int groupBy)
    {
        return super.createLister(loadedList, groupBy)
                .setCustomLister(this);
    }

    @NonNull
    @Override
    public String getSectionName(int position, T object)
    {
        if (!object.isGroupRepresentative())
            if (getGroupBy() == MODE_GROUP_BY_ALBUM)
                return object.albumName;

        return super.getSectionName(position, object);
    }

    public static class GalleryGroupShareable extends GroupShareable
    {
        public String albumName;

        public GalleryGroupShareable(int viewType, String representativeText)
        {
            super(viewType, representativeText);
        }

        public GalleryGroupShareable(long id, String friendlyName, String fileName, String albumName, String mimeType,
                                     long date, long size, Uri uri)
        {
            super(id, friendlyName, fileName, mimeType, date, size, uri);
            this.albumName = albumName;
        }
    }
}
