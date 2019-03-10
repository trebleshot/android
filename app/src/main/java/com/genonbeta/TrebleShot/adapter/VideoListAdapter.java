package com.genonbeta.TrebleShot.adapter;


import android.content.ContentResolver;
import android.content.Context;
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
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.GalleryGroupEditableListAdapter;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */

public class VideoListAdapter
        extends GalleryGroupEditableListAdapter<VideoListAdapter.VideoHolder, GroupEditableListAdapter.GroupViewHolder>
{
    public static final int VIEW_TYPE_TITLE = 1;

    private ContentResolver mResolver;
    private int mSelectedInset;

    public VideoListAdapter(Context context)
    {
        super(context, MODE_GROUP_BY_DATE);
        mResolver = context.getContentResolver();
        mSelectedInset = (int) context.getResources().getDimension(R.dimen.space_list_grid);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == VIEW_TYPE_REPRESENTATIVE)
            return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text);

        return new GroupViewHolder(getInflater().inflate(isGridLayoutRequested()
                ? R.layout.list_video_grid
                : R.layout.list_video, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position)
    {
        try {
            final VideoHolder object = this.getItem(position);
            final View parentView = holder.getView();

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
        } catch (Exception e) {
        }
    }

    @Override
    protected void onLoad(GroupLister<VideoHolder> lister)
    {
        Cursor cursor = mResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

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
                    VideoHolder holder = new VideoHolder(
                            cursor.getInt(idIndex),
                            cursor.getString(titleIndex),
                            cursor.getString(displayIndex),
                            cursor.getString(albumIndex),
                            cursor.getString(typeIndex),
                            cursor.getLong(lengthIndex),
                            cursor.getLong(dateIndex) * 1000,
                            cursor.getLong(sizeIndex),
                            Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

                    lister.offerObliged(this, holder);
                }
                while (cursor.moveToNext());
            }

            cursor.close();
        }
    }

    @Override
    protected VideoHolder onGenerateRepresentative(String representativeText)
    {
        return new VideoHolder(representativeText);
    }

    @Override
    public boolean isGridSupported()
    {
        return true;
    }

    public static class VideoHolder extends GalleryGroupEditableListAdapter.GalleryGroupShareable
    {
        public String duration;

        public VideoHolder(String representativeText)
        {
            super(VIEW_TYPE_REPRESENTATIVE, representativeText);
        }

        public VideoHolder(long id, String friendlyName, String fileName, String albumName, String mimeType, long duration, long date, long size, Uri uri)
        {
            super(id, friendlyName, fileName, albumName, mimeType, date, size, uri);
            this.duration = TimeUtils.getDuration(duration);
        }
    }
}
