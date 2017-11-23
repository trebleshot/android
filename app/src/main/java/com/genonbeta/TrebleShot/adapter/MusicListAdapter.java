package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.Shareable;
import com.genonbeta.TrebleShot.util.SweetImageLoader;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicListAdapter extends ShareableListAdapter<MusicListAdapter.SongHolder> implements SweetImageLoader.Handler<MusicListAdapter.SongHolder, Drawable>
{
	private Drawable mDefaultAlbumDrawable;
	private ContentResolver mResolver;
	private ArrayList<SongHolder> mList = new ArrayList<>();
	private Comparator<SongHolder> mComparator = new Comparator<SongHolder>()
	{
		@Override
		public int compare(SongHolder compareFrom, SongHolder compareTo)
		{
			return compareFrom.song.toLowerCase().compareTo(compareTo.song.toLowerCase());
		}
	};

	public MusicListAdapter(Context context)
	{
		super(context);
		mResolver = context.getContentResolver();
		mDefaultAlbumDrawable = ContextCompat.getDrawable(getContext(), R.drawable.default_album_art);
	}

	@Override
	public ArrayList<SongHolder> onLoad()
	{
		ArrayList<SongHolder> list = new ArrayList<>();
		Cursor cursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				null,
				MediaStore.Audio.Media.IS_MUSIC + "=?",
				new String[]{String.valueOf(1)},
				null);

		if (cursor.moveToFirst()) {
			int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
			int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			int songIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
			int nameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);

			do {
				SongHolder info = new SongHolder(cursor.getString(nameIndex), cursor.getString(artistIndex), cursor.getString(songIndex),
						Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

				Cursor coverCursor = mResolver.query(Uri.parse(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(albumIndex)), null, null, null, null);

				if (coverCursor.moveToFirst()) {
					int coverIndex = coverCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
					info.cover = coverCursor.getString(coverIndex);
				}

				coverCursor.close();
				list.add(info);
			}
			while (cursor.moveToNext());

			Collections.sort(list, mComparator);
		}

		cursor.close();

		return list;
	}

	@Override
	public void onUpdate(ArrayList<SongHolder> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mList.get(position);
	}

	@Override
	public long getItemId(int p1)
	{
		return 0;
	}

	public ArrayList<SongHolder> getList()
	{
		return mList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_music, parent, false);

		SongHolder info = (SongHolder) getItem(position);
		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);
		ImageView image = convertView.findViewById(R.id.image);

		text1.setText(info.song);
		text2.setText(info.artist);

		SweetImageLoader.load(this, getContext(), image, info);

		return convertView;
	}

	@Override
	public Drawable onLoadBitmap(SongHolder object)
	{
		Drawable loadedCover = Drawable.createFromPath(object.cover);
		return loadedCover == null ? mDefaultAlbumDrawable : loadedCover;
	}

	public static class SongHolder extends Shareable
	{
		public String artist;
		public String cover;
		public String song;

		public SongHolder(String displayName, String artist, String song, Uri uri)
		{
			super(displayName, displayName, uri);

			this.artist = artist;
			this.song = song;
		}

		@Override
		public boolean searchMatches(String searchWord)
		{
			return TextUtils.searchWord(artist, searchWord) || TextUtils.searchWord(song, searchWord);
		}
	}
}
