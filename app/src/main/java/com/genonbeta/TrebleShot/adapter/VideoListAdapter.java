package com.genonbeta.TrebleShot.adapter;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.GalleryGroupShareableListAdapter;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */

public class VideoListAdapter
		extends GalleryGroupShareableListAdapter<VideoListAdapter.VideoHolder, GroupShareableListAdapter.ViewHolder>
{
	public static final int VIEW_TYPE_TITLE = 1;

	private ContentResolver mResolver;

	public VideoListAdapter(Context context)
	{
		super(context, MODE_GROUP_BY_DATE);
		mResolver = context.getContentResolver();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return viewType == VIEW_TYPE_REPRESENTATIVE
				? new ViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text)
				: new ViewHolder(getInflater().inflate(isGridLayoutRequested() ? R.layout.list_video_grid : R.layout.list_video, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		final VideoHolder object = this.getItem(position);
		final View parentView = holder.getView();

		if (!holder.tryBinding(object))
		{
			final View selector = parentView.findViewById(R.id.selector);
			ImageView image = parentView.findViewById(R.id.image);
			TextView text1 = parentView.findViewById(R.id.text);
			TextView text2 = parentView.findViewById(R.id.text2);
			TextView text3 = parentView.findViewById(R.id.text3);

			text1.setText(object.friendlyName);
			text2.setText(object.duration);
			text3.setText(FileUtils.sizeExpression(object.size, false));

			if (getSelectionConnection() != null)
				selector.setSelected(object.isSelectableSelected());

			GlideApp.with(getContext())
					.load(object.uri)
					.override(400)
					.centerCrop()
					.into(image);
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
					VideoHolder holder = new VideoHolder(cursor.getInt(idIndex),
							cursor.getString(titleIndex),
							cursor.getString(displayIndex),
							cursor.getString(albumIndex),
							cursor.getString(typeIndex),
							cursor.getLong(lengthIndex),
							cursor.getLong(dateIndex) * 1000,
							cursor.getLong(sizeIndex),
							Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

					lister.offer(holder);
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

	public static class VideoHolder extends GalleryGroupShareableListAdapter.GalleryGroupShareable
	{
		public long id;
		public String duration;

		public VideoHolder(String representativeText)
		{
			super(VIEW_TYPE_REPRESENTATIVE, representativeText);
		}

		public VideoHolder(int id, String friendlyName, String fileName, String albumName, String mimeType, long duration, long date, long size, Uri uri)
		{
			super(friendlyName, fileName, albumName, mimeType, date, size, uri);

			this.id = id;
			this.duration = TimeUtils.getDuration(duration);
		}
	}
}
