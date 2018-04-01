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
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.GalleryGroupShareableListAdapter;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */

public class ImageListAdapter
		extends GalleryGroupShareableListAdapter<ImageListAdapter.ImageHolder, GroupShareableListAdapter.ViewHolder>
		implements FastScrollRecyclerView.SectionedAdapter
{
	private ContentResolver mResolver;

	public ImageListAdapter(Context context)
	{
		super(context, MODE_GROUP_BY_ALBUM);
		mResolver = context.getContentResolver();
	}

	@Override
	protected void onLoad(GroupLister<ImageHolder> lister)
	{
		Cursor cursor = mResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				int idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
				int titleIndex = cursor.getColumnIndex(MediaStore.Images.Media.TITLE);
				int displayIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
				int albumIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
				int dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
				int sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
				int typeIndex = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);

				do {
					ImageHolder holder = new ImageHolder(
							cursor.getInt(idIndex),
							cursor.getString(titleIndex),
							cursor.getString(displayIndex),
							cursor.getString(albumIndex),
							cursor.getString(typeIndex),
							cursor.getLong(dateAddedIndex) * 1000,
							cursor.getLong(sizeIndex),
							Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

					holder.dateTakenString = String.valueOf(AppUtils.formatDateTime(getContext(), holder.date));

					lister.offer(holder);
				}
				while (cursor.moveToNext());
			}

			cursor.close();
		}
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return viewType == VIEW_TYPE_REPRESENTATIVE
				? new ViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text)
				: new ViewHolder(getInflater().inflate(isGridLayoutRequested() ? R.layout.list_image_grid : R.layout.list_image, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		final View parentView = holder.getView();
		final ImageHolder object = getItem(position);

		if (!holder.tryBinding(object)) {
			final View selector = parentView.findViewById(R.id.selector);
			ImageView image = parentView.findViewById(R.id.image);
			TextView text1 = parentView.findViewById(R.id.text);
			TextView text2 = parentView.findViewById(R.id.text2);

			text1.setText(object.friendlyName);
			text2.setText(object.dateTakenString);

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
	protected ImageHolder onGenerateRepresentative(String representativeText)
	{
		return new ImageHolder(representativeText);
	}

	@Override
	public boolean isGridSupported()
	{
		return true;
	}

	@NonNull
	@Override
	public String getSectionName(int position)
	{
		return getList().get(position).friendlyName;
	}

	public static class ImageHolder extends GalleryGroupShareableListAdapter.GalleryGroupShareable
	{
		public long id;
		public String dateTakenString;

		public ImageHolder(String representativeText)
		{
			super(VIEW_TYPE_REPRESENTATIVE, representativeText);
		}

		public ImageHolder(int id, String title, String fileName, String albumName, String mimeType, long date, long size, Uri uri)
		{
			super(title, fileName, albumName, mimeType, date, size, uri);
			this.id = id;
		}
	}
}
