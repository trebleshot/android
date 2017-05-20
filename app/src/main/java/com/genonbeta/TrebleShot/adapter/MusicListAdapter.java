package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicListAdapter extends AbstractEditableListAdapter
{
	private ContentResolver mResolver;
	private String mSearchWord;
	private ArrayList<MusicInfo> mList = new ArrayList<MusicInfo>();
	private ArrayList<MusicInfo> mPendingList = new ArrayList<MusicInfo>();
	private Comparator<MusicInfo> mComparator = new Comparator<MusicInfo>()
	{
		@Override
		public int compare(MusicListAdapter.MusicInfo compareFrom, MusicListAdapter.MusicInfo compareTo)
		{
			return compareFrom.song.toLowerCase().compareTo(compareTo.song.toLowerCase());
		}
	};

	public MusicListAdapter(Context context)
	{
		super(context);
		mResolver = context.getContentResolver();
	}

	protected void onUpdate()
	{
		mPendingList.clear();

		Cursor cursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (cursor.moveToFirst())
		{
			int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
			int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			int songIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);

			do
			{
				MusicInfo info = new MusicInfo(cursor.getString(artistIndex), cursor.getString(songIndex), Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

				if (mSearchWord == null || (mSearchWord != null && (ApplicationHelper.searchWord(info.artist, mSearchWord) || ApplicationHelper.searchWord(info.song, mSearchWord))))
					mPendingList.add(info);
			}
			while (cursor.moveToNext());

			Collections.sort(mPendingList, mComparator);
		}

		cursor.close();
	}

	@Override
	protected void onSearch(String word)
	{
		mSearchWord = word;
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

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_music, parent, false);

		MusicInfo info = (MusicInfo) getItem(position);
		TextView text = (TextView) convertView.findViewById(R.id.text);
		TextView text2 = (TextView) convertView.findViewById(R.id.text2);

		text.setText(info.song);
		text2.setText(info.artist);

		return convertView;
	}

	@Override
	public void notifyDataSetChanged()
	{
		if (mPendingList.size() > 0)
		{
			mList.clear();
			mList.addAll(this.mPendingList);

			mPendingList.clear();
		}

		super.notifyDataSetChanged();
	}


	public static class MusicInfo
	{
		public String artist;
		public String song;
		public Uri uri;

		public MusicInfo(String artist, String song, Uri uri)
		{
			this.artist = artist;
			this.song = song;
			this.uri = uri;
		}
	}
}
