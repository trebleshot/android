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

public class MusicListAdapter
		extends ShareableListAdapter<MusicListAdapter.SongHolder>
		implements SweetImageLoader.Handler<MusicListAdapter.AlbumHolder, Drawable>
{
	private Drawable mDefaultAlbumDrawable;
	private ContentResolver mResolver;

	public MusicListAdapter(Context context)
	{
		super(context);
		mResolver = context.getContentResolver();
		mDefaultAlbumDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_music_note_white_24dp);
	}

	@Override
	public ArrayList<SongHolder> onLoad()
	{
		ArrayList<SongHolder> list = new ArrayList<>();
		ArrayMap<Integer, AlbumHolder> albumList = new ArrayMap<>();
		Cursor songCursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				null,
				MediaStore.Audio.Media.IS_MUSIC + "=?",
				new String[]{String.valueOf(1)},
				null);

		Cursor albumCursor = mResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (albumCursor.moveToFirst()) {
			int idIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID);
			int artIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
			int titleIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);

			do {
				albumList.put(albumCursor.getInt(idIndex), new AlbumHolder(albumCursor.getInt(idIndex), albumCursor.getString(titleIndex), albumCursor.getString(artIndex)));
			} while (albumCursor.moveToNext());
		}

		albumCursor.close();

		if (songCursor.moveToFirst()) {
			int idIndex = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
			int artistIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			int songIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int albumIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
			int nameIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
			int dateIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);
			int sizeIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
			int typeIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);

			do {
				list.add(new SongHolder(songCursor.getString(nameIndex),
						songCursor.getString(artistIndex),
						songCursor.getString(songIndex),
						songCursor.getString(typeIndex),
						songCursor.getInt(albumIndex),
						albumList.get(songCursor.getInt(albumIndex)),
						songCursor.getLong(dateIndex),
						songCursor.getLong(sizeIndex),
						Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + songCursor.getInt(idIndex))));
			}
			while (songCursor.moveToNext());

			Collections.sort(list, getDefaultComparator());
		}

		songCursor.close();

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

	public ArrayList<SongHolder> getList()
	{
		return getItemList();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_music, parent, false);

		final SongHolder holder = (SongHolder) getItem(position);

		final View selector = convertView.findViewById(R.id.selector);
		final View layoutImage = convertView.findViewById(R.id.layout_image);
		ImageView image = convertView.findViewById(R.id.image);
		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);
		TextView text3 = convertView.findViewById(R.id.text3);

		if (getSelectionConnection() != null) {
			selector.setSelected(holder.isSelectableSelected());

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

		text1.setText(holder.song);
		text2.setText(holder.artist);
		text3.setText(holder.albumHolder.title);

		SweetImageLoader.load(this, getContext(), image, holder.albumHolder);

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
		public AlbumHolder albumHolder;

		public SongHolder(String displayName, String artist, String song, String mimeType, int albumId, AlbumHolder albumHolder, long date, long size, Uri uri)
		{
			super(artist + " - " + song, displayName, mimeType, date, size, uri);

			this.artist = artist;
			this.song = song;
			this.albumId = albumId;
			this.albumHolder = albumHolder == null ? new AlbumHolder(albumId, "-", null) : albumHolder;
		}

		@Override
		public String getComparableName()
		{
			return song;
		}

		@Override
		public boolean searchMatches(String searchWord)
		{
			return TextUtils.searchWord(artist, searchWord)
					|| TextUtils.searchWord(song, searchWord)
					|| TextUtils.searchWord(albumHolder.title, searchWord);
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
