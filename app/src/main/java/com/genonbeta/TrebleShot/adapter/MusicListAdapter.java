package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.SweetImageLoader;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicListAdapter extends ShareableListAdapter<MusicListAdapter.SongHolder> implements SweetImageLoader.Handler<MusicListAdapter.AlbumHolder, Drawable>
{
	private Drawable mDefaultAlbumDrawable;
	private ContentResolver mResolver;
	private ArrayList<SongHolder> mList = new ArrayList<>();
	private ArrayMap<Integer, AlbumHolder> mAlbumList = new ArrayMap<>();
	private ArrayList<AlbumHolder> mTmpAlbumList = new ArrayList<>();
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
		Cursor songCursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				null,
				MediaStore.Audio.Media.IS_MUSIC + "=?",
				new String[]{String.valueOf(1)},
				null);

		mTmpAlbumList.clear();

		Cursor albumCursor = mResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (albumCursor.moveToFirst()) {
			int idIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID);
			int artIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
			int titleIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);

			do {
				mTmpAlbumList.add(new AlbumHolder(albumCursor.getInt(idIndex), albumCursor.getString(titleIndex), albumCursor.getString(artIndex)));
			} while (albumCursor.moveToNext());
		}

		albumCursor.close();

		if (songCursor.moveToFirst()) {
			int idIndex = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
			int artistIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			int songIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int albumIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
			int nameIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);

			do {
				list.add(new SongHolder(songCursor.getString(nameIndex), songCursor.getString(artistIndex), songCursor.getString(songIndex), songCursor.getInt(albumIndex),
						Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + songCursor.getInt(idIndex))));
			}
			while (songCursor.moveToNext());

			Collections.sort(list, mComparator);
		}

		songCursor.close();

		return list;
	}

	@Override
	public void onUpdate(ArrayList<SongHolder> passedItem)
	{
		mList.clear();
		mAlbumList.clear();

		mList.addAll(passedItem);

		for (AlbumHolder albumHolder : mTmpAlbumList)
			mAlbumList.put(albumHolder.id, albumHolder);
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

		SongHolder songHolder = (SongHolder) getItem(position);
		AlbumHolder albumHolder = mAlbumList.get(songHolder.albumId);

		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);
		TextView text3 = convertView.findViewById(R.id.text3);
		ImageView image = convertView.findViewById(R.id.image);

		text1.setText(songHolder.song);
		text2.setText(songHolder.artist);
		text3.setText(albumHolder == null ? null : albumHolder.title);

		text3.setVisibility(albumHolder == null ? View.GONE : View.VISIBLE);

		SweetImageLoader.load(this, getContext(), image, albumHolder);

		return convertView;
	}

	@Override
	public Drawable onLoadBitmap(AlbumHolder object)
	{
		Drawable loadedCover = object == null ? null : Drawable.createFromPath(object.art);
		return loadedCover == null ? mDefaultAlbumDrawable : loadedCover;
	}

	public static class SongHolder extends Shareable
	{
		public String artist;
		public String song;
		public int albumId;

		public SongHolder(String displayName, String artist, String song, int albumId, Uri uri)
		{
			super(artist + " - " + song, displayName, uri);

			this.artist = artist;
			this.song = song;
			this.albumId = albumId;
		}

		@Override
		public boolean searchMatches(String searchWord)
		{
			return TextUtils.searchWord(artist, searchWord)
					|| TextUtils.searchWord(song, searchWord);
		}
	}

	public static class AlbumHolder
	{
		public int id;
		public String art;
		public String title;

		public AlbumHolder(int id, String title, String art)
		{
			this.id = id;
			this.art = art;
			this.title = title;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof AlbumHolder ? ((AlbumHolder) obj).id == id : super.equals(obj);
		}
	}
}
