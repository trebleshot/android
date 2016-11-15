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
    private Context mContext;
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
        this.mContext = context;
        this.mResolver = context.getContentResolver();
    }

    protected void onUpdate()
    {
        this.mPendingList.clear();

        Cursor cursor = this.mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        if (cursor.moveToFirst())
        {
            int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);

            do
            {
                MusicInfo info = new MusicInfo(cursor.getString(artistIndex), cursor.getString(songIndex), Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

                if (this.mSearchWord == null || (this.mSearchWord != null && (ApplicationHelper.searchWord(info.artist, this.mSearchWord) || ApplicationHelper.searchWord(info.song, this.mSearchWord))))
                    this.mPendingList.add(info);
            }
            while (cursor.moveToNext());

            Collections.sort(this.mPendingList, this.mComparator);
        }

        cursor.close();
    }

    @Override
    protected void onSearch(String word)
    {
        this.mSearchWord = word;
    }

    @Override
    public int getCount()
    {
        return this.mList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return this.mList.get(position);
    }

    @Override
    public long getItemId(int p1)
    {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup)
    {
        return this.getViewAt(LayoutInflater.from(mContext).inflate(R.layout.list_music, viewGroup, false), position);
    }

    public View getViewAt(View view, int position)
    {
        MusicInfo info = (MusicInfo) this.getItem(position);
        TextView text = (TextView) view.findViewById(R.id.text);
        TextView text2 = (TextView) view.findViewById(R.id.text2);

        text.setText(info.song);
        text2.setText(info.artist);

        return view;
    }

    @Override
    public void notifyDataSetChanged()
    {
        if (mPendingList.size() > 0)
        {
            this.mList.clear();
            this.mList.addAll(this.mPendingList);

            this.mPendingList.clear();
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
