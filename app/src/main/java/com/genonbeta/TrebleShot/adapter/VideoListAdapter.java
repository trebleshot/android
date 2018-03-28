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
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.util.ArrayList;
import java.util.Collections;

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */

public class VideoListAdapter
		extends ShareableListAdapter<VideoListAdapter.VideoHolder, RecyclerViewAdapter.ViewHolder>
{
	private ContentResolver mResolver;

	public VideoListAdapter(Context context)
	{
		super(context);
		mResolver = context.getContentResolver();
	}

	@Override
	public ArrayList<VideoHolder> onLoad()
	{
		ArrayList<VideoHolder> list = new ArrayList<>();
		Cursor cursor = mResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (cursor.moveToFirst()) {
			int idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
			int titleIndex = cursor.getColumnIndex(MediaStore.Video.Media.TITLE);
			int displayIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
			int lengthIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
			int dateIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED);
			int sizeIndex = cursor.getColumnIndex(MediaStore.Video.Media.SIZE);
			int typeIndex = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);

			do {
				VideoHolder info = new VideoHolder(cursor.getInt(idIndex),
						cursor.getString(titleIndex),
						cursor.getString(displayIndex),
						cursor.getString(typeIndex),
						cursor.getLong(lengthIndex),
						cursor.getLong(dateIndex),
						cursor.getLong(sizeIndex),
						Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

				list.add(info);
			}
			while (cursor.moveToNext());

			Collections.sort(list, getDefaultComparator());
		}

		cursor.close();

		return list;
	}

	@NonNull
	@Override
	public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return new RecyclerViewAdapter.ViewHolder(getInflater()
				.inflate(isGridLayoutRequested() ? R.layout.list_video_grid : R.layout.list_video, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position)
	{
		final VideoHolder object = this.getItem(position);
		final View parentView = holder.getView();

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

	@Override
	public boolean isGridSupported()
	{
		return true;
	}

	public static class VideoHolder extends Shareable
	{
		public long id;
		public String duration;

		public VideoHolder(int id, String friendlyName, String fileName, String mimeType, long duration, long date, long size, Uri uri)
		{
			super(friendlyName, fileName, mimeType, date, size, uri);

			this.id = id;
			this.duration = TimeUtils.getDuration(duration);
		}
	}
}
