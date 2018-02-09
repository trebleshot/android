package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.SweetImageLoader;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.util.ArrayList;
import java.util.Collections;

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */

public class ImageListAdapter
		extends ShareableListAdapter<ImageListAdapter.ImageHolder>
		implements SweetImageLoader.Handler<ImageListAdapter.ImageHolder, Bitmap>
{
	private ContentResolver mResolver;
	private Bitmap mDefaultImageBitmap;

	public ImageListAdapter(Context context)
	{
		super(context);

		mResolver = context.getContentResolver();
		mDefaultImageBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_refresh_white_24dp);
	}

	@Override
	public Bitmap onLoadBitmap(ImageHolder object)
	{
		Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(mResolver, object.id, MediaStore.Images.Thumbnails.MINI_KIND, null);
		return bitmap == null ? mDefaultImageBitmap : bitmap;
	}

	@Override
	public ArrayList<ImageHolder> onLoad()
	{
		ArrayList<ImageHolder> list = new ArrayList<>();
		Cursor cursor = mResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (cursor.moveToFirst()) {
			int idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
			int displayIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
			int dateAddedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
			int sizeIndex = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);

			do {
				ImageHolder holder = new ImageHolder(
						cursor.getInt(idIndex),
						cursor.getString(displayIndex),
						cursor.getLong(dateAddedIndex) * 1000,
						cursor.getLong(sizeIndex),
						Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

				holder.dateTakenString = String.valueOf(AppUtils.formatDateTime(getContext(), holder.date));

				list.add(holder);
			}
			while (cursor.moveToNext());

			Collections.sort(list, getDefaultComparator());
		}

		cursor.close();

		return list;
	}

	@Override
	public int getCount()
	{
		return getItemList().size();
	}

	@Override
	public Object getItem(int position)
	{
		return getItemList().get(position);
	}

	@Override
	public long getItemId(int p1)
	{
		return 0;
	}

	public ArrayList<ImageHolder> getList()
	{
		return getItemList();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_image, parent, false);

		final ImageHolder holder = (ImageHolder) this.getItem(position);

		final View selector = convertView.findViewById(R.id.selector);
		final View layoutImage = convertView.findViewById(R.id.layout_image);
		ImageView image = convertView.findViewById(R.id.image);
		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);

		text1.setText(holder.friendlyName);
		text2.setText(holder.dateTakenString);

		if (getSelectionConnection() != null) {
			selector.setSelected(getSelectionConnection().isSelected(holder));

			layoutImage.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getSelectionConnection().setSelected(holder);
					selector.setSelected(holder.isSelectableSelected());
				}
			});
		}

		SweetImageLoader.load(this, getContext(), image, holder);

		return convertView;
	}

	public static class ImageHolder extends Shareable
	{
		public long id;
		public String dateTakenString;

		public ImageHolder(int id, String filename, long date, long size, Uri uri)
		{
			super(filename, filename, date, size, uri);

			this.id = id;
		}
	}
}
