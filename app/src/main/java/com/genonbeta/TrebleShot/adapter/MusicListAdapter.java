package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.RecyclerViewFragment;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.SweetImageLoader;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.util.ArrayList;
import java.util.Collections;

public class MusicListAdapter
		extends ShareableListAdapter<MusicListAdapter.SongHolder, RecyclerViewAdapter.ViewHolder>
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

		if (albumCursor != null) {
			if (albumCursor.moveToFirst()) {
				int idIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID);
				int artIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
				int titleIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);

				do {
					albumList.put(albumCursor.getInt(idIndex), new AlbumHolder(albumCursor.getInt(idIndex), albumCursor.getString(titleIndex), albumCursor.getString(artIndex)));
				} while (albumCursor.moveToNext());
			}

			albumCursor.close();
		}

		if (songCursor != null) {
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
		}

		return list;
	}

	@Override
	public Drawable onLoadBitmap(AlbumHolder object)
	{
		Drawable loadedCover = object == null ? null : Drawable.createFromPath(object.art);
		return loadedCover == null ? mDefaultAlbumDrawable : loadedCover;
	}

	@NonNull
	@Override
	public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return new RecyclerViewAdapter.ViewHolder(getInflater().inflate(R.layout.list_music, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position)
	{
		final SongHolder object = getItem(position);
		final View parentView = holder.getView();

		final View selector = parentView.findViewById(R.id.selector);
		final View layoutImage = parentView.findViewById(R.id.layout_image);
		ImageView image = parentView.findViewById(R.id.image);
		TextView text1 = parentView.findViewById(R.id.text);
		TextView text2 = parentView.findViewById(R.id.text2);
		TextView text3 = parentView.findViewById(R.id.text3);

		if (getSelectionConnection() != null) {
			selector.setSelected(object.isSelectableSelected());

			layoutImage.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getSelectionConnection().setSelected(object);
					selector.setSelected(object.isSelectableSelected());
				}
			});
		}

		text1.setText(object.song);
		text2.setText(object.artist);
		text3.setText(object.albumHolder.title);

		SweetImageLoader.load(this, getContext(), image, object.albumHolder);
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
