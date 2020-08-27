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

package com.genonbeta.TrebleShot.adapter;


import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.IEditableListFragment;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.GalleryGroupEditableListAdapter;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.util.listing.Merger;

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */

public class VideoListAdapter extends GalleryGroupEditableListAdapter<VideoListAdapter.VideoHolder,
        GroupEditableListAdapter.GroupViewHolder>
{
    public static final int VIEW_TYPE_TITLE = 1;

    private final ContentResolver mResolver;
    private final int mSelectedInset;

    public VideoListAdapter(IEditableListFragment<VideoHolder, GroupViewHolder> fragment)
    {
        super(fragment, MODE_GROUP_BY_DATE);
        mResolver = getContext().getContentResolver();
        mSelectedInset = (int) getContext().getResources().getDimension(R.dimen.space_list_grid);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        GroupViewHolder holder = viewType == VIEW_TYPE_DEFAULT ? new GroupViewHolder(getInflater().inflate(
                isGridLayoutRequested() ? R.layout.list_video_grid : R.layout.list_video, parent, false))
                : createDefaultViews(parent, viewType, true);

        if (!holder.isRepresentative()) {
            getFragment().registerLayoutViewClicks(holder);

            View visitView = holder.itemView.findViewById(R.id.visitView);
            visitView.setOnClickListener(v -> getFragment().performLayoutClickOpen(holder));
            visitView.setOnLongClickListener(v -> getFragment().performLayoutLongClick(holder));

            holder.itemView.findViewById(isGridLayoutRequested() ? R.id.selectorContainer
                    : R.id.selector).setOnClickListener(v -> getFragment().setItemSelected(holder, true));
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position)
    {
        try {
            final VideoHolder object = this.getItem(position);
            final View parentView = holder.itemView;

            if (!holder.tryBinding(object)) {
                ViewGroup container = parentView.findViewById(R.id.container);
                ImageView image = parentView.findViewById(R.id.image);
                TextView text1 = parentView.findViewById(R.id.text);
                TextView text2 = parentView.findViewById(R.id.text2);
                TextView text3 = parentView.findViewById(R.id.text3);

                text1.setText(object.friendlyName);
                text2.setText(object.duration);
                text3.setText(FileUtils.sizeExpression(object.size, false));

                parentView.setSelected(object.isSelectableSelected());

                GlideApp.with(getContext())
                        .load(object.uri)
                        .override(300)
                        .centerCrop()
                        .into(image);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onLoad(GroupLister<VideoHolder> lister)
    {
        Cursor cursor = mResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null,
                null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                int titleIndex = cursor.getColumnIndex(MediaStore.Video.Media.TITLE);
                int displayIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
                int albumIndex = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                int lengthIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
                int dateIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED);
                int sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE);
                int typeIndex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);

                do {
                    VideoHolder holder = new VideoHolder(cursor.getInt(idIndex), cursor.getString(titleIndex),
                            cursor.getString(displayIndex), cursor.getString(albumIndex), cursor.getString(typeIndex),
                            cursor.getLong(lengthIndex), cursor.getLong(dateIndex) * 1000,
                            cursor.getLong(sizeIndex), Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI + "/"
                            + cursor.getInt(idIndex)));

                    lister.offerObliged(this, holder);
                }
                while (cursor.moveToNext());
            }

            cursor.close();
        }
    }

    @Override
    protected VideoHolder onGenerateRepresentative(String text, Merger<VideoHolder> merger)
    {
        return new VideoHolder(text);
    }

    public static class VideoHolder extends GalleryGroupEditableListAdapter.GalleryGroupShareable
    {
        public String duration;

        public VideoHolder(String representativeText)
        {
            super(VIEW_TYPE_REPRESENTATIVE, representativeText);
        }

        public VideoHolder(long id, String friendlyName, String fileName, String albumName, String mimeType,
                           long duration, long date, long size, Uri uri)
        {
            super(id, friendlyName, fileName, albumName, mimeType, date, size, uri);
            this.duration = TimeUtils.getDuration(duration);
        }
    }
}
